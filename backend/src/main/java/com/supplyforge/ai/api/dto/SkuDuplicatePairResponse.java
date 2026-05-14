package com.supplyforge.ai.api.dto;

public record SkuDuplicatePairResponse(
        long skuAId, String skuAName, long skuBId, String skuBName, int distance) {}
