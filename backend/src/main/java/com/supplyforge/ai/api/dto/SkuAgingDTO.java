package com.supplyforge.ai.api.dto;

public class SkuAgingDTO {
    private String id;
    private String name;
    private String sku;
    private int quantity;
    private double costPrice;
    private double sellingPrice;
    private double totalValue;
    private String lastOrderDate;
    private int daysInInventory;
    private String agingBucket; // HEALTHY, WATCHLIST, SLOW_MOVING, DEAD_STOCK

    public SkuAgingDTO() {
    }

    public SkuAgingDTO(String id, String name, String sku, int quantity, double costPrice, double sellingPrice,
                       double totalValue, String lastOrderDate, int daysInInventory, String agingBucket) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.quantity = quantity;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.totalValue = totalValue;
        this.lastOrderDate = lastOrderDate;
        this.daysInInventory = daysInInventory;
        this.agingBucket = agingBucket;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    public String getLastOrderDate() {
        return lastOrderDate;
    }

    public void setLastOrderDate(String lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
    }

    public int getDaysInInventory() {
        return daysInInventory;
    }

    public void setDaysInInventory(int daysInInventory) {
        this.daysInInventory = daysInInventory;
    }

    public String getAgingBucket() {
        return agingBucket;
    }

    public void setAgingBucket(String agingBucket) {
        this.agingBucket = agingBucket;
    }
}
