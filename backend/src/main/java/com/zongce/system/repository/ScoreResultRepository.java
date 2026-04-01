package com.zongce.system.repository;

import com.zongce.system.entity.ScoreResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScoreResultRepository extends JpaRepository<ScoreResult, Long> {

    List<ScoreResult> findByClassNameAndTermOrderByRankNoAsc(String className, String term);

    List<ScoreResult> findByTermOrderByClassNameAscRankNoAsc(String term);

    List<ScoreResult> findByClassNameOrderByTermDescRankNoAsc(String className);

    Optional<ScoreResult> findByStudentNoAndTerm(String studentNo, String term);

    List<ScoreResult> findByStudentNoOrderByTermDesc(String studentNo);

    void deleteByClassNameAndTerm(String className, String term);
}
