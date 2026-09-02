package com.zest.products.service;


import com.zest.products.dto.ApiResponseDTO;
import com.zest.products.dto.AuthRegisterDTO;
import com.zest.products.dto.AuthResponseDTO;
import com.zest.products.dto.LoginRequestDTO;
import com.zest.products.entity.User;
import com.zest.products.enums.Role;
import com.zest.products.repository.UserRepository;
import com.zest.products.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
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
    public ApiResponseDTO<Void> register(AuthRegisterDTO request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return new ApiResponseDTO<>("Email already exists", false);
        }

        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            return new ApiResponseDTO<>("Phone number already exists", false);
        }

        /*if (request.getRole() == Role.Admin) {
            return new ApiResponseDTO<>("You cannot register as ADMIN", false);
        }*/

        User user = new User();
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

        userRepository.save(user);

        return new ApiResponseDTO<>("User registered successfully", true);
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
    public ApiResponseDTO<AuthResponseDTO> login(LoginRequestDTO request) {

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            return new ApiResponseDTO<>("Invalid email or password", false);
        }

        User user = userOptional.get();

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches) {
            return new ApiResponseDTO<>("Invalid email or password", false);
        }

        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = refreshTokenService.createRefreshToken(user);

        AuthResponseDTO authData = new AuthResponseDTO(
                token,
                refreshToken,
                jwtUtil.extractExpiration(token),
                user.getUserId(),
                user.getUserName(),
                user.getRole().name()
        );

        return new ApiResponseDTO<>("Login successful", true, authData);
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
    public ApiResponseDTO<AuthResponseDTO> getCurrentUser(String email) {

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return new ApiResponseDTO<>("User not found", false);
        }

        User user = userOptional.get();

        AuthResponseDTO data = new AuthResponseDTO(
                null,
                null,
                user.getUserId(),
                user.getUserName(),
                user.getRole().name()
        );

        return new ApiResponseDTO<>("Current user fetched successfully", true, data);
    }
}

