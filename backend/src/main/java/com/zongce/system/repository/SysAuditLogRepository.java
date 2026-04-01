package com.zongce.system.repository;

import com.zongce.system.entity.SysAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysAuditLogRepository extends JpaRepository<SysAuditLog, Long> {

    List<SysAuditLog> findTop300ByOrderByCreatedAtDesc();
}
