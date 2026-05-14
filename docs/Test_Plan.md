# Kế hoạch Kiểm thử (Test Plan) - SupplyForge AI

Tài liệu này hướng dẫn chi tiết cách thức kiểm thử hệ thống SupplyForge AI, tập trung vào các rủi ro vận hành chuỗi cung ứng thực tế.

---

## 1. Hướng dẫn Chuẩn bị Kiểm thử

### 1.1. Cách tạo dữ liệu kiểm thử (Test Data Generation)
Bạn có thể sử dụng Microsoft Excel hoặc Google Sheets để tạo file `.xlsx` hoặc `.csv`.
- **File Chuẩn:** Cột `sku`, `quantity`, `cost_price`, `selling_price`, `date`.
- **File Rác:** Chứa các dòng trống, cột thừa, hoặc định dạng ngày tháng hỗn hợp.
- **Dữ liệu Tiếng Việt:** Đảm bảo lưu file CSV với encoding **UTF-8** để không lỗi font.

### 1.2. Cách thức Import dữ liệu
Sử dụng **Postman** hoặc **cURL** để gọi API:
- **Endpoint:** `POST /api/v1/workspaces/1/imports/spreadsheet`
- **Body:** `form-data` với key là `file` (chọn file từ máy tính).

### 1.3. Cách đọc Log hệ thống
- **Backend (Spring Boot):** Quan sát Console/Terminal nơi bạn chạy `java -jar` hoặc `mvn spring-boot:run`.
  - Tìm log có chứa `SAXHelper`, `XlsxFirstSheetEventReader` để xem quá trình parse file.
  - Tìm log `Hibernate: insert into ...` để xác nhận dữ liệu đã vào DB.
- **Frontend (Next.js):** Mở trình duyệt, nhấn `F12` -> Tab `Network` để xem payload gửi đi và `Console` để xem log animation.

---

## 2. Kịch bản Kiểm thử Chi tiết (Test Scenarios)

### TC_01: Kiểm thử File Parsing (Độ bền hệ thống)
*Mục tiêu: Đảm bảo hệ thống không "sập" khi nhận file không chuẩn.*
- **Dữ liệu đầu vào:**
  - File Excel 100,000 dòng (test hiệu năng stream).
  - File CSV sai định dạng ngày (ví dụ: `31-02-2024` hoặc `2024/13/01`).
  - Cột SKU chứa ký tự đặc biệt: `Áo thun @#!`, `Giày "Luxury"`.
- **Các bước thực hiện:**
  1. Upload file qua API `/imports/spreadsheet`.
  2. Kiểm tra trạng thái trong bảng `data_sources` (status phải là `READY` hoặc `FAILED` kèm message lỗi cụ thể).
- **Kỳ vọng:** Hệ thống không bị OOM (Out of Memory). Ngày tháng sai định dạng được fallback về ngày hiện tại (`LocalDate.now()`). Ký tự Tiếng Việt hiển thị đúng.

### TC_02: Kiểm thử Smart Routing (Tiết kiệm Token LLM)
*Mục tiêu: Đảm bảo chỉ gửi 5 dòng mẫu cho AI, không rò rỉ toàn bộ file (bảo mật & chi phí).*
- **Các bước thực hiện:**
  1. Upload file Excel lớn (ví dụ 1000 dòng).
  2. Đọc log hoặc Debug tại `SpreadsheetImportService.java` dòng 48.
- **Kỳ vọng:** Hàm `peekHeaderAndFiveSamples` phải ném ra `PeekAbortException` ngay sau dòng thứ 5. Payload gửi sang OpenAI chỉ bao gồm 5 records.

### TC_03: Kiểm thử SKU Normalization (Gôm nhóm SKU)
*Mục tiêu: Kiểm tra logic xử lý chuỗi Tiếng Việt và khoảng cách Levenshtein.*
- **Dữ liệu đầu vào:**
  - SKU 1: `"Áo Thun Đỏ "` (có dấu cách thừa).
  - SKU 2: `"ao thun do"` (không dấu, lowercase).
  - SKU 3: `"Áo thun xanh"` (khác biệt hoàn toàn).
