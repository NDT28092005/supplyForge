package com.supplyforge.ai.infrastructure.excel;

import java.util.Map;
import java.util.TreeMap;

/** Một dòng sheet: cột (0-based) → giá trị hiển thị (đã format). */
public record SpreadsheetRow(int rowIndex, TreeMap<Integer, String> cellsByColumn) {

    public String get(int col) {
        return cellsByColumn.getOrDefault(col, "").trim();
    }

    public static SpreadsheetRow of(int rowIndex, Map<Integer, String> cells) {
        TreeMap<Integer, String> copy = new TreeMap<>();
        cells.forEach((k, v) -> copy.put(k, v == null ? "" : v));
        return new SpreadsheetRow(rowIndex, copy);
    }
}
