package com.zongce.system.entity;

import com.zongce.system.entity.enums.ScoreEntrySource;
import com.zongce.system.entity.enums.ScoreEntryStatus;
import com.zongce.system.entity.enums.ScoreItemCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "score_entry", indexes = {
        @Index(name = "uk_score_entry_student_term_item", columnList = "student_no,term,item_code", unique = true),
        @Index(name = "idx_score_entry_student_term", columnList = "student_no,term"),
        @Index(name = "idx_score_entry_category", columnList = "category,term,class_name")
})
public class ScoreEntry extends AuditableEntity {

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

    @Column(name = "item_code", nullable = false, length = 64)
    private String itemCode;

    @Column(name = "item_name_snapshot", nullable = false, length = 128)
    private String itemNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScoreItemCategory category;

    @Column(name = "raw_score", precision = 8, scale = 2)
    private BigDecimal rawScore;

    @Column(name = "standard_score", precision = 8, scale = 2)
    private BigDecimal standardScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScoreEntryStatus status = ScoreEntryStatus.RECORDED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScoreEntrySource source = ScoreEntrySource.MANUAL;

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

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemNameSnapshot() {
        return itemNameSnapshot;
    }

    public void setItemNameSnapshot(String itemNameSnapshot) {
        this.itemNameSnapshot = itemNameSnapshot;
    }

    public ScoreItemCategory getCategory() {
        return category;
    }

    public void setCategory(ScoreItemCategory category) {
        this.category = category;
    }

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public void setRawScore(BigDecimal rawScore) {
        this.rawScore = rawScore;
    }

    public BigDecimal getStandardScore() {
        return standardScore;
    }

    public void setStandardScore(BigDecimal standardScore) {
        this.standardScore = standardScore;
    }

    public ScoreEntryStatus getStatus() {
        return status;
    }

    public void setStatus(ScoreEntryStatus status) {
        this.status = status;
    }

    public ScoreEntrySource getSource() {
        return source;
    }

    public void setSource(ScoreEntrySource source) {
        this.source = source;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
