package com.zest.products.service;

import com.zest.products.entity.RefreshToken;
import com.zest.products.entity.Users;
import com.zest.products.exception.InvalidRefreshTokenException;
import com.zest.products.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final long EXPIRY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public String createRefreshToken(Users user) {
        String rawToken = generateSecureToken();
        persistToken(rawToken, user);
        return rawToken;
    }

    @Override
    public RefreshToken validateRefreshToken(String rawToken) {
        String hash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            revokeAllTokensForUser(refreshToken.getUser());
            throw new InvalidRefreshTokenException("Refresh token revoked. All sessions invalidated for security.");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        return refreshToken;
    }

    @Override
    public String rotateRefreshToken(String oldRawToken) {
        RefreshToken oldToken = validateRefreshToken(oldRawToken);

        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        String newRawToken = generateSecureToken();
        persistToken(newRawToken, oldToken.getUser());
        return newRawToken;
    }

    @Override
    @Transactional
    public void revokeAllTokensForUser(Users user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    private void persistToken(String rawToken, Users user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plus(EXPIRY_DAYS, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}