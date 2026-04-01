package com.zongce.system.service.impl;

import com.zongce.system.entity.ImportBatch;
import com.zongce.system.entity.ImportErrorDetail;
import com.zongce.system.repository.ImportBatchRepository;
import com.zongce.system.repository.ImportErrorDetailRepository;
import com.zongce.system.service.ImportBatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ImportBatchServiceImpl implements ImportBatchService {

    private final ImportBatchRepository importBatchRepository;
    private final ImportErrorDetailRepository importErrorDetailRepository;

    public ImportBatchServiceImpl(ImportBatchRepository importBatchRepository,
                                  ImportErrorDetailRepository importErrorDetailRepository) {
        this.importBatchRepository = importBatchRepository;
        this.importErrorDetailRepository = importErrorDetailRepository;
    }

    @Override
    @Transactional
    public ImportBatch startBatch(String importType, String fileName, String operator) {
        ImportBatch row = new ImportBatch();
        row.setBatchNo(buildBatchNo(importType));
        row.setImportType(trim(importType));
        row.setFileName(trimToLength(fileName, 255));
        row.setOperator(trimToLength(operator, 64));
        row.setStatus("PROCESSING");
        row.setStartedAt(LocalDateTime.now());
        row.setFinishedAt(null);
        row.setSummary("处理中");
        return importBatchRepository.save(row);
    }

    @Override
    @Transactional
    public ImportBatch finishBatch(ImportBatch batch,
                                   int totalCount,
                                   int successCount,
                                   int failedCount,
                                   String summary,
                                   List<ImportErrorItem> errorItems) {
        batch.setTotalCount(Math.max(totalCount, 0));
        batch.setSuccessCount(Math.max(successCount, 0));
        batch.setFailedCount(Math.max(failedCount, 0));
        if (failedCount == 0) {
            batch.setStatus("SUCCESS");
        } else if (successCount > 0) {
            batch.setStatus("PARTIAL_FAILED");
        } else {
            batch.setStatus("FAILED");
        }
        batch.setSummary(trimToLength(summary, 1000));
        batch.setFinishedAt(LocalDateTime.now());
        ImportBatch saved = importBatchRepository.save(batch);

        if (errorItems != null && !errorItems.isEmpty()) {
            List<ImportErrorDetail> rows = new ArrayList<>();
            for (ImportErrorItem item : errorItems) {
                ImportErrorDetail row = new ImportErrorDetail();
                row.setBatchNo(saved.getBatchNo());
                row.setLineNo(item.lineNo());
                row.setErrorMessage(trimToLength(item.errorMessage(), 500));
                row.setRawContent(trimToLength(item.rawContent(), 1000));
                rows.add(row);
            }
            importErrorDetailRepository.saveAll(rows);
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportBatch> listBatches(String importType, String operator) {
        String type = trim(importType);
        String op = trim(operator);
        if (StringUtils.hasText(type) && StringUtils.hasText(op)) {
            return importBatchRepository.findTop100ByImportTypeAndOperatorOrderByCreatedAtDesc(type, op);
        }
        if (StringUtils.hasText(type)) {
            return importBatchRepository.findTop100ByImportTypeOrderByCreatedAtDesc(type);
        }
        if (StringUtils.hasText(op)) {
            return importBatchRepository.findTop100ByOperatorOrderByCreatedAtDesc(op);
        }
        return importBatchRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportErrorDetail> listErrors(String batchNo) {
        return importErrorDetailRepository.findByBatchNoOrderByLineNoAscIdAsc(batchNo);
    }

    private String buildBatchNo(String importType) {
        String prefix = trim(importType);
        if (!StringUtils.hasText(prefix)) {
            prefix = "IMPORT";
        }
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix.toUpperCase() + "-" + ts + "-" + suffix;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String trimToLength(String value, int maxLength) {
        String cleaned = trim(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}
