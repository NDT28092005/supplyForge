'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';

// Schema chuẩn của hệ thống
const SYSTEM_SCHEMA = [
  { field: 'itemSku', label: 'SKU Sản phẩm' },
  { field: 'productName', label: 'Tên Sản Phẩm' },
  { field: 'price', label: 'Giá Bán' },
  { field: 'cost', label: 'Giá Vốn' },
  { field: 'stock', label: 'Tồn Kho' },
  { field: 'orderDate', label: 'Ngày Đặt Hàng' },
  { field: 'orderId', label: 'Mã Đơn Hàng' },
  { field: 'quantity', label: 'Số Lượng' },
];

interface AiSuggestion {
  dbField: string;
  csvHeader: string;
  confidence: number;
}

interface DataMappingConfirmProps {
  csvHeaders: string[];
  sampleDataRow: string[];
  aiSuggestions: AiSuggestion[];
  onConfirm: (finalMapping: Record<string, string>) => void;
}

export default function DataMappingConfirm({
  csvHeaders,
  sampleDataRow,
  aiSuggestions,
  onConfirm,
}: DataMappingConfirmProps) {
  // state lưu trữ map giữa: SystemField -> CsvHeader được chọn
  const [mapping, setMapping] = useState<Record<string, string>>({});

  useEffect(() => {
    // Khởi tạo mapping dựa trên gợi ý của AI (nếu có)
    const initialMapping: Record<string, string> = {};
    if (aiSuggestions && Array.isArray(aiSuggestions)) {
      aiSuggestions.forEach((sug) => {
        initialMapping[sug.dbField] = sug.csvHeader;
      });
    }
    setMapping(initialMapping);
  }, [aiSuggestions]);

  const handleSelectChange = (dbField: string, selectedHeader: string) => {
    setMapping((prev) => ({ ...prev, [dbField]: selectedHeader }));
  };

  const getConfidence = (dbField: string, currentHeader: string) => {
    if (!aiSuggestions || !Array.isArray(aiSuggestions)) return null;
    const sug = aiSuggestions.find((s) => s.dbField === dbField);
    if (sug && sug.csvHeader === currentHeader) {
      return sug.confidence;
    }
    return null; // Do user tự chọn tay thì không có AI confidence
  };

  const getSampleValue = (header: string) => {
    if (!header) return '---';
    const idx = csvHeaders.indexOf(header);
    return idx >= 0 ? sampleDataRow[idx] : '---';
  };

  const handleConfirm = () => {
    // Loại bỏ các map rỗng (Bỏ qua)
    const finalMapping: Record<string, string> = {};
    Object.keys(mapping).forEach((key) => {
      if (mapping[key] && mapping[key] !== 'IGNORE') {
        finalMapping[key] = mapping[key];
      }
    });
    onConfirm(finalMapping);
  };

  return (
    <section className="min-h-screen bg-background flex flex-col items-center py-24 px-6">
      <motion.div
        className="w-full max-w-4xl"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: 'easeOut' }}
      >
        <p className="text-xs font-mono text-secondary tracking-widest uppercase mb-4 opacity-60">
          Human in the loop · Data Ingestion
        </p>
        <h2 className="font-tight font-black tracking-tightest text-primary text-4xl mb-12">
          Xác nhận Mapping Dữ Liệu
        </h2>

        {/* Table Header */}
        <div className="grid grid-cols-3 gap-6 mb-4 px-4 text-xs font-mono text-secondary uppercase tracking-wider opacity-50">
          <div>Trường Hệ Thống</div>
          <div>Cột Trong File (AI Đề Xuất)</div>
          <div>Giá Trị Mẫu</div>
        </div>

        {/* Table Body */}
        <div className="flex flex-col gap-3 mb-12">
          {SYSTEM_SCHEMA.map((schema) => {
            const currentSelected = mapping[schema.field] || 'IGNORE';
            const confidence = getConfidence(schema.field, currentSelected);
            const isLowConfidence = confidence !== null && confidence < 0.7;

            return (
              <div
                key={schema.field}
                className="grid grid-cols-3 gap-6 items-center border border-border-base bg-surface px-4 py-4 rounded-sm"
              >
                {/* Cột Trái: System Field */}
                <div>
                  <p className="font-tight font-bold text-primary text-base">
                    {schema.label}
                  </p>
                  <p className="text-xs font-mono text-secondary opacity-50">
                    {schema.field}
                  </p>
                </div>

                {/* Cột Giữa: Dropdown */}
                <div>
                  <select
                    value={currentSelected}
                    onChange={(e) => handleSelectChange(schema.field, e.target.value)}
                    className={`w-full bg-background border ${
                      isLowConfidence ? 'border-warning text-warning' : 'border-border-base text-primary'
                    } rounded-sm px-3 py-2 text-sm outline-none focus:border-secondary transition-colors`}
                  >
                    <option value="IGNORE">-- Bỏ qua trường này --</option>
                    {csvHeaders.map((h, i) => (
                      <option key={i} value={h}>
                        {h}
                      </option>
                    ))}
                  </select>
                  {isLowConfidence && (
                    <p className="text-[10px] font-mono text-warning mt-1">
                      AI không chắc chắn (Độ tin cậy: {Math.round(confidence * 100)}%)
                    </p>
                  )}
                </div>

                {/* Cột Phải: Sample Value */}
                <div className="truncate">
                  <span className="font-mono text-sm text-secondary bg-background px-2 py-1 rounded-sm border border-border-base">
                    {getSampleValue(currentSelected)}
                  </span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Action Button */}
        <div className="flex justify-end border-t border-border-base pt-8">
          <motion.button
            onClick={handleConfirm}
            className="bg-accent text-background font-tight font-bold text-sm px-8 py-4 rounded-sm"
            whileHover={{ scale: 1.015, backgroundColor: '#deff5a' }}
            whileTap={{ scale: 0.985 }}
            transition={{ type: 'spring', stiffness: 400, damping: 25 }}
          >
            ⚡ Xác nhận & Bắt đầu xử lý
          </motion.button>
        </div>
      </motion.div>
    </section>
  );
}
