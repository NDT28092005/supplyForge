export const API_BASE =
  typeof process.env.NEXT_PUBLIC_API_URL === 'string'
    ? process.env.NEXT_PUBLIC_API_URL.replace(/\/$/, '')
    : 'http://localhost:8080';

export const WORKSPACE_ID = Number(process.env.NEXT_PUBLIC_WORKSPACE_ID ?? '1');
