package com.zongce.system.repository;

import com.zongce.system.entity.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByToken(String token);

    List<AuthRefreshToken> findByUsernameAndRevokedFalse(String username);
}
