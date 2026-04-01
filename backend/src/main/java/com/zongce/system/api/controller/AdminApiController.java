package com.zongce.system.api.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.CalcRule;
import com.zongce.system.entity.ImportBatch;
import com.zongce.system.entity.OrgClass;
import com.zongce.system.entity.OrgDepartment;
import com.zongce.system.entity.ScoreResult;
import com.zongce.system.entity.enums.RoleType;
import com.zongce.system.repository.OrgClassRepository;
import com.zongce.system.repository.OrgDepartmentRepository;
import com.zongce.system.repository.ScoreResultRepository;
import com.zongce.system.repository.UserRepository;
import com.zongce.system.service.CalcRuleService;
import com.zongce.system.service.ImportBatchService;
import com.zongce.system.service.NotificationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminApiController {

    private static final List<RoleType> MANAGED_TEACHER_ROLES = List.of(
            RoleType.TEACHER_PE, RoleType.TEACHER_STUDY, RoleType.COUNSELOR
    );

    private final UserRepository userRepository;
    private final ScoreResultRepository scoreResultRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrgClassRepository orgClassRepository;
    private final OrgDepartmentRepository orgDepartmentRepository;
    private final CalcRuleService calcRuleService;
    private final ImportBatchService importBatchService;
    private final NotificationService notificationService;
    private final String initialPassword;

    public AdminApiController(UserRepository userRepository,
                              ScoreResultRepository scoreResultRepository,
                              PasswordEncoder passwordEncoder,
                              OrgClassRepository orgClassRepository,
                              OrgDepartmentRepository orgDepartmentRepository,
                              CalcRuleService calcRuleService,
                              ImportBatchService importBatchService,
                              NotificationService notificationService,
                              @Value("${app.user.initial-password:123456}") String initialPassword) {
        this.userRepository = userRepository;
        this.scoreResultRepository = scoreResultRepository;
        this.passwordEncoder = passwordEncoder;
        this.orgClassRepository = orgClassRepository;
        this.orgDepartmentRepository = orgDepartmentRepository;
        this.calcRuleService = calcRuleService;
        this.importBatchService = importBatchService;
        this.notificationService = notificationService;
        this.initialPassword = initialPassword;
    }

    @GetMapping("/students")
    public Map<String, Object> listStudents(@RequestParam(required = false) String className) {
        String classFilter = clean(className);
        List<AppUser> users = StringUtils.hasText(classFilter)
                ? userRepository.findByRoleAndClassNameContainingIgnoreCaseOrderByUsernameAsc(RoleType.STUDENT, classFilter)
                : userRepository.findByRoleOrderByUsernameAsc(RoleType.STUDENT);

        return ok(users.stream().map(this::toUserView).toList());
    }

    @PostMapping("/students")
    public Map<String, Object> createStudent(@RequestBody StudentUpsertRequest request) {
        String username = require(request.username(), "账号不能为空");
        String name = require(request.name(), "姓名不能为空");
        String className = require(request.className(), "班级不能为空");
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在：" + username);
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setName(name);
        user.setRole(RoleType.STUDENT);
        user.setClassName(className);
        user.setDepartment(null);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(user);
        return message("学生创建成功，已设置系统初始密码");
    }

    @PutMapping("/students/{id}")
    public Map<String, Object> updateStudent(@PathVariable Long id,
                                             @RequestBody StudentUpsertRequest request) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (user.getRole() != RoleType.STUDENT) {
            throw new IllegalArgumentException("目标账号不是学生");
        }

        String username = require(request.username(), "账号不能为空");
        Optional<AppUser> existing = userRepository.findByUsername(username);
        if (existing.isPresent() && existing.get().getId().equals(id) == false) {
            throw new IllegalArgumentException("账号已被占用：" + username);
        }

        user.setUsername(username);
        user.setName(require(request.name(), "姓名不能为空"));
        user.setClassName(require(request.className(), "班级不能为空"));
        user.setDepartment(null);
        user.setEnabled(request.enabled() == null || request.enabled());
        userRepository.save(user);
        return message("学生信息已更新");
    }

    @DeleteMapping("/students/{id}")
    public Map<String, Object> deleteStudent(@PathVariable Long id) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (user.getRole() != RoleType.STUDENT) {
            throw new IllegalArgumentException("目标账号不是学生");
        }
        userRepository.delete(user);
        return message("学生账号已删除");
    }

    @PostMapping("/students/{id}/reset-password")
    public Map<String, Object> resetStudentPassword(@PathVariable Long id) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (user.getRole() != RoleType.STUDENT) {
            throw new IllegalArgumentException("目标账号不是学生");
        }
        user.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(user);
        return message("密码已重置为系统初始密码");
    }

    @PostMapping("/students/import")
    public Map<String, Object> importStudents(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("请先上传 CSV 文件");
        }

        String operator = currentUsername();
        ImportBatch batch = importBatchService.startBatch("STUDENT", file.getOriginalFilename(), operator);
        int total = 0;
        int created = 0;
        int updated = 0;
        int failed = 0;
        int success = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        List<ImportBatchService.ImportErrorItem> batchErrors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && looksLikeStudentHeader(line)) {
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }

                total++;
                String[] parts = parseCsvLine(line);
                if (parts.length < 3) {
                    failed++;
                    String reason = "字段不足，至少需要 3 列（学号/姓名/班级）";
                    errors.add(importError(lineNo, reason));
                    batchErrors.add(new ImportBatchService.ImportErrorItem(lineNo, reason, line));
                    continue;
                }

                String username = clean(parts[0]);
                String name = clean(parts[1]);
                String className = clean(parts[2]);

                if (!StringUtils.hasText(username) || !StringUtils.hasText(name) || !StringUtils.hasText(className)) {
                    failed++;
                    String reason = "学号、姓名、班级不能为空";
                    errors.add(importError(lineNo, reason));
                    batchErrors.add(new ImportBatchService.ImportErrorItem(lineNo, reason, line));
                    continue;
                }

                Optional<AppUser> existing = userRepository.findByUsername(username);
                if (existing.isPresent()) {
                    AppUser row = existing.get();
                    if (row.getRole() != RoleType.STUDENT) {
                        failed++;
                        String reason = "账号已存在且不是学生角色：" + username;
                        errors.add(importError(lineNo, reason));
                        batchErrors.add(new ImportBatchService.ImportErrorItem(lineNo, reason, line));
                        continue;
                    }
                    row.setName(name);
                    row.setClassName(className);
                    row.setDepartment(null);
                    row.setEnabled(true);
                    userRepository.save(row);
                    updated++;
                    success++;
                    continue;
                }

                AppUser user = new AppUser();
                user.setUsername(username);
                user.setName(name);
                user.setRole(RoleType.STUDENT);
                user.setClassName(className);
                user.setDepartment(null);
                user.setEnabled(true);
                user.setPassword(passwordEncoder.encode(initialPassword));
                userRepository.save(user);
                created++;
                success++;
            }
        } catch (IOException e) {
            String reason = "读取导入文件失败：" + e.getMessage();
            failed = Math.max(failed, total);
            errors.add(importError(0, reason));
            batchErrors.add(new ImportBatchService.ImportErrorItem(0, reason, null));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("batchNo", batch.getBatchNo());
        summary.put("total", total);
        summary.put("success", success);
        summary.put("created", created);
        summary.put("updated", updated);
        summary.put("failed", failed);
        summary.put("errors", errors);

        String summaryText = "学生导入完成：新增 " + created + "，更新 " + updated + "，失败 " + failed;
        importBatchService.finishBatch(batch, total, success, failed, summaryText, batchErrors);
        notificationService.createForUser(
                operator,
                "学生导入完成",
                "批次 " + batch.getBatchNo() + "：新增 " + created + "，更新 " + updated + "，失败 " + failed,
                "STUDENT_IMPORT",
                "/admin/students"
        );

        Map<String, Object> body = message("导入完成：新增 " + created + "，更新 " + updated + "，失败 " + failed);
        body.put("data", summary);
        return body;
    }

    @GetMapping("/teachers")
    public Map<String, Object> listTeachers(@RequestParam(required = false) RoleType role,
                                            @RequestParam(required = false) String department) {
        String deptFilter = clean(department);
        List<AppUser> rows = userRepository.findByRoleInOrderByRoleAscUsernameAsc(MANAGED_TEACHER_ROLES);
        rows = rows.stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> StringUtils.hasText(deptFilter) == false
                        || (u.getDepartment() != null && u.getDepartment().toLowerCase().contains(deptFilter.toLowerCase())))
                .sorted(Comparator.comparing(AppUser::getRole).thenComparing(AppUser::getUsername))
                .toList();
        return ok(rows.stream().map(this::toUserView).toList());
    }

    @PostMapping("/teachers")
    public Map<String, Object> createTeacher(@RequestBody TeacherUpsertRequest request) {
        RoleType role = request.role();
        validateTeacherRole(role);
        String username = require(request.username(), "账号不能为空");
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在：" + username);
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setName(require(request.name(), "姓名不能为空"));
        user.setRole(role);
        user.setDepartment(clean(request.department()));
        user.setClassName(resolveCounselorClass(role, request.className()));
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(user);
        return message("老师账号创建成功，已设置系统初始密码");
    }

    @PutMapping("/teachers/{id}")
    public Map<String, Object> updateTeacher(@PathVariable Long id,
                                             @RequestBody TeacherUpsertRequest request) {
        RoleType role = request.role();
        validateTeacherRole(role);
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("老师不存在"));
        validateTeacherRole(user.getRole());

        String username = require(request.username(), "账号不能为空");
        Optional<AppUser> existing = userRepository.findByUsername(username);
        if (existing.isPresent() && existing.get().getId().equals(id) == false) {
            throw new IllegalArgumentException("账号已被占用：" + username);
        }

        user.setUsername(username);
        user.setName(require(request.name(), "姓名不能为空"));
        user.setRole(role);
        user.setDepartment(clean(request.department()));
        user.setClassName(resolveCounselorClass(role, request.className()));
        user.setEnabled(request.enabled() == null || request.enabled());
        userRepository.save(user);
        return message("老师信息已更新");
    }

    @DeleteMapping("/teachers/{id}")
    public Map<String, Object> deleteTeacher(@PathVariable Long id) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("老师不存在"));
        validateTeacherRole(user.getRole());
        userRepository.delete(user);
        return message("老师账号已删除");
    }

    @PostMapping("/teachers/{id}/reset-password")
    public Map<String, Object> resetTeacherPassword(@PathVariable Long id) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("老师不存在"));
        validateTeacherRole(user.getRole());
        user.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(user);
        return message("密码已重置为系统初始密码");
    }

    @GetMapping("/scores")
    public Map<String, Object> listScores(@RequestParam(required = false) String className,
                                          @RequestParam(required = false) String term) {
        List<ScoreResult> rows = queryScores(className, term);
        return ok(rows);
    }

    @GetMapping("/scores/export")
    public ResponseEntity<byte[]> exportScores(@RequestParam(required = false) String className,
                                               @RequestParam(required = false) String term) {
        List<ScoreResult> rows = queryScores(className, term);
        String filename = "scores-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";

        StringBuilder csv = new StringBuilder();
        csv.append("term,className,rankNo,studentNo,studentName,studyScore,peScore,moralScore,skillScore,totalScore,ruleName\n");
        for (ScoreResult row : rows) {
            csv.append(csvCell(row.getTerm())).append(',')
                    .append(csvCell(row.getClassName())).append(',')
                    .append(csvCell(row.getRankNo())).append(',')
                    .append(csvCell(row.getStudentNo())).append(',')
                    .append(csvCell(row.getStudentName())).append(',')
                    .append(csvCell(row.getStudyScore())).append(',')
                    .append(csvCell(row.getPeScore())).append(',')
                    .append(csvCell(row.getMoralScore())).append(',')
                    .append(csvCell(row.getSkillScore())).append(',')
                    .append(csvCell(row.getTotalScore())).append(',')
                    .append(csvCell(row.getRuleName()))
                    .append('\n');
        }

        return csvResponse(filename, csv.toString());
    }

    @GetMapping("/org")
    public Map<String, Object> orgData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("classes", orgClassRepository.findAllByOrderByClassNameAsc().stream().map(this::toClassView).toList());
        data.put("departments", orgDepartmentRepository.findAllByOrderByDepartmentNameAsc().stream().map(this::toDepartmentView).toList());
        return ok(data);
    }

    @PostMapping("/org/classes")
    public Map<String, Object> createOrgClass(@RequestBody OrgClassUpsertRequest request) {
        String className = require(request.className(), "班级名称不能为空");
        if (orgClassRepository.findByClassName(className).isPresent()) {
            throw new IllegalArgumentException("班级已存在：" + className);
        }
        OrgClass row = new OrgClass();
        row.setClassName(className);
        row.setMajorName(clean(request.majorName()));
        row.setEnabled(request.enabled() == null || request.enabled());
        orgClassRepository.save(row);
        return message("班级已创建");
    }

    @PutMapping("/org/classes/{id}")
    public Map<String, Object> updateOrgClass(@PathVariable Long id,
                                              @RequestBody OrgClassUpsertRequest request) {
        OrgClass row = orgClassRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("班级不存在"));
        String className = require(request.className(), "班级名称不能为空");
        Optional<OrgClass> existing = orgClassRepository.findByClassName(className);
        if (existing.isPresent() && existing.get().getId().equals(id) == false) {
            throw new IllegalArgumentException("班级名称已被占用：" + className);
        }
        row.setClassName(className);
        row.setMajorName(clean(request.majorName()));
        row.setEnabled(request.enabled() == null || request.enabled());
        orgClassRepository.save(row);
        return message("班级已更新");
    }

    @DeleteMapping("/org/classes/{id}")
    public Map<String, Object> deleteOrgClass(@PathVariable Long id) {
        OrgClass row = orgClassRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("班级不存在"));
        boolean inUse = userRepository.findAll().stream()
                .anyMatch(u -> row.getClassName().equals(u.getClassName()));
        if (inUse) {
            throw new IllegalArgumentException("该班级仍被账号引用，不能删除");
        }
        orgClassRepository.delete(row);
        return message("班级已删除");
    }

    @PostMapping("/org/departments")
    public Map<String, Object> createDepartment(@RequestBody OrgDepartmentUpsertRequest request) {
        String departmentName = require(request.departmentName(), "部门名称不能为空");
        if (orgDepartmentRepository.findByDepartmentName(departmentName).isPresent()) {
            throw new IllegalArgumentException("部门已存在：" + departmentName);
        }
        OrgDepartment row = new OrgDepartment();
        row.setDepartmentName(departmentName);
        row.setEnabled(request.enabled() == null || request.enabled());
        orgDepartmentRepository.save(row);
        return message("部门已创建");
    }

    @PutMapping("/org/departments/{id}")
    public Map<String, Object> updateDepartment(@PathVariable Long id,
                                                @RequestBody OrgDepartmentUpsertRequest request) {
        OrgDepartment row = orgDepartmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("部门不存在"));
        String departmentName = require(request.departmentName(), "部门名称不能为空");
        Optional<OrgDepartment> existing = orgDepartmentRepository.findByDepartmentName(departmentName);
        if (existing.isPresent() && existing.get().getId().equals(id) == false) {
            throw new IllegalArgumentException("部门名称已被占用：" + departmentName);
        }

        row.setDepartmentName(departmentName);
        row.setEnabled(request.enabled() == null || request.enabled());
        orgDepartmentRepository.save(row);
        return message("部门已更新");
    }

    @DeleteMapping("/org/departments/{id}")
    public Map<String, Object> deleteDepartment(@PathVariable Long id) {
        OrgDepartment row = orgDepartmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("部门不存在"));
        boolean inUse = userRepository.findAll().stream()
                .anyMatch(u -> row.getDepartmentName().equals(u.getDepartment()));
        if (inUse) {
            throw new IllegalArgumentException("该部门仍被账号引用，不能删除");
        }
        orgDepartmentRepository.delete(row);
        return message("部门已删除");
    }

    @GetMapping("/accounts")
    public Map<String, Object> listAccounts(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) RoleType role,
                                            @RequestParam(required = false) Boolean enabled) {
        String normalizedKeyword = clean(keyword);
        List<AppUser> rows = userRepository.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> enabled == null || enabled.equals(u.getEnabled()))
                .filter(u -> {
                    if (!StringUtils.hasText(normalizedKeyword)) {
                        return true;
                    }
                    String needle = normalizedKeyword.toLowerCase();
                    return containsIgnoreCase(u.getUsername(), needle)
                            || containsIgnoreCase(u.getName(), needle)
                            || containsIgnoreCase(u.getClassName(), needle)
                            || containsIgnoreCase(u.getDepartment(), needle)
                            || containsIgnoreCase(u.getRole().name(), needle);
                })
                .sorted(Comparator.comparing(AppUser::getRole).thenComparing(AppUser::getUsername))
                .toList();

        return ok(rows.stream().map(this::toUserView).toList());
    }

    @PutMapping("/accounts/{id}/status")
    public Map<String, Object> updateAccountStatus(@PathVariable Long id,
                                                   @RequestBody AccountStatusRequest request) {
        if (request.enabled() == null) {
            throw new IllegalArgumentException("enabled 不能为空");
        }
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        user.setEnabled(request.enabled());
        userRepository.save(user);
        return message(request.enabled() ? "账号已启用" : "账号已禁用");
    }

    @PostMapping("/accounts/{id}/reset-password")
    public Map<String, Object> resetAccountPassword(@PathVariable Long id) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        user.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(user);
        return message("密码已重置为系统初始密码");
    }

    @GetMapping("/rules")
    public Map<String, Object> getCurrentRule() {
        CalcRule rule = calcRuleService.getCurrentRule();
        return ok(toRuleView(rule));
    }

    @PostMapping("/rules")
    public Map<String, Object> saveRule(@RequestBody RuleSaveRequest request) {
        BigDecimal studyWeight = requireWeight(request.studyWeight(), "智育权重不能为空");
        BigDecimal peWeight = requireWeight(request.peWeight(), "体育权重不能为空");
        BigDecimal moralCap = requireNonNegative(request.moralCap(), "德育上限不能为空");
        BigDecimal skillCap = requireNonNegative(request.skillCap(), "技能上限不能为空");

        if (studyWeight.add(peWeight).compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("智育权重 + 体育权重不能大于 1.00");
        }

        CalcRule saved = calcRuleService.saveGlobalRule(
                request.ruleName(),
                studyWeight,
                peWeight,
                moralCap,
                skillCap,
                request.remark()
        );

        Map<String, Object> body = message("规则已保存");
        body.put("data", toRuleView(saved));
        return body;
    }

    private String require(String value, String message) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private BigDecimal requireWeight(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("权重必须在 0 到 1 之间");
        }
        return value;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("上限分值不能小于 0");
        }
        return value;
    }

    private List<ScoreResult> queryScores(String className, String term) {
        String normalizedClassName = clean(className);
        String normalizedTerm = clean(term);

        if (StringUtils.hasText(normalizedClassName) && StringUtils.hasText(normalizedTerm)) {
            return scoreResultRepository.findByClassNameAndTermOrderByRankNoAsc(normalizedClassName, normalizedTerm);
        }
        if (StringUtils.hasText(normalizedTerm)) {
            return scoreResultRepository.findByTermOrderByClassNameAscRankNoAsc(normalizedTerm);
        }
        if (StringUtils.hasText(normalizedClassName)) {
            return scoreResultRepository.findByClassNameOrderByTermDescRankNoAsc(normalizedClassName);
        }
        return List.of();
    }

    private boolean looksLikeStudentHeader(String line) {
        String normalized = line.toLowerCase();
        return normalized.contains("student")
                || normalized.contains("username")
                || normalized.contains("class")
                || line.contains("学号")
                || line.contains("班级");
    }

    private Map<String, Object> importError(int lineNo, String reason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lineNo", lineNo);
        row.put("reason", reason);
        return row;
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String csvContent) {
        byte[] content = ("\uFEFF" + csvContent).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename(filename) + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }

    private String safeFilename(String filename) {
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean containsIgnoreCase(String raw, String needle) {
        return raw != null && raw.toLowerCase().contains(needle);
    }

    private String currentUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
        return StringUtils.hasText(username) ? username : "admin";
    }

    private String resolveCounselorClass(RoleType role, String className) {
        if (role == RoleType.COUNSELOR) {
            return require(className, "辅导员必须绑定负责班级");
        }
        return null;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }

        values.add(current.toString());
        return values.toArray(new String[0]);
    }

    private void validateTeacherRole(RoleType role) {
        if (role == null || MANAGED_TEACHER_ROLES.contains(role) == false) {
            throw new IllegalArgumentException("角色必须是体育老师/学习部老师/辅导员");
        }
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

    private Map<String, Object> toUserView(AppUser user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("name", user.getName());
        view.put("role", user.getRole().name());
        view.put("className", user.getClassName());
        view.put("department", user.getDepartment());
        view.put("enabled", user.getEnabled());
        view.put("createdAt", user.getCreatedAt());
        view.put("updatedAt", user.getUpdatedAt());
        return view;
    }

    private Map<String, Object> toClassView(OrgClass row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", row.getId());
        view.put("className", row.getClassName());
        view.put("majorName", row.getMajorName());
        view.put("enabled", row.getEnabled());
        view.put("createdAt", row.getCreatedAt());
        view.put("updatedAt", row.getUpdatedAt());
        return view;
    }

    private Map<String, Object> toDepartmentView(OrgDepartment row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", row.getId());
        view.put("departmentName", row.getDepartmentName());
        view.put("enabled", row.getEnabled());
        view.put("createdAt", row.getCreatedAt());
        view.put("updatedAt", row.getUpdatedAt());
        return view;
    }

    private Map<String, Object> toRuleView(CalcRule rule) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", rule.getId());
        view.put("ruleName", rule.getRuleName());
        view.put("studyWeight", rule.getStudyWeight());
        view.put("peWeight", rule.getPeWeight());
        view.put("moralCap", rule.getMoralCap());
        view.put("skillCap", rule.getSkillCap());
        view.put("remark", rule.getRemark());
        view.put("enabled", rule.getEnabled());
        view.put("createdAt", rule.getCreatedAt());
        view.put("updatedAt", rule.getUpdatedAt());
        return view;
    }

    public record StudentUpsertRequest(@NotBlank String username,
                                       @NotBlank String name,
                                       @NotBlank String className,
                                       Boolean enabled) {
    }

    public record TeacherUpsertRequest(@NotBlank String username,
                                       @NotBlank String name,
                                       RoleType role,
                                       String department,
                                       String className,
                                       Boolean enabled) {
    }

    public record OrgClassUpsertRequest(@NotBlank String className,
                                        String majorName,
                                        Boolean enabled) {
    }

    public record OrgDepartmentUpsertRequest(@NotBlank String departmentName,
                                             Boolean enabled) {
    }

    public record AccountStatusRequest(Boolean enabled) {
    }

    public record RuleSaveRequest(String ruleName,
                                  BigDecimal studyWeight,
                                  BigDecimal peWeight,
                                  BigDecimal moralCap,
                                  BigDecimal skillCap,
                                  String remark) {
    }
}
