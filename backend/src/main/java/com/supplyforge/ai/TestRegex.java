import java.util.*;
import java.util.regex.*;

public class TestRegex {
    private static final List<Map.Entry<String, Pattern>> KEYWORD_MAP = List.of(
        Map.entry(
            "FMCG_FOOD",
            Pattern.compile(
                "(?i)(?U)\\b(sữa|sua|bột|bot|thực phẩm|thuc pham|ăn|uống|an uong|" +
                "đồ ăn|do an|bánh|banh|kẹo|keo|nước|nuoc|đồ uống|do uong|" +
                "vitamin|supplement|snack|mì|mi\\b|gạo|gao|dầu ăn|dau an|" +
                "gia vị|gia vi|sốt|sot|yogurt|fmcg|date|hsd|han su dung)\\b",
                Pattern.UNICODE_CASE
            )
        ),
        Map.entry(
            "FASHION_FAST",
            Pattern.compile(
                "(?i)(?U)\\b(áo|ao|quần|quan|váy|vay|đầm|dam|đồ\\b|do\\b|size|" +
                "xl|xxl|m\\b|l\\b|s\\b|shirt|dress|pants|skirt|jacket|coat|" +
                "jeans|hoodie|sweater|blouse|suit|phong cách|thời trang|thoi trang|" +
                "mùa|mua|xuân|xuan|hè|he|thu|đông|dong|fashion|wear|outfit|" +
                "sandal|dép|dep|giày|giay|túi|tui|ví|vi\\b|balo|mũ|mu|nón|non)\\b",
                Pattern.UNICODE_CASE
            )
        ),
        Map.entry(
            "ELECTRONICS_TECH",
            Pattern.compile(
                "(?i)(?U)\\b(iphone|samsung|laptop|macbook|dell|lenovo|asus|hp\\b|" +
                "máy tính|may tinh|điện thoại|dien thoai|tai nghe|headphone|" +
                "earphone|airpod|speaker|loa|chuột|chuot|bàn phím|ban phim|" +
                "keyboard|mouse|monitor|màn hình|man hinh|ssd|hdd|ram|cpu|gpu|" +
                "card|chip|usb|hub|cable|cáp|cap|router|modem|wifi|" +
                "gaming|game|controller|console|playstation|xbox|nintendo|" +
                "camera|máy ảnh|may anh|lens|ống kính|ong kinh|drone|" +
                "smartwatch|tablet|ipad|android|ios|tech|gb|tb|mhz|ghz|4k|8k)\\b",
                Pattern.UNICODE_CASE
            )
        ),
        Map.entry(
            "HOME_LIVING",
            Pattern.compile(
                "(?i)(?U)\\b(bàn|ban\\b|ghế|ghe|tủ|tu\\b|kệ|ke|giường|giuong|sofa|" +
                "nội thất|noi that|đèn|den|lamp|đồ gia dụng|do gia dung|" +
                "nồi|noi|chảo|chao|bình|binh|ly|cốc|coc|chén|chen|" +
                "bếp|bep|tủ lạnh|tu lanh|máy lạnh|may lanh|điều hòa|dieu hoa|" +
                "máy giặt|may giat|tivi|tv|home|living|decor|nến|nen|" +
                "thảm|tham|gối|goi|chăn|chan|màn|man\\b|rèm|rem|" +
                "bình hoa|binh hoa|tranh|khung|frame|mirror|gương|guong)\\b",
                Pattern.UNICODE_CASE
            )
        )
    );

    public static void main(String[] args) {
        String[] testCases = {
            "Áo khoác Hoodie Unisex oversize Cotton dày",
            "Laptop ASUS ROG Strix G16 Gaming Core i7",
            "Sữa tươi tiệt trùng TH True Milk ít đường 1L HSD cận",
            "Ghế Sofa giường bọc nỉ thông minh phòng khách",
            "Hộp bút màu học sinh Deli 24 màu tổng hợp"
        };

        for (String tc : testCases) {
            System.out.println("SKU: " + tc);
            boolean matched = false;
            for (Map.Entry<String, Pattern> entry : KEYWORD_MAP) {
                Matcher m = entry.getValue().matcher(tc.toLowerCase());
                if (m.find()) {
                    System.out.println("  -> MATCHED: " + entry.getKey() + " (matched text: '" + m.group() + "')");
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                System.out.println("  -> FALLBACK: GENERAL");
            }
        }
    }
}
