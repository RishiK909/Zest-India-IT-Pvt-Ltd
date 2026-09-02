package com.zest.products.service;

import com.zest.products.dto.ApiResponse;
import com.zest.products.dto.AuthRegisterDto;
import com.zest.products.dto.AuthResponseDto;
import com.zest.products.dto.LoginRequestDTO;

public interface AuthService {

    ApiResponse<Void> register(AuthRegisterDto request);

    ApiResponse<AuthResponseDto> login(LoginRequestDTO request);

    ApiResponse<AuthResponseDto> getCurrentUser(String email);

}
