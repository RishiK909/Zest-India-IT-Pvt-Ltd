package com.zest.products.service;


import com.zest.products.dto.ApiResponse;
import com.zest.products.dto.AuthRegisterDto;
import com.zest.products.dto.AuthResponseDto;
import com.zest.products.dto.LoginRequestDTO;
import com.zest.products.entity.Users;
import com.zest.products.enums.Role;
import com.zest.products.repository.AuthRepository;
import com.zest.products.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService{

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(AuthRepository authRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Registers a new user after validating that the email and phone number
     * are unique. The user's password is securely encoded before being saved.
     *
     * @param request the registration request containing the user's details
     * @return an {@code ApiResponse} indicating whether the registration was
     *         successful or the reason for failure
     */
    @Override
    public ApiResponse<Void> register(AuthRegisterDto request) {

        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            return new ApiResponse<>("Email already exists", false);
        }

        if (authRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            return new ApiResponse<>("Phone number already exists", false);
        }

        if (request.getRole() == Role.Admin) {
            return new ApiResponse<>("You cannot register as ADMIN", false);
        }

        Users user = new Users();
        user.setUserName(request.getUserName());
        user.setEmail(request.getEmail());
        /**
         * Encode the raw password. Generally, a good encoding algorithm applies a
         * SHA-1 or greater hash combined with an 8-byte or greater randomly generated salt.
         */
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setRole(request.getRole());

        authRepository.save(user);

        return new ApiResponse<>("User registered successfully", true);
    }



    /**
     * Authenticates a user using the provided login credentials and generates
     * a JWT token upon successful authentication.
     *
     * @param request the login request containing the user's email and password
     * @return an {@code ApiResponse} containing the authentication result and
     *         JWT token if the login is successful
     */
    @Override
    public ApiResponse<AuthResponseDto> login(LoginRequestDTO request) {

        Optional<Users> userOptional = authRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            return new ApiResponse<>("Invalid email or password", false);
        }

        Users user = userOptional.get();

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches) {
            return new ApiResponse<>("Invalid email or password", false);
        }

        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = refreshTokenService.createRefreshToken(user);

        AuthResponseDto authData = new AuthResponseDto(
                token,
                refreshToken,
                jwtUtil.extractExpiration(token),
                user.getUserId(),
                user.getUserName(),
                user.getRole().name()
        );

        return new ApiResponse<>("Login successful", true, authData);
    }



    /**
     * Retrieves the details of the currently authenticated user based on
     * the provided email address.
     *
     * @param email the email address of the authenticated user
     * @return an {@code ApiResponse} containing the current user's details
     *         if found, or an appropriate error message otherwise
     */
    @Override
    public ApiResponse<AuthResponseDto> getCurrentUser(String email) {

        Optional<Users> userOptional = authRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return new ApiResponse<>("User not found", false);
        }

        Users user = userOptional.get();

        AuthResponseDto data = new AuthResponseDto(
                null,
                null,
                user.getUserId(),
                user.getUserName(),
                user.getRole().name()
        );

        return new ApiResponse<>("Current user fetched successfully", true, data);
    }
}

