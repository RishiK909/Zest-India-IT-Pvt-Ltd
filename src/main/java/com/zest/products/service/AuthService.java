package com.zest.products.service;

import com.zest.products.dto.*;

public interface AuthService {

    ApiResponseDTO<Void> register(AuthRegisterDTO request);

    ApiResponseDTO<AuthResponseDTO> login(LoginRequestDTO request);

    ApiResponseDTO<AuthResponseDTO> getCurrentUser(String email);

}
