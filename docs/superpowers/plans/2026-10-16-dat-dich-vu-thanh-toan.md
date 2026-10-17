# Luồng đặt dịch vụ có thanh toán + realtime quản trị – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Viết lại luồng đặt dịch vụ trên website thành các bước nối tiếp: chọn dịch vụ → chọn theo lần / gói tháng → chọn thời gian (kèm ràng buộc ngày) → đặt dịch vụ → chọn phương thức thanh toán → thanh toán → chờ người nhận (được nhận / phân công, hoặc đơn bị hủy); đồng thời mọi trang quản trị cập nhật theo thời gian thực và nhân viên CSKH được báo ngay khi có đơn mới.

**Architecture:** Trang `/dat-dich-vu/{danhMucId}` thành trình hướng dẫn 5 bước trên một trang (chuyển bước bằng JS, chỉ gửi máy chủ ở bước cuối). Đơn chuyển khoản chỉ được *phát hành* cho cộng tác viên sau khi thanh toán; "đã phát hành" suy ra từ `HoaDon` (không thêm trạng thái đơn). Xác nhận thanh toán đi qua một hàm duy nhất để sau này nối API VietQR. Kênh SSE quản trị hiện có (`RealtimeService` + `realtime.js` + `realtime-worker.js`) được mở rộng: ghi nhớ vai trò của từng kết nối và gửi được sự kiện có nội dung (JSON) cho riêng một vai trò.

**Tech Stack:** Spring Boot 3 (Java 17+), Spring Data JPA, Thymeleaf, MySQL `GiupViecDB`, Server-Sent Events (`SseEmitter`, SharedWorker), flatpickr 4.6.13, JUnit 5.

**Spec:** Yêu cầu của người dùng (2026-10-16), nguyên văn:
> chọn dịch vụ -> chọn theo lần/theo gói -> chọn thời gian thực hiện (nhớ các ràng buộc, thêm ràng buộc chỉ cho đặt theo lần trong vòng 1 tháng, đặt theo gói trong vòng 3 tháng) -> đặt dịch vụ -> chọn phương thức thanh toán -> thanh toán -> chờ người nhận dịch vụ (được phân công hoặc hủy luôn đơn)

Làm rõ sau đó (nguyên văn):
> 1. gói tháng là gói 1 tháng, không phải gói theo 3 tháng, ý tôi là chỉ có thể đặt dịch vụ bắt đầu trong vòng 3 tháng, nếu qua tháng thứ 4 mới bắt đầu thì không chấp nhận
> 2. tôi sẽ thêm API VIETQR sau
> 3. trong vòng 15p không ai nhận đơn thì cũng sẽ hủy
>
> Tất cả trang quản trị nên được realtime, khi có đơn mới thì nên thông báo luôn cho cả chăm sóc khách hàng

Cơ chế 15 phút nhận đơn đã có: `HanNhanDonService`, `TuHuyDonService`.

## Global Constraints

- Không thêm / xoá bảng, cột, không đổi ràng buộc CHECK (`ddl-auto=validate`). `DonDatDichVu.TrangThai` ∈ `ChoDuyet | DaXacNhan | DangThucHien | HoanThanh | DaHuy`; `HoaDon.HinhThucThanhToan` ∈ `TienMat | ChuyenKhoan`; `HoaDon.TrangThaiThanhToan` ∈ `ChuaThanhToan | DaThanhToan | HoanTien`.
- Đặt theo lần: ngày làm ≤ `hôm nay.plusMonths(1)`.
- Gói tháng (gói kéo dài 1 tháng): **ngày bắt đầu gói** ≤ `hôm nay.plusMonths(3)`; bắt đầu từ tháng thứ 4 trở đi thì từ chối.
- Giữ ràng buộc cũ: không chọn ngày quá khứ; giờ bắt đầu (buổi đầu) sau hiện tại ít nhất 1 giờ; gói tháng phải chọn thứ trong tuần đủ số buổi của gói trong 1 tháng kể từ ngày bắt đầu (`LichGoiThang.tinhLichDu`); địa chỉ thuộc khu vực phục vụ.
- Mọi đơn đã phát hành mà 15 phút không có ai nhận (CTV tự nhận hoặc nhân viên phân công) → tự hủy (`app.don.thoi-gian-nhan-phut=15`, tính từ lúc phát hành). Chuyển khoản: thêm hạn thanh toán `app.don.han-thanh-toan-phut=15` tính từ lúc tạo đơn; trả xong mới phát hành và mới bắt đầu 15 phút nhận đơn.
- Chưa có API VietQR: mã QR vẫn dùng đường dẫn ảnh `img.vietqr.io` sẵn có trong `getInvoiceInfo`; xác nhận thanh toán tạm thời bằng nút "Tôi đã chuyển khoản" và **chỉ đi qua** `CustomerApiService.xacNhanThanhToanChuyenKhoan(Integer donDatId, String nguon)` để sau này webhook VietQR gọi lại đúng hàm này.
- Đơn gửi qua API `/v1/bookings` không kèm phương thức → mặc định `TienMat` (ứng dụng di động chưa có bước thanh toán).
- Sự kiện realtime cho nhân viên chỉ mang dữ liệu tóm tắt (mã đơn, tên dịch vụ, ngày giờ, khu vực, phương thức, trạng thái thanh toán) — không gửi số điện thoại, địa chỉ chi tiết của khách.
- Chuỗi hiển thị tiếng Việt có dấu, file UTF-8.
- Thư mục dự án **không phải git repo**: bỏ các bước commit. **Hỏi người dùng trước khi chạy `mvnw` (compile / test) hoặc khởi động server 8083.**
- Lệnh test: `./mvnw -q test -Dtest=<TênLớpTest>` trong `WebsiteCungCapDichVuGiupViec/`.

