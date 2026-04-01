package com.zongce.system.api.controller;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.Declaration;
import com.zongce.system.entity.PeScoreComponent;
import com.zongce.system.entity.ScoreEntry;
import com.zongce.system.entity.ScoreItemCatalog;
import com.zongce.system.entity.ScorePe;
import com.zongce.system.entity.ScoreResult;
import com.zongce.system.entity.ScoreStudy;
import com.zongce.system.entity.enums.DeclarationStatus;
import com.zongce.system.entity.enums.DeclarationType;
import com.zongce.system.entity.enums.ScoreItemCategory;
import com.zongce.system.repository.DeclarationRepository;
import com.zongce.system.repository.PeScoreComponentRepository;
import com.zongce.system.repository.ScoreEntryRepository;
import com.zongce.system.repository.ScoreItemCatalogRepository;
import com.zongce.system.repository.ScorePeRepository;
import com.zongce.system.repository.ScoreResultRepository;
import com.zongce.system.repository.ScoreStudyRepository;
import com.zongce.system.service.CurrentUserService;
import com.zongce.system.service.FileStorageService;
import com.zongce.system.service.NotificationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/student")
@Validated
public class StudentApiController {

    private static final String DEFAULT_STUDY_ITEM_CODE = "STUDY-TOTAL";
    private static final String DEFAULT_PE_ITEM_CODE = "PE-TOTAL";

