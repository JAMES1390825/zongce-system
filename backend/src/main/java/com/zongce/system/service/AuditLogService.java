package com.zongce.system.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditLogService {

    void recordApiOperation(HttpServletRequest request, String username, boolean success, String detail);
}