## Quyết định do người viết plan tự chọn (người dùng cần xem lại)

1. Tiền mặt: đơn phát hành ngay khi đặt, khách trả cho CTV sau buổi làm.
2. Đơn đã chuyển khoản mà bị hủy: giữ `DaThanhToan`, báo khách "bTaskee sẽ liên hệ hoàn tiền"; không tự đổi sang `HoanTien`.
3. Địa chỉ làm việc đặt chung bước 3 (thời gian).
4. Người nhận thông báo đơn mới trên web quản trị: vai trò `ROLE_CSKH` (bộ phận đang phân công đơn trong hệ thống). Ba loại thông báo: đơn mới, đơn chuyển khoản vừa thanh toán, đơn tự hủy. Thông báo hiện dạng thẻ nổi góc màn hình kèm nút "Xem đơn"; không lưu vào CSDL, không phát âm thanh.
5. Trang khách hàng (website công khai) không cần realtime; "tất cả trang quản trị" = các trang dùng `admin-layout`, `cskh-layout`, `marketing-layout` (đều đã nạp `realtime.js`).

## Review Focus

1. Khách mở trang QR rồi bỏ đi → CTV không thấy / không nhận được đơn, CSKH không thể phân công, đơn tự hủy sau 15 phút (Task 2, Task 3).
2. Ứng dụng di động gửi đơn không có phương thức → coi là tiền mặt, phát hành ngay (Task 2: `chuanHoaHinhThuc(null) == "TienMat"`).
3. Ngày biên: đúng `+1 tháng` (theo lần) / `+3 tháng` (gói) được đặt, thêm 1 ngày bị từ chối; cuối tháng 31/01 (Task 1).
4. Khách bấm "Tôi đã chuyển khoản" khi đơn vừa bị hệ thống hủy, hoặc bấm hai lần → từ chối, không tạo biên lai thứ hai (Task 4).
5. Nhân viên mở nhiều tab / vai trò khác CSKH → thông báo đơn mới chỉ hiện ở tài khoản CSKH, mỗi tab một lần, không làm tab đang nhập dở bị tải lại (Task 7).

---

### Task 1: Ràng buộc ngày đặt

**Files:**
- Create: `src/main/java/com/example/Service/RangBuocNgayDat.java`
- Modify: `src/main/java/com/example/Controller/KhachHangWebController.java` (`xuLyDatDichVu`: thay khối "60 ngày" và "ít nhất 1 giờ")
- Modify: `src/main/java/com/example/Service/CustomerApiService.java` (`createBooking`: sau khi có ngày buổi đầu, thay các kiểm tra quá khứ / 1 giờ hiện có)
- Test: `src/test/java/com/example/Service/RangBuocNgayDatTest.java`

**Interfaces:**
- Produces:
  - `public static LocalDate ngayToiDa(String loaiHinhDat, LocalDate homNay)` — `"GoiThang"` → `homNay.plusMonths(3)`, còn lại → `homNay.plusMonths(1)`.
  - `public static String loiNgayDat(String loaiHinhDat, LocalDate ngay, LocalTime gio, LocalDateTime bayGio)` — `null` nếu hợp lệ; ngược lại (nguyên văn):
    - ngày hoặc giờ null → `"Vui lòng chọn ngày và giờ bắt đầu."`
    - ngày < hôm nay → `"Ngày thực hiện không được ở quá khứ."`
    - quá hạn, theo lần → `"Đặt theo lần chỉ được chọn ngày trong vòng 1 tháng tới (đến dd/MM/yyyy)."`
    - quá hạn, gói → `"Gói tháng chỉ được bắt đầu trong vòng 3 tháng tới (đến dd/MM/yyyy)."`
    - ngày+giờ < bayGio + 1 giờ → `"Giờ bắt đầu cần sau thời điểm hiện tại ít nhất 1 giờ."`

