# Đặt nhiều dịch vụ từ giỏ hàng – một lịch, một hóa đơn

Ngày: 26/10/2026 · Phạm vi đợt này: **website** (khách hàng, CSKH) + **app CTV** (nhận đơn) + **app khách** (chỉ hiển thị).
Thay thế hướng "mỗi dịch vụ một đơn" trong `docs/mo-ta-gio-dich-vu.md`.

## 1. Mục tiêu

Khách chọn nhiều dịch vụ trong giỏ hàng, đặt **một lần**: dùng chung **một ngày, một giờ bắt đầu, một địa chỉ**, các dịch vụ làm nối tiếp nhau trong buổi đó, thanh toán **một hóa đơn**. Mỗi dịch vụ có thể do một CTV khác nhau làm, hoặc một CTV nhận hết.

Thành công khi:
- Khách đặt được 2–5 dịch vụ theo lần trong một đơn, thấy đúng tổng tiền, trả tiền một lần.
- Hai CTV cùng nhận một đơn, mỗi người một dịch vụ; không ai nhận trùng dịch vụ đã có người.
- Đơn một dịch vụ (cả trên web lẫn app) và mọi đơn cũ chạy y như trước.

## 2. Ràng buộc đã chốt

| Mục | Giá trị |
|---|---|
| Loại dịch vụ đặt chung | Chỉ **TheoLan**. Dòng **GoiThang** trong giỏ có nút "Đặt riêng" (luồng 3 bước hiện tại), không tick chung |
| Số dịch vụ / đơn | 1–5 |
| Tổng thời lượng | ≤ 8 giờ (tổng `ThoiGianThucHien` các dịch vụ) |
| Giờ kết thúc | = giờ bắt đầu + tổng thời lượng, **không muộn hơn 20:00** |
| Ngày | Như đặt theo lần hiện nay: ≤ hôm nay + 1 tháng; giờ bắt đầu ≥ hiện tại + 1 giờ |
| Khung giờ bắt đầu | 07:00–17:00 như hiện tại; khung làm giờ kết thúc > 20:00 bị khóa |
| Thay đổi CSDL | **Chỉ 1 cột** `PhanCongCTV.chiTietDonDatId` (INT NULL, FK → `ChiTietDonDat.id`) — đã được đồng ý |
| Hết hạn nhận (15 phút) mà còn dịch vụ chưa ai nhận | **Tự hủy đơn** như hiện nay; thông báo khách và các CTV đã nhận một phần |

## 3. Dữ liệu

- Một `DonDatDichVu` (ngày, giờ bắt đầu, giờ kết thúc, địa chỉ, `loaiHinhDat = TheoLan`).
- N dòng `ChiTietDonDat`, mỗi dòng một dịch vụ, `donGia`/`thanhTien`/`bangGia` theo khu vực của địa chỉ.
- Một `HoaDon` (1–1 với đơn như hiện nay).
- `PhanCongCTV.chiTietDonDatId`:
  - `NULL` = phân công cho **cả đơn** (đơn một dịch vụ, mọi dữ liệu cũ).
  - Có giá trị = CTV làm **dịch vụ đó** trong đơn. Một CTV nhận 2 dịch vụ ⇒ 2 dòng phân công.
  - Mỗi `ChiTietDonDat` có tối đa một phân công đang hiệu lực (trạng thái khác `TuChoi`/`DaHuy`).
- Migration: `database/ThemChiTietDonDatVaoPhanCong.sql` — `ALTER TABLE PhanCongCTV ADD COLUMN chiTietDonDatId INT NULL, ADD CONSTRAINT ... FOREIGN KEY (chiTietDonDatId) REFERENCES ChiTietDonDat(id)`; entity thêm `@ManyToOne ChiTietDonDat chiTietDonDat` (nullable). Chạy migration trước khi khởi động bản mới (`ddl-auto=validate`).

## 4. Luồng khách (web)

