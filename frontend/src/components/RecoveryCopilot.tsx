'use client';

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

// ─── Interfaces ─────────────────────────────────────────────────────────────
export interface RecoveryCopilotProps {
  deadStockData: {
    totalFrozenValue: number;
    topDeadSku: {
      name: string;
      quantity: number;
      costPrice: number;
      sellingPrice: number;
      lastOrderDate: string;
      daysInInventory?: number; // Từ API Aging
    } | null;
    topSellingSku: {
      name: string;
      sellingPrice: number;
      costPrice: number;
    } | null;
  };
}

type RpaState = 'idle' | 'initializing' | 'injecting' | 'done';
type LiquidationPhase = 'phase1' | 'phase2' | 'phase3';

interface RpaPayload {
  action: string;
  target_sku: string;
  parameters: Record<string, string | number | boolean>;
}

// ─── Helper Functions ────────────────────────────────────────────────────────
function formatVND(value: number): string {
  return Math.round(value).toLocaleString('vi-VN') + ' ₫';
}

function calculateDaysInInventory(lastOrderDate: string): number {
  const lastDate = new Date(lastOrderDate);
  const now = new Date();
  const diffTime = Math.abs(now.getTime() - lastDate.getTime());
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
}

function getLiquidationPhase(days: number): LiquidationPhase {
  if (days <= 90) return 'phase1';
  if (days <= 120) return 'phase2';
  return 'phase3';
}

// ─── Progressive Liquidation Config ─────────────────────────────────────────
const LIQUIDATION_PHASES = {
  phase1: {
    label: 'Giai đoạn 1',
    title: 'Khuyến mãi mồi',
    range: '61–90 ngày',
    discountPct: 15,
    velocityDays: 30,
    successRate: '78%',
    colorClass: 'text-amber-400',
    borderClass: 'border-amber-400/30',
    bgClass: 'bg-amber-400/[0.03]',
    strategy: 'Thử nghiệm mức giảm nhẹ 15% để test độ co giãn của cầu. Giữ nguyên định vị thương hiệu — đây là giai đoạn thu thập data chứ chưa phải thanh lý.',
    rpaAction: 'SCHEDULE_PROMOTIONAL_DISCOUNT',
  },
  phase2: {
    label: 'Giai đoạn 2',
    title: 'Xả hàng cắt máu',
    range: '91–120 ngày',
    discountPct: 35,
    velocityDays: 14,
    successRate: '89%',
    colorClass: 'text-orange-400',
    borderClass: 'border-orange-400/30',
    bgClass: 'bg-orange-400/[0.03]',
    strategy: 'Thúc đẩy doanh số với mức giảm 35%. Tạo combo mua kèm sản phẩm hot để tăng AOV và giải phóng kho nhanh hơn.',
    rpaAction: 'SCHEDULE_FLASH_SALE',
  },
  phase3: {
    label: 'Giai đoạn 3',
    title: 'Thanh lý dọn kho',
    range: '120+ ngày',
    discountPct: 70,
    velocityDays: 7,
    successRate: '95%',
    colorClass: 'text-red-400',
    borderClass: 'border-red-400/30',
    bgClass: 'bg-red-400/[0.03]',
    strategy: 'Áp dụng giá thanh lý giảm 70% hoặc xả sỉ (B2B wholesale) để thu hồi vốn ngay lập tức. Ưu tiên dòng tiền — margin không còn là mục tiêu ở giai đoạn này.',
    rpaAction: 'SCHEDULE_CLEARANCE_SALE',
  },
};