- [ ] **Step 1: Viết test thất bại** `RangBuocNgayDatTest`, `bayGio = 2026-01-31T08:00`:
  - `ngayToiDa("TheoLan", 2026-01-31) == 2026-02-28`; `ngayToiDa("GoiThang", 2026-01-31) == 2026-04-30`
  - `loiNgayDat("TheoLan", 2026-02-28, 09:00, bayGio) == null`; `loiNgayDat("TheoLan", 2026-03-01, 09:00, bayGio)` chứa `"trong vòng 1 tháng"`
  - `loiNgayDat("GoiThang", 2026-04-30, 09:00, bayGio) == null`; `loiNgayDat("GoiThang", 2026-05-01, 09:00, bayGio)` chứa `"3 tháng"`
  - `loiNgayDat("TheoLan", 2026-01-30, 09:00, bayGio)` chứa `"quá khứ"`; `loiNgayDat("TheoLan", 2026-01-31, 08:30, bayGio)` chứa `"ít nhất 1 giờ"`; `loiNgayDat("TheoLan", 2026-01-31, 09:00, bayGio) == null`
  - `loiNgayDat("TheoLan", null, 09:00, bayGio)` chứa `"chọn ngày và giờ"`
- [ ] **Step 2: Chạy** `./mvnw -q test -Dtest=RangBuocNgayDatTest` (hỏi trước) — FAIL vì chưa có lớp.
- [ ] **Step 3: Tạo `RangBuocNgayDat`** (`final`, constructor private), định dạng ngày `dd/MM/yyyy`.
- [ ] **Step 4: Gắn vào web và API.** `xuLyDatDichVu`: `loiNgayDat(goiThang ? "GoiThang" : "TheoLan", ngayThucHien, gioBatDau, LocalDateTime.now())`, có lỗi thì flash `error` và quay lại. `createBooking`: gọi với `priceResult.getLoaiHinhDat()` và ngày buổi đầu, có lỗi thì `throw new IllegalArgumentException(loi)`.
- [ ] **Step 5: Chạy lại test** — PASS.

### Task 2: Phương thức thanh toán khi đặt + quy tắc phát hành đơn

**Files:**
- Modify: `src/main/java/com/example/DTO/request/BookingCreateRequest.java` (thêm `private String hinhThucThanhToan; // TienMat | ChuyenKhoan, null = TienMat`)
- Modify: `src/main/java/com/example/Service/CustomerApiService.java` (`createBooking`: `HoaDon.hinhThucThanhToan(HanNhanDonService.chuanHoaHinhThuc(req.getHinhThucThanhToan()))`; map kết quả thêm `"hinhThucThanhToan"`)
- Modify: `src/main/java/com/example/Service/HanNhanDonService.java`
- Modify: `src/main/java/com/example/Service/CollaboratorApiService.java` (`getAvailableOrders`)
- Test: `src/test/java/com/example/Service/HanNhanDonServiceTest.java`

**Interfaces:**
- Consumes: `HoaDonRepository.findByDonDat_Id(Integer)` (đã có).
- Produces (trong `HanNhanDonService`, constructor nhận thêm `HoaDonRepository`):
  - `public static String chuanHoaHinhThuc(String hinhThuc)` — `"ChuyenKhoan"` (không phân biệt hoa thường) → `"ChuyenKhoan"`; null / trống / `"TienMat"` → `"TienMat"`; khác → `IllegalArgumentException("Phương thức thanh toán không hợp lệ.")`.
  - `public static LocalDateTime tinhLucPhatHanh(LocalDateTime lanChoDuyetGanNhat, LocalDateTime ngayTao, String hinhThuc, String trangThaiThanhToan, LocalDateTime ngayThanhToan)` — `ChuyenKhoan` chưa `DaThanhToan` → `null`; `ChuyenKhoan` đã trả → muộn hơn giữa `ngayThanhToan` và `lanChoDuyetGanNhat`; còn lại → `lanChoDuyetGanNhat`, null thì `ngayTao`.
  - `batDauPhatHanh(DonDatDichVu)` gọi `tinhLucPhatHanh` với dữ liệu hoá đơn; `hanNhan`, `daHetHan`, `hanNhanNeuDangCho` giữ chữ ký và trả `null` / `false` khi chưa phát hành.
  - `public boolean daPhatHanh(DonDatDichVu don)` — `batDauPhatHanh(don) != null`.
  - `kiemTraConHan(DonDatDichVu)`: chưa phát hành → `IllegalArgumentException("Đơn đang chờ khách thanh toán, chưa phát hành cho cộng tác viên.")` (đã được gọi ở: CTV nhận từ pool, CTV xác nhận lời mời, CSKH phân công).
