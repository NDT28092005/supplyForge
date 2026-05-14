package com.supplyforge.ai.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplyforge.ai.domain.entity.Order;
import com.supplyforge.ai.domain.entity.OrderItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderSyncService {

    private static final Logger log = LoggerFactory.getLogger(OrderSyncService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public OrderSyncService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void syncOrdersFromJson(InputStream jsonInputStream, String userId) {
        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(jsonInputStream);
            List<Map<String, Object>> rawOrders;

            if (rootNode.isArray()) {
                rawOrders = objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {});
            } else if (rootNode.isObject()) {
                // Check for common list wrappers like "orders", "data", "order_list"
                if (rootNode.has("orders") && rootNode.get("orders").isArray()) {
                    rawOrders = objectMapper.convertValue(rootNode.get("orders"), new TypeReference<List<Map<String, Object>>>() {});
                } else if (rootNode.has("order") && rootNode.get("order").isObject()) {
                    // Single order wrapped in "order" field
                    rawOrders = List.of(objectMapper.convertValue(rootNode.get("order"), new TypeReference<Map<String, Object>>() {}));
                } else {
                    // Treat the whole object as a single order
                    rawOrders = List.of(objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {}));
                }
            } else {
                throw new IllegalArgumentException("Unsupported JSON root type: " + rootNode.getNodeType());
            }

            int batchSize = 50;
            int count = 0;

            for (Map<String, Object> rawData : rawOrders) {
                Order order = new Order();
                
                // Generate a 24-char ID (mocking MongoDB ObjectId style or just substring UUID)
                order.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
                order.setUserId(userId);

                // Extract physical columns
                if (rawData.containsKey("orderId")) {
                    order.setOrderId(rawData.get("orderId").toString());
                }
                if (rawData.containsKey("platform")) {
                    order.setPlatform(rawData.get("platform").toString());
                }
                if (rawData.containsKey("amount")) {
                    order.setAmount(new BigDecimal(rawData.get("amount").toString()));
                }
                if (rawData.containsKey("buyerUserName")) {
                    order.setBuyerUserName(rawData.get("buyerUserName").toString());
                }
                if (rawData.containsKey("cancelledAfterPackaged")) {
                    order.setCancelledAfterPackaged(Boolean.valueOf(rawData.get("cancelledAfterPackaged").toString()));
                }
                if (rawData.containsKey("createdAt")) {
                    try {
                        String createdAtStr = rawData.get("createdAt").toString();
                        // Adjust parsing based on actual JSON format, assuming ISO date time
                        order.setCreatedAt(LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_DATE_TIME));
                    } catch (Exception e) {
                        log.warn("Failed to parse createdAt for order {}", order.getOrderId());
                    }
                }
                
                // Set the entire raw JSON into 'data' column for backup/querying
                order.setData(rawData);
                
                // Handle Items if present
                if (rawData.containsKey("items") && rawData.get("items") instanceof List) {
                    List<Map<String, Object>> rawItems = (List<Map<String, Object>>) rawData.get("items");
                    for (Map<String, Object> rawItem : rawItems) {
                        OrderItem item = new OrderItem();
                        
                        if (rawItem.containsKey("itemId")) item.setItemId(rawItem.get("itemId").toString());
                        if (rawItem.containsKey("modelId")) item.setModelId(rawItem.get("modelId").toString());
                        if (rawItem.containsKey("sku")) item.setSku(rawItem.get("sku").toString());
                        if (rawItem.containsKey("productName")) item.setProductName(rawItem.get("productName").toString());
                        if (rawItem.containsKey("quantity")) item.setQuantity(Integer.valueOf(rawItem.get("quantity").toString()));
                        if (rawItem.containsKey("price")) item.setPrice(new BigDecimal(rawItem.get("price").toString()));
                        if (rawItem.containsKey("cost")) item.setCost(new BigDecimal(rawItem.get("cost").toString()));
                        
                        order.addItem(item);
                    }
                }

                entityManager.persist(order);
                count++;

                // Flush and clear session periodically to avoid OutOfMemoryError and use JDBC Batching
                if (count % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                    log.info("Flushed and cleared session after processing {} orders", count);
                }
            }

            // Flush remaining
            if (count % batchSize != 0) {
                entityManager.flush();
                entityManager.clear();
            }

            log.info("Successfully synced {} orders in batch.", count);

        } catch (Exception e) {
            log.error("Error occurred while syncing orders from JSON", e);
            throw new RuntimeException("Data sync failed", e);
        }
    }
}
