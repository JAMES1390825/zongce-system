package com.zongce.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "calc_rule", indexes = {
        @Index(name = "idx_calc_rule_enabled", columnList = "enabled")
})
public class CalcRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 64)
    private String ruleName;

    @Column(name = "study_weight", nullable = false, precision = 6, scale = 4)
    private BigDecimal studyWeight;

    @Column(name = "pe_weight", nullable = false, precision = 6, scale = 4)
    private BigDecimal peWeight;

    @Column(name = "moral_cap", nullable = false, precision = 8, scale = 2)
    private BigDecimal moralCap;

    @Column(name = "skill_cap", nullable = false, precision = 8, scale = 2)
    private BigDecimal skillCap;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false)
    private Boolean enabled = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public BigDecimal getStudyWeight() {
        return studyWeight;
    }

    public void setStudyWeight(BigDecimal studyWeight) {
        this.studyWeight = studyWeight;
    }

    public BigDecimal getPeWeight() {
        return peWeight;
    }

    public void setPeWeight(BigDecimal peWeight) {
        this.peWeight = peWeight;
    }

    public BigDecimal getMoralCap() {
        return moralCap;
    }

    public void setMoralCap(BigDecimal moralCap) {
        this.moralCap = moralCap;
    }

    public BigDecimal getSkillCap() {
        return skillCap;
    }

    public void setSkillCap(BigDecimal skillCap) {
        this.skillCap = skillCap;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
