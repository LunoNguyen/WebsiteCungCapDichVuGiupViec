# Đặt nhiều dịch vụ từ giỏ hàng – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khách đặt 1–5 dịch vụ theo lần từ giỏ hàng trong một đơn (một lịch, một hóa đơn); mỗi dịch vụ do một CTV nhận (hoặc một CTV nhận hết).

**Architecture:** Mở rộng đúng các service đang có (`CustomerApiService.calculatePrice/createBooking`, `CollaboratorApiService.getAvailableOrders/acceptOrderFromPool/completeAssignment`, `PhanCongService`, `TuHuyDonService`, `QuanLyDonService`) để xử lý N dòng `ChiTietDonDat`; đơn một dịch vụ là trường hợp N = 1 và giữ nguyên hành vi. Quy tắc mới nằm trong hai lớp thuần (không chạm CSDL) để unit test: `DatNhieuDichVu` (giỏ, khung giờ, giá) và `PhanCongTheoDichVu` (ai nhận dịch vụ nào, đủ người). Một cột mới `PhanCongCTV.chiTietDonDatId`.

**Tech Stack:** Spring Boot 4.1 / Java 26 / JPA / Thymeleaf / JUnit 6 + Mockito (web, repo `WebsiteCungCapDichVuGiupViec`, nhánh `feature/Luong`); Flutter (app, repo `app_cung_cap_dich_vu_giup_viec`, nhánh `feature/Luong`).

**Spec:** `docs/superpowers/specs/2026-10-26-dat-nhieu-dich-vu-design.md`

## Global Constraints

