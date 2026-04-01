package com.zongce.system.service.impl;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.CalcRule;
import com.zongce.system.entity.ScoreResult;
import com.zongce.system.entity.enums.DeclarationType;
import com.zongce.system.entity.enums.RoleType;
import com.zongce.system.repository.DeclarationRepository;
import com.zongce.system.repository.ScorePeRepository;
import com.zongce.system.repository.ScoreResultRepository;
import com.zongce.system.repository.ScoreStudyRepository;
import com.zongce.system.repository.UserRepository;
import com.zongce.system.service.CalcRuleService;
import com.zongce.system.service.ScoreRecalcService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ScoreRecalcServiceImpl implements ScoreRecalcService {

    private final UserRepository userRepository;
    private final ScoreStudyRepository scoreStudyRepository;
    private final ScorePeRepository scorePeRepository;
    private final DeclarationRepository declarationRepository;
    private final ScoreResultRepository scoreResultRepository;
    private final CalcRuleService calcRuleService;

    public ScoreRecalcServiceImpl(UserRepository userRepository,
                                  ScoreStudyRepository scoreStudyRepository,
                                  ScorePeRepository scorePeRepository,
                                  DeclarationRepository declarationRepository,
                                  ScoreResultRepository scoreResultRepository,
                                  CalcRuleService calcRuleService) {
        this.userRepository = userRepository;
        this.scoreStudyRepository = scoreStudyRepository;
        this.scorePeRepository = scorePeRepository;
        this.declarationRepository = declarationRepository;
        this.scoreResultRepository = scoreResultRepository;
        this.calcRuleService = calcRuleService;
    }

    @Override
    @Transactional
    public void recalcClass(String className, String term) {
        CalcRule rule = calcRuleService.getCurrentRule();
        List<AppUser> students = userRepository.findByRoleAndClassNameAndEnabledTrue(RoleType.STUDENT, className);
        List<ScoreResult> results = new ArrayList<>();

        for (AppUser student : students) {
            BigDecimal study = scoreStudyRepository.findTopByStudentNoAndTermOrderByIdDesc(student.getUsername(), term)
                    .map(s -> safe(s.getScore()))
                    .orElse(BigDecimal.ZERO);

            BigDecimal pe = scorePeRepository.findTopByStudentNoAndTermOrderByIdDesc(student.getUsername(), term)
                    .map(s -> safe(s.getScore()))
                    .orElse(BigDecimal.ZERO);

            BigDecimal moral = safe(declarationRepository.sumApprovedPoints(
                    student.getUsername(), term, DeclarationType.MORAL));
            BigDecimal skill = safe(declarationRepository.sumApprovedPoints(
                    student.getUsername(), term, DeclarationType.SKILL));

            BigDecimal cappedMoral = moral.min(rule.getMoralCap());
            BigDecimal cappedSkill = skill.min(rule.getSkillCap());

            // 规则化算法：智育权重 + 体育权重 + capped 德育 + capped 技能
            BigDecimal total = study.multiply(rule.getStudyWeight())
                    .add(pe.multiply(rule.getPeWeight()))
                    .add(cappedMoral)
                    .add(cappedSkill)
                    .setScale(2, RoundingMode.HALF_UP);

            ScoreResult row = new ScoreResult();
            row.setStudentNo(student.getUsername());
            row.setStudentName(student.getName());
            row.setClassName(className);
            row.setTerm(term);
            row.setStudyScore(study);
            row.setPeScore(pe);
            row.setMoralScore(cappedMoral);
            row.setSkillScore(cappedSkill);
            row.setTotalScore(total);
            row.setRuleName(rule.getRuleName());
            row.setStudyWeight(rule.getStudyWeight());
            row.setPeWeight(rule.getPeWeight());
            row.setMoralCap(rule.getMoralCap());
            row.setSkillCap(rule.getSkillCap());
            results.add(row);
        }

        results.sort(Comparator.comparing(ScoreResult::getTotalScore).reversed());
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRankNo(i + 1);
        }

        scoreResultRepository.deleteByClassNameAndTerm(className, term);
        scoreResultRepository.saveAll(results);
    }

    private BigDecimal safe(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v;
    }
}
