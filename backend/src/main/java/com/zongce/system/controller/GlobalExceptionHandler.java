package com.zongce.system.controller;

import com.zongce.system.service.CurrentUserService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackages = "com.zongce.system.controller")
public class GlobalExceptionHandler {

    private final CurrentUserService currentUserService;

    public GlobalExceptionHandler(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @ExceptionHandler(Exception.class)
    public String handle(Exception e, Model model) {
        model.addAttribute("message", e.getMessage());
        try {
            model.addAttribute("currentUser", currentUserService.mustGetCurrentUser());
        } catch (Exception ignored) {
            // ignore if not logged in
        }
        return "error";
    }
}