    private final CurrentUserService currentUserService;
    private final DeclarationRepository declarationRepository;
    private final ScoreResultRepository scoreResultRepository;
    private final ScoreEntryRepository scoreEntryRepository;
    private final ScoreItemCatalogRepository scoreItemCatalogRepository;
    private final ScoreStudyRepository scoreStudyRepository;
    private final ScorePeRepository scorePeRepository;
    private final PeScoreComponentRepository peScoreComponentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public StudentApiController(CurrentUserService currentUserService,
                                DeclarationRepository declarationRepository,
                                ScoreResultRepository scoreResultRepository,
                                ScoreEntryRepository scoreEntryRepository,
                                ScoreItemCatalogRepository scoreItemCatalogRepository,
                                ScoreStudyRepository scoreStudyRepository,
                                ScorePeRepository scorePeRepository,
                                PeScoreComponentRepository peScoreComponentRepository,
                                FileStorageService fileStorageService,
                                NotificationService notificationService) {
        this.currentUserService = currentUserService;
        this.declarationRepository = declarationRepository;
        this.scoreResultRepository = scoreResultRepository;
        this.scoreEntryRepository = scoreEntryRepository;
        this.scoreItemCatalogRepository = scoreItemCatalogRepository;
        this.scoreStudyRepository = scoreStudyRepository;
        this.scorePeRepository = scorePeRepository;
        this.peScoreComponentRepository = peScoreComponentRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    @PostMapping("/declarations/moral")
    public Map<String, Object> submitMoral(@RequestParam @NotBlank String term,
                                           @RequestParam(required = false) String itemCode,
                                           @RequestParam @NotBlank String itemName,
                                           @RequestParam BigDecimal points,
                                           @RequestParam(required = false) String description,
                                           @RequestParam(required = false) MultipartFile attachment) {
        submitDeclaration(DeclarationType.MORAL, term, itemCode, itemName, points, description, attachment);
        return message("德育申报提交成功");
    }

    @PostMapping("/declarations/skill")
    public Map<String, Object> submitSkill(@RequestParam @NotBlank String term,
                                           @RequestParam(required = false) String itemCode,
                                           @RequestParam @NotBlank String itemName,
                                           @RequestParam BigDecimal points,
                                           @RequestParam(required = false) String description,
                                           @RequestParam(required = false) MultipartFile attachment) {
        submitDeclaration(DeclarationType.SKILL, term, itemCode, itemName, points, description, attachment);
        return message("技能申报提交成功");
    }

    @GetMapping("/declarations")
    public Map<String, Object> listDeclarations() {
        AppUser student = currentUserService.mustGetCurrentUser();
        List<Declaration> rows = declarationRepository.findByStudentNoOrderByCreatedAtDesc(student.getUsername());
        return ok(rows);
    }

    @GetMapping("/declaration-items")
    public Map<String, Object> listDeclarationItems(@RequestParam(required = false) DeclarationType type) {
        Collection<ScoreItemCategory> categories;
        if (type == DeclarationType.MORAL) {
            categories = List.of(ScoreItemCategory.MORAL);
        } else if (type == DeclarationType.SKILL) {
            categories = List.of(ScoreItemCategory.SKILL);
        } else {
            categories = List.of(ScoreItemCategory.MORAL, ScoreItemCategory.SKILL);
        }

        List<ScoreItemCatalog> rows = scoreItemCatalogRepository
                .findByCategoryInAndEnabledTrueOrderByCategoryAscDisplayOrderAscItemCodeAsc(categories);
        List<Map<String, Object>> data = rows.stream().map(this::toScoreItemView).toList();
        return ok(data);
    }

    @GetMapping("/scores")
    public Map<String, Object> listScores() {
        AppUser student = currentUserService.mustGetCurrentUser();
        List<ScoreResult> rows = scoreResultRepository.findByStudentNoOrderByTermDesc(student.getUsername());
        return ok(rows);
    }

    @GetMapping("/transcript")
    public Map<String, Object> transcript(@RequestParam(required = false) String term) {
        AppUser student = currentUserService.mustGetCurrentUser();
        String resolvedTerm = resolveTerm(student.getUsername(), clean(term));

        TranscriptPayload payload = buildTranscriptPayload(student, resolvedTerm);
        return ok(payload.toMap());
    }

    @GetMapping("/transcript/pe-breakdown")
    public Map<String, Object> peBreakdown(@RequestParam(required = false) String term) {
        AppUser student = currentUserService.mustGetCurrentUser();
        String resolvedTerm = resolveTerm(student.getUsername(), clean(term));

        List<Map<String, Object>> rows = buildPeBreakdownRows(student, resolvedTerm);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("term", resolvedTerm);
        data.put("rows", rows);
        data.put("totalWeightedScore", sumWeightedScore(rows));
        return ok(data);
    }

    @GetMapping("/transcript/history")
    public Map<String, Object> transcriptHistory(@RequestParam @NotBlank String itemCode) {
        AppUser student = currentUserService.mustGetCurrentUser();
        String code = require(itemCode, "itemCode 不能为空");

        List<ScoreEntry> rows = scoreEntryRepository.findByStudentNoAndItemCodeOrderByTermDesc(student.getUsername(), code);
        List<Map<String, Object>> data = new ArrayList<>();
        if (!rows.isEmpty()) {
            for (ScoreEntry row : rows) {
                data.add(toHistoryView(row.getTerm(), row.getRawScore(), row.getStandardScore(), row.getStatus().name(), row.getUpdatedAt()));
            }
            return ok(data);
        }

        if (DEFAULT_STUDY_ITEM_CODE.equals(code) || DEFAULT_PE_ITEM_CODE.equals(code)) {
            List<ScoreResult> resultRows = scoreResultRepository.findByStudentNoOrderByTermDesc(student.getUsername());
            for (ScoreResult result : resultRows) {
                BigDecimal score = DEFAULT_STUDY_ITEM_CODE.equals(code) ? result.getStudyScore() : result.getPeScore();
                data.add(toHistoryView(result.getTerm(), score, score, "RECORDED", result.getUpdatedAt()));
            }
        }

        return ok(data);
    }

    @GetMapping("/transcript/export")
    public ResponseEntity<byte[]> exportTranscript(@RequestParam(required = false) String term) {
        AppUser student = currentUserService.mustGetCurrentUser();
        String resolvedTerm = resolveTerm(student.getUsername(), clean(term));

        TranscriptPayload payload = buildTranscriptPayload(student, resolvedTerm);
        List<Map<String, Object>> peBreakdownRows = buildPeBreakdownRows(student, resolvedTerm);

        StringBuilder csv = new StringBuilder();
        csv.append("section,term,itemCode,itemName,category,required,status,rawScore,standardScore,source,remark\n");
        for (Map<String, Object> row : payload.studyItems()) {
            csv.append(csvLine("STUDY", resolvedTerm, row));
        }
        for (Map<String, Object> row : payload.peItems()) {
            csv.append(csvLine("PE", resolvedTerm, row));
        }
        for (Map<String, Object> row : peBreakdownRows) {
            csv.append(csvCell("PE_BREAKDOWN")).append(',')
                    .append(csvCell(resolvedTerm)).append(',')
                    .append(csvCell(row.get("componentCode"))).append(',')
                    .append(csvCell(row.get("componentName"))).append(',')
                    .append(csvCell("PE")).append(',')
                    .append(csvCell("")).append(',')
                    .append(csvCell("")).append(',')
                    .append(csvCell(row.get("rawValue"))).append(',')
                    .append(csvCell(row.get("convertedScore"))).append(',')
                    .append(csvCell(row.get("weight"))).append(',')
                    .append(csvCell(row.get("formulaSnapshot")))
                    .append('\n');
        }

        byte[] content = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        String filename = ("transcript-" + student.getUsername() + "-" + resolvedTerm + ".csv")
                .replaceAll("[\\\\/:*?\"<>|]", "_");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }

    private TranscriptPayload buildTranscriptPayload(AppUser student, String term) {
        List<ScoreItemCatalog> catalogRows = scoreItemCatalogRepository
                .findByCategoryInAndEnabledTrueOrderByCategoryAscDisplayOrderAscItemCodeAsc(
                        List.of(ScoreItemCategory.STUDY, ScoreItemCategory.PE)
                );
        List<ScoreEntry> entryRows = scoreEntryRepository.findByStudentNoAndTermOrderByCategoryAscItemCodeAsc(
                student.getUsername(), term
        );

        Map<String, ScoreEntry> entryByCode = new LinkedHashMap<>();
        for (ScoreEntry row : entryRows) {
            entryByCode.put(row.getItemCode(), row);
        }

        Optional<ScoreStudy> latestStudy = scoreStudyRepository.findTopByStudentNoAndTermOrderByIdDesc(student.getUsername(), term);
        Optional<ScorePe> latestPe = scorePeRepository.findTopByStudentNoAndTermOrderByIdDesc(student.getUsername(), term);

        List<Map<String, Object>> studyItems = new ArrayList<>();
        List<Map<String, Object>> peItems = new ArrayList<>();
        Set<String> visitedCodes = new LinkedHashSet<>();

        int requiredCount = 0;
        int requiredRecordedCount = 0;
        List<Map<String, Object>> missingRequiredItems = new ArrayList<>();

        for (ScoreItemCatalog item : catalogRows) {
            ScoreEntry entry = entryByCode.get(item.getItemCode());
            boolean missing = entry == null;
            String status = missing ? "MISSING" : entry.getStatus().name();

            if (Boolean.TRUE.equals(item.getRequiredFlag())) {
                requiredCount++;
                if (!missing && !"MISSING".equals(status)) {
                    requiredRecordedCount++;
                } else {
                    missingRequiredItems.add(Map.of(
                            "itemCode", item.getItemCode(),
                            "itemName", item.getItemName(),
                            "category", item.getCategory().name()
                    ));
                }
            }

            Map<String, Object> view = toTranscriptItemView(
                    item.getItemCode(),
                    item.getItemName(),
                    item.getCategory().name(),
                    item.getRequiredFlag(),
                    status,
                    entry == null ? null : entry.getRawScore(),
                    entry == null ? null : entry.getStandardScore(),
                    entry == null ? null : entry.getSource().name(),
                    entry == null ? null : entry.getUpdatedAt(),
                    false
            );
            if (item.getCategory() == ScoreItemCategory.STUDY) {
                studyItems.add(view);
            } else if (item.getCategory() == ScoreItemCategory.PE) {
                peItems.add(view);
            }
            visitedCodes.add(item.getItemCode());
        }

        for (ScoreEntry entry : entryRows) {
            if (visitedCodes.contains(entry.getItemCode())) {
                continue;
            }
            Map<String, Object> view = toTranscriptItemView(
                    entry.getItemCode(),
                    entry.getItemNameSnapshot(),
                    entry.getCategory().name(),
                    false,
                    entry.getStatus().name(),
                    entry.getRawScore(),
                    entry.getStandardScore(),
                    entry.getSource().name(),
                    entry.getUpdatedAt(),
                    false
            );
            if (entry.getCategory() == ScoreItemCategory.STUDY) {
                studyItems.add(view);
            } else if (entry.getCategory() == ScoreItemCategory.PE) {
                peItems.add(view);
            }
        }

        if (studyItems.stream().noneMatch(i -> DEFAULT_STUDY_ITEM_CODE.equals(i.get("itemCode"))) && latestStudy.isPresent()) {
            ScoreStudy study = latestStudy.get();
            studyItems.add(toTranscriptItemView(
                    DEFAULT_STUDY_ITEM_CODE,
                    "智育总分（历史聚合）",
                    ScoreItemCategory.STUDY.name(),
                    false,
                    "RECORDED",
                    study.getScore(),
                    study.getScore(),
                    "MIGRATION",
                    study.getUpdatedAt(),
                    true
            ));
        }
        if (peItems.stream().noneMatch(i -> DEFAULT_PE_ITEM_CODE.equals(i.get("itemCode"))) && latestPe.isPresent()) {
            ScorePe pe = latestPe.get();
            peItems.add(toTranscriptItemView(
                    DEFAULT_PE_ITEM_CODE,
                    "体育总分（历史聚合）",
                    ScoreItemCategory.PE.name(),
                    false,
                    "RECORDED",
                    pe.getScore(),
                    pe.getScore(),
                    "MIGRATION",
                    pe.getUpdatedAt(),
                    true
            ));
        }

        Optional<ScoreResult> summaryOpt = scoreResultRepository.findByStudentNoAndTerm(student.getUsername(), term);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("term", term);
        summary.put("studyScore", summaryOpt.map(ScoreResult::getStudyScore).orElse(latestStudy.map(ScoreStudy::getScore).orElse(BigDecimal.ZERO)));
        summary.put("peScore", summaryOpt.map(ScoreResult::getPeScore).orElse(latestPe.map(ScorePe::getScore).orElse(BigDecimal.ZERO)));
        summary.put("moralScore", summaryOpt.map(ScoreResult::getMoralScore).orElse(BigDecimal.ZERO));
        summary.put("skillScore", summaryOpt.map(ScoreResult::getSkillScore).orElse(BigDecimal.ZERO));
        summary.put("totalScore", summaryOpt.map(ScoreResult::getTotalScore).orElse(BigDecimal.ZERO));
        summary.put("rankNo", summaryOpt.map(ScoreResult::getRankNo).orElse(null));

        BigDecimal completionRate = requiredCount == 0
                ? new BigDecimal("100.00")
                : new BigDecimal(requiredRecordedCount)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(requiredCount), 2, RoundingMode.HALF_UP);
        String resultStatus = missingRequiredItems.isEmpty() ? "COMPLETE" : "INCOMPLETE";
        summary.put("resultStatus", resultStatus);
        summary.put("completionRate", completionRate);

        return new TranscriptPayload(term, summary, studyItems, peItems, missingRequiredItems, requiredCount, requiredRecordedCount, completionRate, resultStatus);
    }