- `getAvailableOrders`: `if (hanNhan == null || bayGio.isAfter(hanNhan)) continue;`.

- [ ] **Step 1: Viết test thất bại** `HanNhanDonServiceTest`, `tao = 2026-10-16T09:00`:
  - `chuanHoaHinhThuc(null) == "TienMat"`, `chuanHoaHinhThuc("chuyenkhoan") == "ChuyenKhoan"`, `chuanHoaHinhThuc("Momo")` ném `IllegalArgumentException`
  - `tinhLucPhatHanh(null, tao, "TienMat", "ChuaThanhToan", null) == tao`
  - `tinhLucPhatHanh(tao, tao, "ChuyenKhoan", "ChuaThanhToan", null) == null`
  - `tinhLucPhatHanh(tao, tao, "ChuyenKhoan", "DaThanhToan", tao.plusMinutes(6)) == tao.plusMinutes(6)`
  - `tinhLucPhatHanh(tao.plusHours(2), tao, "ChuyenKhoan", "DaThanhToan", tao.plusMinutes(6)) == tao.plusHours(2)`
  - `tinhLucPhatHanh(null, tao, null, null, null) == tao`
- [ ] **Step 2: Chạy** `./mvnw -q test -Dtest=HanNhanDonServiceTest` — FAIL.
- [ ] **Step 3: Cài đặt** các hàm trên, sửa `getAvailableOrders`.
- [ ] **Step 4: Chạy** `HanNhanDonServiceTest` và `TuHuyDonServiceTest` — PASS.

### Task 3: Tự hủy đơn chuyển khoản quá hạn thanh toán

**Files:**
- Modify: `src/main/java/com/example/Service/TuHuyDonService.java`
- Modify: `src/main/resources/application.properties` (thêm `app.don.han-thanh-toan-phut=15` cạnh `app.don.thoi-gian-nhan-phut`, chú thích tiếng Việt)
- Test: `src/test/java/com/example/Service/TuHuyDonServiceTest.java`

**Interfaces:**
- Consumes: `HoaDonRepository.findByDonDat_Id`.
- Produces: `public static String lyDoQuaHanThanhToan(String hinhThuc, String trangThaiThanhToan, LocalDateTime ngayTao, LocalDateTime luc, long soPhut)` — chỉ khi `ChuyenKhoan` + `ChuaThanhToan` + `luc > ngayTao + soPhut` → `"Quá " + soPhut + " phút chưa thanh toán chuyển khoản (hạn HH:mm dd/MM/yyyy)"`; còn lại `null`.
- Thứ tự trong `xuLyMotDon` (khi chưa có người nhận): quá hạn thanh toán → hết 15 phút nhận đơn (`lyDoHetHanNhan`, đã có) → quá giờ bắt đầu (`lyDoQuaHan`, đã có).

- [ ] **Step 1: Thêm test** `quaHanThanhToanThiHuy` với `tao = 2026-10-16T09:00`, `soPhut = 15`: `(ChuyenKhoan, ChuaThanhToan, tao, tao+15', 15) == null`; `(…, tao+15'+1s, 15)` khác null, chứa `"chưa thanh toán"`; `(ChuyenKhoan, DaThanhToan, tao, tao+1h, 15) == null`; `(TienMat, ChuaThanhToan, tao, tao+1h, 15) == null`.
- [ ] **Step 2: Chạy** `./mvnw -q test -Dtest=TuHuyDonServiceTest` — FAIL.
- [ ] **Step 3: Cài đặt**; `@Value("${app.don.han-thanh-toan-phut:15}")`.
- [ ] **Step 4: Chạy lại** — PASS.

### Task 4: Xác nhận thanh toán chuyển khoản và phát hành đơn

**Files:**
- Modify: `src/main/java/com/example/Service/CustomerApiService.java`
- Modify: `src/main/java/com/example/SuKien/SuKienDon.java` (thêm `public record DaPhatHanh(Integer donId) { }`)
- Modify: `src/main/java/com/example/Service/ThongBaoSuKienService.java`
- Modify: `src/main/java/com/example/Controller/KhachHangWebController.java`
- Create: `src/main/resources/templates/khach-hang/thanh-toan.html`

