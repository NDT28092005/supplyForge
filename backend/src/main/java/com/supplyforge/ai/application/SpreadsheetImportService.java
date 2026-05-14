package com.supplyforge.ai.application;

import com.supplyforge.ai.api.dto.ExcelPeekResponse;
import com.supplyforge.ai.api.dto.SpreadsheetImportResponse;
import com.supplyforge.ai.domain.entity.DataSource;
import com.supplyforge.ai.domain.model.DataSourceStatus;
import com.supplyforge.ai.domain.repository.DataSourceRepository;
import com.supplyforge.ai.infrastructure.excel.CsvPlainReader;
import com.supplyforge.ai.infrastructure.excel.ExcelPeekResult;
import com.supplyforge.ai.infrastructure.excel.XlsxFirstSheetEventReader;
import com.supplyforge.ai.infrastructure.gemini.GeminiColumnMappingService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SpreadsheetImportService {

    private final XlsxFirstSheetEventReader xlsxFirstSheetEventReader;
    private final CsvPlainReader csvPlainReader;
    private final GeminiColumnMappingService geminiColumnMappingService;
    private final SpreadsheetImportTx spreadsheetImportTx;
    private final DataSourceStatusService dataSourceStatusService;
    private final DataSourceRepository dataSourceRepository;

    public SpreadsheetImportService(
            XlsxFirstSheetEventReader xlsxFirstSheetEventReader,
            CsvPlainReader csvPlainReader,
            GeminiColumnMappingService geminiColumnMappingService,
            SpreadsheetImportTx spreadsheetImportTx,
            DataSourceStatusService dataSourceStatusService,
            DataSourceRepository dataSourceRepository) {
        this.xlsxFirstSheetEventReader = xlsxFirstSheetEventReader;
        this.csvPlainReader = csvPlainReader;
        this.geminiColumnMappingService = geminiColumnMappingService;
        this.spreadsheetImportTx = spreadsheetImportTx;
        this.dataSourceStatusService = dataSourceStatusService;
        this.dataSourceRepository = dataSourceRepository;
    }

    /** Không bọc một @Transactional dài — DataSource commit trước khi ingest. */
    public SpreadsheetImportResponse importSpreadsheet(Long workspaceId, MultipartFile file) throws IOException {
        List<DataSource> processing = dataSourceRepository.findByWorkspaceIdAndStatus(workspaceId, DataSourceStatus.PROCESSING);
        List<DataSource> pending = dataSourceRepository.findByWorkspaceIdAndStatus(workspaceId, DataSourceStatus.PENDING);
        if (!processing.isEmpty() || !pending.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hệ thống đang xử lý một file khác, vui lòng chờ");
        }

        Path temp = Files.createTempFile("sf-import-", suffix(file.getOriginalFilename()));
        DataSource dataSource = null;
        try {
            file.transferTo(temp);
            boolean csv = isCsv(file.getOriginalFilename());
            ExcelPeekResult peek = csv
                    ? csvPlainReader.peekHeaderAndFiveSamples(temp)
                    : xlsxFirstSheetEventReader.peekHeaderAndFiveSamples(temp);

            var mappingJson = geminiColumnMappingService.inferColumnMapping(peek);

            dataSource = spreadsheetImportTx.createPendingDataSource(workspaceId, file, mappingJson, temp);

            long rows = spreadsheetImportTx.ingestRows(dataSource.getId(), workspaceId, temp, csv, peek, mappingJson);

            spreadsheetImportTx.markReady(dataSource.getId(), rows);

            return new SpreadsheetImportResponse(
                    dataSource.getId(),
                    rows,
                    mappingJson,
                    new ExcelPeekResponse(peek.headers(), peek.sampleRows()));
        } catch (IOException e) {
            dataSourceStatusService.markFailed(dataSource == null ? null : dataSource.getId(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            dataSourceStatusService.markFailed(dataSource == null ? null : dataSource.getId(), e.getMessage());
            throw e;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static boolean isCsv(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private static String suffix(String original) {
        if (original == null || !original.contains(".")) {
            return ".dat";
        }
        return original.substring(original.lastIndexOf('.'));
    }
}
