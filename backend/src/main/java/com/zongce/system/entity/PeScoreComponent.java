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
@Table(name = "pe_score_component", indexes = {
        @Index(name = "idx_pe_component_student_term", columnList = "student_no,term"),
        @Index(name = "idx_pe_component_code", columnList = "component_code")
})
public class PeScoreComponent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_no", nullable = false, length = 32)
    private String studentNo;

    @Column(name = "student_name", nullable = false, length = 64)
    private String studentName;

    @Column(name = "class_name", nullable = false, length = 64)
    private String className;

    @Column(nullable = false, length = 32)
    private String term;

    @Column(name = "component_code", nullable = false, length = 64)
    private String componentCode;

    @Column(name = "component_name", nullable = false, length = 128)
    private String componentName;

    @Column(name = "raw_value", precision = 10, scale = 2)
    private BigDecimal rawValue;

    @Column(name = "converted_score", precision = 10, scale = 2)
    private BigDecimal convertedScore;

    @Column(precision = 8, scale = 4)
    private BigDecimal weight;

    @Column(name = "weighted_score", precision = 10, scale = 2)
    private BigDecimal weightedScore;

    @Column(name = "formula_snapshot", length = 255)
    private String formulaSnapshot;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getComponentCode() {
        return componentCode;
    }

    public void setComponentCode(String componentCode) {
        this.componentCode = componentCode;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public BigDecimal getRawValue() {
        return rawValue;
    }

    public void setRawValue(BigDecimal rawValue) {
        this.rawValue = rawValue;
    }

    public BigDecimal getConvertedScore() {
        return convertedScore;
    }

    public void setConvertedScore(BigDecimal convertedScore) {
        this.convertedScore = convertedScore;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getWeightedScore() {
        return weightedScore;
    }

    public void setWeightedScore(BigDecimal weightedScore) {
        this.weightedScore = weightedScore;
    }

    public String getFormulaSnapshot() {
        return formulaSnapshot;
    }

    public void setFormulaSnapshot(String formulaSnapshot) {
        this.formulaSnapshot = formulaSnapshot;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
