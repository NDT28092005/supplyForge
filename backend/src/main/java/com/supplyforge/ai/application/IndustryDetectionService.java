package com.supplyforge.ai.application;

import com.supplyforge.ai.domain.industry.IndustryPlaybook;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * IndustryDetectionService — Lightweight NLP để phân loại ngành nghề từ SKU name.
 *
 * Pipeline 2 tầng:
 *   Tầng 1: Keyword Regex Matching (tốc độ cao, zero-cost).
 *   Tầng 2: Fallback về GENERAL nếu không match (có thể mở rộng thành LLM call sau).
 *
 * Thiết kế: Single Responsibility — chỉ detect, không calculate.
 * Dễ thêm ngành mới: Chỉ cần bổ sung entry vào KEYWORD_MAP.
 */
@Service
public class IndustryDetectionService {

    /**
     * Keyword map: mỗi entry là (pattern, playbook).
     * Pattern được compile sẵn để tái sử dụng (performance).
     * Thứ tự ưu tiên: FMCG > FASHION > ELECTRONICS > HOME.
     */
    private static final List<Map.Entry<Pattern, IndustryPlaybook>> KEYWORD_MAP = List.of(
        // ── FMCG & Food (ưu tiên cao nhất — rủi ro cao nhất) ─────────────────
        Map.entry(
            Pattern.compile(
                "(?i)(?U)\\b(sữa|sua|bột|bot|thực phẩm|thuc pham|ăn|uống|an uong|" +
                "đồ ăn|do an|bánh|banh|kẹo|keo|nước|nuoc|đồ uống|do uong|" +
                "vitamin|supplement|snack|mì|mi|gạo|gao|dầu ăn|dau an|" +
                "gia vị|gia vi|sốt|sot|yogurt|fmcg|date|hsd|han su dung)\\b",
                Pattern.UNICODE_CASE
            ),
            IndustryPlaybook.FMCG_FOOD
        ),

        // ── Fashion & Apparel ─────────────────────────────────────────────────
        Map.entry(
            Pattern.compile(
                "(?i)(?U)\\b(áo|ao|quần|quan|váy|vay|đầm|dam|đồ|do|size|" +
                "xl|xxl|m|l|s|shirt|dress|pants|skirt|jacket|coat|" +
                "jeans|hoodie|sweater|blouse|suit|phong cách|thời trang|thoi trang|" +
                "mùa|mua|xuân|xuan|hè|he|thu|đông|dong|fashion|wear|outfit|" +
                "sandal|dép|dep|giày|giay|túi|tui|ví|vi|balo|mũ|mu|nón|non)\\b",
                Pattern.UNICODE_CASE
            ),
            IndustryPlaybook.FASHION_FAST
        ),

        // ── Electronics & Technology ──────────────────────────────────────────
        Map.entry(
            Pattern.compile(
                "(?i)(?U)\\b(iphone|samsung|laptop|macbook|dell|lenovo|asus|hp|" +
                "máy tính|may tinh|điện thoại|dien thoai|tai nghe|headphone|" +
                "earphone|airpod|speaker|loa|chuột|chuot|bàn phím|ban phim|" +
                "keyboard|mouse|monitor|màn hình|man hinh|ssd|hdd|ram|cpu|gpu|" +
                "card|chip|usb|hub|cable|cáp|cap|router|modem|wifi|" +
                "gaming|game|controller|console|playstation|xbox|nintendo|" +
                "camera|máy ảnh|may anh|lens|ống kính|ong kinh|drone|" +
                "smartwatch|tablet|ipad|android|ios|tech|gb|tb|mhz|ghz|4k|8k)\\b",
                Pattern.UNICODE_CASE
            ),
            IndustryPlaybook.ELECTRONICS_TECH
        ),

        // ── Home & Living ─────────────────────────────────────────────────────
        Map.entry(
            Pattern.compile(
                "(?i)(?U)\\b(bàn|ban|ghế|ghe|tủ|tu|kệ|ke|giường|giuong|sofa|" +
                "nội thất|noi that|đèn|den|lamp|đồ gia dụng|do gia dung|" +
                "nồi|noi|chảo|chao|bình|binh|ly|cốc|coc|chén|chen|" +
                "bếp|bep|tủ lạnh|tu lanh|máy lạnh|may lanh|điều hòa|dieu hoa|" +
                "máy giặt|may giat|tivi|tv|home|living|decor|nến|nen|" +
                "thảm|tham|gối|goi|chăn|chan|màn|man|rèm|rem|" +
                "bình hoa|binh hoa|tranh|khung|frame|mirror|gương|guong)\\b",
                Pattern.UNICODE_CASE
            ),
            IndustryPlaybook.HOME_LIVING
        )
    );

    /**
     * Phân loại ngành dựa trên tên SKU và/hoặc category.
     *
     * @param skuName  Tên sản phẩm (từ file Excel của Seller)
     * @param category Category (nếu có). Nullable.
     * @return IndustryPlaybook tương ứng, mặc định là GENERAL.
     */
    public IndustryPlaybook detect(String skuName, String category) {
        // Gộp input để quét một lần
        String combined = buildSearchString(skuName, category);

        // Tầng 1: Keyword Regex — ưu tiên thứ tự FMCG > Fashion > Electronics > Home
        for (Map.Entry<Pattern, IndustryPlaybook> entry : KEYWORD_MAP) {
            if (entry.getKey().matcher(combined).find()) {
                return entry.getValue();
            }
        }

        // Tầng 2: Fallback — Không match bất kỳ ngành nào
        // TODO: Có thể gọi Gemini API ở đây để classify nếu cần độ chính xác cao hơn
        return IndustryPlaybook.GENERAL;
    }

    /**
     * Overload tiện ích — Chỉ cần skuName, không có category.
     */
    public IndustryPlaybook detect(String skuName) {
        return detect(skuName, null);
    }

    /**
     * Tạo chuỗi tìm kiếm tổng hợp từ skuName và category.
     * Lowercase + normalize để pattern matching hoạt động nhất quán.
     */
    private String buildSearchString(String skuName, String category) {
        StringBuilder sb = new StringBuilder();
        if (skuName != null && !skuName.isBlank()) {
            sb.append(skuName.trim().toLowerCase());
        }
        if (category != null && !category.isBlank()) {
            sb.append(" ").append(category.trim().toLowerCase());
        }
        return sb.toString();
    }
}