1. **Giỏ hàng** (`site/gio-hang.html`, `CartCache` trong `site.js`): checkbox chỉ bật cho dòng TheoLan; dòng GoiThang hiện nhãn "Gói tháng" + nút **Đặt riêng** (→ `/dat-dich-vu/{danhMuc}?dv={id}`). Chọn quá 5 dịch vụ hoặc tổng > 8 giờ ⇒ nút "Đặt dịch vụ (n)" tắt kèm lý do.
2. **Đặt dịch vụ (n)** ⇒ `GET /dat-dich-vu/nhieu?dv=1,25,...` (cần đăng nhập). Máy chủ kiểm lại: mọi id tồn tại, đang hoạt động, TheoLan, 1–5 dịch vụ, tổng ≤ 8 giờ; sai ⇒ về `/gio-hang` kèm thông báo.
3. Trang đặt dùng lại `khach-hang/dat-dich-vu.html` (3 bước Thời gian → Xác nhận → Thanh toán). Khung "Dịch vụ đã chọn" liệt kê n dịch vụ (tên, thời lượng, giá) + tổng thời lượng; nút "Sửa" về giỏ hàng. Đơn một dịch vụ giữ nguyên khung hiện có.
4. Bước Thời gian: khung giờ nào có giờ kết thúc > 20:00 bị khóa (giống khung giờ < hiện tại + 1 giờ).
5. Xác nhận: bảng từng dịch vụ + tạm tính + giảm giá + tổng.
6. Gửi: `POST /dat-dich-vu/nhieu` với `dichVuIds`, ngày, giờ, địa chỉ, mã KM, ghi chú, hình thức thanh toán. Thành công ⇒ xóa các dịch vụ vừa đặt khỏi `CartCache` rồi về trang thanh toán (chuyển khoản) hoặc chi tiết đơn (tiền mặt), như hiện nay.

## 5. Giá, khuyến mãi, thanh toán

- `CustomerApiService.calculatePrice` và `createBooking` nhận danh sách dịch vụ (`BookingCreateRequest.getDanhSachDichVu()` đã có sẵn): giá từng dòng theo bảng giá khu vực; `chiPhiGoc` = tổng các dòng.
- Mã khuyến mãi: điều kiện tối thiểu và số tiền giảm tính trên **tổng**; trần giảm tối đa giữ nguyên; lượt dùng tính 1 lần/đơn.
- Hóa đơn, nội dung chuyển khoản, webhook SePay, hạn thanh toán 15 phút, tự hủy khi quá hạn thanh toán: **không đổi** (vẫn một đơn).
- Kiểm tra ngày giờ (`RangBuocNgayDat`) thêm: tổng thời lượng ≤ 8 giờ, giờ kết thúc ≤ 20:00. Cùng câu báo lỗi trên giao diện và máy chủ.
- API app `POST /v1/customer/bookings` dùng chung service nên tự hỗ trợ `chiTiet` nhiều dòng (app khách chưa gửi, đợi giỏ hàng app).

## 6. Nhận đơn & phân công

**Danh sách đơn chờ của CTV** (`CollaboratorApiService.getAvailableOrders`): đơn hiện ra khi CTV có kỹ năng (`DichVuCTV` hoạt động) với **ít nhất một dịch vụ chưa ai nhận**, làm ở khu vực của đơn, còn hạn nhận, và không trùng lịch trên cả khung giờ của đơn. Mỗi đơn trả kèm danh sách dịch vụ: `chiTietId, tenDichVu, thoiGian, daCoNguoiNhan, ctvCoKyNang`.

**Nhận đơn** (`acceptOrderFromPool(ctvId, donId, chiTietIds)`; `chiTietIds` rỗng = mọi dịch vụ còn trống mà CTV có kỹ năng):
1. Khóa dòng đơn (`findByIdForUpdate`), kiểm tra còn hạn nhận.
2. Mỗi `chiTietId` phải thuộc đơn, CTV có kỹ năng, chưa có phân công hiệu lực; sai ⇒ báo lỗi, không nhận gì.
3. Kiểm tra trùng lịch CTV trên khung giờ đơn (giữ `kiemTraDonHoacBaoLoi`).
4. Tạo một `PhanCongCTV` (`DaXacNhan`) cho mỗi dịch vụ. `LichLamViec` cho khung giờ đơn chỉ tạo **một** cho mỗi CTV trong đơn, gắn với phân công đầu tiên của CTV đó (CTV nhận thêm dịch vụ sau thì không tạo lịch mới; kiểm tra trùng lịch bỏ qua lịch của chính đơn này).
5. **Mọi dịch vụ đã có người** ⇒ đơn `ChoDuyet → DaXacNhan` (ghi `LichSuTrangThaiDon`). Chưa đủ ⇒ đơn vẫn trong danh sách chờ cho dịch vụ còn lại.
- Cả đơn một dịch vụ và đơn nhiều dịch vụ đều chống nhận trùng bằng khóa dòng đơn (thay `atomicAcceptOrder`). Đơn một dịch vụ giữ nguyên hành vi: một CTV nhận cả đơn, `chiTietDonDatId = NULL`, đơn sang `DaXacNhan` ngay.

