package com.zest.products.controller;

import com.zest.products.dto.*;
import com.zest.products.entity.RefreshToken;
import com.zest.products.entity.User;
import com.zest.products.exception.InvalidRefreshTokenException;
import com.zest.products.repository.UserRepository;
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
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final AuthDetailServiceImpl authDetailService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, AuthDetailServiceImpl authDetailService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.authDetailService = authDetailService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // ------------------------------- Register User -------------------------------------------------------------------

    @Operation(summary = "Register a new user",
            description = "Creates a new user account with the provided registration details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid registration details or user already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<Void>> register(@Valid @RequestBody AuthRegisterDTO request) {
        ApiResponseDTO<Void> response = authService.register(request);
        return response.isStatus()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }


    // ------------------------------------------ Login User -----------------------------------------------------------
    @Operation(summary = "Login a user",
            description = "Authenticates a user using email and password, and returns an access token and refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        ApiResponseDTO<AuthResponseDTO> response = authService.login(request);
        return response.isStatus()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }


    // --------------------------------------------- Fetch Current User ------------------------------------------------

    @Operation(summary = "Fetch current authenticated user",
            description = "Returns the details of the currently authenticated user based on the security context.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current user fetched successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Unable to resolve current user")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/current-user")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        ApiResponseDTO<AuthResponseDTO> response = authService.getCurrentUser(email);
        return response.isStatus()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }



    // ------------------------------------ Refresh access token -------------------------------------------------------

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
        User user = validated.getUser();

        UserDetails userDetails = authDetailService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        return ResponseEntity.ok(
                new RefreshTokenResponseDTO(newAccessToken, newRefreshToken)
        );
    }


    // ------------------------------------------- Logout --------------------------------------------------------------
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

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found"));

        refreshTokenService.revokeAllTokensForUser(user);

        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }
}