**Interfaces:**
- Consumes: `getInvoiceInfo(Integer donDatId)` (đã có: `maHoaDon`, `tongThanhToan`, `thongTinNganHang.{qrCodeUrl, nganHang, soTaiKhoan, chuTaiKhoan, noiDungChuyenKhoan}`), `HanNhanDonService.daPhatHanh`.
- Produces:
  - `public Map<String, Object> xacNhanThanhToanChuyenKhoan(Integer donDatId, String nguon)` trong `CustomerApiService` — điểm vào duy nhất để ghi nhận chuyển khoản (nút trên web hôm nay, webhook VietQR sau này). Khoá đơn (`findByIdForUpdate`); đơn `DaHuy` → `IllegalArgumentException("Đơn này đã bị hủy, không thanh toán được nữa.")`; gọi `createReceipt` với `hinhThucThanhToan="ChuyenKhoan"`, `nguoiThuTien = nguon`; đơn `ChoDuyet` thì `BoPhatSuKien.phat(new SuKienDon.DaPhatHanh(donDatId))`. Hoá đơn đã trả → lỗi "Hóa đơn này đã được thanh toán." sẵn có của `createReceipt`.
  - `ThongBaoSuKienService` nhận thêm `HanNhanDonService` (trường `@Autowired`); tách phần báo CTV trong `khiCoDonMoi` thành `private void baoCongTacVienKhuVuc(DonDatDichVu don)`. `khiCoDonMoi` chỉ gọi khi `daPhatHanh(don)`; chưa phát hành thì tin cho khách là tiêu đề `"Đơn " + ma + " đang chờ thanh toán"`, nội dung `"Vui lòng chuyển khoản trong 15 phút để bTaskee bắt đầu tìm cộng tác viên."`. Listener mới `khiDaPhatHanh(SuKienDon.DaPhatHanh e)` (cùng bộ annotation `@Async("taskExecutor") @TransactionalEventListener(fallbackExecution = true) @Transactional(propagation = REQUIRES_NEW)`) gửi khách `"Đã nhận thanh toán đơn " + ma` rồi gọi `baoCongTacVienKhuVuc`.
  - Web: `GET /tai-khoan/don-hang/{id}/thanh-toan` → `khach-hang/thanh-toan` (model `hoaDon` = `getInvoiceInfo`, `soGiayConLaiThanhToan`); chỉ cho đơn của khách, `ChuyenKhoan`, `ChuaThanhToan`, `ChoDuyet` — khác thì redirect `/tai-khoan/don-hang/{id}`. `POST /tai-khoan/don-hang/{id}/thanh-toan` → `xacNhanThanhToanChuyenKhoan(id, "Khách xác nhận trên website")`, flash `success` `"Đã ghi nhận thanh toán. bTaskee đang tìm cộng tác viên cho bạn."`, redirect chi tiết đơn; lỗi → flash `error`.
  - `thanh-toan.html` (layout `site-layout`, khung tài khoản như `don-chi-tiet.html`): số tiền, ảnh QR, ngân hàng, số tài khoản, chủ tài khoản, nội dung chuyển khoản (nút sao chép), đếm ngược hạn thanh toán (hết giờ: khoá nút và tải lại sau 65 giây), nút chính "Tôi đã chuyển khoản", nút phụ "Hủy đơn" (hộp xác nhận hủy sẵn có `/tai-khoan/don-hang/{id}/huy`).

- [ ] **Step 1: Thêm `xacNhanThanhToanChuyenKhoan`.**
- [ ] **Step 2: Thêm record `DaPhatHanh`, tách `baoCongTacVienKhuVuc`, thêm listener.**
- [ ] **Step 3: Thêm hai endpoint web + template.**
- [ ] **Step 4: Kiểm tra thủ công** (server do người dùng chạy): đặt đơn chuyển khoản → trang QR đúng số tiền; `/v1/collaborators/available-orders` chưa có đơn; CSKH bấm phân công → báo "chờ khách thanh toán"; bấm "Tôi đã chuyển khoản" → `HoaDon.TrangThaiThanhToan = DaThanhToan`, đúng 1 dòng `BienLai`, CTV thấy đơn, đồng hồ 15 phút bắt đầu; bấm lại ở tab cũ → "đã được thanh toán"; đơn khác để quá 15 phút không trả → `DaHuy`, lịch sử ghi "Quá 15 phút chưa thanh toán chuyển khoản"; mở QR của đơn vừa bị hủy rồi bấm xác nhận → "Đơn này đã bị hủy…".

### Task 5: Trang đặt dịch vụ thành 5 bước

**Files:**
- Modify: `src/main/resources/templates/khach-hang/dat-dich-vu.html`
- Modify: `src/main/resources/static/css/site.css` (khối `/* ---------- Đặt dịch vụ ---------- */`)
- Modify: `src/main/java/com/example/Controller/KhachHangWebController.java` (`datDichVu` GET, `xuLyDatDichVu` POST)

