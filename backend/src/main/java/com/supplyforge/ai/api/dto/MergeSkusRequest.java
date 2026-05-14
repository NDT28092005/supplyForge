package com.supplyforge.ai.api.dto;

import jakarta.validation.constraints.NotNull;

public record MergeSkusRequest(@NotNull Long parentSkuId, @NotNull Long childSkuId) {}
