package com.zongce.system.service;

import jakarta.servlet.http.HttpServletRequest;

public interface LoginLogService {

    void record(String username, boolean success, String message, HttpServletRequest request);
}
