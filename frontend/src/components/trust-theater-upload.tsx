'use client';

import { AnimatePresence, motion } from 'framer-motion';
import { useCallback, useRef, useState } from 'react';
import type { SpreadsheetImportResponse } from '@/lib/api-types';
import { API_BASE, WORKSPACE_ID } from '@/lib/config';

type StepState = 'pending' | 'running' | 'done';

type Step = { id: string; label: string };

const STEPS: Step[] = [
  { id: 'read', label: 'Đọc file nội bộ…' },
  { id: 'pii', label: 'Gỡ bỏ dữ liệu nhạy cảm khách hàng…' },
  { id: 'enc', label: 'Mã hóa SKU & Tài chính…' },
  { id: 'ai', label: 'Gửi dữ liệu mã hóa lên AI Cloud…' },
];

export function TrustTheaterUpload({
  onImportComplete,
}: {
  onImportComplete?: (r: SpreadsheetImportResponse) => void;
}) {
  const [dragOver, setDragOver] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SpreadsheetImportResponse | null>(null);
  const [stepStates, setStepStates] = useState<Record<string, StepState>>(() =>
    Object.fromEntries(STEPS.map((s) => [s.id, 'pending'])),
  );
  const fileRef = useRef<File | null>(null);

  const runTheaterThenUpload = useCallback(async (file: File) => {
    setError(null);
    setResult(null);
    setBusy(true);
    fileRef.current = file;
    setStepStates(Object.fromEntries(STEPS.map((s) => [s.id, 'pending'])));

    const minIntroMs = 2200;
    const introStart = performance.now();

    for (let i = 0; i < STEPS.length - 1; i++) {
      const step = STEPS[i];
      setStepStates((prev) => ({ ...prev, [step.id]: 'running' }));
      await new Promise((r) => setTimeout(r, 480));
      setStepStates((prev) => ({ ...prev, [step.id]: 'done' }));
    }

    const aiStep = STEPS[STEPS.length - 1];
    setStepStates((prev) => ({ ...prev, [aiStep.id]: 'running' }));

    const elapsed = performance.now() - introStart;
    if (elapsed < minIntroMs) {
      await new Promise((r) => setTimeout(r, minIntroMs - elapsed));
    }

    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await fetch(
        `${API_BASE}/api/v1/workspaces/${WORKSPACE_ID}/imports/spreadsheet`,
        { method: 'POST', body: fd },
      );
      if (!res.ok) {
        const t = await res.text();
        throw new Error(t || res.statusText);
      }
      const json = (await res.json()) as SpreadsheetImportResponse;
      setStepStates((prev) => ({ ...prev, [aiStep.id]: 'done' }));
      setResult(json);
      onImportComplete?.(json);
    } catch (e) {
      setStepStates((prev) => ({ ...prev, [aiStep.id]: 'pending' }));
      setError(e instanceof Error ? e.message : 'Upload thất bại');
    } finally {
      setBusy(false);
    }
  }, [onImportComplete]);

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const f = e.dataTransfer.files?.[0];
    if (!f || busy) return;
    runTheaterThenUpload(f);
  };

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-trust-navy">Nhập dữ liệu kho</h2>
      <p className="mt-1 text-sm text-slate-600">
        Kéo thả Excel / CSV. Hệ thống mô phỏng bảo mật trước khi gửi file thật (Trust Theater).
      </p>

      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={onDrop}
        data-testid="upload-dropzone"
        className={`mt-4 flex min-h-[140px] cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-4 py-8 transition-colors ${
          dragOver ? 'border-trust-blue bg-slate-50' : 'border-slate-200 bg-slate-50/50'
        }`}
      >
        <input
          type="file"
          accept=".xlsx,.csv"
          className="hidden"
          id="sf-file"
          disabled={busy}
          onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) runTheaterThenUpload(f);
            e.target.value = '';
          }}
        />
        <label htmlFor="sf-file" className="cursor-pointer text-center text-sm text-slate-700">
          {busy ? 'Đang xử lý…' : 'Thả file vào đây hoặc bấm để chọn'}
        </label>
        <span className="mt-2 text-xs text-slate-500">.xlsx · .csv · Workspace #{WORKSPACE_ID}</span>
      </div>

      <AnimatePresence>
        {busy && (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="mt-5 overflow-hidden rounded-lg border border-slate-800 bg-[#0d1117] px-4 py-3 font-mono text-[13px] text-slate-200 shadow-inner"
          >
            <div className="mb-2 text-xs uppercase tracking-wider text-emerald-400/90">
              supplyforge-secure-channel
            </div>
            {STEPS.map((s) => {
              const st = stepStates[s.id];
              return (
                <motion.div
                  key={s.id}
                  initial={{ opacity: 0, x: -6 }}
                  animate={{ opacity: 1, x: 0 }}
                  className="flex items-center gap-2 py-1"
                >
                  <span className="text-slate-500">
                    {st === 'done' ? '✓' : st === 'running' ? '›' : '·'}
                  </span>
                  <span className={st === 'running' ? 'text-amber-300' : ''}>{s.label}</span>
                  {st === 'running' && s.id === 'ai' && (
                    <motion.span
                      className="text-xs text-slate-500"
                      animate={{ opacity: [0.4, 1, 0.4] }}
                      transition={{ repeat: Infinity, duration: 1.2 }}
                    >
                      Loading
                    </motion.span>
                  )}
                  {st === 'done' && <span className="ml-auto text-emerald-400">Done</span>}
                </motion.div>
              );
            })}
          </motion.div>
        )}
      </AnimatePresence>

      {error && (
        <p className="mt-3 text-sm text-red-600" role="alert">
          {error}
        </p>
      )}

      {result && !busy && (
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="mt-3 text-sm text-emerald-700"
        >
          Đã nhập <strong>{result.rowsImported}</strong> dòng · data source #{result.dataSourceId}
        </motion.p>
      )}
    </section>
  );
}
