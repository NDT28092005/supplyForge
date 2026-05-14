export function formatVnd(value: number | bigint): string {
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Number(value));
}
