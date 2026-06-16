'use client';

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

// ─── Types ───────────────────────────────────────────────────────────────────
interface SkuSummary {
  name: string;
  daysInInventory: number;
  totalValue: number;
  riskScore: number;
}

interface RecoveryCluster {
  clusterId: string;
  clusterName: string;
  rationale: string;
  skuCount: number;
  totalFrozenValue: number;
  totalPotentialRecovery: number;
  averageRiskScore: number;
  urgencyLevel: 'CRITICAL' | 'HIGH' | 'MEDIUM';
  industryCode: string;
  recommendedBatchAction: string;
  estimatedClearDays: number;
  topSkuSamples: SkuSummary[];
}

interface PortfolioCommand {
  totalExpectedRecovery: number;
  totalSkuCount: number;
  estimatedWarehouseSpaceFreedPct: number;
  planHorizonDays: number;
  priorityClusters: RecoveryCluster[];
}

interface CommandCenterProps {
  portfolioData: PortfolioCommand | null;
  loading?: boolean;
  onBack?: () => void;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
function formatVND(value: number): string {
  return Math.round(value).toLocaleString('vi-VN') + ' ₫';
}

function formatVNDCompact(value: number): string {
  if (value >= 1_000_000_000) return (value / 1_000_000_000).toFixed(1) + ' tỷ ₫';
  if (value >= 1_000_000) return Math.round(value / 1_000_000) + ' triệu ₫';
  return formatVND(value);
}

// ─── Risk Score Badge ─────────────────────────────────────────────────────────
function RiskBadge({ score, urgency }: { score: number; urgency: string }) {
  const color = urgency === 'CRITICAL'
    ? 'text-red-400 border-red-400/30 bg-red-400/[0.06]'
    : urgency === 'HIGH'
    ? 'text-orange-400 border-orange-400/30 bg-orange-400/[0.06]'
    : 'text-amber-400 border-amber-400/30 bg-amber-400/[0.06]';

  const label = urgency === 'CRITICAL' ? 'CẤP BÁCH' : urgency === 'HIGH' ? 'ƯU TIÊN CAO' : 'THEO DÕI';

  return (
    <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs font-mono ${color}`}>
      <span className="tabular-nums font-bold">{score}/100</span>
      <span className="opacity-60">·</span>
      <span>{label}</span>
    </div>
  );
}

// ─── Batch Execution Modal ────────────────────────────────────────────────────
function BatchModal({
  cluster,
  onClose,
}: {
  cluster: RecoveryCluster;
  onClose: () => void;
}) {
  const [phase, setPhase] = useState<'preview' | 'launching' | 'done'>('preview');

  const handleLaunch = async () => {
    setPhase('launching');
    // Simulate API / RPA trigger
    await new Promise((r) => setTimeout(r, 2200));
    setPhase('done');
  };

  return (
    <motion.div
      className="fixed inset-0 z-50 flex items-center justify-center px-4"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      {/* Backdrop */}
      <motion.div
        className="absolute inset-0 bg-black/70 backdrop-blur-sm"
        onClick={phase !== 'launching' ? onClose : undefined}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
      />

      {/* Modal */}
      <motion.div
        className="relative z-10 w-full max-w-lg bg-[#0f0f0f] border border-[#262626] rounded-2xl overflow-hidden shadow-2xl"
        initial={{ opacity: 0, scale: 0.94, y: 16 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.94, y: 16 }}
        transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
      >
        {/* Top accent strip */}
        <div className={`h-px w-full ${cluster.urgencyLevel === 'CRITICAL' ? 'bg-red-400' : cluster.urgencyLevel === 'HIGH' ? 'bg-orange-400' : 'bg-amber-400'}`} />

        <div className="p-8">
          {phase === 'preview' && (
            <>
              <p className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-6 opacity-60">
                Xác nhận Batch Execution
              </p>
              <h3 className="font-tight font-black text-primary text-2xl leading-snug mb-4">
                {cluster.clusterName}
              </h3>

              {/* Summary box */}
              <div className="bg-white/[0.02] border border-[#1e1e1e] rounded-xl p-5 mb-6 space-y-3">
                <p className="text-sm text-secondary leading-relaxed">
                  Hệ thống sẽ tự động khởi chạy chiến dịch{' '}
                  <span className="text-primary font-semibold">[{cluster.recommendedBatchAction}]</span>{' '}
                  áp dụng đồng loạt cho{' '}
                  <span className="text-accent font-bold tabular-nums">{cluster.skuCount} SKU</span>{' '}
                  trong nhóm này.
                </p>
                <div className="border-t border-[#1e1e1e] pt-3 grid grid-cols-2 gap-3 text-xs font-mono">
                  <div>
                    <div className="text-secondary opacity-60 mb-0.5 uppercase tracking-wider">Thu hồi kỳ vọng</div>
                    <div className="text-accent font-bold tabular-nums">{formatVNDCompact(cluster.totalPotentialRecovery)}</div>
                  </div>
                  <div>
                    <div className="text-secondary opacity-60 mb-0.5 uppercase tracking-wider">Dự kiến clear kho</div>
                    <div className="text-primary font-bold">{cluster.estimatedClearDays} ngày</div>
                  </div>
                </div>
              </div>

              {/* Top SKU samples */}
              {cluster.topSkuSamples?.length > 0 && (
                <div className="mb-6">
                  <p className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-3 opacity-50">
                    SKU đại diện trong nhóm
                  </p>
                  <div className="space-y-2">
                    {cluster.topSkuSamples.map((sku, i) => (
                      <div key={i} className="flex items-center justify-between text-xs">
                        <span className="text-secondary truncate max-w-[60%]" title={sku.name}>{sku.name}</span>
                        <span className="font-mono text-primary tabular-nums">{formatVNDCompact(sku.totalValue)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="flex gap-3">
                <motion.button
                  onClick={handleLaunch}
                  className="flex-1 bg-accent text-background font-tight font-bold text-sm py-3.5 rounded-xl"
                  whileHover={{ scale: 1.015, backgroundColor: '#deff5a' }}
                  whileTap={{ scale: 0.985 }}
                >
                  Xác nhận &amp; Khởi chạy Workflow
                </motion.button>
                <button
                  onClick={onClose}
                  className="px-5 text-secondary text-sm border border-[#262626] rounded-xl hover:text-primary hover:border-white/20 transition-colors"
                >
                  Huỷ
                </button>
              </div>
            </>
          )}

          {phase === 'launching' && (
            <div className="py-8 flex flex-col items-center gap-6">
              <div className="relative w-16 h-16">
                <motion.div
                  className="absolute inset-0 rounded-full border-2 border-accent/20"
                  animate={{ scale: [1, 1.5, 1], opacity: [0.6, 0, 0.6] }}
                  transition={{ repeat: Infinity, duration: 1.8, ease: 'easeInOut' }}
                />
                <motion.div
                  className="absolute inset-0 rounded-full border-t-2 border-accent"
                  animate={{ rotate: 360 }}
                  transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}
                />
              </div>
              <div className="text-center">
                <p className="text-primary font-tight font-bold text-lg mb-1">Đang kết nối sàn thương mại...</p>
                <p className="text-secondary text-xs font-mono">Workflow RPA đang được khởi tạo</p>
              </div>
              {/* Fake log stream */}
              <div className="w-full bg-black/40 rounded-xl p-4 font-mono text-xs text-secondary space-y-1.5 border border-[#1e1e1e]">
                {['Xác thực API token...', `Chuẩn bị ${cluster.skuCount} SKU...`, 'Đang đẩy chiến dịch lên platform...'].map((log, i) => (
                  <motion.p
                    key={log}
                    initial={{ opacity: 0, x: -4 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.6 }}
                    className="flex items-center gap-2"
                  >
                    <span className="text-accent">›</span>
                    {log}
                  </motion.p>
                ))}
              </div>
            </div>
          )}

          {phase === 'done' && (
            <div className="py-6 flex flex-col items-center gap-5 text-center">
              <motion.div
                className="w-16 h-16 rounded-full bg-accent/10 border border-accent/30 flex items-center justify-center"
                initial={{ scale: 0.5, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ type: 'spring', stiffness: 300, damping: 18 }}
              >
                <span className="text-accent text-2xl font-bold">✓</span>
              </motion.div>
              <div>
                <h3 className="text-primary font-tight font-black text-xl mb-2">Chiến dịch đã được khởi chạy!</h3>
                <p className="text-secondary text-sm max-w-xs mx-auto leading-relaxed">
                  Workflow <span className="text-primary font-medium">{cluster.recommendedBatchAction}</span> đang chạy cho {cluster.skuCount} SKU.
                  Dự kiến thu hồi <span className="text-accent font-bold">{formatVNDCompact(cluster.totalPotentialRecovery)}</span> trong {cluster.estimatedClearDays} ngày.
                </p>
              </div>
              <button
                onClick={onClose}
                className="text-secondary text-sm underline underline-offset-4 hover:text-primary transition-colors"
              >
                Đóng
              </button>
            </div>
          )}
        </div>
      </motion.div>
    </motion.div>
  );
}

// ─── Priority Cluster Card ────────────────────────────────────────────────────
function ClusterCard({
  cluster,
  rank,
  delay,
  onExecute,
}: {
  cluster: RecoveryCluster;
  rank: number;
  delay: number;
  onExecute: () => void;
}) {
  const urgencyColors = {
    CRITICAL: {
      border: 'border-red-400/20',
      bg: 'bg-red-400/[0.03]',
      accent: 'text-red-400',
      strip: 'bg-red-400',
      btn: 'bg-red-500/10 text-red-400 border border-red-400/30 hover:bg-red-500 hover:text-white',
    },
    HIGH: {
      border: 'border-orange-400/20',
      bg: 'bg-orange-400/[0.03]',
      accent: 'text-orange-400',
      strip: 'bg-orange-400',
      btn: 'bg-orange-500/10 text-orange-400 border border-orange-400/30 hover:bg-orange-500 hover:text-white',
    },
    MEDIUM: {
      border: 'border-amber-400/20',
      bg: 'bg-amber-400/[0.03]',
      accent: 'text-amber-400',
      strip: 'bg-amber-400',
      btn: 'bg-amber-500/10 text-amber-400 border border-amber-400/30 hover:bg-amber-500 hover:text-black',
    },
  }[cluster.urgencyLevel];

  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.55, ease: [0.16, 1, 0.3, 1] }}
      className={`relative border ${urgencyColors.border} ${urgencyColors.bg} rounded-2xl overflow-hidden`}
    >
      {/* Left urgency strip */}
      <div className={`absolute left-0 top-0 bottom-0 w-[3px] ${urgencyColors.strip}`} />

      <div className="pl-8 pr-6 py-7">
        <div className="flex flex-col lg:flex-row lg:items-center gap-6">

          {/* ── LEFT: Cluster Info ── */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start gap-4 mb-3">
              {/* Rank number */}
              <span className={`font-tight font-black text-5xl leading-none select-none opacity-20 ${urgencyColors.accent} flex-shrink-0 tabular-nums`}>
                {String(rank).padStart(2, '0')}
              </span>
              <div className="min-w-0">
                <h3 className="font-tight font-black text-primary text-xl leading-tight mb-1.5">
                  {cluster.clusterName}
                </h3>
                <p className="text-secondary text-sm leading-relaxed mb-3">
                  {cluster.rationale}
                </p>
                <RiskBadge score={cluster.averageRiskScore} urgency={cluster.urgencyLevel} />
              </div>
            </div>

            {/* SKU count + days */}
            <div className="flex gap-5 mt-4 text-xs font-mono text-secondary">
              <span>
                <span className="text-primary font-bold tabular-nums">{cluster.skuCount}</span> SKU trong nhóm
              </span>
              <span className="opacity-40">·</span>
              <span>
                Clear kho trong{' '}
                <span className={`font-bold tabular-nums ${urgencyColors.accent}`}>
                  {cluster.estimatedClearDays} ngày
                </span>
              </span>
            </div>

            {/* Recommended action */}
            <div className="mt-3 flex items-center gap-2 text-xs font-mono text-secondary">
              <span className="opacity-40">Chiến lược:</span>
              <span className="text-primary/80">{cluster.recommendedBatchAction}</span>
            </div>
          </div>

          {/* ── MIDDLE: Recovery Amount ── */}
          <div className="lg:w-52 lg:text-right flex-shrink-0">
            <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-1.5 opacity-60">
              Thu hồi kỳ vọng
            </div>
            <div className="font-tight font-black text-3xl text-[#D1FA4A] tabular-nums leading-none">
              {formatVNDCompact(cluster.totalPotentialRecovery)}
            </div>
            <div className="text-xs font-mono text-secondary mt-1.5 opacity-50">
              từ {formatVNDCompact(cluster.totalFrozenValue)} vốn kẹt
            </div>
          </div>

          {/* ── RIGHT: Batch Execute CTA ── */}
          <div className="lg:w-48 flex-shrink-0">
            <motion.button
              onClick={onExecute}
              className={`w-full py-3.5 px-5 rounded-xl font-tight font-bold text-sm transition-all duration-200 ${urgencyColors.btn}`}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.97 }}
            >
              ⚡ Thực thi hàng loạt
            </motion.button>
            <p className="text-[10px] font-mono text-secondary text-center mt-2 opacity-40">
              Batch Execute · {cluster.skuCount} SKU
            </p>
          </div>
        </div>
      </div>
    </motion.div>
  );
}

// ─── Skeleton Loading ─────────────────────────────────────────────────────────
function CommandSkeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      {[0, 1, 2].map((i) => (
        <div key={i} className="border border-[#1e1e1e] rounded-2xl p-7 h-40 bg-white/[0.01]" />
      ))}
    </div>
  );
}

// ─── Empty State ──────────────────────────────────────────────────────────────
function CommandEmpty() {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.96 }}
      animate={{ opacity: 1, scale: 1 }}
      className="border border-[#D1FA4A]/20 rounded-2xl p-10 text-center bg-[#D1FA4A]/[0.02]"
    >
      <h3 className="font-tight font-black text-primary text-2xl mb-3">
        Không có chiến dịch nào cần thực thi.
      </h3>
      <p className="text-secondary text-sm max-w-md mx-auto leading-relaxed">
        Kho hàng đang hoạt động tốt. Hệ thống không phát hiện nhóm SKU nào cần xử lý khẩn cấp.
      </p>
    </motion.div>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────
export default function CommandCenterDashboard({
  portfolioData,
  loading = false,
  onBack,
}: CommandCenterProps) {
  const [activeModal, setActiveModal] = useState<RecoveryCluster | null>(null);

  const clusters = portfolioData?.priorityClusters ?? [];
  const totalRecovery = portfolioData?.totalExpectedRecovery ?? 0;
  const totalSkuCount = portfolioData?.totalSkuCount ?? 0;
  const spacePct = portfolioData?.estimatedWarehouseSpaceFreedPct ?? 0;
  const horizonDays = portfolioData?.planHorizonDays ?? 14;

  return (
    <section className="bg-background min-h-screen px-6 md:px-16 lg:px-24 pb-32 pt-10">
      <div className="max-w-5xl mx-auto w-full">

        {/* ── Back nav ── */}
        {onBack && (
          <motion.button
            onClick={onBack}
            className="text-secondary text-sm hover:text-primary transition-colors font-mono mb-10 block"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            ← Quay lại Dashboard
          </motion.button>
        )}

        {/* ── Hero Statement ── */}
        <motion.div
          className="mb-14"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
        >
          <span className="text-[10px] font-mono text-secondary tracking-widest uppercase opacity-50 mb-4 block">
            Portfolio Command Center · Kế hoạch tuần này
          </span>

          <h1 className="font-tight font-black tracking-tightest text-primary text-5xl sm:text-6xl leading-none mb-5">
            KẾ HOẠCH THU HỒI
            <br />
            <span className="text-[#D1FA4A]">DÒNG TIỀN TUẦN NÀY</span>
          </h1>

          {loading ? (
            <div className="h-5 w-96 bg-white/[0.04] rounded animate-pulse" />
          ) : totalRecovery > 0 ? (
            <p className="text-secondary text-base max-w-2xl leading-relaxed">
              Thực thi{' '}
              <span className="text-primary font-bold">{clusters.length} chiến dịch</span> dưới đây có thể giúp bạn thu hồi{' '}
              <span className="text-[#D1FA4A] font-bold tabular-nums">
                {formatVNDCompact(totalRecovery)}
              </span>{' '}
              và giải phóng{' '}
              <span className="text-[#D1FA4A] font-bold tabular-nums">{spacePct.toFixed(0)}%</span>{' '}
              không gian kho trong{' '}
              <span className="text-primary font-bold">{horizonDays} ngày tới</span>.
            </p>
          ) : (
            <p className="text-secondary text-base">
              Kho hàng hiện không có nhóm nào cần xử lý khẩn cấp.
            </p>
          )}
        </motion.div>

        {/* ── KPI Strip ── */}
        {!loading && totalRecovery > 0 && (
          <motion.div
            className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-12"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15, duration: 0.5 }}
          >
            {[
              { label: 'Tổng thu hồi kỳ vọng', value: formatVNDCompact(totalRecovery), color: 'text-[#D1FA4A]' },
              { label: 'Tổng SKU cần xử lý', value: `${totalSkuCount} SKU`, color: 'text-primary' },
              { label: 'Kho giải phóng', value: `${spacePct.toFixed(0)}%`, color: 'text-orange-400' },
              { label: 'Thời gian thực thi', value: `${horizonDays} ngày`, color: 'text-primary' },
            ].map((kpi, i) => (
              <div
                key={i}
                className="border border-[#1e1e1e] rounded-xl p-4 bg-white/[0.01]"
              >
                <div className="text-[9px] font-mono text-secondary uppercase tracking-wider mb-1.5 opacity-50">
                  {kpi.label}
                </div>
                <div className={`font-tight font-black text-xl tabular-nums ${kpi.color}`}>
                  {kpi.value}
                </div>
              </div>
            ))}
          </motion.div>
        )}

        {/* ── Priority Queue ── */}
        <motion.div
          className="mb-6"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
        >
          <span className="text-[10px] font-mono text-secondary uppercase tracking-widest opacity-50">
            Priority Queue · Top {clusters.length} Chiến dịch cần thực thi ngay
          </span>
        </motion.div>

        {loading ? (
          <CommandSkeleton />
        ) : clusters.length === 0 ? (
          <CommandEmpty />
        ) : (
          <div className="flex flex-col gap-5">
            {clusters.map((cluster, i) => (
              <ClusterCard
                key={cluster.clusterId}
                cluster={cluster}
                rank={i + 1}
                delay={0.25 + i * 0.1}
                onExecute={() => setActiveModal(cluster)}
              />
            ))}
          </div>
        )}

        {/* ── Footer note ── */}
        {!loading && clusters.length > 0 && (
          <motion.p
            className="text-[10px] font-mono text-secondary opacity-30 mt-10 text-center"
            initial={{ opacity: 0 }}
            animate={{ opacity: 0.3 }}
            transition={{ delay: 0.8 }}
          >
            Điểm rủi ro được tính theo công thức DeadStockPriorityScore v1.0 ·
            Trọng số: Vốn kẹt 30% · Ngày tồn 25% · Sell-through 20% · Rủi ro ngành 25%
          </motion.p>
        )}
      </div>

      {/* ── Batch Modal ── */}
      <AnimatePresence>
        {activeModal && (
          <BatchModal
            cluster={activeModal}
            onClose={() => setActiveModal(null)}
          />
        )}
      </AnimatePresence>
    </section>
  );
}
