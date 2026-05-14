<#
.SYNOPSIS
  Khởi tạo cấu trúc supplyforge-ai (đã được scaffold trong repo).
.DESCRIPTION
  Script đặt tại scripts/ để tái sử dụng CI/CD hoặc clone nhanh; không xóa thư mục có sẵn.
#>
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$dirs = @(
  "backend\src\main\java\com\supplyforge\ai",
  "backend\src\main\resources",
  "frontend\src\app",
  "frontend\public"
)
foreach ($d in $dirs) {
  New-Item -ItemType Directory -Force -Path (Join-Path $Root $d) | Out-Null
}

Write-Host "OK: structure ready under $Root"
