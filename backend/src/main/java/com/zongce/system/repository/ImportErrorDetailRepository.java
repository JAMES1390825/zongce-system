package com.zongce.system.repository;

import com.zongce.system.entity.ImportErrorDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportErrorDetailRepository extends JpaRepository<ImportErrorDetail, Long> {

    List<ImportErrorDetail> findByBatchNoOrderByLineNoAscIdAsc(String batchNo);
}
