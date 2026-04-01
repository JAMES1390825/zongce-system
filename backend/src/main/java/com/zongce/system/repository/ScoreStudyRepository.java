package com.zongce.system.repository;

import com.zongce.system.entity.ScoreStudy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScoreStudyRepository extends JpaRepository<ScoreStudy, Long> {

    List<ScoreStudy> findByClassNameAndTermOrderByStudentNoAsc(String className, String term);

    List<ScoreStudy> findByTermOrderByClassNameAscStudentNoAsc(String term);

    Optional<ScoreStudy> findTopByStudentNoAndTermOrderByIdDesc(String studentNo, String term);
}
