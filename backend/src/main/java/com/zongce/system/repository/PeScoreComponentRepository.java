package com.zongce.system.repository;

import com.zongce.system.entity.PeScoreComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeScoreComponentRepository extends JpaRepository<PeScoreComponent, Long> {

    List<PeScoreComponent> findByStudentNoAndTermOrderByIdAsc(String studentNo, String term);
}
