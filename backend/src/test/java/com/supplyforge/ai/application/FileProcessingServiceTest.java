package com.supplyforge.ai.application;

import com.supplyforge.ai.api.dto.ExcelPeekResponse;
import com.supplyforge.ai.api.dto.SpreadsheetImportResponse;
import com.supplyforge.ai.domain.entity.DataSource;
import com.supplyforge.ai.infrastructure.excel.CsvPlainReader;
import com.supplyforge.ai.infrastructure.excel.ExcelPeekResult;
import com.supplyforge.ai.infrastructure.excel.XlsxFirstSheetEventReader;
import com.supplyforge.ai.infrastructure.gemini.GeminiColumnMappingService;
import com.supplyforge.ai.domain.repository.DataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

    @Mock
    private XlsxFirstSheetEventReader xlsxFirstSheetEventReader;

    @Mock
    private CsvPlainReader csvPlainReader;

    @Mock
    private GeminiColumnMappingService geminiColumnMappingService;

    @Mock
    private SpreadsheetImportTx spreadsheetImportTx;

    @Mock
    private DataSourceStatusService dataSourceStatusService;

    @Mock
    private DataSourceRepository dataSourceRepository;

    @InjectMocks
    private SpreadsheetImportService fileProcessingService;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Mock upload Excel lớn: Đảm bảo sử dụng SAX Stream reader thay vì load toàn bộ file vào RAM")
    void testLargeExcelUploadUsesStreaming() throws Exception {
        // Arrange
        Long workspaceId = 1L;
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large_inventory.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "dummy data".getBytes());

        ExcelPeekResult mockPeek = new ExcelPeekResult(List.of("SKU", "QTY"), List.of(List.of("A", "1")));
        when(xlsxFirstSheetEventReader.peekHeaderAndFiveSamples(any(Path.class))).thenReturn(mockPeek);
        
        com.fasterxml.jackson.databind.JsonNode mockMapping = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        when(geminiColumnMappingService.inferColumnMapping(mockPeek)).thenReturn(mockMapping);
        
        DataSource mockDs = new DataSource();
        mockDs.setId(100L);
        when(spreadsheetImportTx.createPendingDataSource(eq(workspaceId), eq(largeFile), eq(mockMapping), any(Path.class)))
                .thenReturn(mockDs);
        when(spreadsheetImportTx.ingestRows(eq(100L), eq(workspaceId), any(Path.class), eq(false), eq(mockPeek), eq(mockMapping)))
                .thenReturn(100000L); // 100k rows

        // Act
        SpreadsheetImportResponse response = fileProcessingService.importSpreadsheet(workspaceId, largeFile);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.dataSourceId());
        assertEquals(100000L, response.rowsImported());

        // Quan trọng nhất: verify rẳng SAX/Event reader được gọi, không phải cơ chế load DOM vào bộ nhớ
        verify(xlsxFirstSheetEventReader).peekHeaderAndFiveSamples(any(Path.class));
        verify(spreadsheetImportTx).ingestRows(eq(100L), eq(workspaceId), any(Path.class), eq(false), eq(mockPeek), eq(mockMapping));
    }
}
