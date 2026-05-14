'use client';

import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface SkuPair {
  skuAId: number;
  skuAName: string;
  skuBId: number;
  skuBName: string;
  distance: number;
}

interface SkuMergeCardProps {
  onComplete: () => void;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const WORKSPACE_ID = process.env.NEXT_PUBLIC_WORKSPACE_ID || '1';

// Tính % tương đồng từ Levenshtein distance
function similarityPct(a: string, b: string, dist: number): number {
  const maxLen = Math.max(a.length, b.length);
  if (maxLen === 0) return 100;
  return Math.round((1 - dist / maxLen) * 100);
}

export default function SkuMergeCard({ onComplete }: SkuMergeCardProps) {
  const [pairs, setPairs] = useState<SkuPair[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [direction, setDirection] = useState<'left' | 'right' | null>(null);
  const [mergedCount, setMergedCount] = useState(0);
  const [skippedCount, setSkippedCount] = useState(0);
  const [hoverAction, setHoverAction] = useState<'skip' | 'merge' | null>(null);

  // Fetch danh sách cặp SKU trùng từ API
  useEffect(() => {
    async function fetchPairs() {
      try {
        const res = await fetch(
          `${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/sku-merge/candidates`
        );
        if (!res.ok) throw new Error(`API lỗi: ${res.status}`);
        const data: SkuPair[] = await res.json();
        setPairs(data);
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    fetchPairs();
  }, []);

  const current = pairs[currentIndex];
  const isLast = currentIndex >= pairs.length;

  const handleAction = useCallback(
    async (action: 'skip' | 'merge') => {
      if (!current) return;
      setDirection(action === 'merge' ? 'right' : 'left');

      if (action === 'merge') {
        try {
          await fetch(
            `${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/sku-merge/merge`,
            {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                parentSkuId: current.skuAId,
                childSkuId: current.skuBId,
              }),
            }
          );
          setMergedCount((c) => c + 1);
        } catch {
          // Gộp thất bại → vẫn tiếp tục xử lý cặp tiếp theo
        }
      } else {
        setSkippedCount((c) => c + 1);
      }

      setTimeout(() => {
        setDirection(null);
        setCurrentIndex((i) => i + 1);
      }, 380);
    },
    [current]
  );

  // Phím tắt ← →
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (isLast) return;
      if (e.key === 'ArrowRight') handleAction('merge');
      if (e.key === 'ArrowLeft') handleAction('skip');
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isLast, handleAction]);

  // ── Loading ──
  if (loading) {
    return (
      <section className="min-h-screen bg-background flex items-center justify-center">
        <p className="text-secondary font-mono text-sm animate-pulse">
          Đang tải danh sách SKU trùng lặp...
        </p>
      </section>
    );
  }

  // ── Error ──
  if (error) {
    return (
      <section className="min-h-screen bg-background flex flex-col items-center justify-center gap-4">
        <p className="text-danger font-mono text-sm">Lỗi: {error}</p>
        <button
          onClick={onComplete}
          className="text-accent underline text-xs"
        >
          Bỏ qua bước này →
        </button>
      </section>
    );
  }

  // ── Không có cặp nào ──
  if (pairs.length === 0) {
    return (
      <section className="min-h-screen bg-background flex flex-col items-center justify-center px-6 py-24 text-center gap-6">
        <p className="text-xs font-mono text-secondary tracking-widest uppercase opacity-60">
          Chuẩn hóa SKU
        </p>
        <h2 className="font-tight font-black tracking-tightest text-primary text-3xl">
          Không phát hiện SKU trùng lặp.
        </h2>
        <p className="text-secondary text-sm max-w-sm">
          Dữ liệu của bạn đã sạch, hoặc các SKU khác nhau đủ để không cần gộp.
        </p>
        <motion.button
          onClick={onComplete}
          className="bg-accent text-background font-tight font-bold text-sm px-8 py-3.5 rounded-sm mt-4"
          whileHover={{ scale: 1.015, backgroundColor: '#deff5a' }}
          whileTap={{ scale: 0.985 }}
          transition={{ type: 'spring', stiffness: 400, damping: 25 }}
        >
          Tiếp tục xem Recovery Plan →
        </motion.button>
      </section>
    );
  }

