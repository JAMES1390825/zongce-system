package com.zongce.system.service.impl;

import com.zongce.system.entity.AuthRefreshToken;
import com.zongce.system.repository.AuthRefreshTokenRepository;
import com.zongce.system.service.AuthTokenService;
import com.zongce.system.service.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AuthTokenServiceImpl implements AuthTokenService {

    private final AuthRefreshTokenRepository authRefreshTokenRepository;
    private final JwtService jwtService;

    public AuthTokenServiceImpl(AuthRefreshTokenRepository authRefreshTokenRepository,
                                JwtService jwtService) {
        this.authRefreshTokenRepository = authRefreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthRefreshToken issueRefreshToken(String username, String userAgent, String clientIp) {
        String token = jwtService.generateRefreshToken(username);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(jwtService.extractExpiration(token), ZoneId.systemDefault());

        AuthRefreshToken row = new AuthRefreshToken();
        row.setToken(token);
        row.setUsername(username);
        row.setExpiresAt(expiresAt);
        row.setRevoked(false);
        row.setUserAgent(shorten(userAgent, 255));
        row.setClientIp(shorten(clientIp, 64));
        return authRefreshTokenRepository.save(row);
    }

    @Override
    @Transactional
    public AuthRefreshToken validateRefreshToken(String refreshToken) {
        AuthRefreshToken row = authRefreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("刷新令牌不存在或已失效"));

        if (Boolean.TRUE.equals(row.getRevoked())) {
            throw new IllegalArgumentException("刷新令牌已失效，请重新登录");
        }
        if (row.getExpiresAt() != null && row.getExpiresAt().isBefore(LocalDateTime.now())) {
            row.setRevoked(true);
            authRefreshTokenRepository.save(row);
            throw new IllegalArgumentException("刷新令牌已过期，请重新登录");
        }
        if (!jwtService.isRefreshToken(refreshToken)) {
            row.setRevoked(true);
            authRefreshTokenRepository.save(row);
            throw new IllegalArgumentException("刷新令牌类型错误");
        }
        String tokenUsername = jwtService.extractUsername(refreshToken);
        if (tokenUsername == null || !tokenUsername.equals(row.getUsername())) {
            row.setRevoked(true);
            authRefreshTokenRepository.save(row);
            throw new IllegalArgumentException("刷新令牌与用户不匹配");
        }
        return row;
    }

    @Override
    @Transactional
    public void revokeToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        authRefreshTokenRepository.findByToken(refreshToken).ifPresent(row -> {
            row.setRevoked(true);
            authRefreshTokenRepository.save(row);
        });
    }

    @Override
    @Transactional
    public void revokeAllByUsername(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        List<AuthRefreshToken> tokens = authRefreshTokenRepository.findByUsernameAndRevokedFalse(username);
        for (AuthRefreshToken token : tokens) {
            token.setRevoked(true);
        }
        authRefreshTokenRepository.saveAll(tokens);
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