- Chỉ đặt chung dịch vụ `TheoLan`; 1–5 dịch vụ; tổng thời lượng ≤ 480 phút; giờ kết thúc ≤ 20:00; ngày ≤ hôm nay + 1 tháng; giờ bắt đầu ≥ hiện tại + 1 giờ; khung giờ bắt đầu 07:00–17:00.
- Thay đổi CSDL duy nhất: `PhanCongCTV.chiTietDonDatId INT NULL` FK → `ChiTietDonDat(id)`. Không thêm bảng/cột nào khác.
- `chiTietDonDatId = NULL` nghĩa là phân công cả đơn (đơn một dịch vụ, dữ liệu cũ) — mọi đường đi hiện có phải giữ nguyên với giá trị này.
- Phân công "hiệu lực" = trạng thái **khác** `TuChoi`, `DaHuy`.
- Dịch vụ không có `ThoiGianThucHien` tính là 120 phút (như `createBooking` hiện tại).
- Ngày hiển thị `dd/MM/yyyy`; mọi câu chữ giao diện tiếng Việt.
- **Không** chạy `@SpringBootTest` (ghi vào CSDL thật). Chạy test bằng `./mvnw -q test -Dtest=<Lớp>`; lệnh này biên dịch lại và làm server 8083 (devtools) khởi động lại — **hỏi người dùng trước lần chạy đầu tiên** của phiên.
- Không chạy migration SQL lên CSDL khi chưa được người dùng cho phép (Task 4 có điểm dừng).
- `flutter run` cần người dùng đồng ý; `flutter test` / `flutter analyze` chạy tự do.
- Commit: chỉ các file của task; kết thúc message bằng `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. Đơn **một dịch vụ** (web, app khách, app CTV, CSKH) phải chạy y như cũ — nhận cả đơn, sang `DaXacNhan` ngay, tự hủy khi chưa ai nhận.
2. Hai CTV nhận **cùng một dịch vụ** gần như đồng thời — người sau phải bị từ chối, không có hai phân công hiệu lực trên một dòng.
3. CTV **từ chối / bị bỏ gán** một dịch vụ khi đơn đã `DaXacNhan` — đơn quay về `ChoDuyet` và dịch vụ đó lại hiện trong danh sách chờ.
4. Mã khuyến mãi trên đơn nhiều dịch vụ — điều kiện tối thiểu xét trên **tổng**, lượt dùng chỉ trừ **một** lần.
5. Giỏ hàng cũ trong `localStorage` (dịch vụ đã ẩn, đổi giá, thiếu trường `loaiHinhDat`) — trang đặt nhiều báo lỗi và quay về giỏ, không tạo đơn sai.

---

## File Structure

| File | Trách nhiệm |
|---|---|
| `src/main/java/com/example/Service/DatNhieuDichVu.java` (mới) | Quy tắc thuần: kiểm tra danh sách dịch vụ, tổng phút, giờ kết thúc, cộng giá + khuyến mãi |
| `src/main/java/com/example/Service/PhanCongTheoDichVu.java` (mới) | Quy tắc thuần: dòng nào còn trống, đủ người, lỗi khi CTV nhận dòng |
| `database/ThemChiTietDonDatVaoPhanCong.sql` (mới) | Migration cột mới |
| `Model/PhanCongCTV.java` | Thêm `chiTietDonDat` |
| `Service/CustomerApiService.java` | `calculatePrice`, `createBooking` nhận N dòng |
| `DTO/response/PriceCalculationResult.java` | Thêm `chiTietGia` |
| `Service/CollaboratorApiService.java`, `API/CollaboratorProfileController.java` | Danh sách chờ + nhận theo dòng; hoàn thành đơn khi mọi phân công xong |
| `Service/TuHuyDonService.java`, `Service/QuanLyDonService.java` | Hết hạn nhận / chuyển trạng thái dựa trên "đủ người" |
| `Service/PhanCongService.java`, `templates/cskh/don-dat-dich-vu.html` | CSKH gán theo dòng |
| `Controller/KhachHangWebController.java`, `templates/khach-hang/dat-dich-vu.html`, `templates/site/gio-hang.html`, `static/js/site.js` | Luồng web |
| App: `lib/services/collaborator_api_service.dart`, `lib/screens/collaborator/collaborator_orders_screen.dart`, `lib/services/dich_vu_don.dart` (mới) | Nhận đơn theo dịch vụ |
| App: `lib/screens/customer/order_detail_screen.dart`, `booking_history_screen.dart`, `thanh_toan_screen.dart` | Hiện mọi dịch vụ của đơn |

---

### Task 1: Quy tắc giỏ hàng và khung giờ (`DatNhieuDichVu`)

**Files:**
- Create: `src/main/java/com/example/Service/DatNhieuDichVu.java`
- Test: `src/test/java/com/example/Service/DatNhieuDichVuTest.java`

**Interfaces:**
- Produces:
  - `public static final int SO_DICH_VU_TOI_DA = 5; TONG_PHUT_TOI_DA = 480; PHUT_MAC_DINH = 120; LocalTime GIO_KET_THUC_MUON_NHAT = LocalTime.of(20, 0);`
  - `public static int phut(DichVu dv)` — `ThoiGianThucHien` hoặc 120.
  - `public static int tongPhut(List<DichVu> ds)`
  - `public static String loiDanhSach(List<DichVu> ds)` — null nếu hợp lệ.
  - `public static LocalTime gioKetThuc(LocalTime batDau, int tongPhut)`
  - `public static String loiGioKetThuc(LocalTime batDau, int tongPhut)` — null nếu kết thúc ≤ 20:00.

- [ ] **Step 1: Write the failing test** — `DatNhieuDichVuTest` (dựng `DichVu` bằng builder, `loaiHinhDat`, `trangThai`, `thoiGianThucHien`):

```java
@Test void danhSachRongBaoLoi() { assertEquals("Vui lòng chọn ít nhất một dịch vụ.", DatNhieuDichVu.loiDanhSach(List.of())); }
@Test void quaNamDichVuBaoLoi() { assertEquals("Mỗi lần đặt tối đa 5 dịch vụ.", DatNhieuDichVu.loiDanhSach(nCai(6, 60))); }
@Test void goiThangKhongDatChung() { assertEquals("Dịch vụ \"Gói A\" là gói tháng, vui lòng đặt riêng.", DatNhieuDichVu.loiDanhSach(List.of(dv("Gói A","GoiThang","HoatDong",60), dv("B","TheoLan","HoatDong",60)))); }
@Test void dichVuDaAnBaoLoi() { assertEquals("Dịch vụ \"B\" hiện không còn nhận đặt.", DatNhieuDichVu.loiDanhSach(List.of(dv("A","TheoLan","HoatDong",60), dv("B","TheoLan","An",60)))); }
@Test void tongQuaTamGioBaoLoi() { assertEquals("Tổng thời lượng tối đa 8 giờ (đang chọn 8 giờ 30 phút).", DatNhieuDichVu.loiDanhSach(List.of(dv("A","TheoLan","HoatDong",240), dv("B","TheoLan","HoatDong",270)))); }
@Test void hopLe() { assertNull(DatNhieuDichVu.loiDanhSach(List.of(dv("A","TheoLan","HoatDong",120), dv("B","TheoLan","HoatDong",null)))); }
@Test void thieuThoiGianTinh120() { assertEquals(240, DatNhieuDichVu.tongPhut(List.of(dv("A","TheoLan","HoatDong",120), dv("B","TheoLan","HoatDong",null)))); }
@Test void gioKetThucCongDon() { assertEquals(LocalTime.of(10, 45), DatNhieuDichVu.gioKetThuc(LocalTime.of(8, 0), 165)); }
@Test void ketThucDung20GioHopLe() { assertNull(DatNhieuDichVu.loiGioKetThuc(LocalTime.of(17, 0), 180)); }
@Test void ketThucSau20GioBaoLoi() { assertEquals("Các dịch vụ phải xong trước 20:00 (dự kiến xong lúc 20:30). Vui lòng chọn giờ sớm hơn.", DatNhieuDichVu.loiGioKetThuc(LocalTime.of(17, 0), 210)); }
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./mvnw -q test -Dtest=DatNhieuDichVuTest`
Expected: FAIL — `cannot find symbol: class DatNhieuDichVu`

- [ ] **Step 3: Implement `DatNhieuDichVu`** — `final` class, constructor private; kiểm tra theo thứ tự rỗng → >5 → GoiThang → không `HoatDong` → tổng phút. Thời lượng trong câu lỗi viết "X giờ" hoặc "X giờ Y phút".

- [ ] **Step 4: Run test to verify it passes** — Run: `./mvnw -q test -Dtest=DatNhieuDichVuTest` — Expected: `Tests run: 10, Failures: 0`

- [ ] **Step 5: Commit**
```bash
git add src/main/java/com/example/Service/DatNhieuDichVu.java src/test/java/com/example/Service/DatNhieuDichVuTest.java
git commit -m "feat(booking): quy tac dat nhieu dich vu (gio hang, khung gio)"
```

---

### Task 2: Giá N dịch vụ + khuyến mãi trên tổng

**Files:**
- Modify: `src/main/java/com/example/Service/DatNhieuDichVu.java`
- Modify: `src/main/java/com/example/Service/CustomerApiService.java:1097-1185` (`calculatePrice`, `tinhSoTienGiam`)
- Modify: `src/main/java/com/example/DTO/response/PriceCalculationResult.java`
- Test: `src/test/java/com/example/Service/DatNhieuDichVuTest.java`

**Interfaces:**
- Consumes: Task 1.
- Produces:
  - `public record DongGia(Integer dichVuId, String tenDichVu, BigDecimal donGia) {}` (lồng trong `DatNhieuDichVu`)
  - `public static BigDecimal tongGoc(List<DongGia> dong)`
  - `public static BigDecimal soTienGiam(ChuongTrinhKhuyenMai ct, BigDecimal tong)` — chuyển thân `CustomerApiService.tinhSoTienGiam` sang đây (CustomerApiService gọi lại), kết quả `min(tong)`.
  - `PriceCalculationResult.chiTietGia : List<DatNhieuDichVu.DongGia>` (luôn có ≥ 1 phần tử).
  - `calculatePrice(req)`: khi `req.getChiTiet()` có ≥ 2 dòng thì giá gốc = `tongGoc` của từng dòng (mỗi dòng tính giá bằng đúng logic hiện có cho một dịch vụ — tách thành `private BigDecimal giaGocMotDichVu(DichVu dv, CalculatePriceRequest req)`); `dichVuId`/`tenDichVu` của kết quả là dòng đầu; `loaiHinhDat = "TheoLan"`. Mã khuyến mãi xét `dieuKienToiThieu` trên tổng.

- [ ] **Step 1: Write the failing test**
```java
@Test void congGiaCacDong() { assertEquals(new BigDecimal("411000"), DatNhieuDichVu.tongGoc(List.of(new DatNhieuDichVu.DongGia(1,"A",new BigDecimal("216000")), new DatNhieuDichVu.DongGia(2,"B",new BigDecimal("195000"))))); }
@Test void giamPhanTramCoTran() { assertEquals(new BigDecimal("50000"), DatNhieuDichVu.soTienGiam(ct("PhanTram","20","50000"), new BigDecimal("411000"))); }
@Test void giamTienKhongQuaTong() { assertEquals(new BigDecimal("100000"), DatNhieuDichVu.soTienGiam(ct("SoTienCoDinh","150000",null), new BigDecimal("100000"))); }
```

- [ ] **Step 2: Run** `./mvnw -q test -Dtest=DatNhieuDichVuTest` — Expected: FAIL (`DongGia` chưa có)
- [ ] **Step 3: Implement** như Interfaces; `getUsablePromotions` dùng `DatNhieuDichVu.soTienGiam` và nhận `chiTiet` giống `calculatePrice` (khách xem bảng khuyến mãi theo tổng).
- [ ] **Step 4: Run** `./mvnw -q test -Dtest=DatNhieuDichVuTest` — Expected: `Tests run: 13, Failures: 0`
- [ ] **Step 5: Commit** `feat(booking): tinh gia va khuyen mai tren tong nhieu dich vu`

---

### Task 3: Tạo đơn N dịch vụ (`createBooking`)

**Files:**
- Modify: `src/main/java/com/example/Service/CustomerApiService.java:1310-1530`
- Test: `src/test/java/com/example/Service/DatNhieuDichVuTest.java`

**Interfaces:**
- Consumes: Task 1, Task 2.
- Produces: `createBooking(req)` chấp nhận `req.getDanhSachDichVu()` 1–5 dòng. Khi ≥ 2 dòng:
  - nạp `DichVu` từng dòng; `DatNhieuDichVu.loiDanhSach` ≠ null ⇒ `IllegalArgumentException(loi)`;
  - ép `loaiHinhDat = "TheoLan"`; `gioKetThuc = DatNhieuDichVu.gioKetThuc(gioBatDau, tongPhut)` (bỏ qua `req.gioKetThuc`); `loiGioKetThuc` ≠ null ⇒ lỗi; `RangBuocNgayDat.loiNgayDat` như cũ;
  - một `ChiTietDonDat` mỗi dòng với `donGia = thanhTien = DongGia.donGia` của dòng đó và `bangGia` của dịch vụ đó;
  - `chiPhiGoc/soTienGiam/thanhTien`, `HoaDon`, `LichSuSuDungKhuyenMai` (một lần) như hiện tại từ kết quả `calculatePrice`.
  - Đơn 1 dòng: đường đi hiện tại không đổi.
- Produces: `public static String tomTatDichVu(List<String> ten)` trong `DatNhieuDichVu` — "A" / "A + B" / "A + 2 dịch vụ khác" (dùng cho thông báo CSKH, thông báo khách, `RealtimeService.jsonDon`).

- [ ] **Step 1: Write the failing test**
```java
@Test void tomTatMotDichVu() { assertEquals("Dọn nhà", DatNhieuDichVu.tomTatDichVu(List.of("Dọn nhà"))); }
@Test void tomTatHaiDichVu() { assertEquals("Dọn nhà + Giặt rèm", DatNhieuDichVu.tomTatDichVu(List.of("Dọn nhà", "Giặt rèm"))); }
@Test void tomTatNhieuDichVu() { assertEquals("Dọn nhà + 2 dịch vụ khác", DatNhieuDichVu.tomTatDichVu(List.of("Dọn nhà", "Giặt rèm", "Máy lạnh"))); }
```
- [ ] **Step 2: Run** `./mvnw -q test -Dtest=DatNhieuDichVuTest` — Expected: FAIL
- [ ] **Step 3: Implement** `tomTatDichVu` và nhánh N dòng của `createBooking`; các chỗ đang lấy `don.getChiTietList().get(0).getDichVu().getTenDichVu()` để làm tiêu đề thông báo (`CollaboratorApiService`, `RealtimeService.jsonDon`) chuyển sang `tomTatDichVu`.
- [ ] **Step 4: Run** `./mvnw -q test -Dtest=DatNhieuDichVuTest,ThongBaoDonCskhTest,RealtimeServiceTest` — Expected: tất cả đạt
- [ ] **Step 5: Commit** `feat(booking): tao don nhieu dich vu mot lich mot hoa don`

---

### Task 4: Cột `PhanCongCTV.chiTietDonDatId` + quy tắc phân công theo dịch vụ

**Files:**
- Create: `database/ThemChiTietDonDatVaoPhanCong.sql`
- Modify: `src/main/java/com/example/Model/PhanCongCTV.java`
- Create: `src/main/java/com/example/Service/PhanCongTheoDichVu.java`
- Test: `src/test/java/com/example/Service/PhanCongTheoDichVuTest.java`

**Interfaces:**
- Produces:
  - SQL: `ALTER TABLE PhanCongCTV ADD COLUMN chiTietDonDatId INT NULL; ALTER TABLE PhanCongCTV ADD CONSTRAINT fk_phancong_chitiet FOREIGN KEY (chiTietDonDatId) REFERENCES ChiTietDonDat(id);`
  - Entity: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chiTietDonDatId") private ChiTietDonDat chiTietDonDat;`
  - `public static boolean hieuLuc(PhanCongCTV pc)` — trạng thái khác `TuChoi`, `DaHuy`.
  - `public static Set<Integer> chiTietConTrong(List<ChiTietDonDat> cts, List<PhanCongCTV> pcs)` — có phân công hiệu lực `chiTietDonDat == null` ⇒ rỗng (cả đơn đã có người); ngược lại là id các dòng chưa có phân công hiệu lực.
  - `public static boolean duNguoi(List<ChiTietDonDat> cts, List<PhanCongCTV> pcs)` — `chiTietConTrong` rỗng **và** có ít nhất một phân công đã xác nhận (`DaXacNhan`/`HoanThanh`).
  - `public static String loiNhan(Set<Integer> chiTietCuaDon, Set<Integer> conTrong, Set<Integer> dichVuCtv, Map<Integer,Integer> dichVuCuaChiTiet, List<Integer> muonNhan)` — null nếu nhận được.

