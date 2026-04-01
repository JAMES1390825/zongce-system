package com.zongce.system.repository;

import com.zongce.system.entity.SysLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysLoginLogRepository extends JpaRepository<SysLoginLog, Long> {

    List<SysLoginLog> findTop300ByOrderByCreatedAtDesc();
}
