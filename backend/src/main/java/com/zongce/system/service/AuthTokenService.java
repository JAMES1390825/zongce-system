package com.zongce.system.service;

import com.zongce.system.entity.AuthRefreshToken;

public interface AuthTokenService {

    AuthRefreshToken issueRefreshToken(String username, String userAgent, String clientIp);

    AuthRefreshToken validateRefreshToken(String refreshToken);

    void revokeToken(String refreshToken);

    void revokeAllByUsername(String username);
}
