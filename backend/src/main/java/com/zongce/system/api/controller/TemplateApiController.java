package com.zongce.system.api.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/templates")
public class TemplateApiController {

    @GetMapping("/students-import.csv")
    public ResponseEntity<byte[]> studentsImportTemplate() {
        String content = "studentNo,studentName,className\n"
                + "stu101,Student A,SE221\n"
                + "stu102,Student B,SE221\n";
        return csvResponse("students_import_template.csv", content);
    }

    @GetMapping("/pe-scores-import.csv")
    public ResponseEntity<byte[]> peScoresImportTemplate() {
        String content = "studentNo,studentName,className,term,itemCode,score\n"
                + "stu001,Student A,SE221,2026-1,PE-1000M,88\n"
                + "stu001,Student A,SE221,2026-1,PE-DAILY,90\n"
                + "stu002,Student B,SE221,2026-1,PE-1000M,92\n";
        return csvResponse("pe_scores_import_template.csv", content);
    }

    @GetMapping("/study-scores-import.csv")
    public ResponseEntity<byte[]> studyScoresImportTemplate() {
        String content = "studentNo,studentName,className,term,itemCode,score\n"
                + "stu001,Student A,SE221,2026-1,STUDY-MATH-A1,85\n"
                + "stu001,Student A,SE221,2026-1,STUDY-DIGITAL-CIRCUIT,88\n"
                + "stu002,Student B,SE221,2026-1,STUDY-MATH-A1,90\n";
        return csvResponse("study_scores_import_template.csv", content);
    }

    private ResponseEntity<byte[]> csvResponse(String fileName, String csvContent) {
        byte[] bytes = ("\uFEFF" + csvContent).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }
}
