'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';

// ─── Types ───────────────────────────────────────────────────────────────────
interface SkuAging {
  id: string;
  name: string;
  sku: string;
  quantity: number;
  costPrice: number;
  sellingPrice: number;
  totalValue: number;
  lastOrderDate: string;
  daysInInventory: number;
  agingBucket: 'HEALTHY' | 'WATCHLIST' | 'SLOW_MOVING' | 'DEAD_STOCK';
}

export interface InventoryAgingData {
  totalHealthyValue: number;
  totalWatchlistValue: number;
  totalSlowMovingValue: number;
  totalDeadStockValue: number;
  totalFrozenValue: number;
  totalBleedingRateMonthly: number;
  healthySkus: SkuAging[];
  watchlistSkus: SkuAging[];
  slowMovingSkus: SkuAging[];
  deadStockSkus: SkuAging[];
}

interface BucketConfig {
  key: keyof Pick<InventoryAgingData, 'healthySkus' | 'watchlistSkus' | 'slowMovingSkus' | 'deadStockSkus'>;
  valueKey: keyof Pick<InventoryAgingData, 'totalHealthyValue' | 'totalWatchlistValue' | 'totalSlowMovingValue' | 'totalDeadStockValue'>;
  label: string;
  range: string;
  color: string; // tailwind text color
  borderColor: string; // tailwind border color
  bgColor: string; // tailwind bg color
  severity: number; // 0=ok, 1=warn, 2=danger, 3=critical
}

const BUCKETS: BucketConfig[] = [
  {
    key: 'healthySkus',
    valueKey: 'totalHealthyValue',
    label: 'Khỏe mạnh',
    range: '0–30 Ngày',
    color: 'text-secondary',
    borderColor: 'border-[#262626]',
    bgColor: 'bg-[#111111]',
    severity: 0,
  },
  {
    key: 'watchlistSkus',
    valueKey: 'totalWatchlistValue',
    label: 'Cảnh báo',
    range: '31–60 Ngày',
    color: 'text-amber-400',
    borderColor: 'border-amber-400/30',
    bgColor: 'bg-amber-400/[0.04]',
    severity: 1,
  },
  {
    key: 'slowMovingSkus',
    valueKey: 'totalSlowMovingValue',
    label: 'Bán chậm',
    range: '61–90 Ngày',
    color: 'text-orange-400',
    borderColor: 'border-orange-400/30',
    bgColor: 'bg-orange-400/[0.04]',
    severity: 2,
  },
  {
    key: 'deadStockSkus',
    valueKey: 'totalDeadStockValue',
    label: 'Tồn kho chết',
    range: '91+ Ngày',
    color: 'text-red-400',
    borderColor: 'border-red-400/30',
    bgColor: 'bg-red-400/[0.04]',
    severity: 3,
  },
];

function formatVND(value: number): string {
  return Math.round(value).toLocaleString('vi-VN') + ' ₫';
}

