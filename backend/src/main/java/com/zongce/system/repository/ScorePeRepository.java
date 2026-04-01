package com.zongce.system.repository;

import com.zongce.system.entity.ScorePe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScorePeRepository extends JpaRepository<ScorePe, Long> {

    List<ScorePe> findByClassNameAndTermOrderByStudentNoAsc(String className, String term);

    List<ScorePe> findByTermOrderByClassNameAscStudentNoAsc(String term);

    Optional<ScorePe> findTopByStudentNoAndTermOrderByIdDesc(String studentNo, String term);
}
