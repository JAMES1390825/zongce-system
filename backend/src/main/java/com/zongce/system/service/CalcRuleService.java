package com.zongce.system.service;

import com.zongce.system.entity.CalcRule;

import java.math.BigDecimal;

public interface CalcRuleService {

    CalcRule getCurrentRule();

    CalcRule saveGlobalRule(String ruleName,
                            BigDecimal studyWeight,
                            BigDecimal peWeight,
                            BigDecimal moralCap,
                            BigDecimal skillCap,
                            String remark);
}
