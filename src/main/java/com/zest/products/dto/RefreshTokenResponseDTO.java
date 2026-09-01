package com.zest.products.dto;

public class RefreshTokenResponseDTO {

    private String accessToken;
    private String refreshToken;

    public RefreshTokenResponseDTO(String newAccessToken, String refreshToken) {
        this.accessToken = newAccessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
