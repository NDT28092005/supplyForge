'use client';

import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import TrustTheaterHero from '@/components/TrustTheaterHero';
import CashflowVault from '@/components/CashflowVault';
import SkuMergeCard from '@/components/SkuMergeCard';
import RecoveryCopilot from '@/components/RecoveryCopilot';
import DataMappingConfirm from '@/components/DataMappingConfirm';
import InventoryAgingRadar, { InventoryAgingData } from '@/components/InventoryAgingRadar';
import SkuDeduplicationTinder, { MatchProposal } from '@/components/SkuDeduplicationTinder';
import CommandCenterDashboard from '@/components/CommandCenterDashboard';

// ─── Types ──────────────────────────────────────────────────────────────────
type AppStage = 'hero' | 'processing' | 'mapping' | 'dashboard' | 'aging' | 'recovery-copilot' | 'dedup' | 'recovery' | 'command-center';

interface DashboardData {
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

// ─── Processing steps shown during analysis ────────────────────────────────
const PROCESSING_STEPS = [
  { label: 'Đang tải file lên server...', duration: 500 },
  { label: 'Mapping cột dữ liệu bằng Gemini...', duration: 800 },
  { label: 'Phân tích dòng tiền và tồn kho...', duration: 1000 },
  { label: 'Kiểm tra SKU trùng lặp...', duration: 700 },
  { label: 'Hoàn tất báo cáo...', duration: 500 },
];

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const WORKSPACE_ID = process.env.NEXT_PUBLIC_WORKSPACE_ID || '1';

// ─── Processing Screen (Analyze File) ─────────────────────────────────────────
function ProcessingScreen({ 
  file, 
  onMappingReady 
}: { 
  file?: File; 
  onMappingReady: (data: any) => void 
}) {
  const [stepIndex, setStepIndex] = useState(0);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    
    async function runAnalysis() {
      try {
        // Step 1: Upload
        setStepIndex(0);
        setProgress(10);
        
        if (!file) {
          await new Promise(r => setTimeout(r, 2000));
        } else {
          const formData = new FormData();
          formData.append('file', file);
          
          const uploadRes = await fetch(`${API_URL}/api/v1/import/analyze`, {
            method: 'POST',
            body: formData,
          });
          
          const data = await uploadRes.json();
          if (!uploadRes.ok) throw new Error(data.error || 'Upload hoặc AI Mapping thất bại.');
          
          setProgress(80);
          setStepIndex(1);
          await new Promise(r => setTimeout(r, 800));

          setProgress(100);
          setStepIndex(2);
          
          if (isMounted) {
            setTimeout(() => onMappingReady(data), 500);
          }
        }
      } catch (err: any) {
        if (isMounted) setError(err.message);
      }
    }

    runAnalysis();
    return () => { isMounted = false; };
  }, [file, onMappingReady]);

  if (error) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center px-6">
        <p className="text-danger font-mono text-sm mb-4">Error: {error}</p>
        <button onClick={() => window.location.reload()} className="text-accent underline text-xs">Thử lại</button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center px-6">
      <motion.div
        className="w-full max-w-md"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <p className="text-xs font-mono text-secondary tracking-widest uppercase mb-12 text-center opacity-50">
          SupplyForge AI · Đang xử lý
        </p>

        <div className="w-full bg-surface border border-border-base rounded-full h-px mb-8 overflow-hidden">
          <motion.div
            className="bg-accent h-px rounded-full"
            animate={{ width: `${progress}%` }}
            transition={{ ease: 'easeOut', duration: 0.5 }}
          />
        </div>

        <div className="space-y-3">
          {PROCESSING_STEPS.map((step, i) => (
            <div
              key={i}
              className={`flex items-center gap-3 transition-all duration-500 ${
                i < stepIndex
                  ? 'opacity-30'
                  : i === stepIndex
                  ? 'opacity-100'
                  : 'opacity-10'
              }`}
            >
              <div
                className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${
                  i < stepIndex
                    ? 'bg-primary'
                    : i === stepIndex
                    ? 'bg-accent'
                    : 'bg-border-base'
                }`}
              />
              <span
                className={`text-sm font-mono ${
                  i === stepIndex ? 'text-primary' : 'text-secondary'
                }`}
              >
                {step.label}
                {i === stepIndex && (
                  <span className="animate-cursor-blink text-accent ml-1">▋</span>
                )}
              </span>
            </div>
          ))}
        </div>

        <div className="mt-10 text-right">
          <span className="font-tight font-black text-2xl tracking-tightest text-accent">
            {Math.round(progress)}%
          </span>
        </div>
      </motion.div>
    </div>
  );
}

// ─── Recovery Plan Screen (Dynamic-ish) ──────────────────────────────────────
function RecoveryPlan({ 
  onRestart, 
  data 
}: { 
  onRestart: () => void;
  data: DashboardData | null;
}) {
  const amountStr = data ? data.totalDeadStockValueVnd.toLocaleString('vi-VN') : '---';
  
  const ACTIONS = [
    {
      priority: '01',
      title: 'Flash Sale SKU mất trend',
      impact: `− ${amountStr} VND đóng băng`,
      timeline: 'Tuần 1–2',
      detail:
        'Giảm giá 25–35% trên tất cả channel. AI gợi ý khung giờ cao điểm và mức giảm tối ưu.',
    },
    {
      priority: '02',
      title: 'Bundle SKU trùng sau khi gộp',
      impact: '+ 12% biên lợi nhuận',
      timeline: 'Tuần 2–3',
      detail:
        'Tạo combo sản phẩm tương tự để tăng AOV và giảm chi phí lưu kho đơn lẻ.',
    },
  ];

  return (
    <section className="min-h-screen bg-background flex flex-col justify-center px-6 md:px-16 lg:px-24 py-24">
      <motion.div
        className="max-w-3xl mx-auto w-full"
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
      >
        <div className="mb-12">
          <p className="text-xs font-mono text-secondary tracking-widest uppercase mb-4 opacity-60">
            AI Recovery Plan · Được tạo tự động
          </p>
          <h2 className="font-tight font-black tracking-tightest text-primary text-4xl sm:text-5xl leading-tight mb-3">
            Hành động để thu hồi
            <br />
            <span className="text-accent">{amountStr} VND</span>.
          </h2>
        </div>

        <div className="space-y-4 mb-12">
          {ACTIONS.map((action, i) => (
            <motion.div
              key={action.priority}
              className="border border-border-base bg-surface rounded-sm p-6 hover:bg-surface-hover transition-colors"
              initial={{ opacity: 0, x: -16 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.12 + 0.2, duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
            >
              <div className="flex flex-col sm:flex-row sm:items-start gap-4">
                <span className="font-tight font-black text-5xl text-border-base leading-none flex-shrink-0 select-none">
                  {action.priority}
                </span>

                <div className="flex-1">
                  <div className="flex flex-wrap items-start justify-between gap-2 mb-2">
                    <h3 className="font-tight font-bold text-primary text-base tracking-tight">
                      {action.title}
                    </h3>
                  </div>
                  <p className="text-accent font-mono text-xs font-semibold mb-2">
                    {action.impact}
                  </p>
                  <p className="text-secondary text-sm leading-relaxed">{action.detail}</p>
                </div>
              </div>
            </motion.div>
          ))}
        </div>

        <div className="border-t border-border-base pt-8 flex flex-col sm:flex-row items-start sm:items-center gap-4">
          <button
            onClick={async () => {
              if (confirm('Bạn có chắc muốn xóa toàn bộ dữ liệu hiện tại để import file mới?')) {
                try {
                  await fetch('http://localhost:8080/api/v1/import/clear', { method: 'DELETE' });
                  onRestart();
                } catch (e) {
                  alert('Lỗi xóa dữ liệu');
                }
              }
            }}
            className="bg-[#2a1b1b] text-danger border border-danger/30 font-tight font-bold text-sm px-8 py-3.5 rounded-sm hover:bg-danger hover:text-background transition-colors"
          >
            Xóa dữ liệu cũ & Import file mới
          </button>
          <button
            onClick={onRestart}
            className="text-secondary text-sm hover:text-primary transition-colors underline underline-offset-4"
          >
            Chỉ phân tích file khác (Không xóa cũ)
          </button>
        </div>
      </motion.div>
    </section>
  );
}

// ─── Demo pairs (fallback khi API không có dữ liệu) ──────────────────────────
function getDemoPairs(): MatchProposal[] {
  return [
    {
      confidenceScore: 0.92,
      skuA: { id: 'd1a', skuCode: 'SKU-001', name: 'iPhone 13 128GB Đen', price: 18990000, stock: 24, source: 'Shopee' },
      skuB: { id: 'd1b', skuCode: 'SKU-045', name: 'iPhone 13 128GB Đen cũ', price: 15500000, stock: 3, source: 'TikTok' },
    },
    {
      confidenceScore: 0.78,
      skuA: { id: 'd2a', skuCode: 'SKU-002', name: 'Áo Polo Nam Xanh Navy Size L', price: 320000, stock: 150, source: 'ERP' },
      skuB: { id: 'd2b', skuCode: 'SKU-078', name: 'Áo Polo Nam Xanh Navy L', price: 299000, stock: 80, source: 'Lazada' },
    },
    {
      confidenceScore: 0.85,
      skuA: { id: 'd3a', skuCode: 'SKU-103', name: 'Tai nghe Sony WH-1000XM5 Đen', price: 7990000, stock: 12, source: 'ERP' },
      skuB: { id: 'd3b', skuCode: 'SKU-104', name: 'Sony WH1000XM5 Màu Đen', price: 8200000, stock: 5, source: 'Shopee' },
    },
  ];
}


// ─── Main Page (Flow Orchestrator) ──────────────────────────────────────────
export default function Home() {
  const [stage, setStage] = useState<AppStage>('hero');
  const [uploadFile, setUploadFile] = useState<File | undefined>();
  const [mappingData, setMappingData] = useState<any>(null);
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [agingData, setAgingData] = useState<InventoryAgingData | null>(null);
  const [portfolioData, setPortfolioData] = useState<any>(null);
  const [portfolioLoading, setPortfolioLoading] = useState(false);
  const [dedupProposals, setDedupProposals] = useState<MatchProposal[]>([]);
  const [isProcessingBatch, setIsProcessingBatch] = useState(false);
  const copilotRef = useRef<HTMLDivElement>(null);

  // Tự động kiểm tra dữ liệu khi vào trang
  useEffect(() => {
    async function checkExistingData() {
      try {
        const res = await fetch(`${API_URL}/api/v1/products/check-data`);
        const result = await res.json();
        
        if (result.hasData) {
          console.log("Dữ liệu đã có sẵn, đang tải Dashboard...");
          const refreshRes = await fetch(`${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/dead-stock/refresh`, {
            method: 'POST',
          });
          if (refreshRes.ok) {
            const data = await refreshRes.json();
            setDashboardData(data);
            // Tải luôn Aging data
            const agingRes = await fetch(`${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/aging`);
            if (agingRes.ok) setAgingData(await agingRes.json());
            setStage('dashboard');
          }
        }
      } catch (err) {
        console.error("Không thể kiểm tra dữ liệu cũ:", err);
      }
    }
    checkExistingData();
  }, []);

  const pageVariants = {
    initial: { opacity: 0 },
    enter: { opacity: 1, transition: { duration: 0.45, ease: 'easeOut' } },
    exit: { opacity: 0, transition: { duration: 0.25, ease: 'easeIn' } },
  };

  const startProcessing = (file?: File) => {
    setUploadFile(file);
    setStage('processing');
  };

  const handleMappingReady = (data: any) => {
    setMappingData(data);
    setStage('mapping');
  };

  const handleConfirmMapping = async (finalMapping: Record<string, string>) => {
    setIsProcessingBatch(true);
    try {
      const formData = new FormData();
      if (uploadFile) formData.append('file', uploadFile);
      formData.append('mappingRule', JSON.stringify(finalMapping));

      const res = await fetch(`${API_URL}/api/v1/import/process`, {
        method: 'POST',
        body: formData,
      });

      if (!res.ok) throw new Error('Lỗi khi import batch data');

      // Sau khi import xong, lấy dữ liệu dashboard mới
      const refreshRes = await fetch(`${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/dead-stock/refresh`, {
        method: 'POST',
      });
      const data = await refreshRes.json();
      setDashboardData(data);
      // Tải Aging data song song
      const agingRes = await fetch(`${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/aging`);
      if (agingRes.ok) setAgingData(await agingRes.json());
      setStage('dashboard');
    } catch (e) {
      alert(e);
    } finally {
      setIsProcessingBatch(false);
    }
  };

  // Khi bấm "Tạo AI Recovery Plan" → đi qua Aging Radar trước
  const handleOpenRecovery = async () => {
    if (!agingData) {
      try {
        const res = await fetch(`${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/aging`);
        if (res.ok) setAgingData(await res.json());
      } catch (e) {
        console.error('Không tải được aging data:', e);
      }
    }
    setStage('aging');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // ── Command Center (Portfolio-level) ────────────────────────────────────────
  const handleOpenCommandCenter = async () => {
    setStage('command-center');
    setPortfolioLoading(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
    try {
      const res = await fetch(
        `${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/portfolio-command`
      );
      if (res.ok) setPortfolioData(await res.json());
    } catch (e) {
      console.error('Không tải được portfolio data:', e);
    } finally {
      setPortfolioLoading(false);
    }
  };

  // ─── Data Refresh Helper ────────────────────────────────────────────────────
  const refreshDashboardAndAging = async () => {
    try {
      // 1. Làm mới dashboard (Dead stock refresh)
      const refreshRes = await fetch(`${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/dead-stock/refresh`, {
        method: 'POST',
      });
      if (refreshRes.ok) {
        const data = await refreshRes.json();
        setDashboardData(data);
      }
      
      // 2. Làm mới aging radar data
      const agingRes = await fetch(`${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/dashboard/aging`);
      if (agingRes.ok) {
        const aData = await agingRes.json();
        setAgingData(aData);
      }
    } catch (e) {
      console.error("Lỗi khi làm mới dữ liệu:", e);
    }
  };

  // ─── SKU Normalization Helpers ─────────────────────────────────────────────
  const handleFetchAndOpenTinder = async () => {
    try {
      const res = await fetch(`${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/sku-merge/candidates`);
      if (res.ok) {
        const data = await res.json();
        if (!data || data.length === 0) {
          alert("Tuyệt vời! Không còn SKU nào bị nghi trùng lặp.");
          return;
        }
        // Map SkuPairDTO → MatchProposal
        const proposals: MatchProposal[] = data.map((p: any) => ({
          confidenceScore: 0.85, 
          skuA: { 
            id: p.skuAId, 
            skuCode: p.skuACode || p.skuAId, 
            name: p.skuAName, 
            price: p.skuAPrice, 
            stock: p.skuAStock, 
            source: p.skuASource 
          },
          skuB: { 
            id: p.skuBId, 
            skuCode: p.skuBCode || p.skuBId, 
            name: p.skuBName, 
            price: p.skuBPrice, 
            stock: p.skuBStock, 
            source: p.skuBSource 
          },
        }));
        setDedupProposals(proposals);
        setStage('dedup');
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    } catch (e) {
      alert("Lỗi tải danh sách SKU trùng.");
    }
  };

  const handlePerformMerge = async (a: any, b: any) => {
    try {
      await fetch(`${API_URL}/api/v1/workspaces/${WORKSPACE_ID}/sku-merge/merge`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          parentSkuId: a.id,
          childSkuId: b.id,
        }),
      });
      console.log('Successfully merged:', a.name, '←', b.name);
    } catch (e) {
      console.error('Merge failed:', e);
    }
  };

  return (
    <AnimatePresence mode="wait">
      {stage === 'hero' && (
        <motion.div key="hero" variants={pageVariants} initial="initial" animate="enter" exit="exit">
          <TrustTheaterHero onUpload={startProcessing} />
        </motion.div>
      )}

      {stage === 'processing' && (
        <motion.div key="processing" variants={pageVariants} initial="initial" animate="enter" exit="exit">
          <ProcessingScreen file={uploadFile} onMappingReady={handleMappingReady} />
        </motion.div>
      )}

      {stage === 'mapping' && mappingData && (
        <motion.div key="mapping" variants={pageVariants} initial="initial" animate="enter" exit="exit" className="absolute inset-0 z-50 bg-background overflow-auto">
          {isProcessingBatch ? (
            <div className="min-h-screen flex items-center justify-center">
              <div className="text-accent font-mono text-xl animate-pulse">Đang nạp dữ liệu vào CSDL siêu tốc...</div>
            </div>
          ) : (
            <DataMappingConfirm
              csvHeaders={mappingData.headers}
              sampleDataRow={mappingData.sampleData}
              aiSuggestions={mappingData.aiSuggestions}
              onConfirm={handleConfirmMapping}
            />
          )}
        </motion.div>
      )}

      {/* ── Aging Radar Stage ── */}
      {stage === 'aging' && agingData && (
        <motion.div key="aging" variants={pageVariants} initial="initial" animate="enter" exit="exit">
          <div className="max-w-5xl mx-auto px-6 md:px-16 lg:px-24 pt-12 pb-4 flex items-center justify-between">
            <button
              onClick={() => setStage('dashboard')}
              className="text-secondary text-sm hover:text-primary transition-colors font-mono"
            >
              ← Quay lại Dashboard
            </button>
            <motion.button
              onClick={() => {
                setStage('recovery-copilot');
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
              className="bg-accent text-background font-tight font-bold text-sm px-6 py-3 rounded-xl"
              whileHover={{ scale: 1.015, backgroundColor: '#deff5a' }}
              whileTap={{ scale: 0.985 }}
            >
              Xem AI Action Plan →
            </motion.button>
          </div>
          <InventoryAgingRadar data={agingData} />
        </motion.div>
      )}

      {/* ── Command Center Stage ── */}
      {stage === 'command-center' && (
        <motion.div key="command-center" variants={pageVariants} initial="initial" animate="enter" exit="exit">
          <CommandCenterDashboard
            portfolioData={portfolioData}
            loading={portfolioLoading}
            onBack={() => setStage('dashboard')}
          />
        </motion.div>
      )}

      {/* Dashboard + RecoveryCopilot cùng trang, slide-in bên dưới */}
      {(stage === 'dashboard' || stage === 'recovery-copilot') && (
        <motion.div key="dashboard" variants={pageVariants} initial="initial" animate="enter" exit="exit">
          <CashflowVault
            data={dashboardData}
            onProceed={handleFetchAndOpenTinder}
            onRecovery={handleOpenRecovery}
            onCommandCenter={handleOpenCommandCenter}
          />

          {stage === 'recovery-copilot' && (
            <motion.div
              ref={copilotRef}
              initial={{ opacity: 0, y: 40 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
            >
              {/* Divider */}
              <div className="max-w-4xl mx-auto px-6 md:px-16 lg:px-24">
                <div className="border-t border-[#262626] mb-16" />
              </div>

              <RecoveryCopilot
                cluster={
                  portfolioData?.priorityClusters?.[0]
                    ? {
                        clusterName: portfolioData.priorityClusters[0].clusterName,
                        industryType:
                          portfolioData.priorityClusters[0].industryCode === 'HOME_LIVING' ? 'FURNITURE' :
                          portfolioData.priorityClusters[0].industryCode === 'ELECTRONICS_TECH' ? 'ELECTRONICS' :
                          portfolioData.priorityClusters[0].industryCode === 'FASHION_FAST' ? 'FASHION' :
                          portfolioData.priorityClusters[0].industryCode === 'FMCG_FOOD' ? 'FMCG' : 'GENERAL',
                        totalSunkCost: portfolioData.priorityClusters[0].totalFrozenValue * 1.05,
                        totalInitialCost: portfolioData.priorityClusters[0].totalFrozenValue,
                        accumulatedStorageFee: portfolioData.priorityClusters[0].totalFrozenValue * 0.05,
                        maxSafeDiscount: 20.6,
                        items: portfolioData.priorityClusters[0].topSkuSamples?.map((s: any) => ({
                          skuName: s.name,
                          agingDays: s.daysInInventory,
                          stock: 100, // mock
                        })) || [],
                        bundleAvailable: agingData?.healthySkus?.[0]
                          ? {
                              hotItemName: agingData.healthySkus[0].name,
                              hotItemPrice: agingData.healthySkus[0].sellingPrice,
                              hotItemCost: agingData.healthySkus[0].costPrice,
                              bundleDiscountPct: 15,
                            }
                          : undefined,
                      }
                    : undefined
                }
              />

              {/* Nav sau Recovery */}
              <div className="max-w-4xl mx-auto px-6 md:px-16 lg:px-24 pb-24">
                <div className="border-t border-[#262626] pt-8 flex flex-col sm:flex-row items-center gap-4">
                  <motion.button
                    onClick={handleFetchAndOpenTinder}
                    className="bg-accent text-background font-tight font-bold text-sm px-8 py-3.5 rounded-xl flex-shrink-0"
                    whileHover={{ scale: 1.015, backgroundColor: '#deff5a' }}
                    whileTap={{ scale: 0.985 }}
                    transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                  >
                    Tiếp tục chuẩn hóa SKU →
                  </motion.button>
                  <button
                    onClick={async () => {
                      if (confirm('Bạn có chắc muốn xóa toàn bộ dữ liệu hiện tại để import file mới?')) {
                        try {
                          await fetch('http://localhost:8080/api/v1/import/clear', { method: 'DELETE' });
                          setAgingData(null);
                          setStage('hero');
                        } catch (e) {
                          alert('Lỗi xóa dữ liệu');
                        }
                      }
                    }}
                    className="bg-[#2a1b1b] text-danger border border-danger/30 font-tight font-bold text-sm px-8 py-3.5 rounded-xl hover:bg-danger hover:text-background transition-colors flex-shrink-0"
                  >
                    Xóa dữ liệu cũ & Import file mới
                  </button>
                  <button
                    onClick={() => setStage('hero')}
                    className="text-secondary text-sm hover:text-primary transition-colors underline underline-offset-4 flex-shrink-0"
                  >
                    Chỉ phân tích file khác
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </motion.div>
      )}

      {stage === 'dedup' && (
        <motion.div key="dedup" variants={pageVariants} initial="initial" animate="enter" exit="exit">
          <SkuDeduplicationTinder
            proposals={dedupProposals}
            onMerge={handlePerformMerge}
            onIgnore={(a, b) => console.log('Ignored:', a.name, b.name)}
            onComplete={async () => {
              await refreshDashboardAndAging(); // Tải lại toàn bộ số liệu mới
              setStage('dashboard');
            }}
          />
        </motion.div>
      )}
    </AnimatePresence>
  );
}
