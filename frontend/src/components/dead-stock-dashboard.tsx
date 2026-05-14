'use client';

import { motion } from 'framer-motion';
import { useCallback, useEffect, useState } from 'react';
import type { DeadStockDashboard } from '@/lib/api-types';
import { API_BASE, WORKSPACE_ID } from '@/lib/config';
import { formatVnd } from '@/lib/format-vnd';

export function DeadStockDashboardPanel({ tick }: { tick?: number }) {
  const [data, setData] = useState<DeadStockDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const load = useCallback(async (refresh = false) => {
    setLoading(true);
    setErr(null);
    try {
      const url = refresh
        ? `${API_BASE}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/dead-stock/refresh`
        : `${API_BASE}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/dead-stock`;
      const res = await fetch(url, { method: refresh ? 'POST' : 'GET' });
      if (!res.ok) throw new Error(await res.text());
      setData(await res.json());
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Lỗi');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(false);
  }, [load, tick]);

  const dead = data?.totalDeadStockValueVnd ?? 0;
  const loss = data?.estimatedLoss30DaysVnd ?? 0;

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold text-trust-navy">Tồn kho & rủi ro</h2>
        <button
          type="button"
          onClick={() => load(true)}
          disabled={loading}
          className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          Làm mới số liệu
        </button>
      </div>

      {loading && <p className="text-sm text-slate-500">Đang tải…</p>}
      {err && <p className="text-sm text-red-600">{err}</p>}

      {data && !loading && (
        <>
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="overflow-hidden rounded-2xl border-2 border-red-200 bg-gradient-to-br from-red-50 to-white shadow-md"
          >
            <div className="border-b border-red-100 bg-red-600/95 px-5 py-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-red-100">
                Cảnh báo · Loss aversion
              </p>
            </div>
            <div className="px-5 py-5">
              <p className="text-sm font-medium text-red-900/90">
                Cảnh báo:{' '}
                <span className="text-2xl font-bold tabular-nums text-red-700">
                  {formatVnd(dead)} VNĐ
                </span>{' '}
                đang bị đóng băng trong tồn kho chết.
              </p>
              <p className="mt-3 text-sm leading-relaxed text-red-900/80">
                Trong 30 ngày tới, bạn có thể bốc hơi thêm{' '}
                <strong className="tabular-nums text-alert-shopee">
                  {formatVnd(loss)} VNĐ
                </strong>{' '}
                do trượt giá & chi phí lưu kho (ước tính MVP).
              </p>
              <p className="mt-2 text-xs text-slate-600">
                SKU gốc: {data.totalRootSkuCount} · SKU “chết” (snapshot cũ &gt; 45 ngày):{' '}
                {data.staleSkuCount} · Tổng giá trị tồn ước tính:{' '}
                {formatVnd(data.totalInventoryValueVnd)} VNĐ
              </p>
              <motion.button
                type="button"
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="mt-5 w-full rounded-xl bg-gradient-to-r from-amber-500 to-alert-shopee py-3 text-sm font-semibold text-white shadow-md"
              >
                ⚡ Tạo Kế Hoạch Xả Hàng Bằng AI
              </motion.button>
              <p className="mt-2 text-center text-[11px] text-slate-500">
                Nút CTA — nối prompt AI ở bước sau
              </p>
            </div>
          </motion.div>

          {data.topStaleSkus.length > 0 && (
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h3 className="text-sm font-semibold text-trust-navy">Top tồn đóng băng</h3>
              <ul className="mt-3 divide-y divide-slate-100">
                {data.topStaleSkus.map((row) => (
                  <li key={row.skuId} className="flex justify-between gap-3 py-2 text-sm">
                    <span className="truncate text-slate-800">{row.name}</span>
                    <span className="shrink-0 tabular-nums text-alert-shopee">
                      {formatVnd(row.frozenValueVnd)} ₫
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </section>
  );
}
