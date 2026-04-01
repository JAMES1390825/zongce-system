package com.zongce.system.service.impl;

import com.zongce.system.entity.ImportBatch;
import com.zongce.system.entity.ScoreEntry;
import com.zongce.system.entity.ScoreItemCatalog;
import com.zongce.system.entity.enums.ScoreEntrySource;
import com.zongce.system.entity.enums.ScoreEntryStatus;
import com.zongce.system.entity.enums.ScoreItemCategory;
import com.zongce.system.repository.ScoreEntryRepository;
import com.zongce.system.repository.ScoreItemCatalogRepository;
import com.zongce.system.service.ImportBatchService;
import com.zongce.system.service.ScoreEntryAggregateService;
import com.zongce.system.service.ScoreImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ScoreImportServiceImpl implements ScoreImportService {

    private final ScoreEntryRepository scoreEntryRepository;
    private final ScoreItemCatalogRepository scoreItemCatalogRepository;
    private final ScoreEntryAggregateService scoreEntryAggregateService;
    private final ImportBatchService importBatchService;

    public ScoreImportServiceImpl(ScoreEntryRepository scoreEntryRepository,
                                  ScoreItemCatalogRepository scoreItemCatalogRepository,
                                  ScoreEntryAggregateService scoreEntryAggregateService,
                                  ImportBatchService importBatchService) {
        this.scoreEntryRepository = scoreEntryRepository;
        this.scoreItemCatalogRepository = scoreItemCatalogRepository;
        this.scoreEntryAggregateService = scoreEntryAggregateService;
        this.importBatchService = importBatchService;
    }

    @Override
    @Transactional
    public ImportResult importPeScores(MultipartFile file, String operator) {
        return importScores(file, operator, "PE_SCORE", ScoreItemCategory.PE, "体育");
    }

    @Override
    @Transactional
    public ImportResult importStudyScores(MultipartFile file, String operator) {
        return importScores(file, operator, "STUDY_SCORE", ScoreItemCategory.STUDY, "智育");
    }

    private ImportResult importScores(MultipartFile file,
                                      String operator,
                                      String batchType,
                                      ScoreItemCategory category,
                                      String label) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择CSV文件");
        }

        ImportBatch batch = importBatchService.startBatch(batchType, file.getOriginalFilename(), operator);
        List<ImportError> errors = new ArrayList<>();
        List<ScoreEntry> entries = new ArrayList<>();
        Set<AffectedKey> affected = new LinkedHashSet<>();

        int total = 0;
        int failed = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && looksLikeHeader(line)) {
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }

                total++;
                String[] parts = parseCsvLine(line);
                if (parts.length < 6) {
                    failed++;
                    errors.add(new ImportError(lineNo, "字段不足，需要 6 列（学号,姓名,班级,学期,项目编码,成绩）", line));
                    continue;
                }

                try {
                    String studentNo = require(parts[0], "学号不能为空");
                    String studentName = require(parts[1], "姓名不能为空");
                    String className = require(parts[2], "班级不能为空");
                    String term = require(parts[3], "学期不能为空");

                    String itemCode = require(parts[4], "项目编码不能为空");
                    BigDecimal score = parseScore(parts[5]);

                    ScoreItemCatalog catalog = requireItemCatalog(itemCode, category);
                    if (isTotalItem(catalog, category)) {
                        throw new IllegalArgumentException("不允许导入总分项目，请导入单项成绩");
                    }
                    entries.add(buildScoreEntry(studentNo, studentName, className, term, score, catalog, operator));
                    affected.add(new AffectedKey(studentNo, studentName, className, term));
                } catch (IllegalArgumentException ex) {
                    failed++;
                    errors.add(new ImportError(lineNo, ex.getMessage(), line));
                }
            }
        } catch (IOException e) {
            errors.add(new ImportError(0, "读取文件失败：" + e.getMessage(), null));
            failed = Math.max(failed, total);
            importBatchService.finishBatch(batch, total, 0, failed, "读取文件失败", toBatchErrors(errors));
            return new ImportResult(batch.getBatchNo(), total, 0, failed, errors);
        }

        int success = entries.size();
        try {
            if (!entries.isEmpty()) {
                upsertEntries(entries);
                for (AffectedKey key : affected) {
                    scoreEntryAggregateService.syncTotalFromEntries(
                            category,
                            key.studentNo(),
                            key.studentName(),
                            key.className(),
                            key.term(),
                            operator
                    );
                }
            }
        } catch (Exception ex) {
            errors.add(new ImportError(0, "保存记录失败：" + ex.getMessage(), null));
            failed = total;
            success = 0;
        }

        if (success > 0) {
            failed = total - success;
        }
        String summary = label + "导入完成：成功 " + success + "，失败 " + failed;
        importBatchService.finishBatch(batch, total, success, failed, summary, toBatchErrors(errors));
        return new ImportResult(batch.getBatchNo(), total, success, failed, errors);
    }

    private List<ImportBatchService.ImportErrorItem> toBatchErrors(List<ImportError> errors) {
        return errors.stream()
                .map(e -> new ImportBatchService.ImportErrorItem(e.lineNo(), e.reason(), e.rawContent()))
                .toList();
    }

    private ScoreItemCatalog requireItemCatalog(String itemCode, ScoreItemCategory category) {
        ScoreItemCatalog item = scoreItemCatalogRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new IllegalArgumentException("成绩项目不存在：" + itemCode));
        if (item.getCategory() != category) {
            throw new IllegalArgumentException("项目分类不匹配：" + itemCode);
        }
        return item;
    }

    private ScoreEntry buildScoreEntry(String studentNo,
                                       String studentName,
                                       String className,
                                       String term,
                                       BigDecimal score,
                                       ScoreItemCatalog catalog,
                                       String operator) {
        ScoreEntry entry = scoreEntryRepository.findByStudentNoAndTermAndItemCode(studentNo, term, catalog.getItemCode())
                .orElseGet(ScoreEntry::new);
        entry.setStudentNo(studentNo);
        entry.setStudentName(studentName);
        entry.setClassName(className);
        entry.setTerm(term);
        entry.setItemCode(catalog.getItemCode());
        entry.setItemNameSnapshot(catalog.getItemName());
        entry.setCategory(catalog.getCategory());
        entry.setRawScore(score);
        entry.setStandardScore(score);
        entry.setStatus(ScoreEntryStatus.RECORDED);
        entry.setSource(ScoreEntrySource.IMPORT);
        entry.setCreatedBy(operator);
        return entry;
    }

    private void upsertEntries(List<ScoreEntry> entries) {
        for (ScoreEntry entry : entries) {
            scoreEntryRepository.save(entry);
        }
    }

    private String require(String value, String message) {
        String cleaned = value == null ? null : value.trim();
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private BigDecimal parseScore(String raw) {
        try {
            BigDecimal score = new BigDecimal(require(raw, "成绩不能为空"));
            if (score.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("成绩不能小于 0");
            }
            return score;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("成绩格式不正确");
        }
    }

    private boolean looksLikeHeader(String line) {
        String normalized = line.toLowerCase();
        return normalized.contains("student")
                || normalized.contains("class")
                || normalized.contains("score")
                || normalized.contains("itemcode")
                || line.contains("学号")
                || line.contains("班级")
                || line.contains("成绩")
                || line.contains("项目编码");
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

    private record AffectedKey(String studentNo, String studentName, String className, String term) {
    }
}
