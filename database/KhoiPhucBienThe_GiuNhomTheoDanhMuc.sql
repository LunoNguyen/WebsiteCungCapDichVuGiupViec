-- ============================================================================
-- KHÔI PHỤC BIẾN THỂ + GỌN DỊCH VỤ THEO DANH MỤC
--   Chạy SAU SuaDuLieu_AnhDichVu_GonDichVu_MoTa.sql (lúc đó mỗi nhóm chỉ còn 1 dịch vụ
--   theo lần + 1 gói tháng).
--   1. Mỗi danh mục (LoaiDichVu) chỉ giữ 2 nhóm dịch vụ:
--        LDV-001 Dọn dẹp     : DV101 Dọn dẹp nhà theo giờ, DV102 Tổng vệ sinh nhà
--        LDV-002 Điện máy    : DV201 Vệ sinh máy lạnh,     DV202 Vệ sinh máy giặt
--        LDV-003 Chăm sóc    : DV301 Trông trẻ tại nhà,    DV302 Chăm sóc người cao tuổi
--        LDV-004 Tiện ích    : DV401 Giặt sấy,             DV402 Nấu ăn gia đình
--        LDV-005 Doanh nghiệp: DV501 Vệ sinh văn phòng,    DV502 Vệ sinh thảm văn phòng
--      Xoá nhóm DV103, DV203, DV303, DV403, DV503. Đơn hàng của nhóm bị xoá được chuyển sang
--      nhóm cùng danh mục (DV103->DV101, DV203->DV201, DV303->DV302, DV403->DV402, DV503->DV501),
--      giữ nguyên đơn giá, thành tiền của đơn.
--   2. Khôi phục đủ các biến thể thời gian / diện tích / số người của 10 nhóm được giữ
--      (giá, đơn vị lấy theo ChiaBienTheDichVu.sql), mỗi biến thể có mô tả riêng, ảnh,
--      bảng giá đang áp dụng; CTV đã đăng ký nhóm được nhận thêm các biến thể mới.
--
-- Không đổi cấu trúc bảng. Chạy lại nhiều lần không tạo trùng (biến thể đã có thì bỏ qua).
-- Nên sao lưu trước khi chạy. Sau khi chạy: xoá cache Redis
--   docker exec redis sh -c "redis-cli --scan --pattern 'neatify:*' | xargs -r redis-cli del"
-- ============================================================================

SET NAMES utf8mb4;
SET @safe_updates_cu = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;
START TRANSACTION;

