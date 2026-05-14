package com.supplyforge.ai.api.dto;

import java.util.List;

public class FileSampleDTO {
    private List<String> headers;
    private List<List<String>> sampleRows;

    public FileSampleDTO() {}

    public FileSampleDTO(List<String> headers, List<List<String>> sampleRows) {
        this.headers = headers;
        this.sampleRows = sampleRows;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<List<String>> getSampleRows() {
        return sampleRows;
    }

    public void setSampleRows(List<List<String>> sampleRows) {
        this.sampleRows = sampleRows;
    }
}
