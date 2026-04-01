package com.zongce.system.api.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.Declaration;
import com.zongce.system.entity.ScoreResult;
import com.zongce.system.entity.enums.DeclarationStatus;
import com.zongce.system.entity.enums.DeclarationType;
import com.zongce.system.repository.DeclarationRepository;
import com.zongce.system.repository.ScoreResultRepository;
import com.zongce.system.service.CurrentUserService;
import com.zongce.system.service.NotificationService;
import com.zongce.system.service.ScoreRecalcService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/counselor")
@Validated
public class CounselorApiController {

    private final CurrentUserService currentUserService;
    private final DeclarationRepository declarationRepository;
    private final ScoreRecalcService scoreRecalcService;
    private final ScoreResultRepository scoreResultRepository;
    private final NotificationService notificationService;

    public CounselorApiController(CurrentUserService currentUserService,
                                  DeclarationRepository declarationRepository,
                                  ScoreRecalcService scoreRecalcService,
                                  ScoreResultRepository scoreResultRepository,
                                  NotificationService notificationService) {
        this.currentUserService = currentUserService;
        this.declarationRepository = declarationRepository;
        this.scoreRecalcService = scoreRecalcService;
        this.scoreResultRepository = scoreResultRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/reviews")
    public Map<String, Object> listReviews(@RequestParam(defaultValue = "PENDING") DeclarationStatus status,
                                           @RequestParam(required = false) DeclarationType type) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        String scopedClass = requireScopedClass(counselor);
        List<Declaration> rows = (type == null)
            ? declarationRepository.findByStatusAndClassNameOrderByCreatedAtAsc(status, scopedClass)
            : declarationRepository.findByStatusAndClassNameAndTypeOrderByCreatedAtAsc(status, scopedClass, type);

        Map<String, Object> body = ok(rows);
        body.put("scopedClass", scopedClass);
        return body;
    }

    @PostMapping("/reviews/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        Declaration row = declarationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        ensureCounselorDataScope(counselor, row);
        ensurePending(row);
        row.setStatus(DeclarationStatus.APPROVED);
        row.setReviewer(counselor.getUsername());
        row.setReviewComment("审核通过");
        declarationRepository.save(row);
        String declarationBizRef = "DECLARATION:" + row.getId();
        notificationService.markReadByBizRef(counselor.getUsername(), "DECLARATION_SUBMITTED", declarationBizRef);
        notificationService.createForUser(
                row.getStudentNo(),
                "申报审核通过",
                row.getItemName() + " 已审核通过",
                "DECLARATION_REVIEW",
                "/student/declare/history"
        );
        return message("审核通过");
    }

    @PostMapping("/reviews/{id}/reject")
    public Map<String, Object> reject(@PathVariable Long id,
                                      @RequestBody(required = false) RejectRequest request) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        Declaration row = declarationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        ensureCounselorDataScope(counselor, row);
        ensurePending(row);
        String comment = (request == null || request.comment() == null || request.comment().isBlank())
                ? "材料不完整"
                : request.comment().trim();
        row.setStatus(DeclarationStatus.REJECTED);
        row.setReviewer(counselor.getUsername());
        row.setReviewComment(comment);
        declarationRepository.save(row);
        String declarationBizRef = "DECLARATION:" + row.getId();
        notificationService.markReadByBizRef(counselor.getUsername(), "DECLARATION_SUBMITTED", declarationBizRef);
        notificationService.createForUser(
                row.getStudentNo(),
                "申报被驳回",
                row.getItemName() + " 被驳回，原因：" + comment,
                "DECLARATION_REVIEW",
                "/student/declare/history"
        );
        return message("已驳回");
    }

    @GetMapping("/class-summary")
    public Map<String, Object> classSummary(@RequestParam(required = false) String className,
                                            @RequestParam(required = false) String term) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        String scopedClass = requireScopedClass(counselor);
        String finalClass = scopedClass;
        String finalTerm = StringUtils.hasText(term) ? term.trim() : Year.now().getValue() + "-1";

        List<ScoreResult> rows = scoreResultRepository.findByClassNameAndTermOrderByRankNoAsc(finalClass, finalTerm);
        Map<String, Object> body = ok(rows);
        body.put("className", finalClass);
        body.put("term", finalTerm);
        body.put("scopedClass", scopedClass);
        return body;
    }

    @PostMapping("/class-summary/recalc")
    public Map<String, Object> recalc(@RequestBody RecalcRequest request) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        String scopedClass = requireScopedClass(counselor);
        String finalClass = scopedClass;
        String term = require(request.term(), "学期不能为空");

        if (StringUtils.hasText(scopedClass) && StringUtils.hasText(request.className())
                && scopedClass.equals(request.className().trim()) == false) {
            throw new IllegalArgumentException("你只能重算自己负责的班级：" + scopedClass);
        }

        scoreRecalcService.recalcClass(finalClass, term);
        notificationService.createForStudentClass(
                finalClass,
                "综测成绩已重算",
                "班级 " + finalClass + " 在 " + term + " 的综测已重算完成",
                "SCORE_RECALC",
                "/student/my-score"
        );
        notificationService.createForUser(
                counselor.getUsername(),
                "班级重算完成",
                "你发起的班级 " + finalClass + "（" + term + "）重算已完成",
                "SCORE_RECALC",
                "/counselor/class-summary"
        );
        return message("重算完成");
    }

    @GetMapping("/class-summary/export")
    public ResponseEntity<byte[]> exportClassSummary(@RequestParam(required = false) String className,
                                                     @RequestParam(required = false) String term) {
        AppUser counselor = currentUserService.mustGetCurrentUser();
        String scopedClass = requireScopedClass(counselor);
        String finalClass = scopedClass;
        String finalTerm = StringUtils.hasText(term) ? term.trim() : Year.now().getValue() + "-1";

        List<ScoreResult> rows = scoreResultRepository.findByClassNameAndTermOrderByRankNoAsc(finalClass, finalTerm);
        String filename = ("class-summary-" + finalClass + "-" + finalTerm + ".csv").replaceAll("[\\\\/:*?\"<>|]", "_");

        StringBuilder csv = new StringBuilder();
        csv.append("term,className,rankNo,studentNo,studentName,studyScore,peScore,moralScore,skillScore,totalScore\n");
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
                    .append(csvCell(row.getTotalScore()))
                    .append('\n');
        }

        byte[] content = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }

    private void ensureCounselorDataScope(AppUser counselor, Declaration row) {
        String scopedClass = requireScopedClass(counselor);
        if (scopedClass.equals(row.getClassName()) == false) {
            throw new IllegalArgumentException("无权审核其他班级申报");
        }
    }

    private String requireScopedClass(AppUser counselor) {
        String scopedClass = clean(counselor.getClassName());
        if (!StringUtils.hasText(scopedClass)) {
            throw new IllegalArgumentException("当前辅导员账号未绑定班级，请联系管理员配置");
        }
        return scopedClass;
    }

    private void ensurePending(Declaration row) {
        if (row.getStatus() != DeclarationStatus.PENDING) {
            throw new IllegalArgumentException("该申报已审核，不能重复操作");
        }
    }

    private String require(String value, String message) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    public record RejectRequest(String comment) {
    }

    public record RecalcRequest(String className, @NotBlank String term) {
    }
}
