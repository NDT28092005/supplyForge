'use client';

import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence, useMotionValue, useTransform } from 'framer-motion';

// ─── Types ────────────────────────────────────────────────────────────────────
export interface SkuCandidate {
  id: string;
  skuCode: string;
  name: string;
  price: number;
  stock: number;
  source: string;
}

export interface MatchProposal {
  confidenceScore: number;
  skuA: SkuCandidate;
  skuB: SkuCandidate;
}

type Decision = 'merge' | 'ignore';

interface UndoEntry {
  proposal: MatchProposal;
  decision: Decision;
  index: number;
}

interface Props {
  proposals: MatchProposal[];
  onMerge?: (a: SkuCandidate, b: SkuCandidate) => void;
  onIgnore?: (a: SkuCandidate, b: SkuCandidate) => void;
  onComplete?: (mergedCount: number, ignoredCount: number) => void;
}

// ─── String Diff Helpers ──────────────────────────────────────────────────────
/**
 * Tokenises both strings, classifies each token as "same" or "different",
 * returns a list of { text, isDiff } segments for each string.
 */
export function highlightDifferences(
  strA: string,
  strB: string
): { a: { text: string; isDiff: boolean }[]; b: { text: string; isDiff: boolean }[] } {
  const tokensA = strA.trim().split(/\s+/);
  const tokensB = strB.trim().split(/\s+/);
  const setA = new Set(tokensA.map((t) => t.toLowerCase()));
  const setB = new Set(tokensB.map((t) => t.toLowerCase()));

  return {
    a: tokensA.map((t) => ({ text: t, isDiff: !setB.has(t.toLowerCase()) })),
    b: tokensB.map((t) => ({ text: t, isDiff: !setA.has(t.toLowerCase()) })),
  };
}

function formatVND(n: number): string {
  return Math.round(n).toLocaleString('vi-VN') + ' ₫';
}

// ─── Sub-components ───────────────────────────────────────────────────────────
function DiffText({ tokens }: { tokens: { text: string; isDiff: boolean }[] }) {
  return (
    <span>
      {tokens.map((tok, i) => (
        <span key={i}>
          {tok.isDiff ? (
            <mark className="bg-white/10 text-primary font-bold rounded px-1 mx-0.5 not-italic">
              {tok.text}
            </mark>
          ) : (
            <span className="text-secondary mr-1">{tok.text}</span>
          )}
        </span>
      ))}
    </span>
  );
}

function SkuColumn({
  sku,
  nameTokens,
  otherPrice,
  otherStock,
  side,
}: {
  sku: SkuCandidate;
  nameTokens: { text: string; isDiff: boolean }[];
  otherPrice: number;
  otherStock: number;
  side: 'A' | 'B';
}) {
  const pricesDiff = sku.price !== otherPrice;
  const stocksDiff = sku.stock !== otherStock;

  return (
    <div className="flex-1 flex flex-col gap-5 p-8">
      {/* Side badge */}
      <div className="flex items-center gap-2">
        <span className="text-[10px] font-mono text-secondary uppercase tracking-[0.25em]">
          SKU {side}
        </span>
        <span className="text-[10px] font-mono text-secondary/40 border border-[#262626] rounded px-2 py-0.5">
          {sku.source}
        </span>
      </div>

      {/* SKU Code */}
      <div className="font-mono text-xs text-secondary/60 -mt-2">{sku.skuCode}</div>

      {/* Name with diff highlight */}
      <div className="text-base leading-relaxed min-h-[3.5rem]">
        <DiffText tokens={nameTokens} />
      </div>

      {/* Data grid */}
      <div className="mt-auto space-y-3 pt-4 border-t border-[#1e1e1e]">
        <DataRow
          label="Giá bán"
          value={formatVND(sku.price)}
          highlight={pricesDiff}
          isHigher={pricesDiff && sku.price > otherPrice}
        />
        <DataRow
          label="Tồn kho"
          value={`${sku.stock.toLocaleString()} đơn vị`}
          highlight={stocksDiff}
          isHigher={stocksDiff && sku.stock > otherStock}
        />
      </div>
    </div>
  );
}

function DataRow({
  label,
  value,
  highlight,
  isHigher,
}: {
  label: string;
  value: string;
  highlight: boolean;
  isHigher: boolean;
}) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-xs text-secondary/50 font-mono uppercase tracking-widest flex-shrink-0">
        {label}
      </span>
      <span
        className={`font-mono text-sm tabular-nums font-semibold ${
          highlight
            ? isHigher
              ? 'text-accent'
              : 'text-orange-400'
            : 'text-secondary'
        }`}
      >
        {value}
      </span>
    </div>
  );
}

