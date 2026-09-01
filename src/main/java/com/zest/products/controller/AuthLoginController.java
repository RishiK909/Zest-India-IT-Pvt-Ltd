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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/auth")
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


    /**
     * Registers a new user with the provided registration details.
     *
     * @param request the registration request containing the user's information
     * @return a {@code ResponseEntity} containing the registration result wrapped
     *         in an {@code ApiResponse<Void>}
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody AuthRegisterDto request) {
        ApiResponse<Void> response = authService.register(request);
        return response.isStatus()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Authenticates a user using the provided login credentials.
     *
     * @param request the login request containing the user's credentials
     * @return a {@code ResponseEntity} containing the authentication result wrapped
     *         in an {@code ApiResponse<AuthResponseDTO>}
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDTO request) {
        ApiResponse<AuthResponseDto> response = authService.login(request);
        return response.isStatus()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Validates and returns the currently authenticated user.
     *
     * @param authentication the authenticated user's security context
     * @return the current user's details wrapped in an ApiResponse
     */
    @GetMapping("/current-user")
    public ResponseEntity<ApiResponse<AuthResponseDto>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        ApiResponse<AuthResponseDto> response = authService.getCurrentUser(email);
        return response.isStatus()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }


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

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {

        Users user = authRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found"));

        refreshTokenService.revokeAllTokensForUser(user);

        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }

}