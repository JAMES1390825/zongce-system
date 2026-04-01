package com.zongce.system.service.impl;

import com.zongce.system.entity.SysAuditLog;
import com.zongce.system.repository.SysAuditLogRepository;
import com.zongce.system.service.AuditLogService;
import com.zongce.system.util.RequestClientUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final SysAuditLogRepository sysAuditLogRepository;

    public AuditLogServiceImpl(SysAuditLogRepository sysAuditLogRepository) {
        this.sysAuditLogRepository = sysAuditLogRepository;
    }

    @Override
    public void recordApiOperation(HttpServletRequest request, String username, boolean success, String detail) {
        String path = request == null ? null : request.getRequestURI();
        String method = request == null ? null : request.getMethod();

        SysAuditLog row = new SysAuditLog();
        row.setUsername(shorten(username, 64));
        row.setModuleName(resolveModuleName(path));
        row.setActionName(shorten((method == null ? "" : method) + " " + (path == null ? "" : path), 128));
        row.setRequestPath(shorten(path, 255));
        row.setHttpMethod(shorten(method, 16));
        row.setSuccess(success);
        row.setTargetId(shorten(resolveTargetId(path), 128));
        row.setClientIp(shorten(RequestClientUtils.getClientIp(request), 64));
        row.setUserAgent(shorten(RequestClientUtils.getUserAgent(request), 255));
        row.setDetail(shorten(detail, 1000));
        sysAuditLogRepository.save(row);
    }

    private String resolveModuleName(String path) {
        if (path == null || path.isBlank()) {
            return "api";
        }
        String[] segments = path.split("/");
        if (segments.length >= 3) {
            return shorten(segments[2], 64);
        }
        return "api";
    }

    private String resolveTargetId(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] segments = path.split("/");
        if (segments.length < 2) {
            return null;
        }
        String last = segments[segments.length - 1];
        if (last == null || last.isBlank() || "api".equalsIgnoreCase(last)) {
            return null;
        }
        return last;
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
