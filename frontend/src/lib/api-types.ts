export type SpreadsheetImportResponse = {
  dataSourceId: number;
  rowsImported: number;
  columnMapping: unknown;
  peekUsedForMapping: { headers: string[]; sampleRows: string[][] };
};

export type SkuDuplicatePair = {
  skuAId: number;
  skuAName: string;
  skuBId: number;
  skuBName: string;
  distance: number;
};

export type DeadStockDashboard = {
  totalDeadStockValueVnd: number;
  estimatedLoss30DaysVnd: number;
  totalInventoryValueVnd: number;
  staleSkuCount: number;
  totalRootSkuCount: number;
  topStaleSkus: Array<{
    skuId: number;
    name: string;
    frozenValueVnd: number;
    lastRecordDate: string;
  }>;
};