// ─── Toast ────────────────────────────────────────────────────────────────────
function UndoToast({
  entry,
  onUndo,
  onDismiss,
}: {
  entry: UndoEntry;
  onUndo: () => void;
  onDismiss: () => void;
}) {
  useEffect(() => {
    const t = setTimeout(onDismiss, 5000);
    return () => clearTimeout(t);
  }, [onDismiss]);

  const label = entry.decision === 'merge' ? 'Đã gộp SKU thành công' : 'Đã bỏ qua cặp SKU';

  return (
    <motion.div
      initial={{ opacity: 0, y: 20, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: 12, scale: 0.95 }}
      transition={{ type: 'spring', stiffness: 380, damping: 30 }}
      className="fixed bottom-8 left-1/2 -translate-x-1/2 z-50 flex items-center gap-4 bg-[#111] border border-[#2a2a2a] rounded-2xl px-5 py-3.5 shadow-2xl"
    >
      <span className="text-sm text-primary">{label}.</span>
      <button
        onClick={onUndo}
        className="text-accent text-sm font-bold hover:opacity-80 transition-opacity underline underline-offset-2"
      >
        Hoàn tác (Undo)
      </button>
      {/* 5s progress bar */}
      <motion.div
        className="absolute bottom-0 left-0 h-[2px] bg-accent/40 rounded-full"
        initial={{ width: '100%' }}
        animate={{ width: '0%' }}
        transition={{ duration: 5, ease: 'linear' }}
      />
    </motion.div>
  );
}