**Interfaces:**
- Consumes: `RangBuocNgayDat.ngayToiDa` (Task 1), `BookingCreateRequest.hinhThucThanhToan` (Task 2), trang thanh toán (Task 4).
- Produces:
  - GET thêm model `ngayToiDaTheoLan`, `ngayToiDaGoiThang` (ISO `yyyy-MM-dd`) → `data-max-theo-lan` / `data-max-goi` trên khung lịch.
  - POST thêm `@RequestParam(defaultValue = "TienMat") String hinhThucThanhToan` → `req.setHinhThucThanhToan(...)`; `ChuyenKhoan` → `redirect:/tai-khoan/don-hang/{id}/thanh-toan`; `TienMat` → `redirect:/tai-khoan/don-hang/{id}` với flash `"Đã đặt dịch vụ. bTaskee đang tìm cộng tác viên cho bạn."`.

Bố cục (giữ các phần đã có: tìm dịch vụ trong nhóm, lịch flatpickr tô buổi gói, chọn nhanh thứ, ghi chú, bảng khuyến mãi, nút quay lại, thanh dưới trên điện thoại):

| Bước | Nội dung | Điều kiện sang bước sau |
|---|---|---|
| 1. Dịch vụ | lưới `.opt` của nhóm | đã chọn một dịch vụ |
| 2. Hình thức | "Theo lần" / "Gói tháng" (nút `data-loai` hiện có); nhóm chỉ có một loại thì tự chọn và bỏ qua bước | đã chọn loại; lưới bước 1 lọc theo loại |
| 3. Thời gian & địa chỉ | địa chỉ, lịch, giờ, thứ trong tuần (gói), ghi chú; dòng nhắc "Đặt theo lần: trong vòng 1 tháng tới" / "Gói tháng: bắt đầu trong vòng 3 tháng tới" | có địa chỉ, ngày, giờ; gói đủ buổi (`lichDu`) |
| 4. Xác nhận | tóm tắt đơn + mã ưu đãi; nút chính **"Đặt dịch vụ"** | — |
| 5. Thanh toán | "Tiền mặt – trả cho cộng tác viên sau buổi làm" / "Chuyển khoản – quét mã VietQR, cần thanh toán trong 15 phút"; input ẩn `hinhThucThanhToan`; nút gửi đổi chữ "Xác nhận đặt" / "Thanh toán" | gửi form |

  - Thanh bước đầu trang: bước đã xong bấm được để quay lại; bước chưa tới bị khoá.
  - Đổi loại ở bước 2: `fpDat.set('maxDate', …)` theo `data-max-*`; ngày đang chọn vượt mức thì kéo về `maxDate` và toast `"Đã đổi ngày về dd/MM vì đặt theo lần chỉ trong 1 tháng."`.
  - Khung tóm tắt bên phải hiện ở mọi bước (màn rộng); bước 5 thêm dòng "Thanh toán: Tiền mặt / Chuyển khoản".
  - Nút "Tiếp tục" / "Quay lại" cuối mỗi bước; Enter trong ô mã ưu đãi không gửi form.
  - Giữ tên trường form: `dichVuId`, `loaiHinhDat`, `ngayThucHien`, `gioBatDau`, `ngayTrongTuan`, `diaChiId`, `codeKhuyenMai`, `yeuCauDacBiet`.

- [ ] **Step 1: Controller.**
- [ ] **Step 2: Template + JS** theo bảng trên.
- [ ] **Step 3: CSS** cho thanh bước và thẻ phương thức thanh toán; đo bằng JS ở 360px và 1100px: không phần tử nào trong `.book` có `scrollWidth > clientWidth` hoặc vượt mép cha.
- [ ] **Step 4: Kiểm tra trên trình duyệt**: đi hết 5 bước với một dịch vụ theo lần và một gói; lịch theo lần không chọn được sau `+1 tháng`, gói không chọn được sau `+3 tháng`; chọn ngày 2 tháng ở gói rồi đổi về theo lần → ngày bị kéo về và có toast; tiền mặt → chi tiết đơn có đồng hồ 15 phút; chuyển khoản → trang QR.

### Task 6: Trang chi tiết đơn – trạng thái chờ

**Files:**
- Modify: `src/main/java/com/example/Service/HanNhanDonService.java`
- Modify: `src/main/java/com/example/Controller/KhachHangWebController.java` (`chiTietDon`, `donHang`, `ganTrangThai`)
- Modify: `src/main/resources/templates/khach-hang/don-chi-tiet.html`

