package com.supplyforge.ai.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplyforge.ai.domain.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductSyncService {

    private static final Logger log = LoggerFactory.getLogger(ProductSyncService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public ProductSyncService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void syncProductsFromJson(InputStream jsonInputStream, String userId) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonInputStream);
            List<Map<String, Object>> rawProducts;

            if (rootNode.isArray()) {
                rawProducts = objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {});
            } else if (rootNode.isObject() && rootNode.has("products") && rootNode.get("products").isArray()) {
                rawProducts = objectMapper.convertValue(rootNode.get("products"), new TypeReference<List<Map<String, Object>>>() {});
            } else {
                log.warn("Unsupported JSON format for products, attempting to process as single object");
                rawProducts = List.of(objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {}));
            }

            int batchSize = 50;
            int count = 0;

            for (Map<String, Object> rawData : rawProducts) {
                Product product = new Product();
                
                // Set ID
                String id = rawData.get("_id") != null ? rawData.get("_id").toString() : UUID.randomUUID().toString().replace("-", "").substring(0, 24);
                product.setId(id);
                product.setUserId(userId);

                // Map Fields (Snake case from JSON or Camel case from dump)
                product.setThirdPartyId(getVal(rawData, "thirdPartyId", "third_party_id", ""));
                product.setItemId(getVal(rawData, "itemId", "item_id", null));
                product.setProductName(getVal(rawData, "productName", "product_name", "Unknown Product"));
                product.setPlatform(getVal(rawData, "platform", "platform", "shopee"));
                product.setItemSku(getVal(rawData, "itemSku", "item_sku", ""));
                
                product.setPrice(getDecimal(rawData, "price"));
                product.setCost(getDecimal(rawData, "cost"));
                product.setStock(getInt(rawData, "stock"));
                
                product.setTotalRevenue(getDecimal(rawData, "totalRevenue", "total_revenue"));
                product.setTotalProfit(getDecimal(rawData, "totalProfit", "total_profit"));
                product.setTotalOrders(getInt(rawData, "totalOrders", "total_orders"));

                product.setData(rawData);
                product.setActiveOnWeb(true);
                product.setCreatedAt(LocalDateTime.now());

                entityManager.persist(product);
                count++;

                if (count % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                    log.info("Synced {} products...", count);
                }
            }

            entityManager.flush();
            entityManager.clear();
            log.info("Successfully synced {} products in total.", count);

        } catch (Exception e) {
            log.error("Failed to sync products", e);
            throw new RuntimeException(e);
        }
    }

    private String getVal(Map<String, Object> data, String key1, String key2, String def) {
        Object v = data.getOrDefault(key1, data.get(key2));
        return v != null ? v.toString() : def;
    }

    private BigDecimal getDecimal(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object v = data.get(key);
            if (v != null) return new BigDecimal(v.toString());
        }
        return BigDecimal.ZERO;
    }

    private Integer getInt(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object v = data.get(key);
            if (v != null) return ((Number) v).intValue();
        }
        return 0;
    }
}
