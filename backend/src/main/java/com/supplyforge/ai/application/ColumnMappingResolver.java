package com.supplyforge.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

/** Đọc JSON mapping (LLM hoặc heuristic) thành chỉ số cột an toàn. */
public final class ColumnMappingResolver {

    private static final Set<DateTimeFormatter> DATE_FORMATTERS = Set.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/yyyy", Locale.forLanguageTag("vi-VN")),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")));

    private ColumnMappingResolver() {}

    public record ResolvedMapping(
            int skuColumnIndex,
            int quantityColumnIndex,
            Integer sellingPriceColumnIndex,
            Integer costPriceColumnIndex,
            Integer recordDateColumnIndex,
            boolean priceIsCost) {

        public Integer effectiveSellingPriceCol() {
            if (priceIsCost) {
                return null;
            }
            return sellingPriceColumnIndex;
        }

        public Integer effectiveCostCol() {
            if (priceIsCost && sellingPriceColumnIndex != null) {
                return sellingPriceColumnIndex;
            }
            return costPriceColumnIndex;
        }
    }

    public static ResolvedMapping resolve(JsonNode mapping, int maxColumnExclusive) {
        int sku = clampIndex(mapping.path("sku_column_index").asInt(0), maxColumnExclusive);
        int qty = clampIndex(mapping.path("quantity_column_index").asInt(1), maxColumnExclusive);
        Integer sell = optIndex(mapping.get("selling_price_column_index"), maxColumnExclusive);
        if (sell == null && mapping.has("price_column_index")) {
            sell = optIndex(mapping.get("price_column_index"), maxColumnExclusive);
        }
        Integer cost = optIndex(mapping.get("cost_price_column_index"), maxColumnExclusive);
        Integer dateCol = optIndex(mapping.get("record_date_column_index"), maxColumnExclusive);
        boolean priceIsCost = mapping.path("price_is_cost").asBoolean(false);
        return new ResolvedMapping(sku, qty, sell, cost, dateCol, priceIsCost);
    }

    public static LocalDate parseRecordDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDate.now();
        }
        String s = raw.trim();
        for (DateTimeFormatter f : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException ignored) {
                // next
            }
        }
        return LocalDate.now();
    }

    private static Integer optIndex(JsonNode node, int maxExclusive) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        int v = node.asInt(-1);
        if (v < 0) {
            return null;
        }
        return clampIndex(v, maxExclusive);
    }

    private static int clampIndex(int index, int maxExclusive) {
        if (maxExclusive <= 0) {
            return Math.max(0, index);
        }
        return Math.min(Math.max(0, index), maxExclusive - 1);
    }
}
