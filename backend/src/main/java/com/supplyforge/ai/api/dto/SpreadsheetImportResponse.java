package com.supplyforge.ai.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record SpreadsheetImportResponse(
        Long dataSourceId,
        long rowsImported,
        JsonNode columnMapping,
        ExcelPeekResponse peekUsedForMapping) {}
