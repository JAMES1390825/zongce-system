package com.zongce.system.api.controller;

import com.zongce.system.entity.ScoreEntry;
import com.zongce.system.entity.ScoreItemCatalog;
import com.zongce.system.entity.ScoreStudy;
import com.zongce.system.entity.enums.ScoreEntrySource;
import com.zongce.system.entity.enums.ScoreEntryStatus;
import com.zongce.system.entity.enums.ScoreItemCategory;
import com.zongce.system.repository.ScoreEntryRepository;
import com.zongce.system.repository.ScoreItemCatalogRepository;
import com.zongce.system.repository.ScoreStudyRepository;
import com.zongce.system.service.CurrentUserService;
import com.zongce.system.service.NotificationService;
import com.zongce.system.service.ScoreEntryAggregateService;
import com.zongce.system.service.ScoreImportService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/study")
@Validated
public class TeacherStudyApiController {

    private final ScoreImportService scoreImportService;
    private final ScoreEntryRepository scoreEntryRepository;
    private final ScoreItemCatalogRepository scoreItemCatalogRepository;
    private final ScoreStudyRepository scoreStudyRepository;
    private final ScoreEntryAggregateService scoreEntryAggregateService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public TeacherStudyApiController(ScoreImportService scoreImportService,
                                     ScoreEntryRepository scoreEntryRepository,
                                     ScoreItemCatalogRepository scoreItemCatalogRepository,
                                     ScoreStudyRepository scoreStudyRepository,
                                     ScoreEntryAggregateService scoreEntryAggregateService,
                                     CurrentUserService currentUserService,
                                     NotificationService notificationService) {
        this.scoreImportService = scoreImportService;
        this.scoreEntryRepository = scoreEntryRepository;
        this.scoreItemCatalogRepository = scoreItemCatalogRepository;
        this.scoreStudyRepository = scoreStudyRepository;
        this.scoreEntryAggregateService = scoreEntryAggregateService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @GetMapping("/items")
    public Map<String, Object> listItems() {
        List<ScoreItemCatalog> rows = scoreItemCatalogRepository
                .findByCategoryInAndEnabledTrueOrderByCategoryAscDisplayOrderAscItemCodeAsc(List.of(ScoreItemCategory.STUDY))
                .stream()
                .filter(item -> isTeacherSelectableItem(item, ScoreItemCategory.STUDY))
                .toList();
        List<Map<String, Object>> data = rows.stream().map(this::toItemView).toList();
        return ok(data);
    }

    @PostMapping("/scores/import")
    public Map<String, Object> importScores(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择CSV文件");
        }
        String operator = currentUserService.mustGetCurrentUser().getUsername();
        ScoreImportService.ImportResult result = scoreImportService.importStudyScores(file, operator);
        notificationService.createForUser(
                operator,
                "智育成绩导入完成",
                "批次 " + result.batchNo() + "：成功 " + result.success() + "，失败 " + result.failed(),
                "STUDY_IMPORT",
                "/teacher/study/import"
        );

        Map<String, Object> body = message("导入完成：成功 " + result.success() + "，失败 " + result.failed());
        body.put("data", result);
        return body;
    }

    @PostMapping("/scores")
    public Map<String, Object> createScore(@RequestBody ScoreUpsertRequest request) {
        upsertEntry(new ScoreEntry(), request);
        return message("智育成绩录入成功");
    }

    @PutMapping("/scores/{id}")
    public Map<String, Object> updateScore(@PathVariable Long id,
                                           @RequestBody ScoreUpsertRequest request) {
        ScoreEntry entry = scoreEntryRepository.findById(id)
                .orElseThrow(() -> scoreStudyRepository.findById(id).isPresent()
                        ? new IllegalArgumentException("智育总分由系统自动汇总，请修改具体科目成绩")
                        : new IllegalArgumentException("记录不存在"));
        upsertEntry(entry, request);
        return message("智育成绩修改成功");
    }