-- ----------------------------------------------------------------------------
-- 1. Xoá các nhóm không giữ
-- ----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_nhom_xoa;
CREATE TEMPORARY TABLE tmp_nhom_xoa (
    ma     VARCHAR(20) NOT NULL PRIMARY KEY,
    maMoi  VARCHAR(20) NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_nhom_xoa (ma, maMoi) VALUES
('DV103', 'DV101'), ('DV203', 'DV201'), ('DV303', 'DV302'), ('DV403', 'DV402'), ('DV503', 'DV501');

-- Dịch vụ thay thế: dòng có id nhỏ nhất cùng loại hình đặt của nhóm cùng danh mục
DROP TEMPORARY TABLE IF EXISTS tmp_thay_the;
CREATE TEMPORARY TABLE tmp_thay_the (
    ma       VARCHAR(20) NOT NULL,
    loaiHinh VARCHAR(20) NOT NULL,
    id       INT NOT NULL,
    PRIMARY KEY (ma, loaiHinh)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_thay_the (ma, loaiHinh, id)
SELECT d.MaDichVu, d.LoaiHinhDat, MIN(d.id)
FROM DichVu d
WHERE d.MaDichVu IN ('DV101', 'DV201', 'DV302', 'DV402', 'DV501')
GROUP BY d.MaDichVu, d.LoaiHinhDat;

DROP TEMPORARY TABLE IF EXISTS tmp_anh_xa;
CREATE TEMPORARY TABLE tmp_anh_xa (
    cu  INT NOT NULL PRIMARY KEY,
    moi INT NOT NULL
);

INSERT INTO tmp_anh_xa (cu, moi)
SELECT d.id, t.id
FROM DichVu d
JOIN tmp_nhom_xoa x ON x.ma = d.MaDichVu
JOIN tmp_thay_the t ON t.ma = x.maMoi AND t.loaiHinh = d.LoaiHinhDat;

-- Kiểm tra: mọi dịch vụ bị xoá phải có dịch vụ thay thế (không có -> lỗi NOT NULL, script dừng)
DROP TEMPORARY TABLE IF EXISTS tmp_kiem_tra;
CREATE TEMPORARY TABLE tmp_kiem_tra (thieu_dich_vu_thay_the INT NOT NULL);
INSERT INTO tmp_kiem_tra
SELECT NULL FROM DichVu d JOIN tmp_nhom_xoa x ON x.ma = d.MaDichVu
WHERE d.id NOT IN (SELECT cu FROM tmp_anh_xa);

-- Bảng giá dòng đơn đang dùng -> bảng giá đang áp dụng của dịch vụ thay thế
DROP TEMPORARY TABLE IF EXISTS tmp_anh_xa_bg;
CREATE TEMPORARY TABLE tmp_anh_xa_bg (
    cu  INT NOT NULL PRIMARY KEY,
    moi INT NULL
);
INSERT INTO tmp_anh_xa_bg (cu, moi)
SELECT b.id,
       (SELECT b2.id FROM BangGiaDichVu b2
        WHERE b2.dichVuId = a.moi
        ORDER BY (b2.TrangThai = 'DangApDung') DESC, (b2.khuVucId IS NULL) DESC, b2.NgayApDung DESC, b2.id DESC
        LIMIT 1)
FROM BangGiaDichVu b
JOIN tmp_anh_xa a ON a.cu = b.dichVuId;

UPDATE ChiTietDonDat c JOIN tmp_anh_xa_bg m ON m.cu = c.bangGiaId SET c.bangGiaId = m.moi;
UPDATE ChiTietDonDat c JOIN tmp_anh_xa a ON a.cu = c.dichVuId SET c.dichVuId = a.moi;

-- CTV đã đăng ký nhóm bị xoá thì nhận dịch vụ thay thế
INSERT INTO DichVuCTV (MaDichVuCTV, congTacVienId, dichVuId, TrangThai, NgayBatDau)
SELECT CONCAT('DVCTV-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 10))), x.congTacVienId, x.moi, 'HoatDong', CURDATE()
FROM (SELECT DISTINCT c.congTacVienId, a.moi
      FROM DichVuCTV c JOIN tmp_anh_xa a ON a.cu = c.dichVuId) x
WHERE NOT EXISTS (SELECT 1 FROM DichVuCTV e WHERE e.congTacVienId = x.congTacVienId AND e.dichVuId = x.moi);

DELETE c FROM DichVuCTV c JOIN tmp_anh_xa a ON a.cu = c.dichVuId;
DELETE b FROM BangGiaDichVu b JOIN tmp_anh_xa a ON a.cu = b.dichVuId;
DELETE d FROM DichVu d JOIN tmp_anh_xa a ON a.cu = d.id;

-- ----------------------------------------------------------------------------
-- 2. Phần mô tả chung theo nhóm (ghép sau phần giới thiệu riêng của từng biến thể)
-- ----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_than;
CREATE TEMPORARY TABLE tmp_than (
    ma       VARCHAR(20) NOT NULL,
    loaiHinh VARCHAR(20) NOT NULL,
    than     TEXT NOT NULL,
    PRIMARY KEY (ma, loaiHinh)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_than (ma, loaiHinh, than) VALUES
('DV101', 'TheoLan', 'Công việc gồm: quét và lau sàn; lau bụi bàn ghế, kệ tủ, cửa sổ trong tầm với; rửa bát, lau mặt bếp; cọ rửa nhà vệ sinh; gom và đổ rác; sắp xếp đồ đạc gọn gàng. Nhân viên mang theo dụng cụ cơ bản (chổi, cây lau, khăn, nước lau sàn). Không bao gồm giặt rèm, sofa, nệm và vệ sinh trên cao quá 2m.'),
('DV102', 'TheoLan', 'Đội nhân viên dùng máy hút bụi công nghiệp và hoá chất an toàn cho gia đình: hút bụi và lau toàn bộ sàn, chân tường; lau kính, khung cửa hai mặt trong tầm với an toàn; tẩy dầu mỡ bếp, máy hút mùi, mặt ngoài tủ bếp; tẩy cặn vòi sen, gạch ốp, bồn cầu; lau công tắc, tay nắm cửa, quạt; thu gom rác. Thích hợp trước Tết, sau sửa chữa nhỏ hoặc khi chuyển vào nhà mới.'),
('DV201', 'TheoLan', 'Thợ tháo rửa lưới lọc, mặt nạ; xịt rửa dàn lạnh, máng nước, quạt bằng bơm áp lực; vệ sinh dàn nóng; thông ống thoát nước để máy không chảy nước; kiểm tra gas, dòng điện và nhiệt độ gió sau khi vệ sinh. Có bạt hứng nước, không làm bẩn tường và đồ đạc. Nên vệ sinh 3–6 tháng/lần để máy lạnh sâu và tiết kiệm điện.'),
('DV202', 'TheoLan', 'Gồm chạy chu trình nước nóng với dung dịch tẩy cặn chuyên dụng, vệ sinh gioăng cửa, ngăn chứa bột giặt, bộ lọc xả và ống thoát, lau sạch vỏ máy. Loại bỏ mùi hôi và vết đen bám trên quần áo.'),
('DV301', 'TheoLan', 'Bảo mẫu cho bé ăn uống theo hướng dẫn của phụ huynh, chơi cùng bé, đọc truyện, hướng dẫn hoạt động phù hợp độ tuổi, vệ sinh cá nhân, thay tã và quần áo, dọn dẹp khu vực bé chơi. Áp dụng cho 1–2 bé từ 1 tuổi trở lên; bảo mẫu đã được kiểm tra lý lịch và có kiến thức sơ cứu cơ bản. Không bao gồm đưa đón bé ra ngoài và nấu ăn cho cả gia đình.'),
('DV302', 'TheoLan', 'Hỗ trợ ăn uống, nhắc và cho uống thuốc theo đơn; vệ sinh cá nhân, thay quần áo; hỗ trợ đi lại, vận động nhẹ, xoa bóp; đo huyết áp, đường huyết (nếu gia đình có máy) và ghi lại; trò chuyện, đồng hành cùng người cao tuổi. Không thực hiện thủ thuật y tế xâm lấn như tiêm, truyền dịch.'),
('DV302', 'GoiThang', 'Mỗi lần chăm sóc gồm hỗ trợ ăn uống, nhắc uống thuốc, vệ sinh cá nhân, hỗ trợ đi lại và vận động nhẹ, đo huyết áp. Ưu tiên cố định một người chăm sóc để người cao tuổi yên tâm; gia đình nhận nhật ký sức khoẻ sau mỗi lần. Lịch theo các thứ gia đình chọn; giá thấp hơn đặt lẻ.'),
('DV401', 'TheoLan', 'Nhân viên nhận và trả đồ tận nhà trong 24 giờ; đồ được giặt riêng cho từng khách, sấy khô, gấp gọn và đóng túi. Đồ len, lụa, vest hoặc cần tẩy vết bẩn đặc biệt vui lòng ghi chú để được báo giá riêng.'),
('DV402', 'TheoLan', 'Bao gồm sơ chế, nấu, bày món và rửa dọn bếp sau khi nấu; thực đơn theo yêu cầu của khách hoặc người nấu gợi ý. Chưa gồm chi phí nguyên liệu (khách tự chuẩn bị hoặc đặt thêm dịch vụ đi chợ hộ).'),
('DV501', 'TheoLan', 'Gồm hút bụi, lau sàn; lau bàn làm việc, ghế, tủ hồ sơ (không xáo trộn giấy tờ); vệ sinh pantry, bồn rửa, lò vi sóng; cọ rửa nhà vệ sinh; lau cửa kính, tay nắm cửa; đổ rác và thay túi rác. Có thể làm ngoài giờ hành chính; có biên bản nghiệm thu và xuất hoá đơn VAT theo yêu cầu.'),
('DV502', 'TheoLan', 'Dùng máy giặt thảm phun hút công nghiệp: hút bụi sâu, xử lý vết cà phê, mực, kẹo cao su; giặt bằng hoá chất chuyên dụng an toàn với sợi thảm; hút khô và khử mùi. Thảm khô sau 4–6 giờ; có thể làm cuối tuần để không ảnh hưởng giờ làm việc.');

-- ----------------------------------------------------------------------------
-- 3. Các biến thể cần khôi phục (giới thiệu riêng nêu rõ thời gian, diện tích, số người)
-- ----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_bt;
CREATE TEMPORARY TABLE tmp_bt (
    stt       INT AUTO_INCREMENT PRIMARY KEY,
    id        INT NULL,
    ma        VARCHAR(20)  NOT NULL,
    loai      INT          NOT NULL,
    ten       VARCHAR(200) NOT NULL,
    loaiHinh  VARCHAR(20)  NOT NULL,
    donVi     VARCHAR(30)  NOT NULL,
    soBuoi    INT NULL,
    tg        INT NULL,
    soNguoi   INT NULL,
    gia       DECIMAL(12,0) NOT NULL,
    gioiThieu TEXT NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_bt (ma, loai, ten, loaiHinh, donVi, soBuoi, tg, soNguoi, gia, gioiThieu) VALUES
-- DV101 Dọn dẹp nhà theo giờ
('DV101', 1, 'Dọn dẹp nhà theo giờ (3 giờ)', 'TheoLan', 'Buổi', NULL, 180, 1, 270000,
 'Gói dọn dẹp 3 giờ do 1 nhân viên thực hiện, phù hợp căn hộ hoặc nhà phố 55–85m² (2–3 phòng ngủ, 1–2 nhà vệ sinh). Thời lượng dài hơn gói 2 giờ đủ để lau kỹ khu bếp, cọ rửa 2 nhà vệ sinh và sắp xếp lại tủ đồ.'),
('DV101', 1, 'Dọn dẹp nhà theo giờ (4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 1, 342000,
 'Gói dọn dẹp 4 giờ do 1 nhân viên thực hiện, phù hợp nhà 85–105m² (3–4 phòng ngủ, 2–3 nhà vệ sinh) hoặc nhà lâu ngày chưa dọn. Ngoài phần dọn hằng ngày, nhân viên còn đủ thời gian lau cửa kính trong tầm với và vệ sinh ban công, sân phơi.'),
-- DV102 Tổng vệ sinh nhà
('DV102', 1, 'Tổng vệ sinh nhà (tối đa 60m², 2 người/3 giờ)', 'TheoLan', 'Lần', NULL, 180, 2, 660000,
 'Tổng vệ sinh nhà đến 60m² (studio, căn hộ 1–2 phòng ngủ) với đội 2 nhân viên làm liên tục trong 3 giờ.'),
('DV102', 1, 'Tổng vệ sinh nhà (tối đa 80m², 2 người/4 giờ)', 'TheoLan', 'Lần', NULL, 240, 2, 880000,
 'Tổng vệ sinh nhà 60–80m² (căn hộ 2 phòng ngủ, 2 nhà vệ sinh) với đội 2 nhân viên làm liên tục trong 4 giờ.'),
('DV102', 1, 'Tổng vệ sinh nhà (tối đa 150m², 3 người/4 giờ)', 'TheoLan', 'Lần', NULL, 240, 3, 1320000,
 'Tổng vệ sinh nhà 100–150m² (nhà phố 1 trệt 2 lầu, căn hộ 3–4 phòng ngủ) với đội 3 nhân viên làm liên tục trong 4 giờ.'),
('DV102', 1, 'Tổng vệ sinh nhà (tối đa 200m², 4 người/4 giờ)', 'TheoLan', 'Lần', NULL, 240, 4, 1760000,
 'Tổng vệ sinh nhà 150–200m² (nhà phố nhiều tầng, căn hộ penthouse) với đội 4 nhân viên chia khu vực, làm liên tục trong 4 giờ.'),
('DV102', 1, 'Tổng vệ sinh nhà (tối đa 400m², 4 người/8 giờ)', 'TheoLan', 'Lần', NULL, 480, 4, 3784000,
 'Tổng vệ sinh nhà 200–400m² (biệt thự, nhà phố lớn có sân vườn) với đội 4 nhân viên làm trọn ngày 8 giờ, chia hai ca sáng – chiều.'),
-- DV201 Vệ sinh máy lạnh
('DV201', 2, 'Vệ sinh máy lạnh (tủ đứng)', 'TheoLan', 'Máy', NULL, 60, 1, 360000,
 'Vệ sinh 1 máy lạnh tủ đứng công suất 2–5 HP (thường đặt ở phòng khách, sảnh, cửa hàng), 1 thợ làm trong khoảng 1 giờ.'),
('DV201', 2, 'Vệ sinh máy lạnh (âm trần)', 'TheoLan', 'Máy', NULL, 90, 2, 432000,
 'Vệ sinh 1 máy lạnh âm trần cassette (mặt thổi 4 hướng), 2 thợ làm trong khoảng 1 giờ 30 phút, có thang và bạt hứng nước chuyên dụng cho trần cao.'),
('DV201', 2, 'Vệ sinh máy lạnh (áp trần)', 'TheoLan', 'Máy', NULL, 90, 2, 660000,
 'Vệ sinh 1 máy lạnh áp trần (treo sát trần, thổi một hướng dài, thường dùng cho phòng lớn, nhà hàng), 2 thợ làm trong khoảng 1 giờ 30 phút.'),
('DV201', 2, 'Vệ sinh máy lạnh (giấu trần)', 'TheoLan', 'Máy', NULL, 90, 2, 240000,
 'Vệ sinh 1 máy lạnh giấu trần nối ống gió, 2 thợ làm trong khoảng 1 giờ 30 phút: mở cửa thăm trần để vệ sinh dàn lạnh, lưới lọc hồi gió và các miệng gió.'),
-- DV202 Vệ sinh máy giặt
('DV202', 2, 'Vệ sinh máy giặt (cửa trên dưới 9kg, không tháo lồng)', 'TheoLan', 'Máy', NULL, 60, 1, 350000,
 'Vệ sinh 1 máy giặt cửa trên (lồng đứng) khối lượng dưới 9kg, không tháo lồng, 1 thợ làm trong khoảng 1 giờ.'),
('DV202', 2, 'Vệ sinh máy giặt (cửa trên trên 9kg, không tháo lồng)', 'TheoLan', 'Máy', NULL, 60, 1, 350000,
 'Vệ sinh 1 máy giặt cửa trên từ 9kg trở lên (máy cỡ lớn cho gia đình đông người), không tháo lồng, 1 thợ làm trong khoảng 1 giờ.'),
('DV202', 2, 'Vệ sinh máy giặt (cửa trước trên 9kg, không tháo lồng)', 'TheoLan', 'Máy', NULL, 90, 1, 450000,
 'Vệ sinh 1 máy giặt cửa trước (lồng ngang) từ 9kg trở lên, không tháo lồng, 1 thợ làm trong khoảng 1 giờ 30 phút; chú trọng gioăng cao su cửa là nơi hay đọng rêu mốc.'),
('DV202', 2, 'Vệ sinh máy giặt (cửa trên dưới 9kg, tháo lồng)', 'TheoLan', 'Máy', NULL, 120, 1, 450000,
 'Vệ sinh tháo lồng 1 máy giặt cửa trên dưới 9kg, 1 thợ làm trong khoảng 2 giờ: tháo rời lồng giặt để chà sạch rêu mốc, cặn bẩn bám mặt ngoài lồng rồi lắp lại và chạy thử.'),
('DV202', 2, 'Vệ sinh máy giặt (cửa trên trên 9kg, tháo lồng)', 'TheoLan', 'Máy', NULL, 120, 1, 550000,
 'Vệ sinh tháo lồng 1 máy giặt cửa trên từ 9kg trở lên, 1 thợ làm trong khoảng 2 giờ: tháo rời lồng giặt cỡ lớn, chà sạch rêu mốc mặt ngoài lồng và đáy thùng rồi lắp lại, chạy thử.'),
('DV202', 2, 'Vệ sinh máy giặt (cửa trước dưới 9kg, tháo lồng)', 'TheoLan', 'Máy', NULL, 150, 2, 650000,
 'Vệ sinh tháo lồng 1 máy giặt cửa trước dưới 9kg, 2 thợ làm trong khoảng 2 giờ 30 phút (máy cửa trước phải tháo vỏ, cụm giảm chấn và trục lồng) để làm sạch triệt để rêu mốc phía sau lồng.'),
('DV202', 2, 'Vệ sinh máy giặt (cửa trước trên 9kg, tháo lồng)', 'TheoLan', 'Máy', NULL, 150, 2, 750000,
 'Vệ sinh tháo lồng 1 máy giặt cửa trước từ 9kg trở lên, 2 thợ làm trong khoảng 2 giờ 30 phút: tháo vỏ, cụm giảm chấn và lồng giặt cỡ lớn để làm sạch rêu mốc, cặn bẩn rồi lắp lại, chạy thử.'),
-- DV301 Trông trẻ tại nhà
('DV301', 3, 'Trông trẻ tại nhà (2 giờ)', 'TheoLan', 'Buổi', NULL, 120, 1, 216000,
 'Một bảo mẫu trông bé tại nhà trong 2 giờ, phù hợp khi bố mẹ đi chợ, đi họp ngắn hoặc cần nghỉ ngơi một chút.'),
('DV301', 3, 'Trông trẻ tại nhà (4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 1, 338000,
 'Một bảo mẫu trông bé nửa ngày 4 giờ (sáng hoặc chiều), bao gồm 1 bữa ăn hoặc bữa phụ và giấc ngủ trưa của bé.'),
('DV301', 3, 'Trông trẻ tại nhà (5 giờ)', 'TheoLan', 'Buổi', NULL, 300, 1, 394000,
 'Một bảo mẫu trông bé trong 5 giờ, phù hợp ca chiều từ lúc bé ngủ trưa dậy đến khi bố mẹ đi làm về; gồm 1 bữa phụ và 1 bữa chính.'),
('DV301', 3, 'Trông trẻ tại nhà (6 giờ)', 'TheoLan', 'Buổi', NULL, 360, 1, 473000,
 'Một bảo mẫu trông bé trong 6 giờ, gồm 2 bữa ăn và giấc ngủ trưa, phù hợp ngày bố mẹ làm việc ngắn hoặc cuối tuần có việc riêng.'),
('DV301', 3, 'Trông trẻ tại nhà (7 giờ)', 'TheoLan', 'Buổi', NULL, 420, 1, 551000,
 'Một bảo mẫu trông bé trong 7 giờ, gần trọn ngày làm việc của bố mẹ; gồm 2 bữa ăn, 1 bữa phụ và giấc ngủ trưa.'),
('DV301', 3, 'Trông trẻ tại nhà (8 giờ)', 'TheoLan', 'Buổi', NULL, 480, 1, 630000,
 'Một bảo mẫu trông bé trọn ngày 8 giờ (thường 8h–16h), gồm các bữa ăn trong ngày và giấc ngủ trưa; bảo mẫu nghỉ giữa ca 30 phút khi bé ngủ.'),
-- DV302 Chăm sóc người cao tuổi
('DV302', 3, 'Chăm sóc người cao tuổi (theo ngày, 8 giờ)', 'TheoLan', 'Ngày', NULL, 480, 1, 605000,
 'Một điều dưỡng hoặc hộ lý chăm sóc 1 người cao tuổi tại nhà trọn ngày 8 giờ (thường 8h–16h), bao gồm 2 bữa ăn, giấc ngủ trưa và các lần uống thuốc trong ngày.'),
('DV302', 3, 'Chăm sóc người cao tuổi (Gói 4 buổi/tháng)', 'GoiThang', 'Gói 4 buổi/tháng', 4, 240, 1, 1040000,
 'Gói chăm sóc người cao tuổi tại nhà 4 buổi trong 1 tháng (khoảng 1 buổi/tuần), mỗi buổi tối đa 4 giờ, phù hợp gia đình chỉ cần người hỗ trợ vào một ngày cố định trong tuần.'),
('DV302', 3, 'Chăm sóc người cao tuổi (Gói 9 buổi/tháng)', 'GoiThang', 'Gói 9 buổi/tháng', 9, 240, 1, 2340000,
 'Gói chăm sóc người cao tuổi tại nhà 9 buổi trong 1 tháng (khoảng 2 buổi/tuần), mỗi buổi tối đa 4 giờ.'),
('DV302', 3, 'Chăm sóc người cao tuổi (Gói 4 ngày/tháng)', 'GoiThang', 'Gói 4 ngày/tháng', 4, 480, 1, 2038000,
 'Gói chăm sóc người cao tuổi tại nhà 4 ngày trong 1 tháng (1 ngày/tuần), mỗi ngày 8 giờ, giúp người nhà được nghỉ trọn một ngày cố định mỗi tuần.'),
('DV302', 3, 'Chăm sóc người cao tuổi (Gói 9 ngày/tháng)', 'GoiThang', 'Gói 9 ngày/tháng', 9, 480, 1, 4586000,
 'Gói chăm sóc người cao tuổi tại nhà 9 ngày trong 1 tháng (khoảng 2 ngày/tuần), mỗi ngày 8 giờ.'),
('DV302', 3, 'Chăm sóc người cao tuổi (Gói 13 ngày/tháng)', 'GoiThang', 'Gói 13 ngày/tháng', 13, 480, 1, 6625000,
 'Gói chăm sóc người cao tuổi tại nhà 13 ngày trong 1 tháng (khoảng 3 ngày/tuần), mỗi ngày 8 giờ, phù hợp người cao tuổi cần theo dõi thường xuyên.'),
-- DV401 Giặt sấy
('DV401', 4, 'Giặt sấy (chăn, mền, mùng, drap)', 'TheoLan', 'Kg', NULL, NULL, 1, 35000,
 'Giặt sấy chăn, mền, mùng, drap tính theo kg bằng máy giặt cỡ lớn 15–20kg, nước giặt dịu nhẹ, sấy ở nhiệt độ vừa để vải không co rút.'),
('DV401', 4, 'Giặt sấy (topper – tấm phủ nệm)', 'TheoLan', 'Kg', NULL, NULL, 1, 55000,
 'Giặt sấy topper (tấm phủ nệm) tính theo kg: hút bụi, xử lý vết bẩn, giặt máy cỡ lớn chế độ nhẹ và sấy nhiệt thấp nhiều đợt để ruột bông khô đều, không vón cục.'),
('DV401', 4, 'Giặt sấy (gối)', 'TheoLan', 'Cái', NULL, NULL, 1, 100000,
 'Giặt sấy gối ngủ, gối ôm tính theo cái: giặt cả vỏ và ruột gối (bông, lông vũ), sấy kèm bóng sấy để ruột gối phồng trở lại.'),
-- DV402 Nấu ăn gia đình
('DV402', 4, 'Nấu ăn gia đình (2–4 người, 4 món)', 'TheoLan', 'Bữa', NULL, 150, 1, 188000,
 'Một người nấu ăn đến tận nhà nấu 1 bữa cho 2–4 người gồm 4 món (thêm 1 món mặn hoặc tráng miệng so với gói 2–3 món), làm trong khoảng 2 giờ 30 phút.'),
('DV402', 4, 'Nấu ăn gia đình (5–8 người, 2–3 món)', 'TheoLan', 'Bữa', NULL, 150, 1, 225000,
 'Một người nấu ăn đến tận nhà nấu 1 bữa cho 5–8 người gồm 2–3 món với khẩu phần lớn, làm trong khoảng 2 giờ 30 phút, phù hợp gia đình nhiều thế hệ.'),
('DV402', 4, 'Nấu ăn gia đình (5–8 người, 4 món)', 'TheoLan', 'Bữa', NULL, 180, 1, 263000,
 'Một người nấu ăn đến tận nhà nấu 1 bữa cho 5–8 người gồm 4 món, làm trong khoảng 3 giờ, phù hợp bữa cơm cuối tuần, giỗ nhỏ hoặc tiếp khách.'),
-- DV501 Vệ sinh văn phòng
('DV501', 5, 'Vệ sinh văn phòng (tối đa 150m², 1 người/3 giờ)', 'TheoLan', 'Buổi', NULL, 180, 1, 270000,
 'Vệ sinh văn phòng 100–150m² (khoảng 15–20 chỗ ngồi) với 1 nhân viên trong 3 giờ.'),
('DV501', 5, 'Vệ sinh văn phòng (tối đa 200m², 1 người/4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 1, 335000,
 'Vệ sinh văn phòng 150–200m² (khoảng 20–30 chỗ ngồi, có phòng họp nhỏ) với 1 nhân viên trong 4 giờ.'),
('DV501', 5, 'Vệ sinh văn phòng (tối đa 250m², 1 người/5 giờ)', 'TheoLan', 'Buổi', NULL, 300, 1, 419000,
 'Vệ sinh văn phòng 200–250m² (khoảng 30–35 chỗ ngồi) với 1 nhân viên trong 5 giờ.'),
('DV501', 5, 'Vệ sinh văn phòng (tối đa 300m², 2 người/3 giờ)', 'TheoLan', 'Buổi', NULL, 180, 2, 540000,
 'Vệ sinh văn phòng 250–300m² (khoảng 35–45 chỗ ngồi) với 2 nhân viên chia khu vực, làm trong 3 giờ.'),
('DV501', 5, 'Vệ sinh văn phòng (tối đa 400m², 2 người/4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 2, 670000,
 'Vệ sinh văn phòng 300–400m² (khoảng 45–60 chỗ ngồi, nhiều phòng họp) với 2 nhân viên làm trong 4 giờ.'),
('DV501', 5, 'Vệ sinh văn phòng (tối đa 500m², 2 người/5 giờ)', 'TheoLan', 'Buổi', NULL, 300, 2, 837000,
 'Vệ sinh văn phòng 400–500m² (khoảng 60–80 chỗ ngồi) với 2 nhân viên làm trong 5 giờ.'),
('DV501', 5, 'Vệ sinh văn phòng (tối đa 750m², 3 người/5 giờ)', 'TheoLan', 'Buổi', NULL, 300, 3, 1256000,
 'Vệ sinh văn phòng 500–750m² (nguyên sàn, khoảng 80–120 chỗ ngồi) với 3 nhân viên làm trong 5 giờ.'),
('DV501', 5, 'Vệ sinh văn phòng (tối đa 900m², 3 người/6 giờ)', 'TheoLan', 'Buổi', NULL, 360, 3, 1561000,
 'Vệ sinh văn phòng 750–900m² (khoảng 120–150 chỗ ngồi) với 3 nhân viên làm trong 6 giờ.'),
-- DV502 Vệ sinh thảm văn phòng
('DV502', 5, 'Vệ sinh thảm văn phòng (51–75m²)', 'TheoLan', 'Lần', NULL, 210, 2, 1100000,
 'Giặt thảm trải sàn văn phòng 51–75m² (văn phòng 10–20 chỗ ngồi hoặc phòng họp lớn), 2 nhân viên làm trong khoảng 3 giờ 30 phút.'),
('DV502', 5, 'Vệ sinh thảm văn phòng (76–100m²)', 'TheoLan', 'Lần', NULL, 240, 2, 1200000,
 'Giặt thảm trải sàn văn phòng 76–100m² (văn phòng mở 20–30 chỗ ngồi), 2 nhân viên làm trong khoảng 4 giờ.');

-- Bỏ qua biến thể đã có (chạy lại không tạo trùng)
UPDATE tmp_bt t JOIN DichVu d ON d.TenDichVu = t.ten SET t.id = d.id;

-- ----------------------------------------------------------------------------
-- 4. Thêm biến thể (mã tạm để lấy id, rồi gán mã nhóm)
-- ----------------------------------------------------------------------------
INSERT INTO DichVu (MaDichVu, loaiDichVuId, TenDichVu, MoTaChiTiet, HinhAnh, ThoiGianThucHien, LoaiHinhDat,
                    DonViTinh, TrangThai, SoBuoi, SoNguoiThucHien, GiaHienTai)
SELECT CONCAT('TMP-', t.stt), t.loai, t.ten, CONCAT(t.gioiThieu, ' ', h.than),
       (SELECT MIN(d.HinhAnh) FROM DichVu d WHERE d.MaDichVu = t.ma),
       t.tg, t.loaiHinh, t.donVi, 'HoatDong', t.soBuoi, t.soNguoi, t.gia
FROM tmp_bt t
JOIN tmp_than h ON h.ma = t.ma AND h.loaiHinh = t.loaiHinh
WHERE t.id IS NULL;

UPDATE tmp_bt t JOIN DichVu d ON d.MaDichVu = CONCAT('TMP-', t.stt) SET t.id = d.id WHERE t.id IS NULL;
UPDATE DichVu d JOIN tmp_bt t ON t.id = d.id SET d.MaDichVu = t.ma WHERE d.MaDichVu LIKE 'TMP-%';

-- Bảng giá đang áp dụng toàn quốc cho biến thể chưa có bảng giá
INSERT INTO BangGiaDichVu (MaBangGia, dichVuId, khuVucId, LoaiHinhDat, DonViTinh, DonGia, NgayApDung, TrangThai, NguoiCapNhat)
SELECT CONCAT('BG-', t.ma, '-', LPAD(t.id, 3, '0')), t.id, NULL, t.loaiHinh, t.donVi, t.gia, CURDATE(), 'DangApDung', 'Khôi phục biến thể dịch vụ'
FROM tmp_bt t
WHERE t.id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM BangGiaDichVu b WHERE b.dichVuId = t.id);

-- CTV đã đăng ký một dịch vụ trong nhóm thì nhận mọi biến thể cùng mã
INSERT INTO DichVuCTV (MaDichVuCTV, congTacVienId, dichVuId, TrangThai, NgayBatDau)
SELECT CONCAT('DVCTV-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 10))), x.congTacVienId, x.dichVuId, 'HoatDong', CURDATE()
FROM (SELECT DISTINCT c.congTacVienId, d2.id AS dichVuId
      FROM DichVuCTV c
      JOIN DichVu d1 ON d1.id = c.dichVuId
      JOIN DichVu d2 ON d2.MaDichVu = d1.MaDichVu) x
WHERE NOT EXISTS (SELECT 1 FROM DichVuCTV e WHERE e.congTacVienId = x.congTacVienId AND e.dichVuId = x.dichVuId);

-- ----------------------------------------------------------------------------
-- 5. Kiểm tra kết quả
-- ----------------------------------------------------------------------------
SELECT l.MaLoaiDichVu, d.MaDichVu,
       MIN(SUBSTRING_INDEX(d.TenDichVu, ' (', 1)) AS tenNhom,
       SUM(d.LoaiHinhDat = 'TheoLan')  AS soTheoLan,
       SUM(d.LoaiHinhDat = 'GoiThang') AS soGoiThang
FROM DichVu d JOIN LoaiDichVu l ON l.id = d.loaiDichVuId
GROUP BY l.MaLoaiDichVu, d.MaDichVu ORDER BY l.MaLoaiDichVu, d.MaDichVu;

-- Mong đợi 0: mô tả trùng, dịch vụ thiếu bảng giá
SELECT COUNT(*) AS soMoTaTrung
FROM (SELECT MoTaChiTiet FROM DichVu GROUP BY MoTaChiTiet HAVING COUNT(*) > 1) t;
SELECT COUNT(*) AS soDichVuThieuBangGia
FROM DichVu d WHERE NOT EXISTS (SELECT 1 FROM BangGiaDichVu b WHERE b.dichVuId = d.id);

DROP TEMPORARY TABLE IF EXISTS tmp_bt;
DROP TEMPORARY TABLE IF EXISTS tmp_than;
DROP TEMPORARY TABLE IF EXISTS tmp_anh_xa_bg;
DROP TEMPORARY TABLE IF EXISTS tmp_kiem_tra;
DROP TEMPORARY TABLE IF EXISTS tmp_anh_xa;
DROP TEMPORARY TABLE IF EXISTS tmp_thay_the;
DROP TEMPORARY TABLE IF EXISTS tmp_nhom_xoa;

COMMIT;
SET SQL_SAFE_UPDATES = @safe_updates_cu;