**Hết hạn nhận** (`TuHuyDonService`): đơn nhiều dịch vụ còn dòng chưa ai nhận ⇒ tự hủy (lý do "Chưa đủ cộng tác viên nhận đơn"), hủy các phân công đã có, thông báo khách + từng CTV đã nhận.

**Trạng thái** (`QuanLyDonService`):
- `DangThucHien` cần **mọi** dịch vụ có CTV đã nhận.
- `HoanThanh` khi mọi phân công của đơn đã hoàn thành (CTV bấm hoàn thành phân công của mình); tự hoàn thành sau 24 giờ giữ nguyên.
- Xóa đơn "chưa phát sinh": không đổi điều kiện.

**CSKH** (`cskh/don-dat-dich-vu.html`, `PhanCongService.goiY/phanCong`): bảng phân công hiện theo từng dịch vụ; gợi ý CTV có đúng kỹ năng của dòng đó; gán / bỏ gán theo dòng. Đơn một dịch vụ giữ giao diện hiện tại.

## 7. App

- **App CTV**: thẻ đơn chờ và chi tiết đơn hiện danh sách dịch vụ (dòng đã có người thì mờ); khi nhận: hộp chọn dịch vụ (mặc định tick dòng còn trống mà CTV có kỹ năng), gửi `chiTietIds`. Lịch làm việc hiện các dịch vụ CTV đó làm.
- **App khách**: chi tiết đơn, thanh toán, lịch sử đơn hiện **tất cả** dịch vụ của đơn và tổng tiền. Không làm giỏ hàng trên app đợt này.

## 8. Lỗi & trường hợp biên

- Giá dịch vụ đổi giữa lúc thêm giỏ và lúc đặt: dùng giá máy chủ tính lúc đặt; trang xác nhận hiện giá mới.
- Dịch vụ trong giỏ bị ẩn/xóa: trang đặt nhiều báo và về giỏ; giỏ hiện dòng đó mờ.
- Hai CTV nhận cùng dịch vụ cùng lúc: khóa dòng đơn ⇒ người sau nhận lỗi "Dịch vụ … vừa có người nhận".
- CTV từ chối / bị bỏ gán một dịch vụ khi đơn đã `DaXacNhan`: dòng đó trống lại, đơn về `ChoDuyet` và quay lại danh sách chờ (hạn nhận tính lại như phát hành lại hiện nay).

## 9. Kiểm thử

Unit test (không chạm CSDL thật):
- Tính giá N dòng + khuyến mãi trên tổng (điều kiện tối thiểu, trần giảm).
- Giờ kết thúc = bắt đầu + tổng thời lượng; chặn tổng > 8 giờ và kết thúc > 20:00.
- Quy tắc nhận: dịch vụ đã có người ⇒ lỗi; thiếu kỹ năng ⇒ lỗi; nhận đủ mọi dòng ⇒ đơn `DaXacNhan`; nhận một phần ⇒ vẫn `ChoDuyet`.
- Hết hạn nhận khi còn dòng trống ⇒ tự hủy.
- `DangThucHien` bị chặn khi còn dòng chưa có CTV.
- Đơn một dịch vụ: các test hiện có vẫn đạt.

Kiểm tra tay: đặt 2 dịch vụ (tiền mặt) trên web; 2 tài khoản CTV mỗi người nhận một dịch vụ trên app; CSKH thấy phân công theo dòng; xóa đơn và dữ liệu thử sau khi kiểm tra.

## 10. Ngoài phạm vi

Giỏ hàng trên app khách; gộp nhiều gói tháng; mỗi dịch vụ một khung giờ riêng trong cùng đơn; thay đổi công thức thu nhập CTV (đợt này giữ cách tính hiện tại trên từng phân công).
