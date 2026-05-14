# Đặc tả Yêu cầu Hệ thống (SRS) - SupplyForge AI

## 1. Business Requirements (Yêu cầu Nghiệp vụ)
**Mục tiêu kinh doanh:**
- **Quản trị Tồn kho & Tài chính:** Tự động hóa quá trình phát hiện và đánh giá "tồn kho chết" (dead stock) - hàng hóa lưu kho quá hạn (trên 45 ngày), giúp doanh nghiệp tính toán chính xác giá trị vốn bị đọng và tỷ lệ khấu hao/tổn thất (4.5% mỗi 30 ngày).
- **Chuẩn hóa Dữ liệu (Data Normalization):** Giải quyết tình trạng rác dữ liệu tồn kho do nhập liệu sai lệch (ví dụ sai chính tả) thông qua việc tự động phát hiện và gộp các SKU trùng lặp (SKU Merge).
- **Tích hợp AI linh hoạt:** Sử dụng LLM để tự động nhận diện ý nghĩa các cột dữ liệu (Column Mapping) khi người dùng upload file Excel/CSV, giảm thiểu công sức map dữ liệu thủ công.

## 2. Stakeholder Requirements (Yêu cầu của các Bên liên quan)
- **User (Chủ sở hữu - Owner):** Người tạo ra Workspace, có toàn quyền quản trị dữ liệu và các thành viên.
- **Workspace Member (Thành viên):** Người được mời vào Workspace với các vai trò cụ thể (được định nghĩa qua `WorkspaceRole` như ADMIN, MEMBER).
- **Phân quyền Dữ liệu:** Mọi dữ liệu (DataSource, Sku, InventoryRecord, InsightAnomaly) đều được cách ly nghiêm ngặt theo `workspace_id`. Một user chỉ có thể truy cập dữ liệu trong workspace mà họ là thành viên.

## 3. Functional Requirements (Yêu cầu Chức năng)

### 3.1. FR1: Import Dữ liệu Tồn kho (Spreadsheet Import)
- **User Story:** *As a user, I want to upload an Excel/CSV file so that I can import my inventory data without manual column mapping.*
- **Acceptance Criteria:**
  - Hệ thống phải hỗ trợ định dạng `.csv` và `.xlsx`.
  - Phải sử dụng cơ chế đọc stream (SAX/Event Reader) thay vì load toàn bộ file vào RAM để chống tràn bộ nhớ với file lớn.
  - Trích xuất tự động 5 dòng đầu tiên (Data Peek) và gửi cho LLM để dự đoán Column Mapping.
  - Trạng thái file (`DataSourceStatus`) chuyển đổi rõ ràng từ `PENDING` -> `COMPLETED` hoặc `FAILED` nếu có lỗi.

### 3.2. FR2: Phát hiện SKU trùng lặp (SKU Candidates)
- **User Story:** *As a user, I want the system to suggest duplicate SKUs so that I can clean up my inventory list.*
- **Acceptance Criteria:**
  - Hệ thống tự động so sánh các SKU (đã chuẩn hóa tên) trong cùng Workspace.
  - Sử dụng thuật toán `Levenshtein distance`. Các SKU có khoảng cách <= 2 được coi là ứng viên trùng lặp.
  - Giới hạn quét tối đa 600 SKUs gốc và trả về tối đa 80 cặp trùng lặp để đảm bảo hiệu năng.

### 3.3. FR3: Gộp mã SKU (SKU Merge)
- **User Story:** *As a user, I want to merge a duplicate child SKU into a parent SKU so that their inventory records are consolidated.*
- **Acceptance Criteria:**
  - Khi gộp, `child_sku` trỏ `parent_sku_id` tới `parent_sku` và đánh cờ `is_duplicate = true`.
  - Hệ thống phải kiểm tra vòng lặp (Circular Dependency) ngăn chặn trường hợp A gộp vào B, rồi B lại gộp vào A. Giới hạn độ sâu tối đa là 64.
  - Validation: Không cho phép gộp 2 SKU thuộc 2 Workspace khác nhau.