- [ ] **Step 1: Write the failing test** (`PhanCongTheoDichVuTest`, dựng entity bằng builder):
```java
@Test void donMotDichVuCoNguoiNhanCaDonLaDuNguoi() { assertTrue(PhanCongTheoDichVu.duNguoi(List.of(ct(10,1)), List.of(pc(null,"DaXacNhan")))); }
@Test void phanCongTuChoiKhongTinh() { assertEquals(Set.of(10), PhanCongTheoDichVu.chiTietConTrong(List.of(ct(10,1)), List.of(pc(null,"TuChoi")))); }
@Test void nhanMotPhanChuaDuNguoi() { assertFalse(PhanCongTheoDichVu.duNguoi(List.of(ct(10,1), ct(11,2)), List.of(pc(10,"DaXacNhan")))); }
@Test void moiDongCoNguoiLaDuNguoi() { assertTrue(PhanCongTheoDichVu.duNguoi(List.of(ct(10,1), ct(11,2)), List.of(pc(10,"DaXacNhan"), pc(11,"DaXacNhan")))); }
@Test void nhanDongDaCoNguoiBaoLoi() { assertEquals("Dịch vụ bạn chọn vừa có người nhận. Vui lòng tải lại đơn.", PhanCongTheoDichVu.loiNhan(Set.of(10,11), Set.of(11), Set.of(1,2), Map.of(10,1,11,2), List.of(10))); }
@Test void nhanDongThieuKyNangBaoLoi() { assertEquals("Bạn chưa đăng ký kỹ năng cho dịch vụ đã chọn.", PhanCongTheoDichVu.loiNhan(Set.of(10,11), Set.of(10,11), Set.of(1), Map.of(10,1,11,2), List.of(11))); }
@Test void nhanDongKhongThuocDonBaoLoi() { assertEquals("Dịch vụ không thuộc đơn này.", PhanCongTheoDichVu.loiNhan(Set.of(10), Set.of(10), Set.of(1), Map.of(10,1), List.of(99))); }
@Test void nhanHopLe() { assertNull(PhanCongTheoDichVu.loiNhan(Set.of(10,11), Set.of(10,11), Set.of(1,2), Map.of(10,1,11,2), List.of(10,11))); }
```
- [ ] **Step 2: Run** `./mvnw -q test -Dtest=PhanCongTheoDichVuTest` — Expected: FAIL
- [ ] **Step 3: Implement** `PhanCongTheoDichVu`, field entity, file SQL.
- [ ] **Step 4: Run** `./mvnw -q test -Dtest=PhanCongTheoDichVuTest` — Expected: `Tests run: 8, Failures: 0`
- [ ] **Step 5: ĐIỂM DỪNG — hỏi người dùng** cho chạy migration lên `GiupViecDB` (docker `mysql`): `docker exec -i mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" GiupViecDB' < database/ThemChiTietDonDatVaoPhanCong.sql`. Server 8083 dùng `ddl-auto=validate` nên phải chạy migration **trước** khi khởi động lại với entity mới. Kiểm tra: `SHOW COLUMNS FROM PhanCongCTV LIKE 'chiTietDonDatId'` trả 1 dòng.
- [ ] **Step 6: Commit** `feat(phan-cong): cot chiTietDonDatId va quy tac phan cong theo dich vu`

