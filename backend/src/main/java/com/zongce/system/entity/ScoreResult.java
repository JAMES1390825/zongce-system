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
@Table(name = "score_result", indexes = {
        @Index(name = "idx_score_result_student_term", columnList = "student_no,term"),
        @Index(name = "idx_score_result_class_term", columnList = "class_name,term"),
        @Index(name = "idx_score_result_rank", columnList = "class_name,term,rank_no")
})
public class ScoreResult extends AuditableEntity {

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

    @Column(name = "study_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal studyScore = BigDecimal.ZERO;

    @Column(name = "pe_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal peScore = BigDecimal.ZERO;

    @Column(name = "moral_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal moralScore = BigDecimal.ZERO;

    @Column(name = "skill_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal skillScore = BigDecimal.ZERO;

    @Column(name = "total_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalScore = BigDecimal.ZERO;

    @Column(name = "rank_no")
    private Integer rankNo;

    @Column(name = "rule_name", length = 64)
    private String ruleName;

    @Column(name = "study_weight", precision = 6, scale = 4)
    private BigDecimal studyWeight;

    @Column(name = "pe_weight", precision = 6, scale = 4)
    private BigDecimal peWeight;

    @Column(name = "moral_cap", precision = 8, scale = 2)
    private BigDecimal moralCap;

    @Column(name = "skill_cap", precision = 8, scale = 2)
    private BigDecimal skillCap;

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

    public BigDecimal getStudyScore() {
        return studyScore;
    }

    public void setStudyScore(BigDecimal studyScore) {
        this.studyScore = studyScore;
    }

    public BigDecimal getPeScore() {
        return peScore;
    }

    public void setPeScore(BigDecimal peScore) {
        this.peScore = peScore;
    }

    public BigDecimal getMoralScore() {
        return moralScore;
    }

    public void setMoralScore(BigDecimal moralScore) {
        this.moralScore = moralScore;
    }

    public BigDecimal getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(BigDecimal skillScore) {
        this.skillScore = skillScore;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getRankNo() {
        return rankNo;
    }

    public void setRankNo(Integer rankNo) {
        this.rankNo = rankNo;
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
}
