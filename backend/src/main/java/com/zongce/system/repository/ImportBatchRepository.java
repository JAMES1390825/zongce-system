package com.zongce.system.repository;

import com.zongce.system.entity.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    Optional<ImportBatch> findByBatchNo(String batchNo);

    List<ImportBatch> findTop100ByOrderByCreatedAtDesc();

    List<ImportBatch> findTop100ByImportTypeOrderByCreatedAtDesc(String importType);

    List<ImportBatch> findTop100ByOperatorOrderByCreatedAtDesc(String operator);

    List<ImportBatch> findTop100ByImportTypeAndOperatorOrderByCreatedAtDesc(String importType, String operator);
}
