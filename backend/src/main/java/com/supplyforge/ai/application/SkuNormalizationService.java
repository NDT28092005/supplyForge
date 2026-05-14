package com.supplyforge.ai.application;

import com.supplyforge.ai.domain.entity.Product;
import com.supplyforge.ai.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkuNormalizationService {

    private final ProductRepository productRepository;

    public SkuNormalizationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Thuật toán: Nhóm các sản phẩm từ các platform khác nhau có cùng itemSku
     * hoặc productName tương đồng (dùng Levenshtein distance).
     */
    public List<List<Product>> findDuplicateCandidates(String userId) {
        List<Product> products = productRepository.findByUserId(userId).stream()
                .filter(p -> p.getActiveOnWeb() == null || p.getActiveOnWeb())
                .collect(Collectors.toList());

        List<List<Product>> groups = new ArrayList<>();
        boolean[] visited = new boolean[products.size()];

        for (int i = 0; i < products.size(); i++) {
            if (visited[i]) continue;
            
            List<Product> currentGroup = new ArrayList<>();
            Product current = products.get(i);
            currentGroup.add(current);
            visited[i] = true;

            for (int j = i + 1; j < products.size(); j++) {
                if (visited[j]) continue;
                Product target = products.get(j);

                if (isSimilar(current, target)) {
                    currentGroup.add(target);
                    visited[j] = true;
                }
            }

            if (currentGroup.size() > 1) {
                groups.add(currentGroup);
            }
        }
        return groups;
    }

    private boolean isSimilar(Product a, Product b) {
        // Cùng itemSku
        if (a.getItemSku() != null && !a.getItemSku().isEmpty() && a.getItemSku().equalsIgnoreCase(b.getItemSku())) {
            return true;
        }
        // Tương đồng productName
        if (a.getProductName() != null && b.getProductName() != null) {
            String nameA = normalizeString(a.getProductName().toLowerCase());
            String nameB = normalizeString(b.getProductName().toLowerCase());
            int distance = calculateLevenshteinDistance(nameA, nameB);
            
            // Allow small typos (Levenshtein distance <= 3) or if one contains another completely
            return distance <= 3 || nameA.contains(nameB) || nameB.contains(nameA);
        }
        return false;
    }

    private String normalizeString(String input) {
        // Strip accents logic could be here, reusing existing VietnameseNormalizer if present
        // Since VietnameseNormalizer is already in the project (as per previous context), I'll use it
        return VietnameseNormalizer.stripAccents(input);
    }

    private int calculateLevenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1),
                                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    /**
     * Nghiệp vụ: Cộng dồn tồn kho (stock), và Tính trung bình trọng số giá vốn (cost).
     * Cập nhật parent, deactivate children.
     */
    @Transactional
    public void mergeProducts(String parentId, List<String> childIds) {
        Product parent = productRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent product not found"));
        
        List<Product> children = productRepository.findAllById(childIds);

        int totalStock = parent.getStock() != null ? parent.getStock() : 0;
        BigDecimal totalValue = (parent.getCost() != null && parent.getStock() != null) 
                                ? parent.getCost().multiply(BigDecimal.valueOf(parent.getStock()))
                                : BigDecimal.ZERO;

        for (Product child : children) {
            int childStock = child.getStock() != null ? child.getStock() : 0;
            BigDecimal childCost = child.getCost() != null ? child.getCost() : BigDecimal.ZERO;

            totalStock += childStock;
            totalValue = totalValue.add(childCost.multiply(BigDecimal.valueOf(childStock)));

            // Deactivate child product
            child.setActiveOnWeb(false);
            productRepository.save(child);
        }

        // Update Parent
        parent.setStock(totalStock);
        if (totalStock > 0) {
            BigDecimal weightedAverageCost = totalValue.divide(BigDecimal.valueOf(totalStock), 2, RoundingMode.HALF_UP);
            parent.setCost(weightedAverageCost);
        } else {
            // Fallback cost if stock is 0
            if (parent.getCost() == null && children.size() > 0 && children.get(0).getCost() != null) {
                parent.setCost(children.get(0).getCost());
            }
        }
        
        productRepository.save(parent);
    }
}