  return (
    <section className="min-h-screen bg-background flex flex-col justify-center items-center px-6 py-24">
      {/* Header */}
      <motion.div
        className="text-center mb-14"
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
      >
        <p className="text-xs font-mono text-secondary tracking-widest uppercase mb-3 opacity-60">
          Chuẩn hóa SKU · Dữ liệu thực tế
        </p>
        <h2 className="font-tight font-black tracking-tightest text-primary text-3xl sm:text-4xl leading-tight">
          AI phát hiện{' '}
          <span className="text-accent">{pairs.length} cặp SKU</span> có thể là một.
        </h2>
        <p className="text-secondary text-sm mt-2">
          Gộp đúng → Giá vốn chính xác → Lợi nhuận thực.
        </p>
      </motion.div>

      {/* Progress */}
      <div className="w-full max-w-md mb-8">
        <div className="flex justify-between text-xs text-secondary font-mono mb-2">
          <span>{currentIndex} / {pairs.length} đã xử lý</span>
          <span className="text-accent">{mergedCount} đã gộp</span>
        </div>
        <div className="w-full bg-surface border border-border-base rounded-full h-1">
          <div
            className="bg-accent h-1 rounded-full transition-all duration-500"
            style={{ width: `${(currentIndex / pairs.length) * 100}%` }}
          />
        </div>
      </div>

      {/* Card area */}
      <div className="relative w-full max-w-md" style={{ minHeight: 300 }}>
        <AnimatePresence mode="wait">
          {!isLast ? (
            <motion.div
              key={`${current.skuAId}-${current.skuBId}`}
              initial={{ opacity: 0, y: 20, scale: 0.97 }}
              animate={{
                opacity: 1,
                y: hoverAction === 'merge' ? -2 : hoverAction === 'skip' ? 2 : 0,
                scale: 1,
                x: direction === 'right' ? 120 : direction === 'left' ? -120 : 0,
                rotate: direction === 'right' ? 5 : direction === 'left' ? -5 : 0,
              }}
              exit={{
                opacity: 0,
                x: direction === 'right' ? 160 : direction === 'left' ? -160 : 0,
                scale: 0.95,
              }}
              transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
              className="bg-surface border border-border-base rounded-sm p-8 w-full"
            >
              {/* Similarity badge */}
              <div className="flex items-center justify-between mb-6">
                <span className="text-xs font-mono text-secondary opacity-60 uppercase tracking-wider">
                  Tương đồng
                </span>
                <span className="text-xs font-mono font-semibold text-accent border border-accent/20 bg-accent/5 px-2 py-0.5 rounded">
                  {similarityPct(current.skuAName, current.skuBName, current.distance)}%
                </span>
              </div>

              {/* SKU pair */}
              <div className="space-y-4 mb-8">
                <div className="border border-border-base rounded-sm px-4 py-3 bg-background">
                  <p className="text-xs font-mono text-secondary mb-1 opacity-50">SKU gốc</p>
                  <p className="text-primary font-tight font-semibold text-lg tracking-tight">
                    {current.skuAName}
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <div className="flex-1 border-t border-border-base border-dashed" />
                  <span className="text-secondary text-xs font-mono opacity-40">có thể là</span>
                  <div className="flex-1 border-t border-border-base border-dashed" />
                </div>

                <div className="border border-border-base rounded-sm px-4 py-3 bg-background">
                  <p className="text-xs font-mono text-secondary mb-1 opacity-50">SKU trùng</p>
                  <p className="text-secondary font-tight font-medium text-lg tracking-tight">
                    {current.skuBName}
                  </p>
                </div>
              </div>

              {/* Distance info */}
              <div className="bg-surface border border-border-base rounded-sm px-4 py-2.5 mb-8 text-center">
                <p className="text-xs text-secondary mb-0.5">
                  Khoảng cách chỉnh sửa (Levenshtein)
                </p>
                <p className="text-primary font-tight font-bold text-base">
                  {current.distance} ký tự khác biệt
                </p>
              </div>

              {/* Actions */}
              <div className="grid grid-cols-2 gap-4">
                <button
                  onClick={() => handleAction('skip')}
                  onMouseEnter={() => setHoverAction('skip')}
                  onMouseLeave={() => setHoverAction(null)}
                  className="group text-secondary text-sm font-medium py-3 border border-border-base rounded-sm hover:border-secondary/40 hover:text-primary transition-all duration-200 text-center"
                >
                  <span className="block text-xs text-secondary/50 font-mono mb-0.5 group-hover:text-secondary/70">
                    Bỏ qua
                  </span>
                  Keep separate
                </button>

                <motion.button
                  onClick={() => handleAction('merge')}
                  onMouseEnter={() => setHoverAction('merge')}
                  onMouseLeave={() => setHoverAction(null)}
                  className="group text-primary text-sm font-semibold py-3 border border-accent/30 bg-accent/5 rounded-sm hover:bg-accent/10 hover:border-accent/60 transition-all duration-200 text-center"
                  whileHover={{ y: -2 }}
                  transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                >
                  <span className="block text-xs text-accent/60 font-mono mb-0.5 group-hover:text-accent/80">
                    Gộp chung
                  </span>
                  <span className="text-accent">Merge</span>
                </motion.button>
              </div>
            </motion.div>
          ) : (
            <motion.div
              key="done"
              initial={{ opacity: 0, scale: 0.96 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
              className="bg-surface border border-border-base rounded-sm p-10 w-full text-center"
            >
              <p className="font-tight font-black tracking-tightest text-3xl text-primary mb-2">
                Hoàn tất chuẩn hóa.
              </p>
              <p className="text-secondary text-sm mb-6">
                Đã gộp {mergedCount} · Bỏ qua {skippedCount} cặp SKU.
              </p>
              <p className="text-accent font-mono text-xs mb-8">
                Dữ liệu đã được cập nhật vào hệ thống.
              </p>
              <motion.button
                onClick={onComplete}
                className="bg-accent text-background font-tight font-bold text-sm px-8 py-3.5 rounded-sm"
                whileHover={{ scale: 1.015, backgroundColor: '#deff5a' }}
                whileTap={{ scale: 0.985 }}
                transition={{ type: 'spring', stiffness: 400, damping: 25 }}
              >
                Xem AI Recovery Plan →
              </motion.button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Keyboard hint */}
      {!isLast && (
        <motion.p
          className="mt-8 text-secondary text-xs font-mono opacity-30"
          initial={{ opacity: 0 }}
          animate={{ opacity: 0.3 }}
          transition={{ delay: 1 }}
        >
          Dùng phím ← Bỏ qua · → Gộp
        </motion.p>
      )}
    </section>
  );
}
