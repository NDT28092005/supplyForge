package com.supplyforge.ai.api.dto;

import java.util.List;

public record ExcelPeekResponse(List<String> headers, List<List<String>> sampleRows) {}
