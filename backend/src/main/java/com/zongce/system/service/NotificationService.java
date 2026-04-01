package com.zongce.system.service;

import com.zongce.system.entity.SysNotification;
import com.zongce.system.entity.enums.RoleType;

import java.util.List;

public interface NotificationService {

    void createForUser(String username, String title, String content, String bizType, String linkUrl, String bizRef);

    default void createForUser(String username, String title, String content, String bizType, String linkUrl) {
        createForUser(username, title, content, bizType, linkUrl, null);
    }

    void createForRole(RoleType role, String title, String content, String bizType, String linkUrl, String bizRef);

    default void createForRole(RoleType role, String title, String content, String bizType, String linkUrl) {
        createForRole(role, title, content, bizType, linkUrl, null);
    }

    void createForCounselorClass(String className, String title, String content, String bizType, String linkUrl, String bizRef);

    default void createForCounselorClass(String className, String title, String content, String bizType, String linkUrl) {
        createForCounselorClass(className, title, content, bizType, linkUrl, null);
    }

    void createForStudentClass(String className, String title, String content, String bizType, String linkUrl, String bizRef);

    default void createForStudentClass(String className, String title, String content, String bizType, String linkUrl) {
        createForStudentClass(className, title, content, bizType, linkUrl, null);
    }

    List<SysNotification> listMine(String username, Boolean unreadOnly, Integer limit);

    long unreadCount(String username);

    void markRead(String username, Long notificationId);

    int markReadByBizRef(String username, String bizType, String bizRef);

    int markAllRead(String username);

    void deleteOne(String username, Long notificationId);

    int deleteRead(String username);
}
