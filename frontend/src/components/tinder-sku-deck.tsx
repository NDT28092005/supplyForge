'use client';

import { motion, useMotionValue, useTransform, PanInfo } from 'framer-motion';
import { useCallback, useEffect, useState } from 'react';
import type { SkuDuplicatePair } from '@/lib/api-types';
import { API_BASE, WORKSPACE_ID } from '@/lib/config';

export function TinderSkuDeck({ onMerged }: { onMerged?: () => void }) {
  const [deck, setDeck] = useState<SkuDuplicatePair[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [swapParent, setSwapParent] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      const res = await fetch(
        `${API_BASE}/api/v1/workspaces/${WORKSPACE_ID}/sku-merge/candidates`,
      );
      if (!res.ok) throw new Error(await res.text());
      setDeck(await res.json());
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Lỗi tải');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const top = deck[0];

  useEffect(() => {
    setSwapParent(false);
  }, [top?.skuAId, top?.skuBId]);

  const skip = () => setDeck((d) => d.slice(1));

  const merge = async () => {
    if (!top) return;
    const parentId = swapParent ? top.skuBId : top.skuAId;
    const childId = swapParent ? top.skuAId : top.skuBId;
    try {
      const res = await fetch(
        `${API_BASE}/api/v1/workspaces/${WORKSPACE_ID}/sku-merge/merge`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ parentSkuId: parentId, childSkuId: childId }),
        },
      );
      if (!res.ok) throw new Error(await res.text());
      setDeck((d) => d.slice(1));
      onMerged?.();
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Gộp thất bại');
    }
  };

  const x = useMotionValue(0);
  const rot = useTransform(x, [-200, 200], [-12, 12]);
  const leftOpacity = useTransform(x, [-120, -40], [1, 0]);
  const rightOpacity = useTransform(x, [40, 120], [0, 1]);

  const onDragEnd = (_: unknown, info: PanInfo) => {
    if (info.offset.x < -100) skip();
    else if (info.offset.x > 100) merge();
    x.set(0);
  };

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-trust-navy">Gộp SKU trùng</h2>
          <p className="mt-1 text-sm text-slate-600">
            Vuốt trái bỏ qua · vuốt phải gộp (theo vai trò cha dưới đây).
          </p>
        </div>
        <button
          type="button"
          onClick={load}
          className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
        >
          Làm mới
        </button>
      </div>

      {loading && <p className="mt-6 text-sm text-slate-500">Đang tải cặp trùng…</p>}
      {err && <p className="mt-4 text-sm text-red-600">{err}</p>}

      {!loading && !top && (
        <p className="mt-6 text-sm text-slate-600">
          Không còn cặp gợi ý (hoặc chưa có SKU). Hãy import file trước.
        </p>
      )}

      {top && (
        <div className="relative mx-auto mt-6 max-w-md">
          <motion.div
            style={{ x, rotate: rot }}
            drag="x"
            dragConstraints={{ left: -180, right: 180 }}
            dragElastic={0.9}
            onDragEnd={onDragEnd}
            className="cursor-grab active:cursor-grabbing"
          >
            <div className="rounded-2xl border-2 border-slate-200 bg-gradient-to-b from-white to-slate-50 p-6 shadow-lg">
              <p className="text-xs font-medium uppercase tracking-wide text-alert-shopee">
                Phát hiện SKU trùng lặp
              </p>
              <p className="mt-3 text-base font-medium text-trust-navy">[{top.skuAName}]</p>
              <p className="text-center text-slate-400">&amp;</p>
              <p className="text-base font-medium text-trust-navy">[{top.skuBName}]</p>
              <p className="mt-2 text-xs text-slate-500">Khoảng cách chuỗi: {top.distance}</p>

              <label className="mt-4 flex cursor-pointer items-center gap-2 text-sm text-slate-700">
                <input
                  type="checkbox"
                  checked={swapParent}
                  onChange={(e) => setSwapParent(e.target.checked)}
                />
                Đổi vai trò: dùng <strong>{swapParent ? top.skuBName : top.skuAName}</strong> làm SKU
                chính
              </label>

              <div className="mt-5 flex gap-3">
                <button
                  type="button"
                  onClick={skip}
                  className="flex-1 rounded-xl border border-slate-300 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  Bỏ qua
                </button>
                <button
                  type="button"
                  onClick={merge}
                  className="flex-1 rounded-xl bg-trust-navy py-3 text-sm font-medium text-white hover:bg-trust-blue"
                >
                  Gộp chung
                </button>
              </div>
            </div>
          </motion.div>

          <motion.div
            style={{ opacity: leftOpacity }}
            className="pointer-events-none absolute left-2 top-1/2 -translate-y-1/2 rounded-lg border-2 border-slate-400 px-3 py-1 text-sm font-bold text-slate-600"
          >
            BỎ QUA
          </motion.div>
          <motion.div
            style={{ opacity: rightOpacity }}
            className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 rounded-lg border-2 border-emerald-600 px-3 py-1 text-sm font-bold text-emerald-700"
          >
            GỘP
          </motion.div>
        </div>
      )}
    </section>
  );
}
