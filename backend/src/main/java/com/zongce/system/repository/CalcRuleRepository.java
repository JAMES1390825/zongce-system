package com.zongce.system.repository;

import com.zongce.system.entity.CalcRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalcRuleRepository extends JpaRepository<CalcRule, Long> {

    Optional<CalcRule> findTopByEnabledTrueOrderByIdDesc();

    Optional<CalcRule> findTopByOrderByIdDesc();

    List<CalcRule> findByEnabledTrueOrderByIdDesc();
}
