package com.zongce.system.api.controller;

import com.zongce.system.entity.ImportBatch;
import com.zongce.system.entity.ImportErrorDetail;
import com.zongce.system.entity.SysAuditLog;
import com.zongce.system.entity.SysLoginLog;
import com.zongce.system.repository.SysAuditLogRepository;
import com.zongce.system.repository.SysLoginLogRepository;
import com.zongce.system.service.ImportBatchService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminOpsApiController {

    private final ImportBatchService importBatchService;
    private final SysAuditLogRepository sysAuditLogRepository;
    private final SysLoginLogRepository sysLoginLogRepository;

    public AdminOpsApiController(ImportBatchService importBatchService,
                                 SysAuditLogRepository sysAuditLogRepository,
                                 SysLoginLogRepository sysLoginLogRepository) {
        this.importBatchService = importBatchService;
        this.sysAuditLogRepository = sysAuditLogRepository;
        this.sysLoginLogRepository = sysLoginLogRepository;
    }

    @GetMapping("/import-batches")
    public Map<String, Object> importBatches(@RequestParam(required = false) String importType,
                                             @RequestParam(required = false) String operator) {
        List<ImportBatch> rows = importBatchService.listBatches(clean(importType), clean(operator));
        return ok(rows);
    }

    @GetMapping("/import-batches/{batchNo}/errors")
    public Map<String, Object> importBatchErrors(@PathVariable String batchNo) {
        List<ImportErrorDetail> rows = importBatchService.listErrors(batchNo);
        return ok(rows);
    }

    @GetMapping("/audit-logs")
    public Map<String, Object> auditLogs(@RequestParam(required = false) String username,
                                         @RequestParam(required = false) String module,
                                         @RequestParam(required = false) Boolean success,
                                         @RequestParam(required = false) Integer limit) {
        int cappedLimit = capLimit(limit);
        String usernameFilter = clean(username);
        String moduleFilter = clean(module);

        List<SysAuditLog> rows = sysAuditLogRepository.findTop300ByOrderByCreatedAtDesc().stream()
                .filter(row -> !StringUtils.hasText(usernameFilter) || containsIgnoreCase(row.getUsername(), usernameFilter))
                .filter(row -> !StringUtils.hasText(moduleFilter) || containsIgnoreCase(row.getModuleName(), moduleFilter))
                .filter(row -> success == null || success.equals(row.getSuccess()))
                .limit(cappedLimit)
                .toList();
        return ok(rows);
    }

    @GetMapping("/login-logs")
    public Map<String, Object> loginLogs(@RequestParam(required = false) String username,
                                         @RequestParam(required = false) Boolean success,
                                         @RequestParam(required = false) Integer limit) {
        int cappedLimit = capLimit(limit);
        String usernameFilter = clean(username);

        List<SysLoginLog> rows = sysLoginLogRepository.findTop300ByOrderByCreatedAtDesc().stream()
                .filter(row -> !StringUtils.hasText(usernameFilter) || containsIgnoreCase(row.getUsername(), usernameFilter))
                .filter(row -> success == null || success.equals(row.getSuccess()))
                .limit(cappedLimit)
                .toList();
        return ok(rows);
    }

    private int capLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 300);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean containsIgnoreCase(String raw, String keyword) {
        return raw != null && raw.toLowerCase().contains(keyword.toLowerCase());
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        return body;
    }
}