**Interfaces:**
- Produces: `public LocalDateTime hanThanhToanNeuDangCho(Integer donId)` — đơn `ChoDuyet` + `ChuyenKhoan` + `ChuaThanhToan` → `ngayTao + han-thanh-toan-phut`, còn lại `null`.
  - `chiTietDon` thêm vào `don`: `hinhThucThanhToan`, `trangThaiThanhToan`, `soGiayConLaiThanhToan`, `choThanhToan` (boolean).
  - `ganTrangThai`: `choThanhToan == true` → nhãn `"Chờ thanh toán"`, tone `"warning"`; `donHang` gắn `choThanhToan` cho từng đơn trong danh sách.
  - `don-chi-tiet.html`: banner chờ thanh toán (đếm ngược + nút "Thanh toán ngay" → trang QR); dòng "Thanh toán: Tiền mặt – trả sau buổi làm" hoặc "Chuyển khoản – Đã thanh toán / Chưa thanh toán"; banner đếm ngược 15 phút nhận đơn chỉ hiện khi đã phát hành; đơn `DaHuy` đã chuyển khoản → "bTaskee sẽ liên hệ hoàn tiền trong 1–3 ngày làm việc."; nút "Hủy đơn" giữ cho `ChoDuyet` / `DaXacNhan`.

- [ ] **Step 1: Service + controller.**
- [ ] **Step 2: Template.**
- [ ] **Step 3: Kiểm tra thủ công**: đơn tiền mặt mới → "Đang tìm cộng tác viên · còn mm:ss"; đơn chuyển khoản chưa trả → nhãn "Chờ thanh toán", banner thanh toán, không có banner tìm CTV; sau khi trả → banner tìm CTV; đơn bị hủy do không ai nhận → "Đã hủy" kèm lý do từ lịch sử.

### Task 7: Thông báo đơn mới tức thời cho CSKH

**Files:**
- Modify: `src/main/java/com/example/Service/RealtimeService.java`
- Modify: `src/main/java/com/example/Controller/RealtimeController.java` (truyền vai trò trong session vào `dangKy`)
- Modify: `src/main/java/com/example/Service/RealtimeSuKienDonService.java`
- Modify: `src/main/resources/static/js/realtime-worker.js`, `src/main/resources/static/js/realtime.js`
- Modify: `src/main/resources/templates/layout/cskh-layout.html` (huy hiệu số đơn chờ nhận cạnh mục "Đơn đặt dịch vụ")
- Modify: `src/main/java/com/example/Controller/CSKHController.java` (endpoint đếm)
- Test: `src/test/java/com/example/Service/RealtimeServiceTest.java`

**Interfaces:**
- Consumes: `SuKienDon.DonMoi`, `SuKienDon.DaPhatHanh` (Task 4), `HanNhanDonService.daPhatHanh`.
- Produces:
  - `RealtimeService.dangKy(String vaiTro)` thay `dangKy()`: lưu kết nối kèm vai trò (`record KetNoiWeb(String vaiTro, SseEmitter emitter)`); `phatSuKien(String)` giữ nguyên hành vi cho mọi kết nối.
  - `public void guiNhanVien(java.util.Set<String> vaiTro, String tenSuKien, String duLieuJson)` — chỉ gửi tới kết nối có vai trò thuộc tập.
  - `public static String jsonDon(String loai, DonDatDichVu don, String ghiChu)` — JSON `{"loai","donId","maDonDat","tenDichVu","ngayGio","khuVuc","hinhThucThanhToan","daThanhToan","ghiChu","url"}`; `url = "/cskh/don-dat-dich-vu?moDon=" + donId`; không có số điện thoại / địa chỉ chi tiết.
  - `RealtimeSuKienDonService`: sau `DonMoi` → `guiNhanVien(Set.of("ROLE_CSKH"), "don-moi", jsonDon("don-moi", don, daPhatHanh ? null : "Chờ khách chuyển khoản"))`; sau `DaPhatHanh` → sự kiện `"don-moi"` với `loai = "da-thanh-toan"`; sau `DoiTrangThai` có `moi == "DaHuy"` mà lịch sử hủy gần nhất có `nguoiThucHien == TuHuyDonService.NGUOI_THUC_HIEN` → `loai = "tu-huy"`, `ghiChu` = `ghiChu` của dòng lịch sử đó (không cần sửa `TuHuyDonService`). Nạp đơn bằng `findByIdWithDetails` trong giao dịch chỉ đọc.
  - `realtime-worker.js` chuyển tiếp `{loai: tenSuKien, duLieu: event.data}` (thay chuỗi trần) cho cả `du-lieu-thay-doi`, `san-sang`, `don-moi`; `realtime.js` nhận cả dạng cũ (chuỗi) và dạng mới.
  - `realtime.js`: `don-moi` → thẻ nổi góc phải dưới (xếp chồng tối đa 3, tự ẩn sau 12 giây, nút "Xem đơn" → `url`, nút đóng); tiêu đề theo `loai`: `"Đơn mới " + maDonDat`, `"Đơn " + maDonDat + " đã thanh toán"`, `"Đơn " + maDonDat + " đã tự hủy"`; nội dung `tenDichVu · ngayGio · khuVuc` (+ `ghiChu`). Không làm mới trang khi đang nhập (giữ quy tắc `dangBan()`); mỗi tab hiện một lần.
  - `GET /cskh/don-dat-dich-vu/so-cho-nhan` → `{"soChoNhan": n}` (đơn `ChoDuyet` đã phát hành, chưa có người nhận); `cskh-layout` hiện huy hiệu, `realtime.js` gọi lại endpoint này khi nhận `don-moi` hoặc `du-lieu-thay-doi` có `"don-dat-dich-vu"`. Trang `/cskh/don-dat-dich-vu` mở sẵn khung chi tiết khi có `?moDon=`.

