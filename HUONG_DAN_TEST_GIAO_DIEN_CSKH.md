# HƯỚNG DẪN KIỂM THỬ TOÀN BỘ GIAO DIỆN ROLE CSKH (CHĂM SÓC KHÁCH HÀNG)

Tài liệu này tổng hợp chi tiết vị trí file template, URL truy cập trên trình duyệt, Controller xử lý và hướng dẫn kiểm thử cho tất cả các màn hình thuộc vai trò **CSKH (Chăm sóc Khách hàng)**.

---

## 1. GIAO DIỆN TRONG ẢNH CHỤP BẠN GỬI LÀ FILE NÀO?

- **Tên màn hình:** Hồ sơ Khách hàng (Quản lý Khách hàng)
- **Vị trí file Template:** `src/main/resources/templates/cskh/khach-hang.html`
- **URL truy cập trên trình duyệt:** `http://localhost:8080/cskh/khach-hang`
- **Controller xử lý:** `com.example.Controller.CSKHController` (hàm `khachHang`, dòng 76-82)
- **Các thành phần giao diện đặc trưng trên ảnh:**
  - Tiêu đề: `Hồ sơ Khách hàng`
  - Mô tả: `Theo dõi lịch sử đơn hàng, địa chỉ, hạng thành viên và ghi chú chăm sóc`
  - Nút hành động: `+ Thêm khách hàng` (Mở modal `createCustomerModal`)
  - 4 Thẻ KPI:
    - 👥 **Tổng khách hàng:** Hiển thị số lượng khách hàng thực tế trong database
    - 👑 **Khách hàng thân thiết:** Khách hàng có nhiều đơn đặt dịch vụ
    - 📦 **Khách đã có đơn hàng:** Khách hàng đang có trạng thái hoạt động
    - ⭐ **Đánh giá trung bình:** Điểm đánh giá hài lòng trung bình (ví dụ: `5.0 ★`)
  - Sidebar đang active: **Khách hàng** -> **Quản lý Khách hàng**

---

## 2. THÔNG TIN ĐĂNG NHẬP ROLE CSKH ĐỂ TEST

- **Trang đăng nhập:** `http://localhost:8080/dang-nhap` hoặc `http://localhost:8080/admin/login`
- **Tên đăng nhập:** `dunght`
- **Mật khẩu:** `123456`
- **Họ tên hiển thị:** Hoàng Thị Dung
- **Vai trò:** `ROLE_CSKH` (Nhân viên CSKH - Phòng CSKH)
- **Trang chuyển hướng sau khi đăng nhập thành công:** `/cskh/dashboard`

---

## 3. DANH SÁCH TẤT CẢ 7 GIAO DIỆN CỦA ROLE CSKH

Tất cả các file giao diện của CSKH đều nằm trong thư mục:
📂 `src/main/resources/templates/cskh/`

| STT | Tên màn hình | File Template HTML | URL kiểm thử | Mô tả chức năng chính |
|:---:|:---|:---|:---|:---|
| **1** | **Dashboard CSKH** | `templates/cskh/dashboard.html` | `/cskh/dashboard` | Bàn làm việc tổng quan CSKH: Thống kê nhanh đơn chờ duyệt, đơn hoàn thành, khiếu nại chưa xử lý; Biểu đồ phân bổ trạng thái đơn; Đơn hàng cần xử lý gấp; Khiếu nại gần đây. |
| **2** | **Hồ sơ Khách hàng** *(Ảnh bạn hỏi)* | `templates/cskh/khach-hang.html` | `/cskh/khach-hang` | Danh sách khách hàng thực tế từ DB; Tìm kiếm/lọc khách hàng; Nút "Thêm khách hàng mới" (Modal); Drawer xem chi tiết lịch sử đặt dịch vụ, địa chỉ, chi tiêu và ghi chú CSKH; Nút gọi điện nhanh. |
| **3** | **Quản lý Đơn đặt dịch vụ** | `templates/cskh/don-dat-dich-vu.html` | `/cskh/don-dat-dich-vu` | Quản lý toàn bộ vòng đời đơn đặt: Chờ duyệt, Đã phân công, Đang thực hiện, Hoàn thành, Đã hủy; Modal tạo đơn mới; Xem chi tiết đơn đặt và thông tin CTV nhận việc. |
| **4** | **Quản lý Khiếu nại & Phản hồi** | `templates/cskh/khieu-nai.html` | `/cskh/khieu-nai` | Tiếp nhận và xử lý khiếu nại (Level 1 & Level 2) về tài sản hư hỏng, thái độ phục vụ, chất lượng công việc; Cập nhật phương án bồi thường/hỗ trợ; Nút "Leo thang lên Giám đốc" nếu ngoài thẩm quyền. |
| **5** | **Đánh giá Dịch vụ** | `templates/cskh/danh-gia.html` | `/cskh/danh-gia` | Theo dõi sao đánh giá (1-5 sao) và nhận xét của khách hàng đối với CTV sau khi hoàn thành dịch vụ; Lọc theo điểm số sao; Xem chi tiết đơn tương ứng. |
| **6** | **Lịch Phân công CTV** | `templates/cskh/lich-phan-cong.html` | `/cskh/lich-phan-cong` | Điều phối và xếp lịch làm việc cho Cộng tác viên; Kiểm tra trạng thái CTV (Sẵn sàng, Đang bận việc, Nghỉ); Ghép CTV phù hợp vào đơn đặt theo khu vực và thời gian. |
| **7** | **Thông báo CSKH** | `templates/cskh/thong-bao.html` | `/cskh/thong-bao` | Quản lý và gửi thông báo nhắc lịch, chương trình khuyến mãi, tin nhắn hỗ trợ trực tiếp đến Khách hàng hoặc Cộng tác viên; Lịch sử gửi thông báo. |

---

## 4. CÁC FILE LIÊN QUAN TRONG SOURCE CODE

- **Controller định tuyến:**
  `src/main/java/com/example/Controller/CSKHController.java`
- **Layout dùng chung (Header, Sidebar CSKH, Script):**
  `src/main/resources/templates/layout/admin-layout.html`
  - Sidebar CSKH nằm ở block `<th:block th:if="${role == 'cskh'}">` (từ dòng 106 đến 143).
- **Service lấy số liệu thống kê cho CSKH:**
  `src/main/java/com/example/Service/ThongKeService.java`
- **Phân quyền và bảo mật Interceptor:**
  `src/main/java/com/example/Validation/AdminAuthenticationInterceptor.java`
- **Khởi tạo dữ liệu mẫu CSKH (Đơn hàng, Khách hàng, Khiếu nại, Đánh giá):**
  `src/main/java/com/example/Config/DatabaseDataInitializer.java`
