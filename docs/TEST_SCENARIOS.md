# Hướng dẫn Kiểm thử Hệ thống SupplyForge AI

Tài liệu này cung cấp các kịch bản kiểm thử cụ thể kèm theo dữ liệu mẫu để bạn có thể thực hiện kiểm định hệ thống từ đầu đến cuối.

## 1. Dữ liệu mẫu (Test Data)
Các file mẫu đã được tạo tại thư mục `test-data/`:
- [inventory_standard.csv](file:///c:/NDT/ndt/Dự%20án/supplyforge-ai/test-data/inventory_standard.csv): Dữ liệu chuẩn.
- [inventory_with_duplicates.csv](file:///c:/NDT/ndt/Dự%20án/supplyforge-ai/test-data/inventory_with_duplicates.csv): Dữ liệu chứa SKU trùng lặp.
- [inventory_dead_stock.csv](file:///c:/NDT/ndt/Dự%20án/supplyforge-ai/test-data/inventory_dead_stock.csv): Dữ liệu tồn kho cũ (>45 ngày).
- [inventory_errors.csv](file:///c:/NDT/ndt/Dự%20án/supplyforge-ai/test-data/inventory_errors.csv): Dữ liệu lỗi để test độ ổn định.

---

## 2. Kịch bản Kiểm thử

### Kịch bản 1: Nhập dữ liệu & Kiểm tra Dashboard
**Mục tiêu:** Kiểm tra luồng import cơ bản và hiển thị dữ liệu.
1.  **Chuẩn bị:** Chạy Backend và Frontend.
2.  **Thực hiện:**
    - Truy cập Tab **"Nhập file"**.
    - Kéo thả file `inventory_standard.csv` vào vùng upload.
    - Quan sát Animation "Trust Theater" (Đọc file -> Mã hóa -> AI).
    - Sau khi hoàn tất, kiểm tra thông báo "Đã nhập 5 dòng thành công".
3.  **Kỳ vọng:**
    - API trả về 200 OK.
    - Bảng `data_sources` trong DB có bản ghi mới với status `READY`.
    - Dashboard hiển thị đúng tổng số SKU vừa nhập.

### Kịch bản 2: Xử lý SKU trùng lặp (Tinder UX)
**Mục tiêu:** Kiểm tra thuật toán phát hiện trùng lặp và tương tác UI.
1.  **Chuẩn bị:** Upload file `inventory_with_duplicates.csv`.
2.  **Thực hiện:**
    - Chuyển sang Tab **"Gộp SKU"**.
    - Hệ thống sẽ hiện thẻ gợi ý gộp (ví dụ: `Áo thun đỏ` và `Ao thun do`).
    - Thử nhấn **"Gộp chung"** cho cặp đầu tiên.
    - Thử nhấn **"Bỏ qua"** cho cặp thứ hai.
3.  **Kỳ vọng:**
    - Nút "Gộp chung" gọi API POST `/sku-merge/merge` thành công.
    - Trong DB, SKU con sẽ có `parent_sku_id` trỏ về SKU cha và `is_duplicate = true`.
    - Thẻ đã xử lý sẽ biến mất khỏi danh sách gợi ý.

### Kịch bản 3: Phân tích Tồn kho chết (Dead Stock)
**Mục tiêu:** Kiểm tra logic tính toán tài chính và thời gian tồn kho.
1.  **Chuẩn bị:** Upload file `inventory_dead_stock.csv`.
2.  **Thực hiện:**
    - Chuyển sang Tab **"Tồn chết"**.
    - Quan sát các chỉ số: "Tổng giá trị đóng băng", "Thiệt hại dự kiến 30 ngày".
3.  **Kỳ vọng:**
    - Các SKU từ năm 2023 và đầu 2024 phải xuất hiện trong danh sách "Top SKU đóng băng".
    - Giá trị đóng băng của `STALE01` (100 cái x 100k = 10tr) phải được tính đúng.
    - SKU `FRESH01` (mới nhập tháng 5/2026) **không được phép** nằm trong danh sách tồn chết.

### Kịch bản 4: Kiểm thử Độ ổn định (Robustness)
**Mục tiêu:** Kiểm tra khả năng tự phục hồi khi dữ liệu lỗi.
1.  **Thực hiện:** Upload file `inventory_errors.csv`.
2.  **Kỳ vọng:**
    - Hệ thống không bị treo (No Crash).
    - SKU có ngày tháng sai (`31/02/2024`) được tự động đưa về ngày hiện tại.
    - SKU thiếu giá vốn được tính bằng 70% giá bán (theo logic nghiệp vụ).
    - SKU thiếu tên hoặc mã có thể bị bỏ qua hoặc log lỗi nhưng không làm dừng tiến trình của các dòng khác.

### Kịch bản 5: Concurrency & Rate Limiting
**Mục tiêu:** Đảm bảo không thể upload 2 file cùng lúc cho 1 workspace.
1.  **Thực hiện:** 
    - Dùng 2 tab trình duyệt hoặc Postman, gửi đồng thời 2 request upload file lớn tới cùng 1 `workspaceId`.
2.  **Kỳ vọng:**
    - Request thứ nhất được xử lý (200 OK).
    - Request thứ hai phải nhận lỗi `409 Conflict` kèm message "Hệ thống đang xử lý một file khác...".

---

## 3. Cách đọc Log kiểm thử
- **Backend Log:** Theo dõi console chạy Spring Boot. Tìm các dòng `Batch insert successful`, `Levenshtein distance calculated`.
- **Frontend Log:** Mở F12 -> Console để xem các log về Animation và kết quả trả về từ API.
- **Database:** Chạy lệnh SQL: `SELECT * FROM data_sources ORDER BY created_at DESC;` để kiểm tra trạng thái thực của các phiên nhập dữ liệu.
