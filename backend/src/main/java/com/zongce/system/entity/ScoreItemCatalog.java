package com.zongce.system.entity;

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
@Table(name = "score_item_catalog", indexes = {
        @Index(name = "uk_score_item_catalog_code", columnList = "item_code", unique = true),
        @Index(name = "idx_score_item_catalog_category", columnList = "category,enabled,display_order")
})
public class ScoreItemCatalog extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false, length = 64)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 128)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScoreItemCategory category;

    @Column(name = "required_flag", nullable = false)
    private Boolean requiredFlag = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "default_weight", precision = 8, scale = 4)
    private BigDecimal defaultWeight;

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

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public ScoreItemCategory getCategory() {
        return category;
    }

    public void setCategory(ScoreItemCategory category) {
        this.category = category;
    }

    public Boolean getRequiredFlag() {
        return requiredFlag;
    }

    public void setRequiredFlag(Boolean requiredFlag) {
        this.requiredFlag = requiredFlag;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public BigDecimal getDefaultWeight() {
        return defaultWeight;
    }

    public void setDefaultWeight(BigDecimal defaultWeight) {
        this.defaultWeight = defaultWeight;
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
