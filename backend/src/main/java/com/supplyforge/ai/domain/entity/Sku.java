package com.supplyforge.ai.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "skus")
public class Sku extends BaseAuditableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "original_name", nullable = false, length = 2000)
    private String originalName;

    @Column(name = "normalized_name", nullable = false, length = 2000)
    private String normalizedName;

    @Column(name = "is_duplicate", nullable = false)
    private boolean duplicate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_sku_id")
    private Sku parentSku;

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public Sku getParentSku() {
        return parentSku;
    }

    public void setParentSku(Sku parentSku) {
        this.parentSku = parentSku;
    }
}
