package com.supplyforge.ai.application;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Tiện ích xóa dấu tiếng Việt và normalize chuỗi để so sánh SKU fuzzy.
 * <p>
 * Ví dụ: "Áo thun đỏ" → "ao thun do"
 *        "Mũ bảo hiểm" → "mu bao hiem"
 * </p>
 */
public final class VietnameseNormalizer {

    private static final Pattern NON_ASCII_MARK = Pattern.compile("\\p{M}");
    // Ký tự đ/Đ không bị decompose bởi NFD, cần thay thủ công
    private static final Pattern D_STROKE = Pattern.compile("[đĐ]");

    private VietnameseNormalizer() {}

    /**
     * Xóa dấu và chuyển về chữ thường dạng ASCII thuần.
     * "Áo thun đỏ" → "ao thun do"
     */
    public static String stripAccents(String input) {
        if (input == null) return "";
        // Thay đ/Đ trước khi NFD decompose
        String replaced = D_STROKE.matcher(input).replaceAll(m ->
                m.group().equals("đ") ? "d" : "D");
        // NFD decompose: tách base char + combining mark
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        // Xóa combining marks (dấu huyền, sắc, nặng, hỏi, ngã, mũ, móc...)
        return NON_ASCII_MARK.matcher(decomposed).replaceAll("").toLowerCase();
    }
}
