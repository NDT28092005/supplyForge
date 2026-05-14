package com.supplyforge.ai.application;

import com.supplyforge.ai.api.dto.FileSampleDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DynamicFileImportService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * TASK 1: TRÍCH XUẤT MẪU (PEEK DATA)
     * Đọc stream trực tiếp từ file CSV, lấy đúng Header và 5 dòng đầu tiên rồi đóng Stream.
     * Không load toàn bộ file vào RAM.
     */
    public FileSampleDTO peekSampleData(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không hợp lệ hoặc rỗng.");
        }

        // Hiện tại MVP áp dụng cho CSV. (Có thể mở rộng Apache POI SAX cho Excel sau).
        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            throw new UnsupportedOperationException("Hệ thống hiện tại chỉ hỗ trợ trích xuất luồng Streaming cho file CSV.");
        }

        List<String> headers = new ArrayList<>();
        List<List<String>> sampleRows = new ArrayList<>();

        // Sử dụng BufferedReader để đọc dạng Streaming từng dòng
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String headerLine = br.readLine();
            if (headerLine != null) {
                // Tách cột bằng dấu phẩy (xử lý đơn giản cho MVP)
                headers = parseCsvLine(headerLine);
            }

            String line;
            int count = 0;
            // Đọc đúng 5 dòng dữ liệu rồi ngưng (Đảm bảo O(1) memory)
            while ((line = br.readLine()) != null && count < 5) {
                sampleRows.add(parseCsvLine(line));
                count++;
            }
        } // Stream tự động đóng nhờ try-with-resources

        return new FileSampleDTO(headers, sampleRows);
    }

    // Parser đơn giản cho file CSV, tách theo dấu phẩy (bỏ qua dấu phẩy trong ngoặc kép)
    private List<String> parseCsvLine(String line) {
        String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            result.add(token.replaceAll("^\"|\"$", "").trim());
        }
        return result;
    }

    /**
     * TASK 4: ĐỌC VÀ CHUYỂN ĐỔI TOÀN BỘ FILE (STREAMING & BATCH INSERT)
     * Nhận FinalMappingRule, đọc lại file stream và Batch Insert xuống PostgreSQL.
     */
    @Transactional
    public void processBatchImport(MultipartFile file, Map<String, String> finalMappingRule, String userId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không hợp lệ hoặc rỗng.");
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String headerLine = br.readLine();
            if (headerLine == null) return;
            List<String> fileHeaders = parseCsvLine(headerLine);

            // Tìm index của các cột dựa trên mapping
            Map<String, Integer> headerIndexMap = new HashMap<>();
            for (Map.Entry<String, String> entry : finalMappingRule.entrySet()) {
                String dbField = entry.getKey();
                String csvHeader = entry.getValue();
                int idx = fileHeaders.indexOf(csvHeader);
                if (idx >= 0) {
                    headerIndexMap.put(dbField, idx);
                }
            }

            int batchSize = 1000;
            int count = 0;
            String line;

            String sql = "INSERT INTO products (_id, user_id, product_name, item_sku, price, cost, stock, third_party_id, created_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            while ((line = br.readLine()) != null) {
                List<String> row = parseCsvLine(line);

                String productName = getValue(row, headerIndexMap, "productName");
                String itemSku = getValue(row, headerIndexMap, "itemSku");
                String priceStr = getValue(row, headerIndexMap, "price");
                String costStr = getValue(row, headerIndexMap, "cost");
                String stockStr = getValue(row, headerIndexMap, "stock");
                if (stockStr == null) stockStr = getValue(row, headerIndexMap, "quantity");
                String dateStr = getValue(row, headerIndexMap, "orderDate");

                java.sql.Timestamp createdAt = parseDate(dateStr);
                if (createdAt == null) {
                    // Fallback to random 0-90 days ago if no date mapped or parsing fails
                    long randomOffset = java.util.concurrent.ThreadLocalRandom.current().nextLong(0, 90L * 24 * 60 * 60 * 1000);
                    createdAt = new java.sql.Timestamp(System.currentTimeMillis() - randomOffset);
                }

                // Tạo mới record
                entityManager.createNativeQuery(sql)
                        .setParameter(1, UUID.randomUUID().toString().substring(0, 24))
                        .setParameter(2, userId)
                        .setParameter(3, productName != null ? productName : "Unknown")
                        .setParameter(4, itemSku)
                        .setParameter(5, parseBigDecimal(priceStr))
                        .setParameter(6, parseBigDecimal(costStr))
                        .setParameter(7, parseInteger(stockStr))
                        .setParameter(8, "csv_import_" + System.currentTimeMillis()) // third_party_id dummy
                        .setParameter(9, createdAt)
                        .executeUpdate();

                count++;
                if (count % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            // Flush phần dư
            entityManager.flush();
            entityManager.clear();
        }
    }

    private String getValue(List<String> row, Map<String, Integer> indexMap, String dbField) {
        Integer idx = indexMap.get(dbField);
        if (idx != null && idx < row.size()) {
            return row.get(idx);
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null || val.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer parseInteger(String val) {
        if (val == null || val.isBlank()) return 0;
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private java.sql.Timestamp parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            // Hỗ trợ định dạng dd/MM/yyyy hoặc yyyy-MM-dd
            String cleanVal = val.trim();
            if (cleanVal.contains("/")) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return java.sql.Timestamp.valueOf(java.time.LocalDate.parse(cleanVal, formatter).atStartOfDay());
            } else {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
                return java.sql.Timestamp.valueOf(java.time.LocalDate.parse(cleanVal, formatter).atStartOfDay());
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void clearData(String userId) {
        entityManager.createNativeQuery("DELETE FROM order_items WHERE order_id IN (SELECT _id FROM orders WHERE user_id = :userId)").setParameter("userId", userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM orders WHERE user_id = :userId").setParameter("userId", userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM products WHERE user_id = :userId").setParameter("userId", userId).executeUpdate();
    }
}
