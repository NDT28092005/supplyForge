'use client';

import { motion } from 'framer-motion';

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.18, delayChildren: 0.1 },
  },
};

const blockVariants = {
  hidden: { opacity: 0, y: 28 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.65, ease: [0.16, 1, 0.3, 1] },
  },
};

export interface DashboardData {
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
}

interface CashflowVaultProps {
  data: DashboardData | null;
  onProceed: () => void;
  onRecovery: () => void;
  onCommandCenter?: () => void;
}

export default function CashflowVault({ data, onProceed, onRecovery, onCommandCenter }: CashflowVaultProps) {
  const deadStockValue = data?.totalDeadStockValueVnd ?? 0;
  const estimatedLoss = data?.estimatedLoss30DaysVnd ?? 0;
  const staleCount = data?.staleSkuCount ?? 0;
  const totalSku = data?.totalRootSkuCount ?? 0;
  const totalInv = data?.totalInventoryValueVnd ?? 0;
  const topSkus = data?.topStaleSkus ?? [];
  const frozenPct = totalInv > 0 ? ((deadStockValue / totalInv) * 100).toFixed(1) : '0';

  return (
    <section className="min-h-screen bg-background flex flex-col justify-center px-6 md:px-16 lg:px-24 py-24">
      <motion.div
        className="max-w-4xl mx-auto w-full space-y-12"
        variants={containerVariants}
        initial="hidden"
        animate="visible"
      >
        {/* ── Tiêu đề chính ── */}
        <motion.div variants={blockVariants} className="text-center">
          <p className="text-xs font-mono text-secondary tracking-widest uppercase mb-6 opacity-60">
            Kết quả phân tích tồn kho · AI Powered by Gemini
          </p>
          <h1 className="font-tight font-black tracking-tightest text-primary leading-[1.0] text-5xl sm:text-6xl xl:text-7xl mb-5">
            Bạn đang có
            <br />
            <span className="text-accent tabular-nums">{deadStockValue.toLocaleString('vi-VN')} VND</span>
            <br />
            bị đóng băng.
          </h1>
          <p className="text-danger text-sm font-medium">
            + <span className="tabular-nums">{estimatedLoss.toLocaleString('vi-VN')}</span> VND có thể mất thêm trong 30 ngày tới
            <span className="text-secondary font-normal"> (chi phí lưu kho 4.5%/tháng)</span>.
          </p>

          <div className="mt-10 flex items-center gap-4">
            <div className="flex-1 border-t border-[#262626]" />
            <span className="text-secondary text-xs font-mono opacity-50">logic tính toán</span>
            <div className="flex-1 border-t border-[#262626]" />
          </div>
        </motion.div>

        {/* ── Giải thích logic tính ── */}
        <motion.div variants={blockVariants}>
          <div className="bg-[#111111] border border-[#262626] rounded-2xl p-8 space-y-6">
            <p className="text-xs font-mono text-secondary uppercase tracking-widest opacity-60">Cách tính</p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
              <div className="border-l-2 border-accent pl-5">
                <div className="font-tight font-bold text-primary mb-2">Bước 1 · Xác định SKU tồn chết</div>
                <p className="text-secondary text-sm leading-relaxed">
                  SKU có ngày ghi nhận cuối trong kho{' '}
                  <span className="text-primary font-medium">trước 45 ngày</span> → coi là "tồn chết".
                </p>
              </div>
              <div className="border-l-2 border-warn/50 pl-5">
                <div className="font-tight font-bold text-primary mb-2">Bước 2 · Tính vốn đóng băng</div>
                <p className="text-secondary text-sm leading-relaxed">
                  <span className="font-mono text-primary">Số lượng × Giá vốn</span>
                  {' '}(fallback: Giá bán × 70% nếu không có giá vốn).
                </p>
              </div>
              <div className="border-l-2 border-[#333] pl-5">
                <div className="font-tight font-bold text-primary mb-2">Bước 3 · Ước tính thiệt hại</div>
                <p className="text-secondary text-sm leading-relaxed">
                  Vốn đóng băng × 4.5% = chi phí cơ hội + hao mòn theo chuẩn ngành Ecommerce VN.
                </p>
              </div>
            </div>
          </div>
        </motion.div>

        {/* ── Số liệu tổng quan ── */}
        <motion.div variants={blockVariants}>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {[
              { label: 'Tổng SKU phân tích', value: totalSku.toLocaleString(), accent: false },
              { label: 'SKU tồn chết > 45 ngày', value: staleCount.toLocaleString(), accent: true },
              { label: 'Tổng vốn tồn kho', value: totalInv.toLocaleString('vi-VN'), unit: 'VND', accent: false },
              { label: '% Vốn bị đóng băng', value: frozenPct + '%', accent: true },
            ].map((m) => (
              <div key={m.label} className="bg-[#111111] border border-[#262626] rounded-2xl p-6">
                <div className={`font-tight font-black tabular-nums text-2xl leading-none mb-2 ${m.accent ? 'text-accent' : 'text-primary'}`}>
                  {m.value}
                </div>
                {'unit' in m && <div className="text-xs text-secondary/60 font-mono mb-1">{m.unit}</div>}
                <div className="text-xs text-secondary">{m.label}</div>
              </div>
            ))}
          </div>
        </motion.div>

        {/* ── Chi tiết từng SKU tồn chết ── */}
        {topSkus.length > 0 && (
          <motion.div variants={blockVariants} className="space-y-3">
            <p className="text-xs font-mono text-secondary uppercase tracking-widest opacity-60">
              Top SKU tồn chết · Xếp theo giá trị đóng băng giảm dần
            </p>
            <div className="space-y-2">
              {topSkus.map((sku, i) => (
                <div
                  key={sku.skuId}
                  className="flex items-center justify-between bg-[#111111] border border-[#262626] rounded-xl px-6 py-5 hover:bg-[#161616] transition-colors"
                >
                  <div className="flex items-center gap-5 min-w-0">
                    <span className="font-tight font-black text-2xl text-[#333] select-none flex-shrink-0 w-8 text-right tabular-nums">
                      {String(i + 1).padStart(2, '0')}
                    </span>
                    <div className="min-w-0">
                      <div className="font-tight font-semibold text-primary text-sm truncate">{sku.name}</div>
                      <div className="text-xs text-secondary font-mono mt-1">
                        Ghi nhận cuối:{' '}
                        <span className="text-warn tabular-nums">{sku.lastRecordDate}</span>
                      </div>
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0 ml-6">
                    <div className="font-tight font-bold text-accent text-sm whitespace-nowrap tabular-nums">
                      {sku.frozenValueVnd.toLocaleString('vi-VN')} VND
                    </div>
                    <div className="text-xs text-secondary mt-0.5">bị đóng băng</div>
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        )}

        {/* ── CTAs ── */}
        <motion.div
          variants={blockVariants}
          className="flex flex-col sm:flex-row items-center gap-4 pt-2"
        >
          {/* Primary: Portfolio Command Center */}
          {onCommandCenter && (
            <motion.button
              onClick={onCommandCenter}
              className="bg-accent text-background font-tight font-bold text-sm px-10 py-4 rounded-xl tracking-tight w-full sm:w-auto flex-shrink-0"
              whileHover={{ scale: 1.015, backgroundColor: '#deff5a' }}
              whileTap={{ scale: 0.985 }}
              transition={{ type: 'spring', stiffness: 400, damping: 25 }}
            >
              ⚡ Command Center — Kế hoạch tuần này
            </motion.button>
          )}

          {/* Secondary: Recovery Plan (SKU-level deep dive) */}
          <button
            onClick={onRecovery}
            className="text-secondary text-sm hover:text-primary transition-colors underline underline-offset-4 whitespace-nowrap"
          >
            Xem AI Recovery Plan theo từng SKU
          </button>

          {/* Tertiary: normalize SKUs */}
          <button
            onClick={onProceed}
            className="text-secondary text-sm hover:text-primary transition-colors underline underline-offset-4 whitespace-nowrap opacity-60"
          >
            Chuẩn hóa SKU trước
          </button>
        </motion.div>

      </motion.div>
    </section>
  );
}
