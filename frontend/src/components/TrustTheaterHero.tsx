'use client';

import { useEffect, useRef, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

const TERMINAL_LINES = [
  { text: '> Reading inventory file...', color: 'text-secondary', delay: 0 },
  { text: '> Detecting duplicate SKU logic...', color: 'text-secondary', delay: 1400, suffix: ' [Done]', suffixColor: 'text-primary' },
  { text: '> Removing sensitive customer data...', color: 'text-secondary', delay: 2900, suffix: ' [Secured]', suffixColor: 'text-primary' },
  { text: '> Analyzing dead stock risk...', color: 'text-secondary', delay: 4200 },
  {
    text: '> Potential frozen capital found: 412,000,000 VND',
    color: 'text-accent',
    delay: 5600,
    large: true,
  },
];

function TerminalLine({
  line,
  globalStart,
}: {
  line: (typeof TERMINAL_LINES)[0];
  globalStart: number;
}) {
  const [visible, setVisible] = useState(false);
  const [typedLength, setTypedLength] = useState(0);
  const fullText = line.text;

  useEffect(() => {
    const showTimer = setTimeout(() => {
      setVisible(true);
      let i = 0;
      const interval = setInterval(() => {
        i++;
        setTypedLength(i);
        if (i >= fullText.length) clearInterval(interval);
      }, 22);
      return () => clearInterval(interval);
    }, globalStart + line.delay);

    return () => clearTimeout(showTimer);
  }, [fullText.length, globalStart, line.delay]);

  if (!visible) return null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 4 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
      className={`font-mono text-sm leading-relaxed ${line.large ? 'text-base font-semibold mt-3' : ''} ${line.color}`}
    >
      {fullText.slice(0, typedLength)}
      {typedLength < fullText.length && (
        <span className="animate-cursor-blink text-accent">▋</span>
      )}
      {typedLength >= fullText.length && line.suffix && (
        <span className={line.suffixColor}>{line.suffix}</span>
      )}
    </motion.div>
  );
}

interface TrustTheaterHeroProps {
  onUpload: (file?: File) => void;
}

export default function TrustTheaterHero({ onUpload }: TrustTheaterHeroProps) {
  const [animStart] = useState(() => Date.now());
  const fileRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      onUpload(e.target.files[0]);
    }
  };

  return (
    <section className="min-h-screen bg-background flex flex-col justify-center px-6 md:px-16 lg:px-24">
      {/* Nav */}
      <nav className="fixed top-0 left-0 right-0 z-50 flex items-center justify-between px-6 md:px-16 lg:px-24 py-5 border-b border-border-base bg-background/90 backdrop-blur-sm">
        <span className="text-primary font-tight font-semibold tracking-tightest text-sm">
          SupplyForge<span className="text-accent">.</span>
        </span>
        <span className="text-secondary text-xs">MVP · v0.1</span>
      </nav>

      {/* Hero grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 lg:gap-24 pt-24 pb-20 max-w-7xl mx-auto w-full">
        {/* Left — Copy */}
        <motion.div
          className="flex flex-col justify-center"
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
        >
          {/* Eyebrow */}
          <div className="mb-6">
            <span className="text-xs font-mono text-secondary border border-border-base rounded px-2.5 py-1 tracking-widest uppercase">
              AI Inventory Intelligence
            </span>
          </div>

          {/* Headline */}
          <h1 className="font-tight font-black tracking-tightest text-primary leading-[1.05] text-4xl sm:text-5xl xl:text-6xl mb-6">
            Upload file kho.
            <br />
            <span className="text-accent">AI sẽ tìm ra</span>
            <br />
            tiền đang bị chôn.
          </h1>

          {/* Subhead */}
          <p className="text-secondary text-base sm:text-lg leading-relaxed mb-10 max-w-md">
            Phát hiện tồn kho chết, SKU lỗi và thất thoát dòng tiền trong
            dưới{' '}
            <span className="text-primary font-medium">60 giây</span>.
            Không cần cài phần mềm, không cần nhập tay.
          </p>

          {/* CTA */}
          <div className="flex flex-col sm:flex-row items-start gap-4">
            <input
              ref={fileRef}
              type="file"
              accept=".xlsx,.xls,.csv"
              className="hidden"
              onChange={handleFileChange}
            />
            <motion.button
              onClick={() => fileRef.current?.click()}
              className="bg-accent text-background font-tight font-bold text-sm px-7 py-3.5 rounded-sm tracking-tight transition-all"
              whileHover={{ scale: 1.015, backgroundColor: '#deff5a' }}
              whileTap={{ scale: 0.985 }}
              transition={{ type: 'spring', stiffness: 400, damping: 25 }}
            >
              [ Upload File Excel ]
            </motion.button>
            <button
              onClick={onUpload}
              className="text-secondary text-sm underline underline-offset-4 hover:text-primary transition-colors"
            >
              hoặc dùng dữ liệu demo
            </button>
          </div>

          {/* Trust signals */}
          <div className="mt-10 pt-8 border-t border-border-base flex flex-wrap gap-8">
            {[
              { value: '< 60s', label: 'Phân tích xong' },
              { value: '0 VND', label: 'Chi phí ban đầu' },
              { value: '100%', label: 'Dữ liệu không rời máy' },
            ].map((item) => (
              <div key={item.label}>
                <div className="text-xl font-tight font-black text-primary tracking-tightest">
                  {item.value}
                </div>
                <div className="text-xs text-secondary mt-0.5">{item.label}</div>
              </div>
            ))}
          </div>
        </motion.div>

        {/* Right — Terminal */}
        <motion.div
          className="flex items-center justify-center"
          initial={{ opacity: 0, y: 32 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.2, ease: [0.16, 1, 0.3, 1] }}
        >
          <div className="w-full max-w-lg border border-border-base rounded-sm bg-[#000000] overflow-hidden shadow-2xl">
            {/* Terminal header bar */}
            <div className="flex items-center gap-2 px-4 py-3 border-b border-border-base">
              <div className="w-2.5 h-2.5 rounded-full bg-[#FF5F56]" />
              <div className="w-2.5 h-2.5 rounded-full bg-[#FFBD2E]" />
              <div className="w-2.5 h-2.5 rounded-full bg-[#27C93F]" />
              <span className="ml-3 text-xs text-secondary font-mono">
                supplyforge-ai · analysis engine
              </span>
            </div>

            {/* Terminal body */}
            <div className="p-5 min-h-[220px] space-y-2">
              <div className="text-secondary text-xs font-mono mb-4 opacity-50">
                # inventory_report_q1_2024.xlsx — 48,321 rows
              </div>
              {TERMINAL_LINES.map((line, i) => (
                <TerminalLine key={i} line={line} globalStart={animStart} />
              ))}
            </div>

            {/* Terminal footer */}
            <div className="px-5 pb-4 pt-1 border-t border-border-base">
              <p className="text-xs text-secondary font-mono opacity-40">
                — Phân tích hoàn tất. Dữ liệu không được lưu trữ trên server. —
              </p>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
