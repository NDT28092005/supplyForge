package com.supplyforge.ai.infrastructure.excel;

import java.util.List;

public record ExcelPeekResult(List<String> headers, List<List<String>> sampleRows) {}