- [ ] **Step 1: Viết test thất bại** `RealtimeServiceTest`: lớp con thử `SseEmitter` ghi lại các lần `send`; `dangKy` với `"ROLE_CSKH"` và `"ROLE_HCNS"` (dùng hàm đăng ký nhận emitter để thay được bằng emitter giả — `dangKy(String vaiTro, SseEmitter emitter)` package-private, `dangKy(String)` gọi lại nó); `guiNhanVien(Set.of("ROLE_CSKH"), "don-moi", "{}")` → emitter CSKH nhận 1 sự kiện `don-moi`, emitter HCNS 0; `phatSuKien("/x")` → cả hai nhận `du-lieu-thay-doi`; `jsonDon(...)` không chứa khoá `"soDienThoai"` và `"diaChi"`.
- [ ] **Step 2: Chạy** `./mvnw -q test -Dtest=RealtimeServiceTest` — FAIL.
- [ ] **Step 3: Cài đặt** phía máy chủ (RealtimeService, RealtimeController, RealtimeSuKienDonService, endpoint đếm).
- [ ] **Step 4: Chạy lại test** — PASS.
- [ ] **Step 5: Cài đặt phía trình duyệt** (worker, realtime.js, cskh-layout, `?moDon=`).
- [ ] **Step 6: Kiểm tra thủ công**: mở 2 tab CSKH và 1 tab HCNS; khách đặt đơn tiền mặt → cả 2 tab CSKH hiện "Đơn mới …", huy hiệu tăng, tab HCNS không hiện thẻ; đặt đơn chuyển khoản → "Đơn mới … · Chờ khách chuyển khoản", trả xong → "… đã thanh toán"; để đơn hết 15 phút → "… đã tự hủy" kèm lý do, huy hiệu giảm; đang mở hộp thoại phân công thì trang không tự tải lại.

### Task 8: Lấp chỗ hở realtime trên mọi trang quản trị

**Files:**
- Modify: `src/main/java/com/example/Validation/CacheInvalidationInterceptor.java`
- Modify: `src/main/java/com/example/Validation/WebMvcConfig.java` (thêm `"/dat-dich-vu/**", "/tai-khoan/**", "/dang-ky", "/xac-thuc-otp"` vào `cacheInvalidationInterceptor`; thêm `path.startsWith("/tai-khoan/thong-bao")` vào `khongDoiSoLieu` của `CacheInvalidationInterceptor` vì đánh dấu đã đọc không đổi số liệu quản trị)
- Modify: `src/main/java/com/example/Service/BangGiaService.java` (`lamMoi`: có bảng giá / giá hiện tại thay đổi thì `realtimeService.phatSuKien("/hcns/dich-vu-bang-gia")`)
- Modify: `src/main/java/com/example/Service/ThongBaoService.java` (sau khi ghi thông báo cho người dùng: `phatSuKien("/thong-bao")`)

**Interfaces:**
- Consumes: `RealtimeService.phatSuKien(String)` (đã có).
- Produces: không có hàm mới; bảo đảm mọi thao tác ghi do khách trên website, do tác vụ nền đổi giá, và thông báo sinh tự động đều làm các trang quản trị đang mở tự kiểm tra lại (`realtime.js` chỉ tải lại khi nội dung `main` thật sự khác).

- [ ] **Step 1: Sửa ba file trên** (`BangGiaService` chỉ phát khi có ít nhất một bản ghi được lưu trong lần chạy).
- [ ] **Step 2: Kiểm tra thủ công**: mở `/cskh/don-dat-dich-vu`, `/cskh/khach-hang`, `/hcns/dich-vu-bang-gia`, `/giam-doc/dashboard`, `/marketing/dashboard`; khách sửa địa chỉ / đổi thông tin trên web → trang CSKH khách hàng tự cập nhật; khách thanh toán trên web → danh sách đơn tự cập nhật; tới giờ một bảng giá hẹn trước có hiệu lực → trang bảng giá tự cập nhật trong vòng 1 phút; đăng nhập / tính giá không làm trang tải lại (vẫn nằm trong danh sách `khongDoiSoLieu`).
