package com.zongce.system.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.enums.RoleType;
import com.zongce.system.repository.UserRepository;
import com.zongce.system.service.CurrentUserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Validated
public class HomeController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public HomeController(CurrentUserService currentUserService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AppUser current = currentUserService.mustGetCurrentUser();
        model.addAttribute("user", current);
        model.addAttribute("currentUser", current);
        model.addAttribute("role", current.getRole().name());
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        AppUser current = currentUserService.mustGetCurrentUser();
        model.addAttribute("user", current);
        model.addAttribute("currentUser", current);
        return "profile";
    }

    @PostMapping("/profile/password")
    public String updatePassword(@RequestParam @NotBlank String oldPassword,
                                 @RequestParam @NotBlank String newPassword,
                                 Model model) {
        AppUser user = currentUserService.mustGetCurrentUser();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            model.addAttribute("error", "旧密码不正确");
            model.addAttribute("user", user);
            model.addAttribute("currentUser", user);
            return "profile";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("success", "密码修改成功，请下次使用新密码登录");
        return "profile";
    }

    @GetMapping("/forbidden")
    public String forbidden() {
        return "forbidden";
    }

    @GetMapping("/role-home")
    public String roleHome() {
        RoleType role = currentUserService.mustGetCurrentUser().getRole();
        return switch (role) {
            case ADMIN -> "redirect:/admin/students";
            case TEACHER_PE -> "redirect:/teacher/pe/import";
            case TEACHER_STUDY -> "redirect:/teacher/study/import";
            case COUNSELOR -> "redirect:/counselor/review/moral";
            case STUDENT -> "redirect:/student/declare/moral";
        };
    }
}