// ─── RPA Button Component ────────────────────────────────────────────────────
function RpaExecutionButton({ cta, payload }: { cta: string; payload: RpaPayload }) {
  const [state, setState] = useState<RpaState>('idle');
  const [showPayload, setShowPayload] = useState(false);

  const handleClick = () => {
    if (state !== 'idle') return;
    setState('initializing');
    setTimeout(() => {
      setState('injecting');
      setTimeout(() => setState('done'), 1500);
    }, 1000);
  };

  return (
    <div
      className="relative flex justify-end"
      onMouseEnter={() => setShowPayload(true)}
      onMouseLeave={() => setShowPayload(false)}
    >
      <AnimatePresence>
        {showPayload && state === 'idle' && (
          <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 5, scale: 0.95 }}
            transition={{ duration: 0.15 }}
            className="absolute bottom-full mb-3 right-0 bg-[#0B0B0B] border border-[#262626] rounded-xl p-4 shadow-xl z-10 min-w-[280px]"
          >
            <div className="flex items-center justify-between mb-2">
              <span className="text-[10px] font-mono text-secondary uppercase tracking-widest">API Payload Preview</span>
              <span className="w-1.5 h-1.5 rounded-full bg-accent animate-pulse" />
            </div>
            <pre className="text-[11px] font-mono text-primary/80 whitespace-pre-wrap">
              {JSON.stringify(payload, null, 2)}
            </pre>
          </motion.div>
        )}
      </AnimatePresence>

      <motion.button
        onClick={handleClick}
        className={`relative flex items-center justify-center min-w-[240px] gap-2 text-sm font-bold px-6 py-3.5 rounded-xl border transition-all duration-300 select-none overflow-hidden ${
          state === 'done'
            ? 'border-accent/40 text-accent bg-accent/5'
            : 'border-[#262626] text-primary bg-white/5 hover:bg-white/10 hover:border-white/20'
        }`}
        whileHover={state === 'idle' ? { scale: 1.015 } : {}}
        whileTap={state === 'idle' ? { scale: 0.985 } : {}}
      >
        <AnimatePresence mode="wait">
          {state === 'idle' && (
            <motion.span key="idle" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.15 }}>
              {cta}
            </motion.span>
          )}
          {state === 'initializing' && (
            <motion.span key="init" className="flex items-center gap-2 text-secondary font-mono text-xs" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }}>
              <span className="inline-block w-3 h-3 border-2 border-secondary/30 border-t-secondary rounded-full animate-spin" />
              Initializing API...
            </motion.span>
          )}
          {state === 'injecting' && (
            <motion.span key="inject" className="flex items-center gap-2 text-primary font-mono text-xs" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }}>
              <span className="inline-block w-3 h-3 border-2 border-accent/30 border-t-accent rounded-full animate-spin" />
              Injecting Script...
            </motion.span>
          )}
          {state === 'done' && (
            <motion.span key="done" className="flex items-center gap-2 font-mono" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ type: 'spring', stiffness: 400, damping: 25 }}>
              Chiến dịch đã lên lịch
            </motion.span>
          )}
        </AnimatePresence>
      </motion.button>
    </div>
  );
}

// ─── Progressive Liquidation Panel ──────────────────────────────────────────
function ProgressiveLiquidationPanel({
  phase,
  sku,
  days,
}: {
  phase: LiquidationPhase;
  sku: NonNullable<RecoveryCopilotProps['deadStockData']['topDeadSku']>;
  days: number;
}) {
  const cfg = LIQUIDATION_PHASES[phase];
  const salePrice = sku.sellingPrice * (1 - cfg.discountPct / 100);
  const marginAfterDiscount = ((salePrice - sku.costPrice) / salePrice) * 100;
  const isLoss = marginAfterDiscount < 0;
  const revenueRecovered = salePrice * sku.quantity;

  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.3, duration: 0.6 }}
      className={`border ${cfg.borderClass} ${cfg.bgClass} rounded-2xl p-8 flex flex-col gap-6`}
    >
      {/* Header */}
      <div>
        <div className="flex items-center gap-3 mb-4">
          <span className={`text-xs font-mono uppercase tracking-widest ${cfg.colorClass}`}>{cfg.label}</span>
          <span className="text-[#262626] text-xs font-mono">·</span>
          <span className="text-xs font-mono text-secondary">{cfg.range}</span>
          <span className={`ml-auto inline-flex items-center gap-1.5 text-xs font-mono px-3 py-1 rounded-lg bg-white/5 border border-white/10 ${cfg.colorClass}`}>
            <span className="w-1.5 h-1.5 rounded-full bg-current opacity-80" />
            Xác suất clear: {cfg.successRate}
          </span>
        </div>
        <h3 className="font-tight font-bold text-primary text-2xl tracking-tight mb-2">
          {cfg.title}
        </h3>
        <p className="text-secondary text-sm max-w-xl leading-relaxed">{cfg.strategy}</p>
      </div>

      {/* Metrics */}
      <div className="bg-[#0B0B0B] border border-[#262626] rounded-xl p-6">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-6">
          <div>
            <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Giảm giá</div>
            <div className={`font-tight font-black text-xl tabular-nums ${cfg.colorClass}`}>-{cfg.discountPct}%</div>
          </div>
          <div>
            <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Giá bán mới</div>
            <div className="font-tight font-black text-primary text-xl tabular-nums">{formatVND(salePrice)}</div>
          </div>
          <div>
            <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Margin ròng</div>
            <div className={`font-tight font-black text-xl tabular-nums ${isLoss ? 'text-red-400' : 'text-accent'}`}>
              {isLoss ? '' : '+'}{marginAfterDiscount.toFixed(1)}%
            </div>
          </div>
          <div>
            <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Thu hồi</div>
            <div className="font-tight font-black text-primary text-xl tabular-nums">{formatVND(revenueRecovered)}</div>
          </div>
        </div>

        {/* SKU + days badge */}
        <div className="flex items-center gap-3 mt-6 pt-4 border-t border-[#262626]">
          <div>
            <div className="text-xs font-mono text-secondary">SKU đang phân tích</div>
            <div className="text-primary font-medium text-sm">{sku.name}</div>
          </div>
          <div className={`ml-auto font-mono text-xs px-3 py-1.5 rounded-lg ${cfg.bgClass} ${cfg.borderClass} border ${cfg.colorClass}`}>
            {days} ngày trong kho
          </div>
        </div>
      </div>

      {/* Velocity */}
      <div className="flex items-end justify-between gap-6">
        <div>
          <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Thời gian clear kho dự kiến</div>
          <div className={`font-tight font-black text-2xl tabular-nums ${cfg.colorClass}`}>{cfg.velocityDays} Ngày</div>
        </div>
        <RpaExecutionButton
          cta={`Kích hoạt ${cfg.title}`}
          payload={{
            action: cfg.rpaAction,
            target_sku: sku.name,
            parameters: {
              discount_percentage: cfg.discountPct,
              duration_days: cfg.velocityDays,
              days_in_inventory: days,
              phase: phase,
            },
          }}
        />
      </div>
    </motion.div>
  );
}

