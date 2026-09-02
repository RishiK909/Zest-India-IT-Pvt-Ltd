package com.zest.products.service;

import com.zest.products.entity.RefreshToken;
import com.zest.products.entity.User;
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

/**
 * Handles creation, validation, and rotation of refresh tokens.
 * Raw token are not stored in database only SHA-256 has is stored.
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final long EXPIRY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token for the given user and saves its hash in the database.
     *
     * @param user the user for whom the token is created
     * @return the raw (plain) token
     */
    @Override
    public String createRefreshToken(User user) {
        String rawToken = generateSecureToken();
        persistToken(rawToken, user);
        return rawToken;
    }

    /**
     * Checks if a raw refresh token is valid.
     * Throws an error if the token does not exist, is expired, or was already
     * revoked. If a revoked token is reused, all tokens for that user are revoked
     * as well, since this usually means the token was stolen.
     *
     * @param rawToken the raw token received from the client
     * @return the matching RefreshToken entity if valid
     */
    @Override
    public RefreshToken validateRefreshToken(String rawToken) {
        String hash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            // Reuse of a revoked token = possible theft. Kill all sessions
            revokeAllTokensForUser(refreshToken.getUser());
            throw new InvalidRefreshTokenException("Refresh token revoked. All sessions invalidated for security.");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        return refreshToken;
    }

    /**
     * Rotates a refresh token: validates the old one, revokes it, and issues a new one.
     * This way, a token can only be used once — reusing it triggers theft detection.
     *
     * @param oldRawToken the current raw refresh token
     * @return a new raw refresh token to replace the old one
     */
    @Override
    public String rotateRefreshToken(String oldRawToken) {
        RefreshToken oldToken = validateRefreshToken(oldRawToken);

        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        String newRawToken = generateSecureToken();
        persistToken(newRawToken, oldToken.getUser());
        return newRawToken;
    }

    /**
     * Revokes (deletes) all refresh tokens belonging to a user.
     * Used during logout, or when token theft is suspected.
     *
     * @param user the user whose tokens should be revoked
     */
    @Override
    @Transactional
    public void revokeAllTokensForUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    /**
     * Builds a RefreshToken entity and saves it to the database.
     */
    private void persistToken(String rawToken, User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plus(EXPIRY_DAYS, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Generates a random, secure token using SecureRandom.
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hashes a token using SHA-256.
     */
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