// ─── Swipeable Card ───────────────────────────────────────────────────────────
function SwipeCard({
  proposal,
  onDecide,
}: {
  proposal: MatchProposal;
  onDecide: (d: Decision) => void;
}) {
  const [hovered, setHovered] = useState<Decision | null>(null);
  const x = useMotionValue(0);
  const rotate = useTransform(x, [-220, 220], [-12, 12]);
  const opacity = useTransform(x, [-220, 0, 220], [0.4, 1, 0.4]);

  // Swipe label overlays
  const mergeOpacity = useTransform(x, [30, 100], [0, 1]);
  const ignoreOpacity = useTransform(x, [-100, -30], [1, 0]);

  const { a: tokensA, b: tokensB } = highlightDifferences(
    proposal.skuA.name,
    proposal.skuB.name
  );

  const confidence = proposal.confidenceScore;
  const confColor =
    confidence >= 0.9
      ? 'text-[#D1FA4A]'
      : confidence >= 0.7
      ? 'text-amber-400'
      : 'text-orange-400';

  function handleDragEnd(_: unknown, info: { offset: { x: number } }) {
    if (info.offset.x > 110) onDecide('merge');
    else if (info.offset.x < -110) onDecide('ignore');
  }

  return (
    <motion.div
      drag="x"
      dragConstraints={{ left: 0, right: 0 }}
      style={{ x, rotate, opacity }}
      onDragEnd={handleDragEnd}
      whileDrag={{ scale: 1.02 }}
      className="absolute inset-0 cursor-grab active:cursor-grabbing touch-none"
    >
      <div className="h-full flex flex-col bg-[#0D0D0D] border border-[#1e1e1e] rounded-3xl overflow-hidden">
        {/* ── Confidence Header ── */}
        <div className="flex items-center justify-between px-8 pt-7 pb-4 border-b border-[#1a1a1a]">
          <div>
            <div className="text-[10px] font-mono text-secondary uppercase tracking-[0.25em] mb-1">
              AI Match Confidence
            </div>
            <div className={`font-tight font-black text-3xl tabular-nums ${confColor}`}>
              {Math.round(confidence * 100)}%
            </div>
          </div>
          <div className="text-right">
            <div className="text-[10px] font-mono text-secondary/40 uppercase tracking-widest mb-1">
              Fuzzy Match Score
            </div>
            <div className="w-32 h-1.5 bg-[#1e1e1e] rounded-full overflow-hidden">
              <motion.div
                className={`h-full rounded-full ${
                  confidence >= 0.9
                    ? 'bg-[#D1FA4A]'
                    : confidence >= 0.7
                    ? 'bg-amber-400'
                    : 'bg-orange-400'
                }`}
                initial={{ width: 0 }}
                animate={{ width: `${confidence * 100}%` }}
                transition={{ duration: 0.6, ease: 'easeOut' }}
              />
            </div>
          </div>
        </div>

        {/* ── Side-by-side comparison ── */}
        <div className="flex flex-1 overflow-auto divide-x divide-[#1a1a1a]">
          <SkuColumn
            sku={proposal.skuA}
            nameTokens={tokensA}
            otherPrice={proposal.skuB.price}
            otherStock={proposal.skuB.stock}
            side="A"
          />
          <SkuColumn
            sku={proposal.skuB}
            nameTokens={tokensB}
            otherPrice={proposal.skuA.price}
            otherStock={proposal.skuA.stock}
            side="B"
          />
        </div>

        {/* ── Swipe Overlays ── */}
        <motion.div
          className="absolute inset-0 bg-[#D1FA4A]/5 rounded-3xl border-2 border-[#D1FA4A]/40 flex items-center justify-center pointer-events-none"
          style={{ opacity: mergeOpacity }}
        >
          <span className="text-[#D1FA4A] font-tight font-black text-4xl tracking-tight rotate-[-12deg]">
            GỘP
          </span>
        </motion.div>
        <motion.div
          className="absolute inset-0 bg-red-400/5 rounded-3xl border-2 border-red-400/40 flex items-center justify-center pointer-events-none"
          style={{ opacity: ignoreOpacity }}
        >
          <span className="text-red-400 font-tight font-black text-4xl tracking-tight rotate-[12deg]">
            BỎ QUA
          </span>
        </motion.div>

        {/* ── Action Buttons ── */}
        <div className="flex gap-3 p-6 border-t border-[#1a1a1a]">
          <motion.button
            id="sku-tinder-ignore"
            onClick={() => onDecide('ignore')}
            onMouseEnter={() => setHovered('ignore')}
            onMouseLeave={() => setHovered(null)}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.97 }}
            className="flex-1 py-4 rounded-2xl border border-[#2a2a2a] text-secondary text-sm font-bold tracking-wide hover:border-red-400/40 hover:text-red-400 transition-all duration-200"
          >
            Bỏ qua · Ignore
          </motion.button>
          <motion.button
            id="sku-tinder-merge"
            onClick={() => onDecide('merge')}
            onMouseEnter={() => setHovered('merge')}
            onMouseLeave={() => setHovered(null)}
            whileHover={{ scale: 1.02, backgroundColor: '#deff5a' }}
            whileTap={{ scale: 0.97 }}
            className="flex-1 py-4 rounded-2xl bg-[#D1FA4A] text-black text-sm font-black tracking-wide transition-all duration-200"
          >
            Gộp chung · Merge
          </motion.button>
        </div>
      </div>

      {/* Hover hint */}
      <AnimatePresence>
        {hovered && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute bottom-24 left-1/2 -translate-x-1/2 text-[10px] font-mono text-secondary/40 pointer-events-none whitespace-nowrap"
          >
            {hovered === 'merge' ? '→ Kéo phải để gộp' : '← Kéo trái để bỏ qua'}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────
export default function SkuDeduplicationTinder({
  proposals,
  onMerge,
  onIgnore,
  onComplete,
}: Props) {
  const [index, setIndex] = useState(0);
  const [exiting, setExiting] = useState<{ direction: Decision; key: number } | null>(null);
  const [mergedCount, setMergedCount] = useState(0);
  const [ignoredCount, setIgnoredCount] = useState(0);
  const [undoStack, setUndoStack] = useState<UndoEntry[]>([]);
  const [toast, setToast] = useState<UndoEntry | null>(null);
  const [done, setDone] = useState(false);

  const current = proposals[index];
  const total = proposals.length;

  const handleDecide = useCallback(
    (decision: Decision) => {
      if (!current) return;

      const entry: UndoEntry = { proposal: current, decision, index };
      setUndoStack((prev) => [...prev, entry]);
      setToast(entry);

      if (decision === 'merge') {
        onMerge?.(current.skuA, current.skuB);
        setMergedCount((c) => c + 1);
      } else {
        onIgnore?.(current.skuA, current.skuB);
        setIgnoredCount((c) => c + 1);
      }

      setExiting({ direction: decision, key: index });
      setTimeout(() => {
        setExiting(null);
        const next = index + 1;
        if (next >= total) {
          setDone(true);
          onComplete?.(mergedCount + (decision === 'merge' ? 1 : 0), ignoredCount + (decision === 'ignore' ? 1 : 0));
        } else {
          setIndex(next);
        }
      }, 350);
    },
    [current, index, total, mergedCount, ignoredCount, onMerge, onIgnore, onComplete]
  );

  const handleUndo = useCallback(() => {
    const prev = undoStack[undoStack.length - 1];
    if (!prev) return;
    setUndoStack((s) => s.slice(0, -1));
    setToast(null);
    setIndex(prev.index);
    setDone(false);
    if (prev.decision === 'merge') setMergedCount((c) => Math.max(0, c - 1));
    else setIgnoredCount((c) => Math.max(0, c - 1));
  }, [undoStack]);

  // ── Done state ────────────────────────────────────────────────────────────
  if (done) {
    return (
      <section className="min-h-screen bg-background flex items-center justify-center px-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.96 }}
          animate={{ opacity: 1, scale: 1 }}
          className="max-w-md w-full text-center"
        >
          <div className="text-xs font-mono text-secondary uppercase tracking-widest mb-6 opacity-60">
            Deduplication · Hoàn tất
          </div>
          <h2 className="font-tight font-black text-primary text-4xl tracking-tight mb-4">
            Sạch bong!
          </h2>
          <p className="text-secondary text-sm leading-relaxed mb-8">
            Đã xử lý <span className="text-primary font-semibold tabular-nums">{total}</span> cặp SKU —{' '}
            <span className="text-[#D1FA4A] tabular-nums font-bold">{mergedCount} gộp</span>,{' '}
            <span className="text-secondary tabular-nums">{ignoredCount} bỏ qua</span>.
          </p>
          <div className="grid grid-cols-2 gap-3 text-left bg-[#0D0D0D] border border-[#1e1e1e] rounded-2xl p-6 mb-8">
            <div>
              <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-1">Gộp lại</div>
              <div className="font-tight font-black text-[#D1FA4A] text-3xl tabular-nums">{mergedCount}</div>
            </div>
            <div>
              <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-1">Bỏ qua</div>
              <div className="font-tight font-black text-secondary text-3xl tabular-nums">{ignoredCount}</div>
            </div>
          </div>
        </motion.div>
      </section>
    );
  }

  if (!current) return null;

  const progressPct = total > 0 ? (index / total) * 100 : 0;

  return (
    <section className="min-h-screen bg-background flex flex-col px-4 md:px-8 py-8">
      {/* ── Progress bar ── */}
      <div className="max-w-3xl mx-auto w-full mb-6">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-mono text-secondary uppercase tracking-widest opacity-60">
            SKU Deduplication · Tinder Mode
          </span>
          <span className="text-xs font-mono text-secondary tabular-nums">
            {index + 1} / {total}
          </span>
        </div>
        <div className="h-px bg-[#1e1e1e] rounded-full overflow-hidden">
          <motion.div
            className="h-full bg-[#D1FA4A]/50 rounded-full"
            animate={{ width: `${progressPct}%` }}
            transition={{ ease: 'easeOut', duration: 0.3 }}
          />
        </div>
      </div>

      {/* ── Card Stack ── */}
      <div className="max-w-3xl mx-auto w-full flex-1 relative" style={{ minHeight: 480 }}>
        {/* Background stack hint (next card peek) */}
        {index + 1 < total && (
          <div className="absolute inset-x-4 bottom-0 top-4 bg-[#0a0a0a] border border-[#1a1a1a] rounded-3xl" />
        )}

        <AnimatePresence>
          {exiting ? (
            <motion.div
              key={`exiting-${exiting.key}`}
              className="absolute inset-0"
              animate={{
                x: exiting.direction === 'merge' ? 400 : -400,
                rotate: exiting.direction === 'merge' ? 20 : -20,
                opacity: 0,
              }}
              transition={{ duration: 0.35, ease: [0.32, 0, 0.67, 0] }}
            >
              <SwipeCard proposal={proposals[exiting.key]} onDecide={() => {}} />
            </motion.div>
          ) : (
            <motion.div
              key={`card-${index}`}
              className="absolute inset-0"
              initial={{ opacity: 0, scale: 0.97, y: 12 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
            >
              <SwipeCard proposal={current} onDecide={handleDecide} />
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* ── Drag hint ── */}
      <div className="max-w-3xl mx-auto w-full mt-4 flex justify-center gap-8 text-[10px] font-mono text-secondary/30 uppercase tracking-widest">
        <span>← Kéo trái = Bỏ qua</span>
        <span>Kéo phải = Gộp →</span>
      </div>

      {/* ── Undo Toast ── */}
      <AnimatePresence>
        {toast && (
          <UndoToast
            key={toast.index}
            entry={toast}
            onUndo={handleUndo}
            onDismiss={() => setToast(null)}
          />
        )}
      </AnimatePresence>
    </section>
  );
}
