package com.zongce.system.service.impl;

import com.zongce.system.entity.CalcRule;
import com.zongce.system.repository.CalcRuleRepository;
import com.zongce.system.service.CalcRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CalcRuleServiceImpl implements CalcRuleService {

    private static final BigDecimal DEFAULT_STUDY_WEIGHT = new BigDecimal("0.60");
    private static final BigDecimal DEFAULT_PE_WEIGHT = new BigDecimal("0.20");
    private static final BigDecimal DEFAULT_MORAL_CAP = new BigDecimal("20.00");
    private static final BigDecimal DEFAULT_SKILL_CAP = new BigDecimal("20.00");

    private final CalcRuleRepository calcRuleRepository;

    public CalcRuleServiceImpl(CalcRuleRepository calcRuleRepository) {
        this.calcRuleRepository = calcRuleRepository;
    }

    @Override
    @Transactional
    public CalcRule getCurrentRule() {
        return calcRuleRepository.findTopByEnabledTrueOrderByIdDesc()
                .orElseGet(this::createDefaultRule);
    }

    @Override
    @Transactional
    public CalcRule saveGlobalRule(String ruleName,
                                   BigDecimal studyWeight,
                                   BigDecimal peWeight,
                                   BigDecimal moralCap,
                                   BigDecimal skillCap,
                                   String remark) {
        disableCurrentRules();

        CalcRule rule = new CalcRule();
        rule.setRuleName(cleanRuleName(ruleName));
        rule.setStudyWeight(normalizeWeight(studyWeight));
        rule.setPeWeight(normalizeWeight(peWeight));
        rule.setMoralCap(normalizeCap(moralCap));
        rule.setSkillCap(normalizeCap(skillCap));
        rule.setRemark(clean(remark));
        rule.setEnabled(true);

        return calcRuleRepository.save(rule);
    }

    private CalcRule createDefaultRule() {
        disableCurrentRules();

        CalcRule rule = new CalcRule();
        rule.setRuleName("默认规则");
        rule.setStudyWeight(DEFAULT_STUDY_WEIGHT);
        rule.setPeWeight(DEFAULT_PE_WEIGHT);
        rule.setMoralCap(DEFAULT_MORAL_CAP);
        rule.setSkillCap(DEFAULT_SKILL_CAP);
        rule.setRemark("系统初始化默认规则");
        rule.setEnabled(true);
        return calcRuleRepository.save(rule);
    }

    private void disableCurrentRules() {
        List<CalcRule> activeRules = calcRuleRepository.findByEnabledTrueOrderByIdDesc();
        if (activeRules.isEmpty()) {
            return;
        }
        for (CalcRule row : activeRules) {
            row.setEnabled(false);
        }
        calcRuleRepository.saveAll(activeRules);
    }

    private String cleanRuleName(String value) {
        String cleaned = clean(value);
        return cleaned == null ? "未命名规则" : cleaned;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal normalizeWeight(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCap(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