---

### Task 5: CTV xem và nhận đơn theo dịch vụ (API)

**Files:**
- Modify: `src/main/java/com/example/Service/CollaboratorApiService.java:1577-1700` (`getAvailableOrders`, `acceptOrderFromPool`)
- Modify: `src/main/java/com/example/API/CollaboratorProfileController.java:163-180`
- Modify: `src/main/java/com/example/Repository/PhanCongCTVRepository.java` (nếu cần `findAllByDonDat_IdIn`)
- Test: `src/test/java/com/example/Service/PhanCongTheoDichVuTest.java`

**Interfaces:**
- Consumes: Task 4.
- Produces:
  - `getAvailableOrders(ctvId)`: đơn `ChoDuyet` còn hạn nhận, trong khu vực (giữ logic cũ), **và** `chiTietConTrong` chứa ít nhất một dòng có dịch vụ thuộc kỹ năng CTV (`DichVuCTV` `HoatDong`). Bỏ đơn mà CTV trùng lịch trên khung giờ của đơn (gọi `phanCongService.kiemTraDonHoacBaoLoi` trong `try/catch`, lỗi ⇒ bỏ qua đơn). Mỗi item thêm `dichVus: [{chiTietId, dichVuId, tenDichVu, thoiGianPhut, daCoNguoiNhan, ctvCoKyNang}]` và `tenDichVu = tomTatDichVu(...)`.
  - `public Map<String,Object> acceptOrderFromPool(Integer congTacVienId, Integer donDatId, List<Integer> chiTietIds)` — bản 2 tham số cũ gọi bản mới với `null`.
    - `donDatDichVuRepository.findByIdForUpdate(donDatId)` thay `atomicAcceptOrder`; trạng thái phải `ChoDuyet` (sai ⇒ `IllegalStateException("Đơn này vừa có người nhận rồi! Vui lòng chọn đơn khác.")`); `hanNhanDonService.kiemTraConHan(don)`.
    - **Đơn 1 dòng**: hành vi cũ (một phân công `chiTietDonDat = null`, lịch, đơn `DaXacNhan`, lịch sử, thông báo).
    - **Đơn ≥ 2 dòng**: `chiTietIds` null/rỗng ⇒ mọi dòng còn trống mà CTV có kỹ năng; `loiNhan(...)` ≠ null ⇒ `IllegalStateException(loi)`; kiểm tra trùng lịch như cũ; mỗi dòng một `PhanCongCTV` (`DaXacNhan`, `chiTietDonDat` = dòng); `LichLamViec` chỉ tạo nếu CTV chưa có phân công hiệu lực nào khác trong đơn; `duNguoi` ⇒ đơn `DaXacNhan` + `LichSuTrangThaiDon` ("Đủ cộng tác viên cho mọi dịch vụ"); chưa đủ ⇒ đơn giữ `ChoDuyet`, ghi lịch sử "CTV … nhận: <tên dịch vụ>".
  - Controller: body `Map<String,Object>`; đọc `congTacVienId`/`taiKhoanId` như cũ, `chiTietIds` (List số, tùy chọn).
  - `PhanCongService.kiemTraDonHoacBaoLoi(...)` bỏ qua `LichLamViec` thuộc chính đơn đang nhận.