    @GetMapping("/scores/{id}")
    public Map<String, Object> getScore(@PathVariable Long id) {
        return scoreEntryRepository.findById(id)
                .map(entry -> ok(toEntryView(entry)))
                .orElseGet(() -> scoreStudyRepository.findById(id)
                        .map(this::toLegacyTotalView)
                        .map(this::ok)
                        .orElseThrow(() -> new IllegalArgumentException("记录不存在")));
    }

    @GetMapping("/scores")
    public Map<String, Object> listScores(@RequestParam(required = false) String className,
                                          @RequestParam(required = false) String term) {
        String cls = clean(className);
        String trm = clean(term);
        List<ScoreEntry> rows = queryEntries(cls, trm);
        if (!rows.isEmpty()) {
            return ok(rows.stream().map(this::toEntryView).toList());
        }
        return ok(queryLegacyRows(cls, trm).stream().map(this::toLegacyTotalView).toList());
    }

    private List<ScoreEntry> queryEntries(String className, String term) {
        if (StringUtils.hasText(className) && StringUtils.hasText(term)) {
            return scoreEntryRepository.findByCategoryAndClassNameAndTermOrderByStudentNoAscItemCodeAsc(
                    ScoreItemCategory.STUDY, className, term
            );
        }
        if (StringUtils.hasText(term)) {
            return scoreEntryRepository.findByCategoryAndTermOrderByClassNameAscStudentNoAscItemCodeAsc(
                    ScoreItemCategory.STUDY, term
            );
        }
        if (StringUtils.hasText(className)) {
            return scoreEntryRepository.findByCategoryAndClassNameOrderByStudentNoAscTermAscItemCodeAsc(
                    ScoreItemCategory.STUDY, className
            );
        }
        return scoreEntryRepository.findByCategoryOrderByClassNameAscStudentNoAscTermAscItemCodeAsc(ScoreItemCategory.STUDY);
    }

    private List<ScoreStudy> queryLegacyRows(String className, String term) {
        if (StringUtils.hasText(className) && StringUtils.hasText(term)) {
            return scoreStudyRepository.findByClassNameAndTermOrderByStudentNoAsc(className, term);
        }
        if (StringUtils.hasText(term)) {
            return scoreStudyRepository.findByTermOrderByClassNameAscStudentNoAsc(term);
        }
        return scoreStudyRepository.findAll().stream()
                .filter(row -> StringUtils.hasText(className) == false || className.equals(row.getClassName()))
                .toList();
    }

    private void upsertEntry(ScoreEntry entry, ScoreUpsertRequest request) {
        BigDecimal score = request.score();
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("智育分必须大于等于0");
        }
        String studentNo = require(request.studentNo(), "学号不能为空");
        String studentName = require(request.studentName(), "姓名不能为空");
        String className = require(request.className(), "班级不能为空");
        String term = require(request.term(), "学期不能为空");

        String operator = currentUserService.mustGetCurrentUser().getUsername();
        String itemCode = clean(request.itemCode());
        if (!StringUtils.hasText(itemCode)) {
            throw new IllegalArgumentException("成绩项目不能为空");
        }
        ScoreItemCatalog item = requireItem(itemCode);
        if (isTotalItem(item, ScoreItemCategory.STUDY)) {
            throw new IllegalArgumentException("不允许直接录入智育总分，请录入分项成绩");
        }

        entry.setStudentNo(studentNo);
        entry.setStudentName(studentName);
        entry.setClassName(className);
        entry.setTerm(term);
        entry.setItemCode(item.getItemCode());
        entry.setItemNameSnapshot(item.getItemName());
        entry.setCategory(ScoreItemCategory.STUDY);
        entry.setRawScore(score);
        entry.setStandardScore(score);
        entry.setStatus(ScoreEntryStatus.RECORDED);
        entry.setSource(ScoreEntrySource.MANUAL);
        entry.setCreatedBy(operator);
        scoreEntryRepository.save(entry);