- **Các bước thực hiện:**
  1. Gọi API `/sku-merge/candidates`.
- **Kỳ vọng:** 
  - SKU 1 và SKU 2 phải được hệ thống gợi ý gộp (vì sau khi chuẩn hóa chúng giống nhau). 
  - Khoảng cách Levenshtein giữa các cặp gợi ý không được vượt quá 2.

### TC_04: Kiểm thử Financial Calculation (Tính toán tài chính)
*Mục tiêu: Assert tính chính xác của Dead Stock khi dữ liệu thiếu hụt.*
- **Dữ liệu đầu vào:**
  - Record A: `quantity=10`, `cost_price=null`, `selling_price=100.000`.
  - Record B: `quantity=5`, `cost_price=50.000`, `selling_price=0`.
  - Record C: Ngày nhập > 90 ngày trước (chắc chắn là Dead Stock).
- **Các bước thực hiện:**
  1. Gọi API `/dashboard/dead-stock`.
- **Kỳ vọng:**
  - Record A: Giá trị tồn kho = `10 * (100.000 * 0.7) = 700.000` (Tự động lấy 70% giá bán làm giá vốn).
  - Tổng thiệt hại (Loss) = `Tổng giá trị Dead Stock * 4.5%`.

### TC_05: Kiểm thử Cách ly Dữ liệu (Cross-Tenant Data Isolation)
*Mục tiêu: Đảm bảo tử huyệt SaaS - không để rò rỉ dữ liệu giữa các Workspace.*
- **Dữ liệu đầu vào:**
  - Workspace_A (Quần áo) và Workspace_B (Mỹ phẩm).
- **Các bước thực hiện:**
  1. Upload dữ liệu Quần áo vào Workspace_A.
  2. Dùng Token/Session của Workspace_B để gọi API `/dashboard/dead-stock` của Workspace_A (truyền ID của A vào URL).
- **Kỳ vọng:** Hệ thống phải trả về `403 Forbidden` hoặc `404 Not Found`. Kết quả trả về tuyệt đối không chứa từ khóa "Quần áo".

### TC_06: Kiểm thử E2E UX "Trust Theater"
*Mục tiêu: Đảm bảo chiến lược "hack" tâm lý người dùng hoạt động đúng (Animation chạy trước API).*
- **Các bước thực hiện:**
  1. Mở DevTools (F12) -> Tab Network -> Chế độ Slow 3G.
  2. Kéo thả file Excel vào vùng Upload.
- **Kỳ vọng:**
  1. Các bước: "Đọc file" -> "Xóa dữ liệu nhạy cảm" -> "Gửi LLM" phải hiển thị tuần tự trên UI.
  2. Trong khi Animation đang chạy, không được có Network Request nào được bắn đi.
  3. Chỉ khi bước cuối cùng kết thúc, request `POST /imports/spreadsheet` mới được trigger.

### TC_07: Kiểm thử Concurrency & Rate Limiting (Chống Spam)
*Mục tiêu: Ngăn chặn kẹt luồng xử lý khi người dùng click liên tục.*
- **Các bước thực hiện:**
  1. Dùng cURL hoặc Postman Runner bắn 5 request upload file liên tục vào 1 Workspace ID trong 1 giây.
- **Kỳ vọng:**
  1. Request đầu tiên được chấp nhận (`PROCESSING`).
  2. 4 request sau bị từ chối với status `429 Too Many Requests` hoặc `409 Conflict`.

---

## 3. Danh sách Kiểm tra nhanh (Checklist)
- [ ] File `.xlsx` dung lượng > 10MB upload thành công?
- [ ] Sau khi gộp SKU, record cũ có bị đánh dấu `is_duplicate = true` không?
- [ ] Snapshot Dashboard có tự động hết hạn sau 1 giờ không?
- [ ] Upload file trắng (không có data) có trả về lỗi 400 không?
- [ ] Truy cập chéo Workspace ID có bị chặn (403/404) không?
- [ ] Upload 2 file cùng lúc có bị chặn (429/409) không?
