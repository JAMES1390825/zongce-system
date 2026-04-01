package com.zongce.system.api.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.AuthRefreshToken;
import com.zongce.system.repository.UserRepository;
import com.zongce.system.service.AuthTokenService;
import com.zongce.system.service.CurrentUserService;
import com.zongce.system.service.JwtService;
import com.zongce.system.service.LoginLogService;
import com.zongce.system.util.RequestClientUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthTokenService authTokenService;
    private final LoginLogService loginLogService;

    public AuthApiController(AuthenticationManager authenticationManager,
                             UserRepository userRepository,
                             CurrentUserService currentUserService,
                             PasswordEncoder passwordEncoder,
                             JwtService jwtService,
                             AuthTokenService authTokenService,
                             LoginLogService loginLogService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authTokenService = authTokenService;
        this.loginLogService = loginLogService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request,
                                     HttpServletRequest httpRequest) {
        String username = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password();
        if (username.isBlank() || password.isBlank()) {
            loginLogService.record(username, false, "用户名和密码不能为空", httpRequest);
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (Exception ex) {
            loginLogService.record(username, false, "账号或密码错误", httpRequest);
            throw ex;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        String accessToken = jwtService.generateAccessToken(user);
        AuthRefreshToken refreshToken = authTokenService.issueRefreshToken(
                user.getUsername(),
                RequestClientUtils.getUserAgent(httpRequest),
                RequestClientUtils.getClientIp(httpRequest)
        );

        loginLogService.record(username, true, "登录成功", httpRequest);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "登录成功");
        body.put("user", toUserView(user));
        body.put("tokenType", "Bearer");
        body.put("accessToken", accessToken);
        body.put("refreshToken", refreshToken.getToken());
        body.put("accessTokenExpiresAt", toDateTime(jwtService.extractExpiration(accessToken)));
        body.put("refreshTokenExpiresAt", refreshToken.getExpiresAt());
        return body;
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestBody RefreshRequest request,
                                       HttpServletRequest httpRequest) {
        String refreshToken = request.refreshToken() == null ? "" : request.refreshToken().trim();
        if (refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken 不能为空");
        }

        AuthRefreshToken persisted = authTokenService.validateRefreshToken(refreshToken);
        AppUser user = userRepository.findByUsername(persisted.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("账号已禁用，无法刷新登录状态");
        }

        authTokenService.revokeToken(refreshToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        AuthRefreshToken newRefreshToken = authTokenService.issueRefreshToken(
                user.getUsername(),
                RequestClientUtils.getUserAgent(httpRequest),
                RequestClientUtils.getClientIp(httpRequest)
        );

        SecurityContext context = new SecurityContextImpl();
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "刷新成功");
        body.put("tokenType", "Bearer");
        body.put("accessToken", newAccessToken);
        body.put("refreshToken", newRefreshToken.getToken());
        body.put("accessTokenExpiresAt", toDateTime(jwtService.extractExpiration(newAccessToken)));
        body.put("refreshTokenExpiresAt", newRefreshToken.getExpiresAt());
        return body;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestBody(required = false) RefreshRequest request,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? null : authentication.getName();

        if (request != null && StringUtils.hasText(request.refreshToken())) {
            authTokenService.revokeToken(request.refreshToken().trim());
        }
        if (StringUtils.hasText(username)) {
            authTokenService.revokeAllByUsername(username);
        }

        new SecurityContextLogoutHandler().logout(httpRequest, response, authentication);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "已退出登录");
        return body;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        AppUser user = currentUserService.mustGetCurrentUser();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("user", toUserView(user));
        return body;
    }

    @PutMapping("/password")
    public Map<String, Object> updatePassword(@RequestBody PasswordUpdateRequest request) {
        AppUser user = currentUserService.mustGetCurrentUser();
        if (request.oldPassword() == null || request.oldPassword().isBlank()
                || request.newPassword() == null || request.newPassword().isBlank()) {
            throw new IllegalArgumentException("旧密码和新密码不能为空");
        }
        if (request.newPassword().trim().length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("旧密码不正确");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword().trim()));
        userRepository.save(user);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "密码修改成功");
        return body;
    }

    private Map<String, Object> toUserView(AppUser user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("name", user.getName());
        view.put("role", user.getRole().name());
        view.put("className", user.getClassName());
        view.put("department", user.getDepartment());
        view.put("enabled", user.getEnabled());
        view.put("createdAt", user.getCreatedAt());
        view.put("updatedAt", user.getUpdatedAt());
        return view;
    }

    private LocalDateTime toDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record PasswordUpdateRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {
    }
}
