package com.zongce.system.api.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.SysNotification;
import com.zongce.system.service.CurrentUserService;
import com.zongce.system.service.NotificationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public NotificationApiController(CurrentUserService currentUserService,
                                     NotificationService notificationService) {
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public Map<String, Object> listMine(@RequestParam(required = false) Boolean unreadOnly,
                                        @RequestParam(required = false) Integer limit) {
        AppUser user = currentUserService.mustGetCurrentUser();
        List<SysNotification> rows = notificationService.listMine(user.getUsername(), unreadOnly, limit);
        Map<String, Object> body = ok(rows);
        body.put("unreadCount", notificationService.unreadCount(user.getUsername()));
        return body;
    }

    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount() {
        AppUser user = currentUserService.mustGetCurrentUser();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("count", notificationService.unreadCount(user.getUsername()));
        return body;
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> markRead(@PathVariable Long id) {
        AppUser user = currentUserService.mustGetCurrentUser();
        notificationService.markRead(user.getUsername(), id);
        return message("已标记为已读");
    }

    @PostMapping("/read-all")
    public Map<String, Object> readAll() {
        AppUser user = currentUserService.mustGetCurrentUser();
        int count = notificationService.markAllRead(user.getUsername());
        return message("已全部设为已读（" + count + " 条）");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteOne(@PathVariable Long id) {
        AppUser user = currentUserService.mustGetCurrentUser();
        notificationService.deleteOne(user.getUsername(), id);
        return message("已删除通知");
    }

    @PostMapping("/delete-read")
    public Map<String, Object> deleteRead() {
        AppUser user = currentUserService.mustGetCurrentUser();
        int count = notificationService.deleteRead(user.getUsername());
        return message("已删除已读通知（" + count + " 条）");
    }

    private Map<String, Object> message(String msg) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", msg);
        return body;
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        return body;
    }
}
