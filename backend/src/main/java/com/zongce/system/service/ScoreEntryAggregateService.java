package com.zongce.system.service;

import com.zongce.system.entity.ScoreEntry;
import com.zongce.system.entity.ScorePe;
import com.zongce.system.entity.ScoreStudy;
import com.zongce.system.entity.enums.ScoreItemCategory;
import com.zongce.system.repository.ScoreEntryRepository;
import com.zongce.system.repository.ScorePeRepository;
import com.zongce.system.repository.ScoreStudyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ScoreEntryAggregateService {

    public static final String DEFAULT_STUDY_ITEM_CODE = "STUDY-TOTAL";
    public static final String DEFAULT_PE_ITEM_CODE = "PE-TOTAL";

    private final ScoreEntryRepository scoreEntryRepository;
    private final ScoreStudyRepository scoreStudyRepository;
    private final ScorePeRepository scorePeRepository;

    public ScoreEntryAggregateService(ScoreEntryRepository scoreEntryRepository,
                                      ScoreStudyRepository scoreStudyRepository,
                                      ScorePeRepository scorePeRepository) {
        this.scoreEntryRepository = scoreEntryRepository;
        this.scoreStudyRepository = scoreStudyRepository;
        this.scorePeRepository = scorePeRepository;
    }

    @Transactional
    public void syncTotalFromEntries(ScoreItemCategory category,
                                     String studentNo,
                                     String studentName,
                                     String className,
                                     String term,
                                     String operator) {
        String totalCode = totalCode(category);
        BigDecimal sumByItems = scoreEntryRepository.sumStandardScoreExcludingItem(studentNo, term, category, totalCode);
        BigDecimal finalScore = sumByItems.compareTo(BigDecimal.ZERO) > 0
                ? sumByItems
                : scoreEntryRepository.findByStudentNoAndTermAndItemCode(studentNo, term, totalCode)
                .map(ScoreEntry::getStandardScore)
                .orElse(BigDecimal.ZERO);

        if (category == ScoreItemCategory.STUDY) {
            ScoreStudy row = scoreStudyRepository.findTopByStudentNoAndTermOrderByIdDesc(studentNo, term)
                    .orElseGet(ScoreStudy::new);
            row.setStudentNo(studentNo);
            row.setStudentName(studentName);
            row.setClassName(className);
            row.setTerm(term);
            row.setScore(finalScore);
            row.setCreatedBy(operator);
            scoreStudyRepository.save(row);
            return;
        }

        if (category == ScoreItemCategory.PE) {
            ScorePe row = scorePeRepository.findTopByStudentNoAndTermOrderByIdDesc(studentNo, term)
                    .orElseGet(ScorePe::new);
            row.setStudentNo(studentNo);
            row.setStudentName(studentName);
            row.setClassName(className);
            row.setTerm(term);
            row.setScore(finalScore);
            row.setCreatedBy(operator);
            scorePeRepository.save(row);
        }
    }

    public String totalCode(ScoreItemCategory category) {
        return category == ScoreItemCategory.STUDY ? DEFAULT_STUDY_ITEM_CODE : DEFAULT_PE_ITEM_CODE;
    }
}