### 3.4. FR4: Dashboard Tồn kho chết (Dead Stock Analytics)
- **User Story:** *As a user, I want to view my dead stock valuation so that I know exactly how much capital is tied up.*
- **Acceptance Criteria:**
  - "Tồn kho chết" được định nghĩa là hàng không có giao dịch mới (Record Date) trong vòng **45 ngày** qua.
  - Nếu `cost_price` (giá vốn) trống, hệ thống tự động ước tính giá vốn bằng **70% của `selling_price`** (giá bán).
  - Tự động tính toán thiệt hại lưu kho ước tính (Loss Rate) là **4.5%** trên tổng giá trị hàng chết.
  - Dashboard API hỗ trợ cơ chế Snapshot (lưu kết quả vào `insights_and_anomalies`), có hiệu lực (TTL) trong 1 giờ để giảm tải tính toán.

## 4. Non-Functional Requirements (Yêu cầu Phi chức năng)
- **Security:**
  - Toàn bộ API được thiết kế stateless (không lưu session). CSRF disabled.
  - Hiện tại (MVP) đang mở quyền truy cập (permit All cho `/api/**` và `/actuator/health`). Giai đoạn tiếp theo sẽ áp dụng xác thực JWT/OAuth2.
- **Performance / Scalability:**
  - Xử lý file lớn bằng Streaming (`XlsxFirstSheetEventReader`, `CsvPlainReader`).
  - Tối ưu hóa thuật toán gom nhóm SKU: chỉ giới hạn N=600 và C=80.
- **Data Retention & Caching:**
  - Các bản phân tích phức tạp (Dashboard Insight) được lưu trữ vào bảng Anomaly dưới dạng JSON (`payload`), có `valid_until` (thời hạn) là 1 giờ kể từ lúc tạo để đóng vai trò như Cache layer.

## 5. Data Dictionary (Từ điển Dữ liệu)

| Entity (Table) | Trạng thái / Cột chính | Kiểu dữ liệu | Ràng buộc / Khóa ngoại (Mối quan hệ) | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| **User** (`users`) | `email`<br>`password_hash`<br>`display_name` | String(320)<br>String<br>String(200) | `email`: Unique, Not Null | Thông tin định danh người dùng. |
| **Workspace** (`workspaces`) | `name`<br>`slug`<br>`owner_user_id` | String(200)<br>String(80)<br>Long | `slug`: Unique<br>`owner_user_id` -> User.id | Khu vực làm việc độc lập của công ty. |
| **WorkspaceMember** (`workspace_members`) | `workspace_id`<br>`user_id`<br>`role` | Long<br>Long<br>Enum | Unique(`workspace_id`, `user_id`) | Quản lý phân quyền (MEMBER, ADMIN...). |
| **Sku** (`skus`) | `workspace_id`<br>`original_name`<br>`normalized_name`<br>`is_duplicate`<br>`parent_sku_id` | Long<br>String(2000)<br>String(2000)<br>Boolean<br>Long | `workspace_id` -> Workspace.id<br>`parent_sku_id` -> Sku.id | Mặt hàng (Stock Keeping Unit). Hỗ trợ cấu trúc cây (Parent-Child) khi Merge. |
| **InventoryRecord** (`inventory_records`) | `sku_id`<br>`record_date`<br>`quantity`<br>`cost_price`<br>`selling_price`<br>`data_source_id` | Long<br>Date<br>Decimal(18,4)<br>Decimal(18,4)<br>Decimal(18,4)<br>Long | `sku_id` -> Sku.id<br>`data_source_id` -> DataSource.id | Lịch sử số lượng hàng tồn. |
| **DataSource** (`data_sources`) | `workspace_id`<br>`original_filename`<br>`status`<br>`column_mapping_json` | Long<br>String(512)<br>Enum<br>JSON | `workspace_id` -> Workspace.id | File dữ liệu người dùng upload. Lưu kết quả LLM map cột vào `column_mapping_json`. |
| **InsightAnomaly** (`insights_and_anomalies`) | `workspace_id`<br>`insight_type`<br>`severity`<br>`payload`<br>`valid_until` | Long<br>String(64)<br>Enum<br>JSON<br>Instant | `workspace_id` -> Workspace.id<br>`sku_id` -> Sku.id (Nullable) | Lưu trữ các snapshot phân tích hoặc cảnh báo rủi ro tồn kho. |