- [ ] **Step 1: Write the failing test** — thêm vào `PhanCongTheoDichVuTest`:
```java
@Test void macDinhNhanMoiDongConTrongCoKyNang() { assertEquals(List.of(10), PhanCongTheoDichVu.chonMacDinh(Set.of(10,11), Set.of(1), Map.of(10,1,11,2))); }
```
  với `public static List<Integer> chonMacDinh(Set<Integer> conTrong, Set<Integer> dichVuCtv, Map<Integer,Integer> dichVuCuaChiTiet)` (sắp tăng dần).
- [ ] **Step 2: Run** `./mvnw -q test -Dtest=PhanCongTheoDichVuTest` — Expected: FAIL
- [ ] **Step 3: Implement** `chonMacDinh` và các thay đổi trong Interfaces.
- [ ] **Step 4: Run** `./mvnw -q test -Dtest=PhanCongTheoDichVuTest,PhanCongServiceTest,HanNhanDonServiceTest` — Expected: tất cả đạt
- [ ] **Step 5: Commit** `feat(ctv): xem va nhan don theo tung dich vu`

---

### Task 6: Hết hạn nhận, chuyển trạng thái, hoàn thành dựa trên "đủ người"

**Files:**
- Modify: `src/main/java/com/example/Service/TuHuyDonService.java:100-200`
- Modify: `src/main/java/com/example/Service/QuanLyDonService.java:120-135`
- Modify: `src/main/java/com/example/Service/CollaboratorApiService.java:1014-1080` (`completeAssignment`), và luồng từ chối/bỏ gán (`rejectAssignment`, `PhanCongService.boPhanCong`)
- Test: `src/test/java/com/example/Service/TuHuyDonServiceTest.java`, `QuanLyDonServiceTest.java`

**Interfaces:**
- Consumes: Task 4 (`duNguoi`, `hieuLuc`).
- Produces:
  - `public static String lyDoHetHanNhan(LocalDateTime han, boolean duNguoi, boolean coNguoiNhanMotPhan, boolean daLamBuoiNao, LocalDateTime luc, long soPhut)` — thay chữ ký cũ; `duNguoi` ⇒ null; `coNguoiNhanMotPhan` ⇒ "Hết 15 phút chờ (hạn …) nhưng chưa đủ cộng tác viên nhận đơn"; còn lại giữ câu cũ.
  - `quet(...)`: `daCoNguoiNhan` thay bằng `duNguoi`; hủy đơn chưa đủ người thì chuyển các phân công hiệu lực sang `DaHuy`, xóa `LichLamViec` chưa làm, gửi thông báo từng CTV ("Đơn … đã hủy vì chưa đủ cộng tác viên").
  - `QuanLyDonService.loiChuyenTrangThai`: sang `DangThucHien` cần `duNguoi` (thay `noneMatch DaXacNhan`), câu lỗi "Còn dịch vụ chưa có cộng tác viên nhận."
  - `completeAssignment`: đơn chỉ sang `HoanThanh` khi **mọi** phân công hiệu lực của đơn là `HoanThanh`.
  - Từ chối / bỏ gán một dòng của đơn `DaXacNhan` ⇒ đơn về `ChoDuyet` và phát hành lại (dùng cùng cơ chế phát hành lại hiện có của `HanNhanDonService`).

