package com.supplyforge.ai.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "_id", length = 24, nullable = false)
    private String id;

    @Column(name = "user_id", length = 24, nullable = false)
    private String userId;

    @Column(name = "third_party_id", length = 255, nullable = false)
    private String thirdPartyId;

    @Column(name = "item_id", length = 255)
    private String itemId;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "platform", length = 50)
    private String platform;

    @Column(name = "item_sku", length = 255)
    private String itemSku;

    @Column(name = "price", precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "cost", precision = 18, scale = 2)
    private BigDecimal cost;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "total_revenue", precision = 18, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_profit", precision = 18, scale = 2)
    private BigDecimal totalProfit;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "json")
    private Map<String, Object> data;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "json")
    private Object images; // Can be List or Object depending on structure

    @Column(name = "active_on_web")
    private Boolean activeOnWeb;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getThirdPartyId() { return thirdPartyId; }
    public void setThirdPartyId(String thirdPartyId) { this.thirdPartyId = thirdPartyId; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getItemSku() { return itemSku; }
    public void setItemSku(String itemSku) { this.itemSku = itemSku; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public BigDecimal getTotalProfit() { return totalProfit; }
    public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public Object getImages() { return images; }
    public void setImages(Object images) { this.images = images; }
    public Boolean getActiveOnWeb() { return activeOnWeb; }
    public void setActiveOnWeb(Boolean activeOnWeb) { this.activeOnWeb = activeOnWeb; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
