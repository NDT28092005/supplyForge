package com.supplyforge.ai.api;

import com.supplyforge.ai.api.dto.SpreadsheetImportResponse;
import com.supplyforge.ai.application.SpreadsheetImportService;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/v1/workspaces/{workspaceId}/imports", produces = MediaType.APPLICATION_JSON_VALUE)
public class SpreadsheetImportController {

    private final SpreadsheetImportService spreadsheetImportService;

    public SpreadsheetImportController(SpreadsheetImportService spreadsheetImportService) {
        this.spreadsheetImportService = spreadsheetImportService;
    }

    @PostMapping(value = "/spreadsheet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SpreadsheetImportResponse importSpreadsheet(
            @PathVariable Long workspaceId, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file trống");
        }
        return spreadsheetImportService.importSpreadsheet(workspaceId, file);
    }
}
