package com.zongce.system.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ScoreImportService {

    ImportResult importPeScores(MultipartFile file, String operator);

    ImportResult importStudyScores(MultipartFile file, String operator);

    record ImportResult(String batchNo, int total, int success, int failed, List<ImportError> errors) {
    }

    record ImportError(int lineNo, String reason, String rawContent) {
    }
}
