package com.zest.products.service;

import com.zest.products.entity.RefreshToken;
import com.zest.products.entity.Users;

public interface RefreshTokenService {

    String createRefreshToken(Users user);

    RefreshToken validateRefreshToken(String rawToken);

    String rotateRefreshToken(String oldRawToken);

    void revokeAllTokensForUser(Users user);
}