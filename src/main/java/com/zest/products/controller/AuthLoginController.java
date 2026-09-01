package com.zest.products.controller;

import com.zest.products.dto.*;
import com.zest.products.entity.RefreshToken;
import com.zest.products.entity.Users;
import com.zest.products.exception.InvalidRefreshTokenException;
import com.zest.products.repository.AuthRepository;
import com.zest.products.security.JwtUtil;
import com.zest.products.service.AuthDetailServiceImpl;
import com.zest.products.service.AuthService;
import com.zest.products.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "APIs for user registration, login, token refresh and logout")
public class AuthLoginController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final AuthDetailServiceImpl authDetailService;
    private final JwtUtil jwtUtil;
    private final AuthRepository authRepository;

    public AuthLoginController(AuthService authService, RefreshTokenService refreshTokenService, AuthDetailServiceImpl authDetailService, JwtUtil jwtUtil, AuthRepository authRepository) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.authDetailService = authDetailService;
        this.jwtUtil = jwtUtil;
        this.authRepository = authRepository;
    }

    @Operation(summary = "Register a new user",
            description = "Creates a new user account with the provided registration details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = com.zest.products.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid registration details or user already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<com.zest.products.dto.ApiResponse<Void>> register(@Valid @RequestBody AuthRegisterDto request) {
        com.zest.products.dto.ApiResponse<Void> response = authService.register(request);
        return response.isStatus()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @Operation(summary = "Login a user",
            description = "Authenticates a user using email and password, and returns an access token and refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<com.zest.products.dto.ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDTO request) {
        com.zest.products.dto.ApiResponse<AuthResponseDto> response = authService.login(request);
        return response.isStatus()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @Operation(summary = "Fetch current authenticated user",
            description = "Returns the details of the currently authenticated user based on the security context.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current user fetched successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Unable to resolve current user")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/current-user")
    public ResponseEntity<com.zest.products.dto.ApiResponse<AuthResponseDto>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        com.zest.products.dto.ApiResponse<AuthResponseDto> response = authService.getCurrentUser(email);
        return response.isStatus()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @Operation(summary = "Refresh access token",
            description = "Rotates the given refresh token and issues a new access token along with a new refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequestDTO request) {

        String newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());

        RefreshToken validated = refreshTokenService.validateRefreshToken(newRefreshToken);
        Users user = validated.getUser();

        UserDetails userDetails = authDetailService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        return ResponseEntity.ok(
                new RefreshTokenResponseDTO(newAccessToken, newRefreshToken)
        );
    }

    @Operation(summary = "Logout from all devices",
            description = "Revokes all active refresh tokens for the authenticated user, logging them out from all devices.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {

        Users user = authRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found"));

        refreshTokenService.revokeAllTokensForUser(user);

        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }
}