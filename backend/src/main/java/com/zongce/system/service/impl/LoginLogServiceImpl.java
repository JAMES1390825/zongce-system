package com.zongce.system.service.impl;

import com.zongce.system.entity.SysLoginLog;
import com.zongce.system.repository.SysLoginLogRepository;
import com.zongce.system.service.LoginLogService;
import com.zongce.system.util.RequestClientUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final SysLoginLogRepository sysLoginLogRepository;

    public LoginLogServiceImpl(SysLoginLogRepository sysLoginLogRepository) {
        this.sysLoginLogRepository = sysLoginLogRepository;
    }

    @Override
    public void record(String username, boolean success, String message, HttpServletRequest request) {
        SysLoginLog row = new SysLoginLog();
        row.setUsername(shorten(username, 64));
        row.setSuccess(success);
        row.setMessage(shorten(message, 255));
        row.setClientIp(shorten(RequestClientUtils.getClientIp(request), 64));
        row.setUserAgent(shorten(RequestClientUtils.getUserAgent(request), 255));
        sysLoginLogRepository.save(row);
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