// ─── Main Component ──────────────────────────────────────────────────────────
export default function RecoveryCopilot({ deadStockData }: RecoveryCopilotProps) {
  const { totalFrozenValue, topDeadSku, topSellingSku } = deadStockData;

  // Trạng thái Healthy Kho
  if (totalFrozenValue === 0 || !topDeadSku) {
    return (
      <section className="bg-background px-6 md:px-16 lg:px-24 pb-32 pt-8">
        <div className="max-w-4xl mx-auto w-full">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
            className="bg-surface border border-[#D1FA4A]/30 rounded-2xl p-8 shadow-[0_0_20px_rgba(209,250,74,0.15)] relative overflow-hidden"
          >
            <div className="absolute -top-10 -right-10 w-40 h-40 bg-[#D1FA4A] rounded-full blur-[80px] opacity-20"></div>
            <h2 className="font-tight font-black tracking-tightest text-primary text-4xl mb-4 relative z-10">
              Kho hàng <span className="text-[#D1FA4A]">Tối ưu xuất sắc!</span>
            </h2>
            <p className="text-secondary text-sm max-w-xl leading-relaxed relative z-10">
              Hệ thống không phát hiện bất kỳ dòng tiền nào bị đóng băng. Các chiến lược nhập hàng của bạn đang hoạt động cực kỳ hiệu quả. Hãy tiếp tục duy trì!
            </p>
          </motion.div>
        </div>
      </section>
    );
  }

  // Dùng daysInInventory từ API Aging nếu có, fallback về tính từ lastOrderDate
  const days = topDeadSku.daysInInventory ?? calculateDaysInInventory(topDeadSku.lastOrderDate);
  const phase = getLiquidationPhase(days);
  const skuLabel = topDeadSku.name;

  // Tính Smart Bundle nếu có topSellingSku
  let bundleMarginPct = 0;
  let bundleRevenue = 0;
  let hotSkuLabel = 'Sản phẩm Hot Trend';
  if (topSellingSku) {
    hotSkuLabel = topSellingSku.name;
    const discountedDeadPrice = topDeadSku.sellingPrice * 0.85;
    bundleRevenue = topSellingSku.sellingPrice + discountedDeadPrice;
    const bundleCost = topDeadSku.costPrice + topSellingSku.costPrice;
    bundleMarginPct = ((bundleRevenue - bundleCost) / bundleRevenue) * 100;
  }

  return (
    <section className="bg-background px-6 md:px-16 lg:px-24 pb-32 pt-8">
      <div className="max-w-4xl mx-auto w-full">
        {/* ── Header ── */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
          className="mb-12"
        >
          <div className="flex flex-col gap-3">
            <span className="text-xs font-mono text-secondary tracking-widest uppercase opacity-60">
              AI Dynamic Execution · Progressive Liquidation
            </span>
            <h2 className="font-tight font-black tracking-tightest text-primary text-4xl sm:text-5xl leading-none">
              AI Action Plan:{' '}
              <span className="text-accent tabular-nums">Thu hồi {formatVND(totalFrozenValue)}</span>
            </h2>
            <p className="text-secondary text-sm max-w-2xl leading-relaxed mt-2">
              Lô hàng <span className="text-primary font-medium">[{skuLabel}]</span> đã nằm kho{' '}
              <span className={`font-mono tabular-nums font-bold ${phase === 'phase1' ? 'text-amber-400' : phase === 'phase2' ? 'text-orange-400' : 'text-red-400'}`}>
                {days} ngày
              </span>
              . AI đề xuất chiến lược{' '}
              <span className={`font-bold ${phase === 'phase1' ? 'text-amber-400' : phase === 'phase2' ? 'text-orange-400' : 'text-red-400'}`}>
                {LIQUIDATION_PHASES[phase].title}
              </span>{' '}
              phù hợp với vòng đời hiện tại.
            </p>
          </div>
        </motion.div>

        <div className="flex flex-col gap-8">
          {/* ── Progressive Liquidation (AI-recommended phase) ── */}
          <ProgressiveLiquidationPanel phase={phase} sku={topDeadSku} days={days} />

          {/* ── Smart Bundle (Cross-sell) ── */}
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2, duration: 0.6 }}
            className="bg-[#111111] border border-[#262626] rounded-2xl p-8 flex flex-col gap-8"
          >
            <div>
              <span className="inline-flex items-center gap-2 text-xs font-mono text-primary bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 mb-4">
                <span className="w-1.5 h-1.5 rounded-full bg-accent opacity-80" />
                Chiến lược bổ sung · Xác suất clear: 84%
              </span>
              <h3 className="font-tight font-bold text-primary text-2xl tracking-tight mb-2">Smart Bundle (Cross-sell)</h3>
              <p className="text-secondary text-sm max-w-xl leading-relaxed">
                Phân tích giỏ hàng cho thấy 68% user mua{' '}
                <span className="text-primary font-medium">[{skuLabel}]</span> thường tìm kiếm thêm{' '}
                <span className="text-primary font-medium">[{hotSkuLabel}]</span>. Tạo Bundle giảm 15% món tồn sẽ tăng Conversion Rate lên gấp 3 lần.
              </p>
            </div>

            <div className="bg-[#0B0B0B] border border-[#262626] rounded-xl p-6">
              <div className="flex flex-col sm:flex-row items-center gap-6">
                <div className="flex-1 text-center sm:text-left">
                  <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Món chính (tồn kho)</div>
                  <div className="font-tight font-semibold text-primary text-base truncate" title={skuLabel}>{skuLabel}</div>
                  <div className="text-xs text-secondary mt-1 font-mono tabular-nums">SL: {topDeadSku.quantity} | Tồn: {days} ngày</div>
                </div>
                <div className="text-[#262626] text-2xl font-light">+</div>
                <div className="flex-1 text-center sm:text-left">
                  <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Sản phẩm mồi</div>
                  <div className="font-tight font-semibold text-primary text-base truncate" title={hotSkuLabel}>{hotSkuLabel}</div>
                  <div className="text-xs text-accent mt-1 font-mono">Bán chạy nhất 30 ngày</div>
                </div>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row items-end justify-between gap-6">
              <div className="flex gap-8">
                <div>
                  <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Dự kiến thu hồi</div>
                  <div className="font-tight font-black text-primary text-xl tabular-nums">
                    {formatVND(bundleRevenue > 0 ? bundleRevenue * topDeadSku.quantity : totalFrozenValue * 0.85)}
                  </div>
                </div>
                <div>
                  <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-1">Lãi gộp (Margin)</div>
                  <div className={`font-tight font-black text-xl tabular-nums ${bundleMarginPct > 0 ? 'text-accent' : 'text-red-400'}`}>
                    {bundleMarginPct > 0 ? '+' : ''}{bundleMarginPct.toFixed(1)}%
                  </div>
                </div>
              </div>
              <RpaExecutionButton
                cta="Thực thi Bundle trên Shopee"
                payload={{
                  action: 'CREATE_BUNDLE_DEAL',
                  target_sku: topDeadSku.name,
                  parameters: {
                    companion_sku: hotSkuLabel,
                    discount_percentage: 15,
                    duration_days: 14,
                  },
                }}
              />
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
