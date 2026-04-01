package com.zongce.system.service;

import com.zongce.system.entity.ImportBatch;
import com.zongce.system.entity.ImportErrorDetail;

import java.util.List;

public interface ImportBatchService {

    ImportBatch startBatch(String importType, String fileName, String operator);

    ImportBatch finishBatch(ImportBatch batch,
                            int totalCount,
                            int successCount,
                            int failedCount,
                            String summary,
                            List<ImportErrorItem> errorItems);

    List<ImportBatch> listBatches(String importType, String operator);

    List<ImportErrorDetail> listErrors(String batchNo);

    record ImportErrorItem(Integer lineNo, String errorMessage, String rawContent) {
    }
}
