import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        background: '#0B0B0B',
        surface: '#111111',
        'surface-hover': '#1A1A1A',
        primary: '#F5F5F5',
        secondary: '#9CA3AF',
        accent: '#D1FA4A',
        'border-base': '#262626',
        danger: '#F87171',
        warn: '#FB923C',
      },
      fontFamily: {
        sans: ['var(--font-inter)', 'system-ui', 'sans-serif'],
        tight: ['var(--font-inter-tight)', 'var(--font-inter)', 'system-ui', 'sans-serif'],
      },
      letterSpacing: {
        tightest: '-0.04em',
      },
      animation: {
        'cursor-blink': 'cursor-blink 1s step-end infinite',
      },
      keyframes: {
        'cursor-blink': {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0' },
        },
      },
    },
  },
  plugins: [],
};

export default config;
