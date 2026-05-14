package com.supplyforge.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.supplyforge.ai.domain.entity.DataSource;
import com.supplyforge.ai.domain.entity.InventoryRecord;
import com.supplyforge.ai.domain.entity.Sku;
import com.supplyforge.ai.domain.entity.Workspace;
import com.supplyforge.ai.domain.model.DataSourceStatus;
import com.supplyforge.ai.domain.repository.DataSourceRepository;
import com.supplyforge.ai.domain.repository.InventoryRecordRepository;
import com.supplyforge.ai.domain.repository.SkuRepository;
import com.supplyforge.ai.domain.repository.WorkspaceRepository;
import com.supplyforge.ai.infrastructure.excel.CsvPlainReader;
import com.supplyforge.ai.infrastructure.excel.ExcelPeekResult;
import com.supplyforge.ai.infrastructure.excel.SpreadsheetRow;
import com.supplyforge.ai.infrastructure.excel.XlsxFirstSheetEventReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SpreadsheetImportTx {

    private static final int BATCH = 200;

    private final WorkspaceRepository workspaceRepository;
    private final DataSourceRepository dataSourceRepository;
    private final SkuRepository skuRepository;
    private final InventoryRecordRepository inventoryRecordRepository;
    private final XlsxFirstSheetEventReader xlsxFirstSheetEventReader;
    private final CsvPlainReader csvPlainReader;

    @PersistenceContext
    private EntityManager entityManager;

    public SpreadsheetImportTx(
            WorkspaceRepository workspaceRepository,
            DataSourceRepository dataSourceRepository,
            SkuRepository skuRepository,
            InventoryRecordRepository inventoryRecordRepository,
            XlsxFirstSheetEventReader xlsxFirstSheetEventReader,
            CsvPlainReader csvPlainReader) {
        this.workspaceRepository = workspaceRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.skuRepository = skuRepository;
        this.inventoryRecordRepository = inventoryRecordRepository;
        this.xlsxFirstSheetEventReader = xlsxFirstSheetEventReader;
        this.csvPlainReader = csvPlainReader;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DataSource createPendingDataSource(
            Long workspaceId, MultipartFile file, JsonNode mappingJson, Path tempPath) {
        Workspace workspace = workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace không tồn tại"));
        DataSource ds = new DataSource();
        ds.setWorkspace(workspace);
        ds.setOriginalFilename(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
        ds.setStorageKey("temp-local:" + tempPath.getFileName());
        ds.setContentType(file.getContentType());
        ds.setByteSize(file.getSize());
        ds.setStatus(DataSourceStatus.PROCESSING);
        ds.setColumnMappingJson(mappingJson);
        return dataSourceRepository.save(ds);
    }

    @Transactional
    public long ingestRows(
            Long dataSourceId,
            Long workspaceId,
            Path temp,
            boolean csv,
            ExcelPeekResult peek,
            JsonNode mappingJson)
            throws IOException {
        DataSource dataSource = dataSourceRepository
                .findById(dataSourceId)
                .orElseThrow(() -> new IllegalStateException("dataSource missing"));
        Workspace workspace = workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() -> new IllegalStateException("workspace missing"));

        int maxCols = maxColumnWidth(peek);
        ColumnMappingResolver.ResolvedMapping mapping =
                ColumnMappingResolver.resolve(mappingJson, Math.max(1, maxCols));

        ImportAccumulator acc = new ImportAccumulator(workspace, dataSource, mapping,
                skuRepository.findByWorkspaceId(workspaceId));
        if (csv) {
            csvPlainReader.streamDataRows(temp, acc::onRow);
        } else {
            xlsxFirstSheetEventReader.streamDataRows(temp, acc::onRow);
        }
        acc.flush();
        return acc.imported.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markReady(Long dataSourceId, long rowCount) {
        dataSourceRepository.findById(dataSourceId).ifPresent(ds -> {
            ds.setStatus(DataSourceStatus.READY);
            ds.setRowCount(rowCount);
            dataSourceRepository.save(ds);
        });
    }

    private static int maxColumnWidth(ExcelPeekResult peek) {
        int w = peek.headers().size();
        for (List<String> row : peek.sampleRows()) {
            w = Math.max(w, row.size());
        }
        return Math.max(w, 1);
    }

    private final class ImportAccumulator {

        private final Workspace workspace;
        private final DataSource dataSource;
        private final ColumnMappingResolver.ResolvedMapping mapping;
        private final Map<String, Long> skuByNormalized = new HashMap<>();
        private final List<InventoryRecord> batch = new ArrayList<>(BATCH);
        private final AtomicLong imported = new AtomicLong(0);

        ImportAccumulator(Workspace workspace, DataSource dataSource,
                ColumnMappingResolver.ResolvedMapping mapping, List<Sku> existingSkus) {
            this.workspace = workspace;
            this.dataSource = dataSource;
            this.mapping = mapping;
            // Pre-populate cache với SKU đã có trong DB → tránh tạo duplicate khi import lại
            for (Sku s : existingSkus) {
                skuByNormalized.putIfAbsent(s.getNormalizedName(), s.getId());
            }
        }

        void onRow(SpreadsheetRow row) {
            String skuRaw = row.get(mapping.skuColumnIndex());
            if (skuRaw.isBlank()) {
                return;
            }
            BigDecimal qty = DecimalParse.parse(row.get(mapping.quantityColumnIndex()));
            Integer sellCol = mapping.effectiveSellingPriceCol();
            Integer costCol = mapping.effectiveCostCol();
            BigDecimal selling = sellCol == null ? null : DecimalParse.parse(row.get(sellCol));
            BigDecimal cost = costCol == null ? null : DecimalParse.parse(row.get(costCol));

            LocalDate recordDate =
                    mapping.recordDateColumnIndex() == null
                            ? LocalDate.now()
                            : ColumnMappingResolver.parseRecordDate(
                                    row.get(mapping.recordDateColumnIndex()));

            String norm = normalizeSkuName(skuRaw);
            Long skuId = skuByNormalized.computeIfAbsent(norm, key -> findOrCreateSku(key, skuRaw));

            InventoryRecord rec = new InventoryRecord();
            rec.setSku(skuRepository.getReferenceById(skuId));
            rec.setRecordDate(recordDate);
            rec.setQuantity(qty);
            rec.setCostPrice(cost);
            rec.setSellingPrice(selling);
            rec.setDataSource(dataSource);
            batch.add(rec);
            if (batch.size() >= BATCH) {
                flush();
            }
        }

        void flush() {
            if (batch.isEmpty()) {
                return;
            }
            inventoryRecordRepository.saveAll(batch);
            inventoryRecordRepository.flush();
            imported.addAndGet(batch.size());
            batch.clear();
            entityManager.clear();
        }

        private Long findOrCreateSku(String normalized, String original) {
            // Double-check DB (phòng race condition multi-thread hoặc cache miss)
            List<Sku> existing = skuRepository.findByWorkspaceId(workspace.getId());
            for (Sku s : existing) {
                if (normalized.equals(s.getNormalizedName())) {
                    return s.getId();
                }
            }
            Sku sku = new Sku();
            sku.setWorkspace(workspace);
            sku.setOriginalName(original);
            sku.setNormalizedName(normalized);
            sku.setDuplicate(false);
            sku = skuRepository.save(sku);
            skuRepository.flush();
            return sku.getId();
        }
    }

    private static String normalizeSkuName(String raw) {
        // Bước 1: trim + lowercase + collapse whitespace
        String base = raw.trim().toLowerCase(java.util.Locale.forLanguageTag("vi-VN")).replaceAll("\\s+", " ");
        // Bước 2: xóa dấu tiếng Việt để Levenshtein so sánh đúng
        return VietnameseNormalizer.stripAccents(base);
    }
}