- [ ] **Step 1: Write the failing test**
```java
@Test void nhanMotPhanHetHanThiHuyVoiLyDoChuaDu() { assertEquals("Hết 15 phút chờ (hạn 10:15 26/10/2026) nhưng chưa đủ cộng tác viên nhận đơn", TuHuyDonService.lyDoHetHanNhan(LocalDateTime.of(2026,10,26,10,15), false, true, false, LocalDateTime.of(2026,10,26,10,16), 15)); }
@Test void duNguoiKhongHuy() { assertNull(TuHuyDonService.lyDoHetHanNhan(LocalDateTime.of(2026,10,26,10,15), true, true, false, LocalDateTime.of(2026,10,26,10,16), 15)); }
```
  và trong `QuanLyDonServiceTest`: `assertEquals("Còn dịch vụ chưa có cộng tác viên nhận.", QuanLyDonService.loiDuNguoi(false))`, `assertNull(QuanLyDonService.loiDuNguoi(true))` với `static String loiDuNguoi(boolean duNguoi)` được `loiChuyenTrangThai` gọi.
- [ ] **Step 2: Run** `./mvnw -q test -Dtest=TuHuyDonServiceTest,QuanLyDonServiceTest` — Expected: FAIL (chữ ký mới)
- [ ] **Step 3: Implement**; cập nhật các test cũ gọi `lyDoHetHanNhan` sang chữ ký 6 tham số (`coNguoiNhanMotPhan = false`).
- [ ] **Step 4: Run** `./mvnw -q test -Dtest=TuHuyDonServiceTest,QuanLyDonServiceTest,PhanCongTheoDichVuTest` — Expected: tất cả đạt
- [ ] **Step 5: Commit** `feat(don): het han nhan va trang thai theo du nguoi`

---

### Task 7: CSKH phân công theo dịch vụ

**Files:**
- Modify: `src/main/java/com/example/Service/PhanCongService.java:385-700` (`goiY`, `phanCong`, `boPhanCong`)
- Modify: controller đang gọi `phanCong` (`Controller/DonDatChiTietController.java` hoặc `CSKHController.java`)
- Modify: `src/main/resources/templates/cskh/don-dat-dich-vu.html` (bảng phân công trong panel chi tiết)
- Test: `src/test/java/com/example/Service/PhanCongTheoDichVuTest.java`

**Interfaces:**
- Consumes: Task 4, 5.
- Produces:
  - `goiY(donDatId)` thêm `dichVus: [{chiTietId, tenDichVu, phanCong: {phanCongId, hoTen, trangThai} | null}]`; mỗi CTV trong `phuHop` thêm `chiTietCoTheNhan: [chiTietId…]` (dòng còn trống mà CTV có kỹ năng). Đơn 1 dòng: giữ nguyên trường cũ.
  - `public PhanCongCTV phanCong(Integer donDatId, Integer ctvId, Integer chiTietDonDatId, String nguoiThucHien)` — bản 3 tham số cũ gọi với `null`. Dòng đã có phân công hiệu lực ⇒ `IllegalArgumentException("Dịch vụ này đã có cộng tác viên.")`; CTV không có kỹ năng của dòng ⇒ `IllegalArgumentException("Cộng tác viên chưa đăng ký kỹ năng cho dịch vụ này.")`.
  - Giao diện: đơn ≥ 2 dịch vụ hiện mỗi dịch vụ một hàng "Tên dịch vụ – CTV đã gán / nút Gán"; nút Gán mở danh sách gợi ý lọc theo `chiTietCoTheNhan`. Đơn 1 dịch vụ giữ giao diện hiện tại.

- [ ] **Step 1: Write the failing test**
```java
@Test void ctvChiNhanDuocDongCoKyNang() { assertEquals(List.of(11), PhanCongTheoDichVu.chonMacDinh(Set.of(10,11), Set.of(2), Map.of(10,1,11,2))); }
```
  (dùng lại `chonMacDinh` làm `chiTietCoTheNhan`).
- [ ] **Step 2: Run** `./mvnw -q test -Dtest=PhanCongTheoDichVuTest` — Expected: PASS ngay nếu Task 5 đúng (đây là test hồi quy cho cách dùng mới) — nếu PASS ngay thì ghi chú trong ledger, không coi là lỗi.
- [ ] **Step 3: Implement** Interfaces.
- [ ] **Step 4: Run** `./mvnw -q test -Dtest=PhanCongTheoDichVuTest,PhanCongServiceTest` — Expected: tất cả đạt
- [ ] **Step 5: Commit** `feat(cskh): phan cong cong tac vien theo tung dich vu`

---

### Task 8: Luồng web – giỏ hàng và trang đặt nhiều dịch vụ

