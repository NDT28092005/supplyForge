package com.supplyforge.ai.application;

import java.math.BigDecimal;

/** Parse số cho file seller VN — ưu tiên đơn giản, an toàn khi sai định dạng. */
public final class DecimalParse {

    private DecimalParse() {}

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        String s = raw.trim().replace(" ", "").replace("\u00A0", "");
        int comma = s.lastIndexOf(',');
        int dot = s.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            if (comma > dot) {
                s = s.replace(".", "").replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (comma >= 0) {
            s = s.replace(',', '.');
        } else {
            long dots = s.chars().filter(c -> c == '.').count();
            if (dots > 1) {
                s = s.replace(".", "");
            }
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
