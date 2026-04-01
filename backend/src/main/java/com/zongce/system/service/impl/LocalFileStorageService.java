package com.zongce.system.service.impl;

import com.zongce.system.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path baseDir;

    public LocalFileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String category, String studentNo) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            LocalDate now = LocalDate.now();
            Path targetDir = baseDir
                    .resolve(now.getYear() + "-" + now.getMonthValue())
                    .resolve(category)
                    .resolve(studentNo == null ? "common" : studentNo)
                    .normalize();
            Files.createDirectories(targetDir);

            String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                    ? file.getOriginalFilename()
                    : "file.bin";
            String ext = "";
            int idx = originalFilename.lastIndexOf('.');
            if (idx >= 0) {
                ext = originalFilename.substring(idx);
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path targetFile = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            Path relative = baseDir.relativize(targetFile);
            return "/files/" + relative.toString().replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("附件保存失败", e);
        }
    }
}
