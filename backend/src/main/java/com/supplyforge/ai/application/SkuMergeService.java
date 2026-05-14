package com.supplyforge.ai.application;

import com.supplyforge.ai.api.dto.SkuDuplicatePairResponse;
import com.supplyforge.ai.domain.entity.Sku;
import com.supplyforge.ai.domain.repository.SkuRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SkuMergeService {

    private static final int MAX_ROOT_SKUS_SCAN = 600;
    private static final int MAX_PAIRS = 80;
    // Khoảng cách tối đa tính trên chuỗi ĐÃ xóa dấu → bắt được sai chính tả dấu tiếng Việt
    private static final int MAX_LEVENSHTEIN_STRIPPED = 2;

    private final SkuRepository skuRepository;

    public SkuMergeService(SkuRepository skuRepository) {
        this.skuRepository = skuRepository;
    }

    public List<SkuDuplicatePairResponse> listDuplicateCandidates(Long workspaceId) {
        List<Sku> roots = skuRepository.findByWorkspaceIdAndParentSkuIsNull(workspaceId);
        // Dedup by normalizedName (phòng trùng DB cũ)
        Map<String, Sku> seen = new java.util.LinkedHashMap<>();
        for (Sku s : roots) {
            seen.putIfAbsent(s.getNormalizedName(), s);
        }
        List<Sku> deduped = new ArrayList<>(seen.values());
        if (deduped.size() > MAX_ROOT_SKUS_SCAN) {
            deduped = deduped.subList(0, MAX_ROOT_SKUS_SCAN);
        }

        // Pre-compute chuỗi đã xóa dấu cho từng SKU
        String[] stripped = new String[deduped.size()];
        for (int i = 0; i < deduped.size(); i++) {
            stripped[i] = VietnameseNormalizer.stripAccents(deduped.get(i).getNormalizedName());
        }

        List<SkuDuplicatePairResponse> out = new ArrayList<>();
        int cap = MAX_PAIRS;
        for (int i = 0; i < deduped.size() && out.size() < cap; i++) {
            Sku a = deduped.get(i);
            String sa = stripped[i];
            if (sa.isBlank()) continue;
            for (int j = i + 1; j < deduped.size() && out.size() < cap; j++) {
                Sku b = deduped.get(j);
                String sb = stripped[j];
                if (sb.isBlank()) continue;
                // Loại nhanh nếu độ dài lệch quá nhiều
                if (Math.abs(sa.length() - sb.length()) > MAX_LEVENSHTEIN_STRIPPED + 1) continue;
                int d = Levenshtein.distance(sa, sb);
                if (d > 0 && d <= MAX_LEVENSHTEIN_STRIPPED) {
                    out.add(new SkuDuplicatePairResponse(
                            a.getId(), a.getOriginalName(), b.getId(), b.getOriginalName(), d));
                }
            }
        }
        return out;
    }

    @Transactional
    public void mergeSkus(Long workspaceId, long parentSkuId, long childSkuId) {
        if (parentSkuId == childSkuId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể gộp SKU với chính nó");
        }
        Sku parent = skuRepository
                .findById(parentSkuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "parent sku"));
        Sku child = skuRepository
                .findById(childSkuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "child sku"));
        if (!parent.getWorkspace().getId().equals(workspaceId)
                || !child.getWorkspace().getId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SKU không thuộc workspace");
        }
        Sku walk = parent;
        for (int g = 0; walk != null && g < 64; g++) {
            if (walk.getId().equals(child.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Không gộp: tạo vòng cha–con");
            }
            walk = walk.getParentSku();
        }
        child.setParentSku(parent);
        child.setDuplicate(true);
        skuRepository.save(child);
    }
}
