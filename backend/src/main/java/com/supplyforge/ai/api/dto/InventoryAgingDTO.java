package com.supplyforge.ai.api.dto;

import java.util.ArrayList;
import java.util.List;

public class InventoryAgingDTO {
    private double totalHealthyValue;
    private double totalWatchlistValue;
    private double totalSlowMovingValue;
    private double totalDeadStockValue;

    private double totalFrozenValue; // SLOW_MOVING + DEAD_STOCK
    private double totalBleedingRateMonthly;

    private List<SkuAgingDTO> healthySkus = new ArrayList<>();
    private List<SkuAgingDTO> watchlistSkus = new ArrayList<>();
    private List<SkuAgingDTO> slowMovingSkus = new ArrayList<>();
    private List<SkuAgingDTO> deadStockSkus = new ArrayList<>();

    public double getTotalHealthyValue() {
        return totalHealthyValue;
    }

    public void setTotalHealthyValue(double totalHealthyValue) {
        this.totalHealthyValue = totalHealthyValue;
    }

    public double getTotalWatchlistValue() {
        return totalWatchlistValue;
    }

    public void setTotalWatchlistValue(double totalWatchlistValue) {
        this.totalWatchlistValue = totalWatchlistValue;
    }

    public double getTotalSlowMovingValue() {
        return totalSlowMovingValue;
    }

    public void setTotalSlowMovingValue(double totalSlowMovingValue) {
        this.totalSlowMovingValue = totalSlowMovingValue;
    }

    public double getTotalDeadStockValue() {
        return totalDeadStockValue;
    }

    public void setTotalDeadStockValue(double totalDeadStockValue) {
        this.totalDeadStockValue = totalDeadStockValue;
    }

    public double getTotalFrozenValue() {
        return totalFrozenValue;
    }

    public void setTotalFrozenValue(double totalFrozenValue) {
        this.totalFrozenValue = totalFrozenValue;
    }

    public double getTotalBleedingRateMonthly() {
        return totalBleedingRateMonthly;
    }

    public void setTotalBleedingRateMonthly(double totalBleedingRateMonthly) {
        this.totalBleedingRateMonthly = totalBleedingRateMonthly;
    }

    public List<SkuAgingDTO> getHealthySkus() {
        return healthySkus;
    }

    public void setHealthySkus(List<SkuAgingDTO> healthySkus) {
        this.healthySkus = healthySkus;
    }

    public List<SkuAgingDTO> getWatchlistSkus() {
        return watchlistSkus;
    }

    public void setWatchlistSkus(List<SkuAgingDTO> watchlistSkus) {
        this.watchlistSkus = watchlistSkus;
    }

    public List<SkuAgingDTO> getSlowMovingSkus() {
        return slowMovingSkus;
    }

    public void setSlowMovingSkus(List<SkuAgingDTO> slowMovingSkus) {
        this.slowMovingSkus = slowMovingSkus;
    }

    public List<SkuAgingDTO> getDeadStockSkus() {
        return deadStockSkus;
    }

    public void setDeadStockSkus(List<SkuAgingDTO> deadStockSkus) {
        this.deadStockSkus = deadStockSkus;
    }
}