**Files:**
- Modify: `src/main/java/com/example/Controller/KhachHangWebController.java` (thêm `GET/POST /dat-dich-vu/nhieu`)
- Modify: `src/main/resources/templates/khach-hang/dat-dich-vu.html`
- Modify: `src/main/resources/templates/site/gio-hang.html`, `src/main/resources/static/js/site.js` (`CartCache`)
- Modify: `src/main/resources/templates/site/dich-vu-thong-tin.html` (nút Thêm vào giỏ gửi thêm `data-loai`)

**Interfaces:**
- Consumes: Task 1–3.
- Produces:
  - `GET /dat-dich-vu/nhieu?dv=1,25` — cần đăng nhập; nạp `DichVu`, `DatNhieuDichVu.loiDanhSach` ≠ null ⇒ `redirect:/gio-hang` + flash `loi`; model `dvNhieu` (List `DichVuView`), `tongPhut`, `danhMuc` = danh mục của dịch vụ đầu (cho breadcrumb). Render `khach-hang/dat-dich-vu.html`.
  - `POST /dat-dich-vu/nhieu` — tham số giống POST một dịch vụ, thay `dichVuId` bằng `dichVuIds` (List). Dựng `BookingCreateRequest.chiTiet`. Thành công ⇒ redirect như hiện nay, thêm `?xoaGio=1,25`. Lỗi ⇒ quay lại `GET /dat-dich-vu/nhieu?dv=...` với flash lỗi.
  - `CartCache.removeMany(ids)`; `site.js` khi URL có `xoaGio` thì gọi `removeMany` rồi xóa tham số khỏi URL (`history.replaceState`).
  - Item giỏ có `loaiHinhDat` (`'TheoLan'|'GoiThang'`); item cũ thiếu trường thì suy từ `donViTinh === 'Gói tháng'`.
  - Giỏ: dòng GoiThang không có checkbox, có nút "Đặt riêng" (`/dat-dich-vu/{danhMucId}?dv={id}`); vượt 5 dịch vụ hoặc tổng > 480 phút ⇒ nút "Đặt dịch vụ (n)" tắt + dòng lý do; nút dẫn tới `/dat-dich-vu/nhieu?dv=…`. Giỏ chỉ chọn 1 dịch vụ ⇒ dẫn tới trang đặt một dịch vụ hiện có.
  - Trang đặt (chế độ `dvNhieu != null`): khung "Dịch vụ đã chọn" liệt kê từng dịch vụ (tên, thời lượng, giá) + "Tổng thời lượng"; nút "Sửa" → `/gio-hang`; JS khóa khung giờ mà `batDau + tongPhut > 20:00`; gọi `/v1/bookings/calculate-price` và `/v1/bookings/available-promotions` với `chiTiet: [{dichVuId}...]`; bảng xác nhận liệt kê từng dòng từ `chiTietGia`; không hiện lựa chọn gói tháng.

- [ ] **Step 1: Kiểm tra tay trên trình duyệt (trang trước khi sửa)** — giỏ có 2 dịch vụ theo lần + 1 gói tháng; xác nhận lỗi hiện tại (chỉ hiện dịch vụ đầu).
- [ ] **Step 2: Implement** Interfaces; chép template/static sang `target/classes` (hoặc `./mvnw process-resources`) để server 8083 phục vụ bản mới.
- [ ] **Step 3: Kiểm tra trên trình duyệt** (khách hàng thử trên localhost):
  - giỏ: dòng gói tháng có "Đặt riêng", không tick được; chọn 6 dịch vụ ⇒ nút tắt kèm lý do;
  - đặt 2 dịch vụ: thấy cả 2 dịch vụ + "Tổng thời lượng 2 giờ 45 phút"; khung 17:30 bị khóa; tạm tính = tổng 2 giá; áp mã ⇒ giảm trên tổng;
  - đặt tiền mặt thành công ⇒ chi tiết đơn có 2 dịch vụ; giỏ còn lại đúng dịch vụ gói tháng;
  - đơn một dịch vụ từ trang chi tiết vẫn đặt được như cũ.
- [ ] **Step 4: Xóa đơn thử** bằng đúng cách xóa của `QuanLyDonService.xoaDon` (đơn chưa phát sinh) trên trang CSKH, hoặc hỏi người dùng.
- [ ] **Step 5: Commit** `feat(web): dat nhieu dich vu tu gio hang`

---

### Task 9: App CTV – nhận đơn theo dịch vụ (repo app)

**Files:**
- Create: `lib/services/dich_vu_don.dart`
- Modify: `lib/services/collaborator_api_service.dart:213-230` (`acceptOrderFromPool`)
- Modify: `lib/screens/collaborator/collaborator_orders_screen.dart` (thẻ đơn chờ, chi tiết đơn, nút nhận ~dòng 3681), `lib/screens/collaborator/collaborator_schedule_screen.dart`
- Modify (web): `src/main/java/com/example/Service/CollaboratorApiService.java` (`getSchedules`, `getScheduleDetail`)
- Test: `test/dich_vu_don_test.dart`

**Interfaces:**
- Consumes: Task 5 (JSON `dichVus`, body `chiTietIds`).
- Produces:
  - `class DichVuDon { final int chiTietId; final String ten; final int phut; final bool daCoNguoi; final bool coKyNang; factory DichVuDon.fromJson(Map<String,dynamic>); bool get nhanDuoc => !daCoNguoi && coKyNang; }`
  - `static List<int> DichVuDon.chonMacDinh(List<DichVuDon> ds)` — id các dòng `nhanDuoc`.
  - `acceptOrderFromPool({required int congTacVienId, required int donDatId, List<int>? chiTietIds})` — gửi `chiTietIds` khi khác null.
  - Giao diện: đơn ≥ 2 dịch vụ ⇒ thẻ hiện danh sách dịch vụ (dòng đã có người mờ, ghi "Đã có người nhận"); bấm Nhận mở bottom sheet checkbox (mặc định `chonMacDinh`, dòng không `nhanDuoc` bị khóa), nút "Nhận n dịch vụ" (tắt khi n = 0). Đơn 1 dịch vụ giữ nút nhận hiện tại.
  - Lịch làm việc / chi tiết ca của CTV: liệt kê các dịch vụ CTV đó nhận trong đơn (API `getSchedules`/`getScheduleDetail` trả thêm `dichVuCuaToi: [tenDichVu…]` từ các phân công của CTV trong đơn).

