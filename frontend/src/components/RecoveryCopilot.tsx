'use client';

import { useState, useMemo } from 'react';
import { motion } from 'framer-motion';

export interface OperationalCluster {
  clusterName: string;
  industryType: 'FURNITURE' | 'FASHION' | 'FMCG' | 'ELECTRONICS' | 'GENERAL';
  totalSunkCost: number;
  totalInitialCost: number;
  accumulatedStorageFee: number;
  maxSafeDiscount: number;
  items: Array<{
    skuName: string;
    agingDays: number;
    stock: number;
  }>;
  bundleAvailable?: {
    hotItemName: string;
    hotItemPrice: number;
    hotItemCost: number;
    bundleDiscountPct: number;
  };
}

export interface RecoveryCopilotProps {
  cluster?: OperationalCluster;
}

function formatVND(value: number): string {
  return Math.round(value).toLocaleString('vi-VN') + ' ₫';
}

export default function RecoveryCopilot({ cluster }: RecoveryCopilotProps) {
  const [rpaState, setRpaState] = useState<'idle' | 'running' | 'done'>('idle');

  const recommendedStrategy = useMemo(() => {
    if (!cluster) return null;
    
    // ĐIỀU CHỈNH CHIẾN LƯỢC THEO ĐẶC THÙ NGÀNH (INDUSTRY ADAPTIVE ADVISOR)
    if (cluster.industryType === 'FURNITURE') {
      return {
        title: 'Bảo vệ thương hiệu & B2B',
        discount: 15,
        velocityDays: 45,
        description: 'Hold & Target Marketing + B2B Wholesale / Kênh kiến trúc sư nội thất. Chuyển kho vệ tinh giá rẻ để giảm chi phí lưu kho thay vì phá giá làm mất định vị thương hiệu.',
      };
    }
    
    // Ngành khác (FASHION, FMCG, ELECTRONICS)
    return {
      title: 'Thanh lý dọn kho (Cắt máu)',
      discount: 70,
      velocityDays: 14,
      description: 'Áp dụng giá thanh lý giảm sâu trên các nền tảng thương mại điện tử để thu hồi dòng tiền ngay lập tức. Phù hợp cho chu kỳ xả hàng nhanh.',
    };
  }, [cluster]);

  if (!cluster || !recommendedStrategy) {
    return (
      <section className="bg-background px-6 md:px-16 lg:px-24 pb-32 pt-8">
        <div className="max-w-4xl mx-auto w-full">
          <div className="p-8 border border-[#262626] rounded-2xl bg-white/[0.02]">
            <p className="text-secondary font-mono text-sm">Chưa có dữ liệu Cluster để phân tích.</p>
          </div>
        </div>
      </section>
    );
  }

  // KHẮC PHỤC LỖI TOÁN HỌC & LOGIC TÀI CHÍNH (CFO ALIGNMENT)
  const isExceedingSafeDiscount = recommendedStrategy.discount > cluster.maxSafeDiscount;
  
  const expectedRevenue = cluster.totalInitialCost * (1 - recommendedStrategy.discount / 100);
  const netRecovery = expectedRevenue - cluster.totalSunkCost;

  const handleLaunch = () => {
    if (rpaState !== 'idle') return;
    setRpaState('running');
    setTimeout(() => setRpaState('done'), 2000);
  };

  return (
    <section className="bg-background px-6 md:px-16 lg:px-24 pb-32 pt-16">
      <div className="max-w-4xl mx-auto w-full flex flex-col gap-12">
        {/* Hedaer */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-4 opacity-60">
            Portfolio Command Center · Cluster View
          </div>
          <h2 className="font-tight font-black tracking-tightest text-primary text-4xl sm:text-5xl leading-tight mb-5">
            {cluster.clusterName}
          </h2>
          <div className="flex flex-wrap gap-8 text-xs font-mono text-secondary tabular-nums">
            <div className="flex flex-col gap-1">
              <span className="opacity-50 uppercase tracking-widest">Tổng vốn ban đầu</span>
              <span className="text-primary font-bold text-lg">{formatVND(cluster.totalInitialCost)}</span>
            </div>
            <div className="flex flex-col gap-1">
              <span className="opacity-50 uppercase tracking-widest">Chi phí chìm</span>
              <span className="text-orange-400 font-bold text-lg">{formatVND(cluster.totalSunkCost)}</span>
            </div>
            <div className="flex flex-col gap-1">
              <span className="opacity-50 uppercase tracking-widest">Phí lưu kho lũy kế</span>
              <span className="text-orange-400 font-bold text-lg">{formatVND(cluster.accumulatedStorageFee)}</span>
            </div>
            <div className="flex flex-col gap-1">
              <span className="opacity-50 uppercase tracking-widest">Số lượng SKU</span>
              <span className="text-primary font-bold text-lg">{cluster.items.length}</span>
            </div>
          </div>
        </motion.div>

        {/* CFO Alignment / Threshold Guard */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-4 opacity-60">
            Financial Assessment
          </div>
          <div className={`p-8 border rounded-2xl ${isExceedingSafeDiscount ? 'bg-red-950/10 border-red-900/30' : 'bg-white/[0.02] border-[#262626]'}`}>
            {isExceedingSafeDiscount ? (
              <div className="text-red-400 font-bold text-sm tracking-widest uppercase mb-3">
                CẢNH BÁO: VƯỢT NGƯỠNG AN TOÀN DÒNG TIỀN
              </div>
            ) : (
              <div className="text-accent font-bold text-sm tracking-widest uppercase mb-3">
                TRONG NGƯỠNG AN TOÀN TÀI CHÍNH
              </div>
            )}
            
            <div className="text-secondary text-sm leading-relaxed mb-8 max-w-3xl">
              {isExceedingSafeDiscount 
                ? `Chiến dịch thanh lý dọn kho này yêu cầu mức giảm giá ${recommendedStrategy.discount}%, vượt quá ngưỡng hòa vốn dòng tiền tối đa của bạn là ${cluster.maxSafeDiscount}%. Bạn đang chấp nhận lỗ ròng dòng tiền mặt để lấy lại không gian kho bãi.`
                : `Mức giảm giá đề xuất ${recommendedStrategy.discount}% nằm trong ngưỡng an toàn (tối đa ${cluster.maxSafeDiscount}%). Doanh nghiệp có thể thu hồi dòng tiền mà không bị lỗ ròng (Net Loss).`}
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-xs font-mono">
              <div>
                <div className="text-secondary mb-2 uppercase tracking-widest opacity-60">Ngưỡng hòa vốn</div>
                <div className="text-primary text-xl tabular-nums font-bold">{cluster.maxSafeDiscount}%</div>
              </div>
              <div>
                <div className="text-secondary mb-2 uppercase tracking-widest opacity-60">Giảm giá đề xuất</div>
                <div className={`text-xl tabular-nums font-bold ${isExceedingSafeDiscount ? 'text-red-400' : 'text-accent'}`}>{recommendedStrategy.discount}%</div>
              </div>
              <div>
                <div className="text-secondary mb-2 uppercase tracking-widest opacity-60">Dự kiến thu hồi</div>
                <div className="text-primary text-xl tabular-nums font-bold">{formatVND(expectedRevenue)}</div>
              </div>
              <div>
                <div className="text-secondary mb-2 uppercase tracking-widest opacity-60">Lãi / Lỗ ròng</div>
                <div className={`text-xl tabular-nums font-bold ${netRecovery < 0 ? 'text-red-400' : 'text-accent'}`}>
                  {netRecovery < 0 ? '-' : '+'}{formatVND(Math.abs(netRecovery))}
                </div>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Action Plan */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}>
          <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-4 opacity-60">
            Execution Strategy
          </div>
          <div className="p-8 border border-[#262626] rounded-2xl bg-[#0A0A0A]">
            <h3 className="font-tight font-black text-3xl text-primary mb-4">{recommendedStrategy.title}</h3>
            <p className="text-secondary text-sm leading-relaxed mb-8 max-w-2xl">{recommendedStrategy.description}</p>
            
            <div className="flex flex-col sm:flex-row justify-between sm:items-end gap-6 border-t border-[#1e1e1e] pt-6">
              <div>
                <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-2 opacity-60">Thời gian giải phóng kho</div>
                <div className="text-primary font-bold text-2xl tabular-nums">{recommendedStrategy.velocityDays} Ngày</div>
              </div>
              <button 
                onClick={handleLaunch}
                className={`px-10 py-4 text-sm font-bold tracking-tight rounded-xl transition-all font-mono uppercase ${rpaState === 'done' ? 'bg-accent/10 text-accent border border-accent/20' : 'bg-white text-black hover:bg-white/90'}`}
              >
                {rpaState === 'idle' ? 'Thực thi hàng loạt' : rpaState === 'running' ? 'Đang thiết lập...' : 'Đã lên lịch thành công'}
              </button>
            </div>
          </div>
        </motion.div>

        {/* TRIỆT TIÊU PLACEHOLDER: Render có điều kiện, data binding thực */}
        {cluster.bundleAvailable && (
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }}>
            <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-4 opacity-60">
              Smart Bundle (Cross-sell)
            </div>
            <div className="p-8 border border-[#262626] rounded-2xl bg-white/[0.02]">
              <div className="flex flex-col md:flex-row gap-8 items-center">
                <div className="flex-1 w-full text-center md:text-left">
                  <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-2 opacity-60">Sản phẩm mồi (Hot)</div>
                  <div className="text-primary font-bold text-lg leading-tight mb-2">{cluster.bundleAvailable.hotItemName}</div>
                  <div className="text-accent font-mono text-sm tabular-nums">Giá: {formatVND(cluster.bundleAvailable.hotItemPrice)}</div>
                </div>
                <div className="text-3xl font-light text-[#333]">+</div>
                <div className="flex-1 w-full text-center md:text-left">
                  <div className="text-[10px] font-mono text-secondary uppercase tracking-widest mb-2 opacity-60">Tồn kho cần xả</div>
                  <div className="text-primary font-bold text-lg leading-tight mb-2">{cluster.clusterName}</div>
                  <div className="text-orange-400 font-mono text-sm tabular-nums">Giảm {cluster.bundleAvailable.bundleDiscountPct}% khi mua kèm</div>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </div>
    </section>
  );
}