        scoreEntryAggregateService.syncTotalFromEntries(
                ScoreItemCategory.STUDY,
                studentNo,
                studentName,
                className,
                term,
                operator
        );
    }

    private ScoreItemCatalog requireItem(String itemCode) {
        ScoreItemCatalog item = scoreItemCatalogRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new IllegalArgumentException("成绩项目不存在：" + itemCode));
        if (item.getCategory() != ScoreItemCategory.STUDY) {
            throw new IllegalArgumentException("项目不属于智育分类");
        }
        return item;
    }

    private Map<String, Object> toItemView(ScoreItemCatalog row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("itemCode", row.getItemCode());
        view.put("itemName", row.getItemName());
        view.put("requiredFlag", row.getRequiredFlag());
        view.put("displayOrder", row.getDisplayOrder());
        return view;
    }

    private Map<String, Object> toEntryView(ScoreEntry row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", row.getId());
        view.put("studentNo", row.getStudentNo());
        view.put("studentName", row.getStudentName());
        view.put("className", row.getClassName());
        view.put("term", row.getTerm());
        view.put("itemCode", row.getItemCode());
        view.put("itemName", row.getItemNameSnapshot());
        view.put("score", row.getStandardScore());
        view.put("totalScore", resolveTotalScore(row.getStudentNo(), row.getTerm(), row.getStandardScore()));
        view.put("rawScore", row.getRawScore());
        view.put("status", row.getStatus().name());
        view.put("source", row.getSource().name());
        view.put("createdBy", row.getCreatedBy());
        view.put("updatedAt", row.getUpdatedAt());
        return view;
    }

    private Map<String, Object> toLegacyTotalView(ScoreStudy row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", row.getId());
        view.put("studentNo", row.getStudentNo());
        view.put("studentName", row.getStudentName());
        view.put("className", row.getClassName());
        view.put("term", row.getTerm());
        view.put("itemCode", ScoreEntryAggregateService.DEFAULT_STUDY_ITEM_CODE);
        view.put("itemName", "智育总分");
        view.put("score", row.getScore());
        view.put("totalScore", row.getScore());
        view.put("rawScore", row.getScore());
        view.put("status", ScoreEntryStatus.RECORDED.name());
        view.put("source", ScoreEntrySource.MIGRATION.name());
        view.put("createdBy", row.getCreatedBy());
        view.put("updatedAt", row.getUpdatedAt());
        return view;
    }

    private BigDecimal resolveTotalScore(String studentNo, String term, BigDecimal fallback) {
        return scoreStudyRepository.findTopByStudentNoAndTermOrderByIdDesc(studentNo, term)
                .map(ScoreStudy::getScore)
                .orElse(fallback);
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

    private boolean isTeacherSelectableItem(ScoreItemCatalog item, ScoreItemCategory expectedCategory) {
        if (item == null || item.getCategory() != expectedCategory) {
            return false;
        }
        return !isTotalItem(item, expectedCategory);
    }

    private boolean isTotalItem(ScoreItemCatalog item, ScoreItemCategory category) {
        String normalizedCode = normalizeCode(item == null ? null : item.getItemCode());
        String normalizedDefault = normalizeCode(scoreEntryAggregateService.totalCode(category));
        if (normalizedCode.equals(normalizedDefault) || normalizedCode.endsWith("-TOTAL")) {
            return true;
        }
        String itemName = item == null ? null : item.getItemName();
        return StringUtils.hasText(itemName) && itemName.contains("总分");
    }

    private String normalizeCode(String itemCode) {
        if (!StringUtils.hasText(itemCode)) {
            return "";
        }
        return itemCode.trim()
                .replace('_', '-')
                .toUpperCase(Locale.ROOT);
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

    public record ScoreUpsertRequest(@NotBlank String studentNo,
                                     @NotBlank String studentName,
                                     @NotBlank String className,
                                     @NotBlank String term,
                                     String itemCode,
                                     BigDecimal score) {
    }
}
