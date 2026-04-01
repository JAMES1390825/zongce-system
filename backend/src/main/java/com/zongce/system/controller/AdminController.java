package com.zongce.system.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.ScoreResult;
import com.zongce.system.entity.enums.RoleType;
import com.zongce.system.repository.ScoreResultRepository;
import com.zongce.system.repository.UserRepository;
import com.zongce.system.service.CurrentUserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@Validated
public class AdminController {

    private static final List<RoleType> MANAGED_TEACHER_ROLES = List.of(
            RoleType.TEACHER_PE,
            RoleType.TEACHER_STUDY,
            RoleType.COUNSELOR
    );

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final ScoreResultRepository scoreResultRepository;
    private final String initialPassword;

    public AdminController(UserRepository userRepository,
                           CurrentUserService currentUserService,
                           PasswordEncoder passwordEncoder,
                           ScoreResultRepository scoreResultRepository,
                           @Value("${app.user.initial-password:123456}") String initialPassword) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.scoreResultRepository = scoreResultRepository;
        this.initialPassword = initialPassword;
    }

    @GetMapping("/admin/users")
    public String userPageCompat() {
        return "redirect:/admin/students";
    }

    @GetMapping("/admin/students")
    public String studentPage(@RequestParam(required = false) String className,
                              org.springframework.ui.Model model) {
        String classFilter = clean(className);
        List<AppUser> users = StringUtils.hasText(classFilter)
                ? userRepository.findByRoleAndClassNameContainingIgnoreCaseOrderByUsernameAsc(RoleType.STUDENT, classFilter)
                : userRepository.findByRoleOrderByUsernameAsc(RoleType.STUDENT);

        model.addAttribute("users", users);
        model.addAttribute("className", classFilter == null ? "" : classFilter);
        addCommonModel(model);
        return "admin/students";
    }

    @PostMapping("/admin/students")
    public String createStudent(@RequestParam @NotBlank String username,
                                @RequestParam @NotBlank String name,
                                @RequestParam @NotBlank String className,
                                RedirectAttributes redirectAttributes) {
        String normalizedUsername = normalizeRequired(username, "账号不能为空", redirectAttributes);
        if (normalizedUsername == null) {
            return "redirect:/admin/students";
        }
        String normalizedClassName = normalizeRequired(className, "班级不能为空", redirectAttributes);
        if (normalizedClassName == null) {
            return "redirect:/admin/students";
        }

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "用户名已存在：" + normalizedUsername);
            return "redirect:/admin/students";
        }

        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setName(name.trim());
        user.setRole(RoleType.STUDENT);
        user.setClassName(normalizedClassName);
        user.setDepartment(null);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "学生创建成功，已设置系统初始密码");
        return "redirect:/admin/students";
    }

    @PostMapping("/admin/students/{id}/update")
    public String updateStudent(@PathVariable Long id,
                                @RequestParam @NotBlank String username,
                                @RequestParam @NotBlank String name,
                                @RequestParam @NotBlank String className,
                                @RequestParam(defaultValue = "true") boolean enabled,
                                RedirectAttributes redirectAttributes) {
        AppUser row = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在，ID=" + id));
        if (row.getRole() != RoleType.STUDENT) {
            throw new IllegalArgumentException("仅可在学生列表编辑学生账号");
        }

        String normalizedUsername = username.trim();
        if (normalizedUsername.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "账号不能为空");
            return "redirect:/admin/students";
        }
        String normalizedClassName = className.trim();
        if (normalizedClassName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "班级不能为空");
            return "redirect:/admin/students";
        }

        Optional<AppUser> owner = userRepository.findByUsername(normalizedUsername);
        if (owner.isPresent() && owner.get().getId().equals(id) == false) {
            redirectAttributes.addFlashAttribute("error", "账号已被占用：" + normalizedUsername);
            return "redirect:/admin/students";
        }

        row.setUsername(normalizedUsername);
        row.setName(name.trim());
        row.setClassName(normalizedClassName);
        row.setDepartment(null);
        row.setEnabled(enabled);
        userRepository.save(row);

        redirectAttributes.addFlashAttribute("success", "学生信息已更新");
        return "redirect:/admin/students";
    }

    @PostMapping("/admin/students/{id}/delete")
    public String deleteStudent(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        AppUser row = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在，ID=" + id));
        if (row.getRole() != RoleType.STUDENT) {
            throw new IllegalArgumentException("仅可在学生列表删除学生账号");
        }

        userRepository.delete(row);
        redirectAttributes.addFlashAttribute("success", "学生账号已删除");
        return "redirect:/admin/students";
    }

    @PostMapping("/admin/students/{id}/reset-password")
    public String resetStudentPassword(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        AppUser row = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在，ID=" + id));
        if (row.getRole() != RoleType.STUDENT) {
            throw new IllegalArgumentException("仅可在学生列表重置学生密码");
        }

        row.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(row);
        redirectAttributes.addFlashAttribute("success", "密码已重置为系统初始密码（账号：" + row.getUsername() + "）");
        return "redirect:/admin/students";
    }

    @GetMapping("/admin/teachers")
    public String teacherPage(@RequestParam(required = false) RoleType role,
                              @RequestParam(required = false) String department,
                              org.springframework.ui.Model model) {
        String departmentFilter = clean(department);
        List<AppUser> rows = userRepository.findByRoleInOrderByRoleAscUsernameAsc(MANAGED_TEACHER_ROLES);
        rows = rows.stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> StringUtils.hasText(departmentFilter) == false
                        || (u.getDepartment() != null && u.getDepartment().toLowerCase().contains(departmentFilter.toLowerCase())))
                .sorted(Comparator.comparing(AppUser::getRole).thenComparing(AppUser::getUsername))
                .toList();

        model.addAttribute("users", rows);
        model.addAttribute("department", departmentFilter == null ? "" : departmentFilter);
        model.addAttribute("selectedRole", role == null ? "" : role.name());
        model.addAttribute("roles", MANAGED_TEACHER_ROLES);
        addCommonModel(model);
        return "admin/teachers";
    }

    @PostMapping("/admin/teachers")
    public String createTeacher(@RequestParam @NotBlank String username,
                                @RequestParam @NotBlank String name,
                                @RequestParam RoleType role,
                                @RequestParam(required = false) String department,
                                @RequestParam(required = false) String className,
                                RedirectAttributes redirectAttributes) {
        validateTeacherRole(role);
        String normalizedUsername = normalizeRequired(username, "账号不能为空", redirectAttributes);
        if (normalizedUsername == null) {
            return "redirect:/admin/teachers";
        }

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "用户名已存在：" + normalizedUsername);
            return "redirect:/admin/teachers";
        }

        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setName(name.trim());
        user.setRole(role);
        user.setDepartment(clean(department));
        user.setClassName(role == RoleType.COUNSELOR ? clean(className) : null);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "老师账号创建成功，已设置系统初始密码");
        return "redirect:/admin/teachers";
    }

    @PostMapping("/admin/teachers/{id}/update")
    public String updateTeacher(@PathVariable Long id,
                                @RequestParam @NotBlank String username,
                                @RequestParam @NotBlank String name,
                                @RequestParam RoleType role,
                                @RequestParam(required = false) String department,
                                @RequestParam(required = false) String className,
                                @RequestParam(defaultValue = "true") boolean enabled,
                                RedirectAttributes redirectAttributes) {
        validateTeacherRole(role);
        AppUser row = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("老师不存在，ID=" + id));
        validateTeacherRole(row.getRole());

        String normalizedUsername = username.trim();
        if (normalizedUsername.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "账号不能为空");
            return "redirect:/admin/teachers";
        }

        Optional<AppUser> owner = userRepository.findByUsername(normalizedUsername);
        if (owner.isPresent() && owner.get().getId().equals(id) == false) {
            redirectAttributes.addFlashAttribute("error", "账号已被占用：" + normalizedUsername);
            return "redirect:/admin/teachers";
        }

        row.setUsername(normalizedUsername);
        row.setName(name.trim());
        row.setRole(role);
        row.setDepartment(clean(department));
        row.setClassName(role == RoleType.COUNSELOR ? clean(className) : null);
        row.setEnabled(enabled);
        userRepository.save(row);

        redirectAttributes.addFlashAttribute("success", "老师信息已更新");
        return "redirect:/admin/teachers";
    }

    @PostMapping("/admin/teachers/{id}/delete")
    public String deleteTeacher(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        AppUser row = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("老师不存在，ID=" + id));
        validateTeacherRole(row.getRole());

        userRepository.delete(row);
        redirectAttributes.addFlashAttribute("success", "老师账号已删除");
        return "redirect:/admin/teachers";
    }

    @PostMapping("/admin/teachers/{id}/reset-password")
    public String resetTeacherPassword(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        AppUser row = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("老师不存在，ID=" + id));
        validateTeacherRole(row.getRole());
        row.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(row);
        redirectAttributes.addFlashAttribute("success", "密码已重置为系统初始密码（账号：" + row.getUsername() + "）");
        return "redirect:/admin/teachers";
    }

    @GetMapping("/admin/scores")
    public String globalScores(@RequestParam(required = false) String className,
                               @RequestParam(required = false) String term,
                               org.springframework.ui.Model model) {
        String normalizedClassName = clean(className);
        String normalizedTerm = clean(term);
        List<ScoreResult> rows;

        if (StringUtils.hasText(normalizedClassName) && StringUtils.hasText(normalizedTerm)) {
            rows = scoreResultRepository.findByClassNameAndTermOrderByRankNoAsc(normalizedClassName, normalizedTerm);
        } else if (StringUtils.hasText(normalizedTerm)) {
            rows = scoreResultRepository.findByTermOrderByClassNameAscRankNoAsc(normalizedTerm);
        } else if (StringUtils.hasText(normalizedClassName)) {
            rows = scoreResultRepository.findByClassNameOrderByTermDescRankNoAsc(normalizedClassName);
        } else {
            rows = List.of();
        }

        model.addAttribute("rows", rows);
        model.addAttribute("className", normalizedClassName == null ? "" : normalizedClassName);
        model.addAttribute("term", normalizedTerm == null ? "" : normalizedTerm);
        addCommonModel(model);
        return "admin/scores";
    }

    private String normalizeRequired(String value,
                                     String message,
                                     RedirectAttributes redirectAttributes) {
        String normalized = clean(value);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        redirectAttributes.addFlashAttribute("error", message);
        return null;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateTeacherRole(RoleType role) {
        if (MANAGED_TEACHER_ROLES.contains(role) == false) {
            throw new IllegalArgumentException("该页面仅支持管理体育老师/学习部老师/辅导员");
        }
    }

    private void addCommonModel(org.springframework.ui.Model model) {
        model.addAttribute("currentUser", currentUserService.mustGetCurrentUser());
    }
}
