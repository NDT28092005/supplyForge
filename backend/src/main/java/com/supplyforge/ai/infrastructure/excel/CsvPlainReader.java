package com.supplyforge.ai.infrastructure.excel;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** CSV đơn giản (dấu phẩy, không xử lý dấu ngoặc phức tạp) — phù hợp MVP, RAM thấp. */
@Component
public class CsvPlainReader {

    public ExcelPeekResult peekHeaderAndFiveSamples(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            List<String> headers = headerLine == null ? List.of() : splitLine(headerLine);
            List<List<String>> samples = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                samples.add(splitLine(line));
            }
            return new ExcelPeekResult(headers, samples);
        }
    }

    public void streamDataRows(Path path, Consumer<SpreadsheetRow> onDataRow) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                return;
            }
            String line;
            int row = 1;
            while ((line = br.readLine()) != null) {
                List<String> cells = splitLine(line);
                if (cells.stream().allMatch(String::isBlank)) {
                    row++;
                    continue;
                }
                TreeMapBuilder b = new TreeMapBuilder();
                for (int c = 0; c < cells.size(); c++) {
                    b.put(c, cells.get(c) == null ? "" : cells.get(c).trim());
                }
                onDataRow.accept(SpreadsheetRow.of(row, b.build()));
                row++;
            }
        }
    }

    private static List<String> splitLine(String line) {
        String[] parts = line.split(",", -1);
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            out.add(p.trim());
        }
        return out;
    }

    private static final class TreeMapBuilder {
        private final java.util.TreeMap<Integer, String> map = new java.util.TreeMap<>();

        void put(int i, String v) {
            map.put(i, v);
        }

        java.util.TreeMap<Integer, String> build() {
            return map;
        }
    }
}
