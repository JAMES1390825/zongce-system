package com.zongce.system.repository;

import com.zongce.system.entity.ScoreEntry;
import com.zongce.system.entity.enums.ScoreItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ScoreEntryRepository extends JpaRepository<ScoreEntry, Long> {

    List<ScoreEntry> findByStudentNoAndTermOrderByCategoryAscItemCodeAsc(String studentNo, String term);

    Optional<ScoreEntry> findByStudentNoAndTermAndItemCode(String studentNo, String term, String itemCode);

    List<ScoreEntry> findByStudentNoAndItemCodeOrderByTermDesc(String studentNo, String itemCode);

    List<ScoreEntry> findByCategoryOrderByClassNameAscStudentNoAscTermAscItemCodeAsc(ScoreItemCategory category);

    List<ScoreEntry> findByCategoryAndClassNameOrderByStudentNoAscTermAscItemCodeAsc(ScoreItemCategory category,
                                                                                       String className);

    List<ScoreEntry> findByCategoryAndTermOrderByClassNameAscStudentNoAscItemCodeAsc(ScoreItemCategory category,
                                                                                       String term);

    List<ScoreEntry> findByCategoryAndClassNameAndTermOrderByStudentNoAscItemCodeAsc(ScoreItemCategory category,
                                                                                       String className,
                                                                                       String term);

    @Query("select coalesce(sum(e.standardScore), 0) from ScoreEntry e " +
            "where e.studentNo = :studentNo and e.term = :term and e.category = :category " +
            "and e.itemCode <> :excludeItemCode and e.status <> com.zongce.system.entity.enums.ScoreEntryStatus.MISSING")
    BigDecimal sumStandardScoreExcludingItem(@Param("studentNo") String studentNo,
                                             @Param("term") String term,
                                             @Param("category") ScoreItemCategory category,
                                             @Param("excludeItemCode") String excludeItemCode);
}
