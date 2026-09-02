package com.zest.products.service;

import com.zest.products.entity.RefreshToken;
import com.zest.products.entity.User;

public interface RefreshTokenService {

    String createRefreshToken(User user);

    RefreshToken validateRefreshToken(String rawToken);

    String rotateRefreshToken(String oldRawToken);

    void revokeAllTokensForUser(User user);
}