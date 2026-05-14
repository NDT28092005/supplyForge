package com.supplyforge.ai.api.dto;

import java.util.List;

public class AiMappingRuleDTO {
    private List<MappingRule> mappingRules;

    public List<MappingRule> getMappingRules() {
        return mappingRules;
    }

    public void setMappingRules(List<MappingRule> mappingRules) {
        this.mappingRules = mappingRules;
    }

    public static class MappingRule {
        private String dbField;
        private String csvHeader;
        private double confidence;

        // Getters and setters
        public String getDbField() { return dbField; }
        public void setDbField(String dbField) { this.dbField = dbField; }
        public String getCsvHeader() { return csvHeader; }
        public void setCsvHeader(String csvHeader) { this.csvHeader = csvHeader; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