// ─── Bucket Card ──────────────────────────────────────────────────────────────
function BucketCard({ bucket, data }: { bucket: BucketConfig; data: InventoryAgingData }) {
  const [hovered, setHovered] = useState(false);
  const skus = data[bucket.key] as SkuAging[];
  const value = data[bucket.valueKey] as number;
  const count = skus.length;

  return (
    <motion.div
      className={`relative border ${bucket.borderColor} ${bucket.bgColor} rounded-2xl p-6 flex flex-col gap-4 cursor-default transition-all duration-300`}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      whileHover={{ y: -2 }}
    >
      {/* Severity indicator */}
      <div className="flex items-center justify-between">
        <span className={`text-xs font-mono uppercase tracking-widest ${bucket.color}`}>
          {bucket.range}
        </span>
        <div className="flex gap-1">
          {[0, 1, 2, 3].map((i) => (
            <div
              key={i}
              className={`w-1.5 h-1.5 rounded-full transition-all duration-300 ${
                i <= bucket.severity
                  ? bucket.severity === 0
                    ? 'bg-secondary'
                    : bucket.severity === 1
                    ? 'bg-amber-400'
                    : bucket.severity === 2
                    ? 'bg-orange-400'
                    : 'bg-red-400'
                  : 'bg-[#262626]'
              }`}
            />
          ))}
        </div>
      </div>

      <div>
        <div className={`font-tight font-black text-2xl ${bucket.color}`}>{bucket.label}</div>
        <div className="text-secondary text-xs font-mono mt-1">{count} SKU</div>
      </div>

      {/* Hover detail panel */}
      <motion.div
        initial={{ opacity: 0, height: 0 }}
        animate={{ opacity: hovered ? 1 : 0, height: hovered ? 'auto' : 0 }}
        transition={{ duration: 0.2 }}
        className="overflow-hidden"
      >
        <div className="border-t border-[#262626] pt-4 mt-2 space-y-3">
          <div>
            <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Tổng vốn kẹt</div>
            <div className={`font-tight font-black text-xl tabular-nums ${value > 0 ? bucket.color : 'text-secondary'}`}>
              {value > 0 ? formatVND(value) : '—'}
            </div>
          </div>

          {/* Top 3 SKUs */}
          {skus.slice(0, 3).map((sku) => (
            <div key={sku.id} className="flex items-center justify-between text-xs">
              <span className="text-secondary truncate max-w-[60%]" title={sku.name}>{sku.name}</span>
              <span className="font-mono text-primary tabular-nums">{sku.daysInInventory}n</span>
            </div>
          ))}
          {count > 3 && (
            <div className="text-xs text-secondary opacity-50">+{count - 3} SKU khác</div>
          )}
          {count === 0 && (
            <div className="text-xs text-secondary opacity-50 italic">Không có sản phẩm nào</div>
          )}
        </div>
      </motion.div>

      {/* Collapsed summary */}
      {!hovered && value > 0 && (
        <div className={`font-mono text-sm tabular-nums ${bucket.color} opacity-70`}>
          {formatVND(value)}
        </div>
      )}
      {!hovered && value === 0 && (
        <div className="text-secondary text-sm opacity-40">Không có</div>
      )}
    </motion.div>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────
export default function InventoryAgingRadar({ data }: { data: InventoryAgingData }) {
  const totalValue =
    data.totalHealthyValue +
    data.totalWatchlistValue +
    data.totalSlowMovingValue +
    data.totalDeadStockValue;

  const bleedingRate = data.totalBleedingRateMonthly;
  const hasFrozen = data.totalFrozenValue > 0;

  // Proportion bars
  const getBar = (val: number) => (totalValue > 0 ? (val / totalValue) * 100 : 0);

  return (
    <section className="bg-background px-6 md:px-16 lg:px-24 pb-24 pt-8">
      <div className="max-w-5xl mx-auto w-full">
        {/* ── Hero Header (Loss Aversion) ── */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
          className="mb-16"
        >
          <span className="text-xs font-mono text-secondary tracking-widest uppercase opacity-60">
            Inventory Aging Radar · Phân tích Chi phí Ẩn
          </span>

          {hasFrozen ? (
            <h2 className="font-tight font-black tracking-tightest text-primary text-4xl sm:text-5xl leading-tight mt-4 mb-4">
              Tồn kho của bạn đang{' '}
              <span className="text-red-400">"đốt"</span>{' '}
              <span className="text-red-400 tabular-nums">{formatVND(bleedingRate)}</span>{' '}
              mỗi tháng.
            </h2>
          ) : (
            <h2 className="font-tight font-black tracking-tightest text-primary text-4xl sm:text-5xl leading-tight mt-4 mb-4">
              Tồn kho <span className="text-[#D1FA4A]">đang hoạt động hiệu quả.</span>
            </h2>
          )}

          <p className="text-secondary text-sm max-w-2xl leading-relaxed">
            {hasFrozen
              ? 'Chi phí này đến từ phí lưu kho (25%/năm) và khấu hao lỗi thời (10%/năm) đối với các sản phẩm trên 60 ngày. Đây là tiền thực sự đang mất đi mỗi tháng, không phải con số ước tính.'
              : 'Không có sản phẩm nào bị kẹt quá 60 ngày. Hệ thống không ghi nhận chi phí ẩn nào. Tiếp tục duy trì chiến lược nhập hàng hiện tại!'}
          </p>

          {/* Bleeding Rate breakdown */}
          {hasFrozen && (
            <div className="flex gap-8 mt-8">
              <div>
                <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">
                  Phí lưu kho / tháng
                </div>
                <div className="font-tight font-black text-orange-400 text-xl tabular-nums">
                  {formatVND(data.totalFrozenValue * (0.25 / 12))}
                </div>
                <div className="text-xs text-secondary mt-0.5">25%/năm × vốn kẹt</div>
              </div>
              <div className="border-l border-[#262626] pl-8">
                <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">
                  Khấu hao / tháng
                </div>
                <div className="font-tight font-black text-red-400 text-xl tabular-nums">
                  {formatVND(data.totalFrozenValue * (0.10 / 12))}
                </div>
                <div className="text-xs text-secondary mt-0.5">10%/năm × vốn kẹt</div>
              </div>
              <div className="border-l border-[#262626] pl-8">
                <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">
                  Tổng vốn đóng băng
                </div>
                <div className="font-tight font-black text-primary text-xl tabular-nums">
                  {formatVND(data.totalFrozenValue)}
                </div>
                <div className="text-xs text-secondary mt-0.5">SLOW_MOVING + DEAD_STOCK</div>
              </div>
            </div>
          )}
        </motion.div>

        {/* ── Proportion Bar ── */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3, duration: 0.5 }}
          className="mb-8"
        >
          <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-3 opacity-60">
            Phân bổ vốn tồn kho theo vòng đời
          </div>
          <div className="flex h-2 rounded-full overflow-hidden gap-px bg-[#0B0B0B]">
            <motion.div
              className="bg-secondary/40 rounded-l-full"
              initial={{ width: 0 }}
              animate={{ width: `${getBar(data.totalHealthyValue)}%` }}
              transition={{ duration: 0.8, delay: 0.4, ease: 'easeOut' }}
            />
            <motion.div
              className="bg-amber-400/60"
              initial={{ width: 0 }}
              animate={{ width: `${getBar(data.totalWatchlistValue)}%` }}
              transition={{ duration: 0.8, delay: 0.5, ease: 'easeOut' }}
            />
            <motion.div
              className="bg-orange-400/60"
              initial={{ width: 0 }}
              animate={{ width: `${getBar(data.totalSlowMovingValue)}%` }}
              transition={{ duration: 0.8, delay: 0.6, ease: 'easeOut' }}
            />
            <motion.div
              className="bg-red-400/70 rounded-r-full"
              initial={{ width: 0 }}
              animate={{ width: `${getBar(data.totalDeadStockValue)}%` }}
              transition={{ duration: 0.8, delay: 0.7, ease: 'easeOut' }}
            />
          </div>
          <div className="flex justify-between text-xs font-mono text-secondary mt-2 opacity-50">
            <span>0 ngày</span>
            <span>30</span>
            <span>60</span>
            <span>90</span>
            <span>91+</span>
          </div>
        </motion.div>

        {/* ── 4 Bucket Cards ── */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {BUCKETS.map((bucket, i) => (
            <motion.div
              key={bucket.key}
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 + i * 0.08, duration: 0.5 }}
            >
              <BucketCard bucket={bucket} data={data} />
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