- [ ] **Step 1: Write the failing test**
```dart
test('chonMacDinh lấy dòng còn trống và có kỹ năng', () {
  final ds = [DichVuDon.fromJson({'chiTietId': 10, 'tenDichVu': 'A', 'thoiGianPhut': 120, 'daCoNguoiNhan': false, 'ctvCoKyNang': true}),
              DichVuDon.fromJson({'chiTietId': 11, 'tenDichVu': 'B', 'thoiGianPhut': 45, 'daCoNguoiNhan': true, 'ctvCoKyNang': true}),
              DichVuDon.fromJson({'chiTietId': 12, 'tenDichVu': 'C', 'thoiGianPhut': 60, 'daCoNguoiNhan': false, 'ctvCoKyNang': false})];
  expect(DichVuDon.chonMacDinh(ds), [10]);
});
test('thiếu trường thì coi như không nhận được', () {
  expect(DichVuDon.fromJson({'chiTietId': 1}).nhanDuoc, isFalse);
});
```
- [ ] **Step 2: Run** `flutter test test/dich_vu_don_test.dart` — Expected: FAIL (thiếu file)
- [ ] **Step 3: Implement** Interfaces.
- [ ] **Step 4: Run** `flutter test` và `flutter analyze` — Expected: mọi test đạt; không có lỗi `error`.
- [ ] **Step 5: Commit** (repo app) `feat(ctv-app): nhan don theo tung dich vu`

---

### Task 10: App khách – hiện mọi dịch vụ của đơn (repo app + API)

**Files:**
- Modify (web): `src/main/java/com/example/Service/CustomerApiService.java` — các hàm trả chi tiết/lịch sử đơn cho app thêm `dichVus: [{dichVuId, tenDichVu, donGia}]` và `tenDichVu = tomTatDichVu(...)`
- Modify (app): `lib/screens/customer/order_detail_screen.dart`, `booking_history_screen.dart`, `thanh_toan_screen.dart`
- Test (app): `test/dich_vu_don_test.dart`

**Interfaces:**
- Consumes: Task 3 (`tomTatDichVu`).
- Produces (app): `static List<({String ten, int gia})> DichVuDon.tuDonKhach(Map<String,dynamic> don)` — đọc `dichVus`; không có thì một dòng từ `tenDichVu`/`thanhTien` (đơn cũ).
  - Chi tiết đơn & thanh toán: mỗi dịch vụ một dòng "tên … giá"; lịch sử đơn: tiêu đề dùng `tenDichVu` (đã là tóm tắt).

- [ ] **Step 1: Write the failing test**
```dart
test('đơn cũ không có dichVus vẫn hiện một dòng', () {
  expect(DichVuDon.tuDonKhach({'tenDichVu': 'Dọn nhà', 'thanhTien': 216000}).map((d) => d.ten), ['Dọn nhà']);
});
test('đơn nhiều dịch vụ hiện đủ dòng', () {
  expect(DichVuDon.tuDonKhach({'dichVus': [{'tenDichVu': 'A', 'donGia': 1}, {'tenDichVu': 'B', 'donGia': 2}]}).length, 2);
});
```
- [ ] **Step 2: Run** `flutter test test/dich_vu_don_test.dart` — Expected: FAIL
- [ ] **Step 3: Implement** (web API trước, rồi app).
- [ ] **Step 4: Run** `flutter test`, `flutter analyze`; web `./mvnw -q test -Dtest=DatNhieuDichVuTest` — Expected: đạt
- [ ] **Step 5: Commit** web và app riêng: `feat(api): tra ve moi dich vu cua don` / `feat(app): hien moi dich vu cua don`

---

### Task 11: Kiểm tra đầu–cuối và dọn dữ liệu thử

**Files:** không sửa code (chỉ sửa nếu phát hiện lỗi — quay lại task tương ứng).

- [ ] **Step 1:** Chạy toàn bộ test nghiệp vụ web: `./mvnw -q test -Dtest="com.example.Service.*Test,com.example.API.*Test"` — Expected: tất cả đạt. App: `flutter test` — Expected: tất cả đạt.
- [ ] **Step 2:** Trên web đặt đơn 2 dịch vụ (tiền mặt) bằng tài khoản khách thử; CSKH thấy thẻ "Đơn mới" với tên tóm tắt và bảng phân công 2 dòng.
- [ ] **Step 3:** (Cần người dùng đồng ý `flutter run`) Hai tài khoản CTV thử, mỗi người nhận một dịch vụ trên app; sau người thứ hai đơn sang "Đã xác nhận"; người thứ ba thử nhận dòng đã có người ⇒ báo lỗi.
- [ ] **Step 4:** Đơn thứ hai chỉ một CTV nhận một dòng rồi để hết 15 phút ⇒ đơn tự hủy với lý do "chưa đủ cộng tác viên", CTV nhận thông báo.
- [ ] **Step 5:** Xóa các đơn, phân công, lịch, thông báo thử (như các lần dọn dữ liệu thử trước), báo người dùng danh sách đã xóa.
