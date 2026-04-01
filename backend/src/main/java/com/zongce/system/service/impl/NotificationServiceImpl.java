package com.zongce.system.service.impl;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.SysNotification;
import com.zongce.system.entity.enums.RoleType;
import com.zongce.system.repository.SysNotificationRepository;
import com.zongce.system.repository.UserRepository;
import com.zongce.system.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final SysNotificationRepository sysNotificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(SysNotificationRepository sysNotificationRepository,
                                   UserRepository userRepository) {
        this.sysNotificationRepository = sysNotificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void createForUser(String username, String title, String content, String bizType, String linkUrl, String bizRef) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        SysNotification row = new SysNotification();
        row.setUsername(username.trim());
        row.setTitle(trimToLength(title, 128, "系统通知"));
        row.setContent(trimToLength(content, 1000, "你有一条新通知"));
        row.setBizType(trimToLength(bizType, 64, null));
        row.setBizRef(trimToLength(bizRef, 128, null));
        row.setLinkUrl(trimToLength(linkUrl, 255, null));
        row.setReadFlag(false);
        row.setReadAt(null);
        sysNotificationRepository.save(row);
    }

    @Override
    @Transactional
    public void createForRole(RoleType role, String title, String content, String bizType, String linkUrl, String bizRef) {
        if (role == null) {
            return;
        }
        List<AppUser> users = userRepository.findByRoleAndEnabledTrue(role);
        for (AppUser user : users) {
            createForUser(user.getUsername(), title, content, bizType, linkUrl, bizRef);
        }
    }

    @Override
    @Transactional
    public void createForCounselorClass(String className,
                                        String title,
                                        String content,
                                        String bizType,
                                        String linkUrl,
                                        String bizRef) {
        List<AppUser> users = userRepository.findByRoleAndEnabledTrue(RoleType.COUNSELOR);
        for (AppUser user : users) {
            String scopedClass = user.getClassName();
            if (!StringUtils.hasText(scopedClass) || scopedClass.equals(className)) {
                createForUser(user.getUsername(), title, content, bizType, linkUrl, bizRef);
            }
        }
    }

    @Override
    @Transactional
    public void createForStudentClass(String className,
                                      String title,
                                      String content,
                                      String bizType,
                                      String linkUrl,
                                      String bizRef) {
        if (!StringUtils.hasText(className)) {
            return;
        }
        List<AppUser> users = userRepository.findByRoleAndClassNameAndEnabledTrue(RoleType.STUDENT, className.trim());
        for (AppUser user : users) {
            createForUser(user.getUsername(), title, content, bizType, linkUrl, bizRef);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysNotification> listMine(String username, Boolean unreadOnly, Integer limit) {
        int capped = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);
        List<SysNotification> rows = Boolean.TRUE.equals(unreadOnly)
                ? sysNotificationRepository.findTop100ByUsernameAndReadFlagOrderByCreatedAtDesc(username, false)
                : sysNotificationRepository.findTop100ByUsernameOrderByCreatedAtDesc(username);
        if (rows.size() <= capped) {
            return rows;
        }
        return rows.subList(0, capped);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return sysNotificationRepository.countByUsernameAndReadFlag(username, false);
    }

    @Override
    @Transactional
    public void markRead(String username, Long notificationId) {
        SysNotification row = sysNotificationRepository.findByIdAndUsername(notificationId, username)
                .orElseThrow(() -> new IllegalArgumentException("通知不存在"));
        if (!Boolean.TRUE.equals(row.getReadFlag())) {
            row.setReadFlag(true);
            row.setReadAt(LocalDateTime.now());
            sysNotificationRepository.save(row);
        }
    }

    @Override
    @Transactional
    public int markReadByBizRef(String username, String bizType, String bizRef) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(bizType) || !StringUtils.hasText(bizRef)) {
            return 0;
        }
        List<SysNotification> rows = sysNotificationRepository
                .findByUsernameAndBizTypeAndBizRefAndReadFlagOrderByCreatedAtDesc(username, bizType, bizRef, false);
        if (rows.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SysNotification row : rows) {
            row.setReadFlag(true);
            row.setReadAt(now);
        }
        sysNotificationRepository.saveAll(rows);
        return rows.size();
    }

    @Override
    @Transactional
    public int markAllRead(String username) {
        List<SysNotification> rows = sysNotificationRepository.findByUsernameAndReadFlagOrderByCreatedAtDesc(username, false);
        if (rows.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SysNotification row : rows) {
            row.setReadFlag(true);
            row.setReadAt(now);
        }
        sysNotificationRepository.saveAll(rows);
        return rows.size();
    }

    @Override
    @Transactional
    public void deleteOne(String username, Long notificationId) {
        long removed = sysNotificationRepository.deleteByIdAndUsername(notificationId, username);
        if (removed <= 0) {
            throw new IllegalArgumentException("通知不存在");
        }
    }

    @Override
    @Transactional
    public int deleteRead(String username) {
        return (int) sysNotificationRepository.deleteByUsernameAndReadFlag(username, true);
    }

    private String trimToLength(String value, int maxLength, String fallback) {
        String raw = value;
        if (!StringUtils.hasText(raw)) {
            raw = fallback;
        }
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String cleaned = raw.trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}
