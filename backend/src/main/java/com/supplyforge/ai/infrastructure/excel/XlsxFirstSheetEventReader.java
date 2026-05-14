package com.supplyforge.ai.infrastructure.excel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Đọc .xlsx qua SAX (XSSFReader) — không load toàn workbook vào heap.
 */
@Component
public class XlsxFirstSheetEventReader {

    private static final DataFormatter FORMATTER = new DataFormatter();

    public ExcelPeekResult peekHeaderAndFiveSamples(Path path) throws IOException {
        PeekHandler handler = new PeekHandler();
        parseFirstSheet(path, handler);
        return new ExcelPeekResult(handler.headers, handler.samples);
    }

    /** Gọi cho mỗi dòng dữ liệu (bỏ dòng 0 = header). Bỏ qua dòng trống. */
    public void streamDataRows(Path path, Consumer<SpreadsheetRow> onDataRow) throws IOException {
        parseFirstSheet(path, new StreamHandler(onDataRow));
    }

    private void parseFirstSheet(Path path, XSSFSheetXMLHandler.SheetContentsHandler handler) throws IOException {
        try (OPCPackage pkg = OPCPackage.open(Files.newInputStream(path))) {
            XSSFReader reader = new XSSFReader(pkg);
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            StylesTable styles = reader.getStylesTable();
            Iterator<InputStream> sheets = reader.getSheetsData();
            if (sheets == null || !sheets.hasNext()) {
                return;
            }
            try (InputStream sheetStream = sheets.next()) {
                XMLReader parser = SAXHelper.newXMLReader();
                XSSFSheetXMLHandler contentHandler =
                        new XSSFSheetXMLHandler(styles, null, sst, handler, FORMATTER, false);
                parser.setContentHandler(contentHandler);
                try {
                    parser.parse(new InputSource(sheetStream));
                } catch (PeekAbortException e) {
                    // dừng sớm khi peek
                } catch (SAXException e) {
                    if (e.getCause() instanceof PeekAbortException) {
                        return;
                    }
                    throw new IOException("Lỗi đọc XML trong sheet XLSX", e);
                } catch (RuntimeException e) {
                    if (e instanceof PeekAbortException) {
                        return;
                    }
                    throw e;
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Không đọc được file XLSX (định dạng Office 2007+ zip/XML)", e);
        }
    }

    private static final class PeekHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final List<String> headers = new ArrayList<>();
        private final List<List<String>> samples = new ArrayList<>();
        private TreeMap<Integer, String> current = new TreeMap<>();

        @Override
        public void startRow(int rowNum) {
            current = new TreeMap<>();
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == 0) {
                headers.addAll(denseRow(current));
                return;
            }
            if (rowNum <= 5) {
                samples.add(denseRow(current));
            }
            if (rowNum >= 5) {
                throw new PeekAbortException();
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int col = new CellReference(cellReference).getCol();
            current.put(col, formattedValue == null ? "" : formattedValue.trim());
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // no-op
        }
    }

    private static final class StreamHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final Consumer<SpreadsheetRow> onDataRow;
        private TreeMap<Integer, String> current = new TreeMap<>();

        StreamHandler(Consumer<SpreadsheetRow> onDataRow) {
            this.onDataRow = onDataRow;
        }

        @Override
        public void startRow(int rowNum) {
            current = new TreeMap<>();
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == 0) {
                return;
            }
            if (current.values().stream().allMatch(String::isBlank)) {
                return;
            }
            onDataRow.accept(SpreadsheetRow.of(rowNum, new TreeMap<>(current)));
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int col = new CellReference(cellReference).getCol();
            current.put(col, formattedValue == null ? "" : formattedValue.trim());
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // no-op
        }
    }

    private static List<String> denseRow(TreeMap<Integer, String> map) {
        if (map.isEmpty()) {
            return List.of();
        }
        int max = map.lastKey();
        List<String> out = new ArrayList<>(max + 1);
        for (int i = 0; i <= max; i++) {
            out.add(map.getOrDefault(i, ""));
        }
        return out;
    }
}
