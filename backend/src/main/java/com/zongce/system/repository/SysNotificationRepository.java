package com.zongce.system.repository;

import com.zongce.system.entity.SysNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysNotificationRepository extends JpaRepository<SysNotification, Long> {

    List<SysNotification> findTop100ByUsernameOrderByCreatedAtDesc(String username);

    List<SysNotification> findTop100ByUsernameAndReadFlagOrderByCreatedAtDesc(String username, Boolean readFlag);

    List<SysNotification> findByUsernameAndReadFlagOrderByCreatedAtDesc(String username, Boolean readFlag);

    List<SysNotification> findByUsernameAndBizTypeAndBizRefAndReadFlagOrderByCreatedAtDesc(String username,
                                                                                             String bizType,
                                                                                             String bizRef,
                                                                                             Boolean readFlag);

    long countByUsernameAndReadFlag(String username, Boolean readFlag);

    long deleteByIdAndUsername(Long id, String username);

    long deleteByUsernameAndReadFlag(String username, Boolean readFlag);

    Optional<SysNotification> findByIdAndUsername(Long id, String username);
}