    private List<Map<String, Object>> buildPeBreakdownRows(AppUser student, String term) {
        List<PeScoreComponent> componentRows = peScoreComponentRepository.findByStudentNoAndTermOrderByIdAsc(student.getUsername(), term);
        if (!componentRows.isEmpty()) {
            return componentRows.stream().map(row -> {
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("componentCode", row.getComponentCode());
                view.put("componentName", row.getComponentName());
                view.put("rawValue", row.getRawValue());
                view.put("convertedScore", row.getConvertedScore());
                view.put("weight", row.getWeight());
                view.put("weightedScore", row.getWeightedScore());
                view.put("formulaSnapshot", row.getFormulaSnapshot());
                return view;
            }).toList();
        }

        List<ScoreEntry> entryRows = scoreEntryRepository.findByStudentNoAndTermOrderByCategoryAscItemCodeAsc(student.getUsername(), term);
        List<Map<String, Object>> fromPeEntries = new ArrayList<>();
        for (ScoreEntry entry : entryRows) {
            if (entry.getCategory() != ScoreItemCategory.PE) {
                continue;
            }
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("componentCode", entry.getItemCode());
            view.put("componentName", entry.getItemNameSnapshot());
            view.put("rawValue", entry.getRawScore());
            view.put("convertedScore", entry.getStandardScore());
            view.put("weight", null);
            view.put("weightedScore", entry.getStandardScore());
            view.put("formulaSnapshot", "当前学期未配置分项公式，按录入成绩展示");
            fromPeEntries.add(view);
        }
        if (!fromPeEntries.isEmpty()) {
            return fromPeEntries;
        }

        Optional<ScorePe> peOpt = scorePeRepository.findTopByStudentNoAndTermOrderByIdDesc(student.getUsername(), term);
        BigDecimal peTotal = peOpt.map(ScorePe::getScore)
                .orElseGet(() -> scoreResultRepository.findByStudentNoAndTerm(student.getUsername(), term)
                        .map(ScoreResult::getPeScore)
                        .orElse(BigDecimal.ZERO));
        if (peTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("componentCode", DEFAULT_PE_ITEM_CODE);
        legacy.put("componentName", "体育总分（历史数据）");
        legacy.put("rawValue", peTotal);
        legacy.put("convertedScore", peTotal);
        legacy.put("weight", new BigDecimal("1.0000"));
        legacy.put("weightedScore", peTotal);
        legacy.put("formulaSnapshot", "当前学期未录入分项过程，展示历史总分数据");
        return List.of(legacy);
    }

    private BigDecimal sumWeightedScore(List<Map<String, Object>> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            Object value = row.get("weightedScore");
            if (value instanceof BigDecimal bigDecimal) {
                total = total.add(bigDecimal);
            } else if (value != null) {
                try {
                    total = total.add(new BigDecimal(String.valueOf(value)));
                } catch (NumberFormatException ignored) {
                    // ignore invalid numeric value in response composition
                }
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveTerm(String studentNo, String term) {
        if (StringUtils.hasText(term)) {
            return term;
        }
        List<ScoreResult> scores = scoreResultRepository.findByStudentNoOrderByTermDesc(studentNo);
        if (!scores.isEmpty()) {
            return scores.get(0).getTerm();
        }
        return Year.now().getValue() + "-1";
    }

    private void submitDeclaration(DeclarationType type,
                                   String term,
                                   String itemCode,
                                   String itemName,
                                   BigDecimal points,
                                   String description,
                                   MultipartFile attachment) {
        if (points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("分值必须大于0");
        }

        AppUser student = currentUserService.mustGetCurrentUser();
        String attachmentPath = null;
        if (attachment != null && attachment.isEmpty() == false) {
            attachmentPath = fileStorageService.store(attachment, "declaration", student.getUsername());
        }

        Declaration declaration = new Declaration();
        declaration.setStudentNo(student.getUsername());
        declaration.setStudentName(student.getName());
        declaration.setClassName(student.getClassName());
        declaration.setType(type);
        declaration.setTerm(require(term, "学期不能为空"));
        declaration.setItemCode(clean(itemCode));
        declaration.setItemName(require(itemName, "项目名称不能为空"));
        declaration.setPoints(points);
        declaration.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        declaration.setAttachmentPath(attachmentPath);
        declaration.setStatus(DeclarationStatus.PENDING);
        declarationRepository.save(declaration);

        String declarationBizRef = "DECLARATION:" + declaration.getId();

        notificationService.createForCounselorClass(
                student.getClassName(),
                "有新的待审核申报",
                student.getName() + "（" + student.getUsername() + "）提交了" + (type == DeclarationType.MORAL ? "德育" : "技能") + "申报",
                "DECLARATION_SUBMITTED",
            "/counselor/reviews",
            declarationBizRef
        );
    }

    private Map<String, Object> toScoreItemView(ScoreItemCatalog row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("itemCode", row.getItemCode());
        view.put("itemName", row.getItemName());
        view.put("category", row.getCategory().name());
        view.put("requiredFlag", row.getRequiredFlag());
        view.put("displayOrder", row.getDisplayOrder());
        return view;
    }

    private Map<String, Object> toTranscriptItemView(String itemCode,
                                                     String itemName,
                                                     String category,
                                                     Boolean requiredFlag,
                                                     String status,
                                                     BigDecimal rawScore,
                                                     BigDecimal standardScore,
                                                     String source,
                                                     Object updatedAt,
                                                     boolean legacyData) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("itemCode", itemCode);
        view.put("itemName", itemName);
        view.put("category", category);
        view.put("requiredFlag", requiredFlag);
        view.put("status", status);
        view.put("rawScore", rawScore);
        view.put("standardScore", standardScore);
        view.put("source", source);
        view.put("updatedAt", updatedAt);
        view.put("legacyData", legacyData);
        return view;
    }

    private Map<String, Object> toHistoryView(String term,
                                              BigDecimal rawScore,
                                              BigDecimal standardScore,
                                              String status,
                                              Object updatedAt) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("term", term);
        view.put("rawScore", rawScore);
        view.put("standardScore", standardScore);
        view.put("status", status);
        view.put("updatedAt", updatedAt);
        return view;
    }

    private String csvLine(String section, String term, Map<String, Object> row) {
        return csvCell(section) + ','
                + csvCell(term) + ','
                + csvCell(row.get("itemCode")) + ','
                + csvCell(row.get("itemName")) + ','
                + csvCell(row.get("category")) + ','
                + csvCell(row.get("requiredFlag")) + ','
                + csvCell(row.get("status")) + ','
                + csvCell(row.get("rawScore")) + ','
                + csvCell(row.get("standardScore")) + ','
                + csvCell(row.get("source")) + ','
                + csvCell(row.get("legacyData"))
                + '\n';
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

    private String require(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
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

    private record TranscriptPayload(
            String term,
            Map<String, Object> summary,
            List<Map<String, Object>> studyItems,
            List<Map<String, Object>> peItems,
            List<Map<String, Object>> missingRequiredItems,
            int requiredCount,
            int requiredRecordedCount,
            BigDecimal completionRate,
            String resultStatus
    ) {
        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("term", term);
            map.put("summary", summary);
            map.put("studyItems", studyItems);
            map.put("peItems", peItems);
            map.put("missingRequiredItems", missingRequiredItems);
            map.put("requiredCount", requiredCount);
            map.put("requiredRecordedCount", requiredRecordedCount);
            map.put("completionRate", completionRate);
            map.put("resultStatus", resultStatus);
            return map;
        }
    }
}
