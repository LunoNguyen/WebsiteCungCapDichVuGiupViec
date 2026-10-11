-- ============================================================================
-- CHUẨN HOÁ DANH MỤC DỊCH VỤ
--   * Mỗi loại dịch vụ giữ 3 dịch vụ (gộp các biến thể cùng tên: "2 giờ / 3 giờ / 4 giờ",
--     "sofa 1 ghế / 2 ghế / 3 ghế"... thành một dịch vụ).
--   * Mỗi dịch vụ có 1 dòng đặt theo lần + 1 gói tháng, cùng tên và cùng mã gốc:
--       DV101   = Dọn dẹp nhà theo giờ (theo lần)
--       DV101-T = Dọn dẹp nhà theo giờ (gói tháng)
--     (Cột DichVu.MaDichVu là UNIQUE nên gói tháng mang hậu tố -T.)
--   * Viết mô tả chi tiết cho từng dịch vụ, cập nhật bảng giá tương ứng.
--   * Không đổi cấu trúc bảng. Đơn hàng cũ, cộng tác viên đã đăng ký dịch vụ bị gộp
--     được chuyển sang dịch vụ còn giữ cùng nhóm.
--
-- Cách chạy (nên sao lưu trước):
--   docker exec mysql mysqldump -uroot -p GiupViecDB > backup_GiupViecDB.sql
--   docker exec -i mysql mysql -uroot -p --default-character-set=utf8mb4 GiupViecDB < ChuanHoaDichVu.sql
-- Sau khi chạy: xoá cache Redis để website hiện danh mục mới
--   docker exec redis sh -c "redis-cli --scan --pattern 'neatify:*' | xargs -r redis-cli del"
-- ============================================================================

SET NAMES utf8mb4;
-- MySQL Workbench bật "Safe Updates" mặc định, chặn UPDATE/DELETE theo JOIN (Error 1175).
-- Tắt cho phiên chạy script này, bật lại ở cuối.
SET @safe_updates_cu = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;
START TRANSACTION;

-- ----------------------------------------------------------------------------
-- 1. Danh sách dịch vụ sau chuẩn hoá. id = dịch vụ cũ được giữ lại, NULL = tạo mới.
--    mau = biểu thức nhận diện các dịch vụ cũ cùng nhóm (để chuyển đơn hàng / CTV sang).
-- ----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_dv;
CREATE TEMPORARY TABLE tmp_dv (
    id        INT NULL,
    ma        VARCHAR(20)  NOT NULL PRIMARY KEY,
    loai      INT          NOT NULL,
    ten       VARCHAR(200) NOT NULL,
    moTa      TEXT         NOT NULL,
    loaiHinh  VARCHAR(20)  NOT NULL,
    donVi     VARCHAR(30)  NOT NULL,
    soBuoi    INT NULL,
    tg        INT NULL,
    soNguoi   INT NULL,
    gia       DECIMAL(12,0) NOT NULL,
    mau       VARCHAR(200) NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_dv (id, ma, loai, ten, moTa, loaiHinh, donVi, soBuoi, tg, soNguoi, gia, mau) VALUES
-- ===== 1. Dọn Dẹp, Vệ Sinh Nhà Cửa =====
(1, 'DV101', 1, 'Dọn dẹp nhà theo giờ',
 'Nhân viên dọn dẹp đến tận nhà trong 2 giờ để làm sạch các khu vực sinh hoạt hằng ngày: quét và lau sàn; lau bụi bàn ghế, kệ tủ, cửa sổ trong tầm với; vệ sinh bồn rửa, bếp nấu, mặt bếp; cọ rửa nhà vệ sinh, bồn cầu, lavabo; gom và đổ rác; sắp xếp lại đồ đạc gọn gàng. Phù hợp căn hộ dưới 55m² hoặc nhà cần dọn nhanh. Nhân viên mang theo dụng cụ cơ bản; khách có thể chuẩn bị nước lau sàn, nước rửa chén nếu muốn dùng loại riêng. Không bao gồm giặt rèm, sofa, nệm và vệ sinh trên cao quá 2m.',
 'TheoLan', 'Buổi (2 giờ)', NULL, 120, 1, 216000, 'Dọn dẹp nhà'),
(NULL, 'DV101-T', 1, 'Dọn dẹp nhà theo giờ',
 'Gói dọn dẹp định kỳ 8 buổi trong 1 tháng (2 buổi/tuần), mỗi buổi 2 giờ với cùng nội dung như dọn theo lần: lau sàn, lau bụi, vệ sinh bếp, nhà vệ sinh, đổ rác, sắp xếp đồ đạc. Khách chọn các thứ cố định trong tuần, hệ thống xếp lịch và ưu tiên cùng một cộng tác viên để quen nếp nhà. Buổi bị huỷ khi khách báo trước 24 giờ được dời sang ngày khác trong tháng. Tiết kiệm khoảng 8% so với đặt lẻ từng buổi.',
 'GoiThang', 'Gói 8 buổi/tháng', 8, 120, 1, 1590000, NULL),

(6, 'DV102', 1, 'Tổng vệ sinh nhà',
 'Đội 3 nhân viên tổng vệ sinh toàn bộ ngôi nhà đến 100m² trong khoảng 3 giờ, dùng máy hút bụi công nghiệp và hoá chất chuyên dụng an toàn cho gia đình. Bao gồm: hút bụi và lau toàn bộ sàn, chân tường; lau cửa kính, khung cửa, cửa sổ hai mặt (trong tầm với an toàn); vệ sinh bếp, tẩy dầu mỡ máy hút mùi, mặt bếp, mặt ngoài tủ bếp; tẩy cặn vòi sen, gạch ốp, bồn cầu; lau công tắc, tay nắm cửa, quạt; thu gom rác. Thích hợp trước Tết, sau sửa chữa nhỏ hoặc khi chuyển vào nhà mới. Nhà trên 100m² được báo giá theo diện tích thực tế.',
 'TheoLan', 'Lần (tối đa 100m²)', NULL, 180, 3, 990000, 'Tổng vệ sinh|Vệ sinh công nghiệp|Chuyển nhà'),
(NULL, 'DV102-T', 1, 'Tổng vệ sinh nhà',
 'Gói tổng vệ sinh 2 lần mỗi tháng (cách nhau khoảng 2 tuần) cho nhà đến 100m². Mỗi lần 3 nhân viên làm trong 3 giờ với đầy đủ hạng mục của tổng vệ sinh: sàn, kính, bếp, nhà vệ sinh, công tắc, quạt. Giữ nhà sạch sâu đều đặn mà không cần đặt lại mỗi lần. Tiết kiệm khoảng 7% so với đặt lẻ.',
 'GoiThang', 'Gói 2 lần/tháng', 2, 180, 3, 1850000, NULL),

(21, 'DV103', 1, 'Vệ sinh sofa, nệm, thảm',
 'Giặt sạch sofa nỉ/vải, nệm và thảm tại nhà bằng máy phun hút chuyên dụng: hút bụi khô, xử lý vết bẩn cục bộ, phun dung dịch làm sạch, hút lại nước bẩn, khử mùi và diệt khuẩn bằng hơi nước nóng. Đơn giá tính cho 1 bộ sofa 3 chỗ; nệm, thảm, rèm hoặc ghế thêm được báo giá theo kích thước khi nhân viên khảo sát. Sofa khô hoàn toàn sau 4–6 giờ trong điều kiện thoáng gió. Sofa da được vệ sinh và dưỡng da bằng dung dịch riêng.',
 'TheoLan', 'Bộ (sofa 3 chỗ)', NULL, 120, 2, 600000, 'sofa|nệm|thảm|rèm|ghế|đôn'),
(NULL, 'DV103-T', 1, 'Vệ sinh sofa, nệm, thảm',
 'Gói bảo dưỡng định kỳ mỗi tháng 1 lần cho 1 bộ sofa 3 chỗ (hoặc nệm, thảm tương đương), giúp hạn chế bụi mịn, mạt bụi và mùi hôi tích tụ. Phù hợp gia đình có trẻ nhỏ, thú cưng hoặc người bị dị ứng. Lịch cố định theo ngày khách chọn; giá rẻ hơn 10% so với đặt lẻ.',
 'GoiThang', 'Gói 1 lần/tháng', 1, 120, 2, 540000, NULL),

-- ===== 2. Vệ Sinh Điện Máy =====
(51, 'DV201', 2, 'Vệ sinh máy lạnh treo tường',
 'Vệ sinh máy lạnh treo tường 1–2.5 HP bằng bơm áp lực: tháo và rửa lưới lọc, mặt nạ; xịt rửa dàn lạnh, máng nước, quạt lồng sóc; vệ sinh dàn nóng; thông ống thoát nước để máy không chảy nước; kiểm tra gas, dòng điện và nhiệt độ gió sau khi vệ sinh. Thợ dùng bạt hứng nước, không làm bẩn tường và đồ đạc. Nên vệ sinh 3–6 tháng/lần để máy lạnh sâu và tiết kiệm điện. Bơm gas, máy âm trần, tủ đứng được báo giá thêm.',
 'TheoLan', 'Máy', NULL, 45, 1, 216000, 'máy lạnh'),
(NULL, 'DV201-T', 2, 'Vệ sinh máy lạnh treo tường',
 'Gói bảo dưỡng máy lạnh định kỳ hằng tháng cho 1 máy treo tường: vệ sinh lưới lọc, dàn lạnh, kiểm tra gas và đường thoát nước mỗi tháng. Phù hợp phòng chạy máy lạnh liên tục như phòng ngủ trẻ nhỏ, cửa hàng, phòng máy chủ. Phát hiện sớm hư hỏng trước khi máy ngừng chạy; giảm 10% so với đặt lẻ.',
 'GoiThang', 'Gói 1 lần/tháng', 1, 45, 1, 195000, NULL),

(63, 'DV202', 2, 'Vệ sinh máy giặt cửa trước',
 'Vệ sinh máy giặt cửa trước dưới 9kg không tháo lồng: chạy chu trình nước nóng với dung dịch tẩy cặn chuyên dụng, vệ sinh gioăng cao su cửa, ngăn chứa bột giặt, bộ lọc xả và ống thoát; lau sạch vỏ máy. Loại bỏ cặn bột giặt, rêu mốc gây mùi hôi và vết đen trên quần áo. Máy trên 9kg, máy cửa trên hoặc cần tháo lồng giặt sẽ được báo giá thêm khi kiểm tra.',
 'TheoLan', 'Máy (dưới 9kg)', NULL, 90, 1, 350000, 'máy giặt'),
(NULL, 'DV202-T', 2, 'Vệ sinh máy giặt cửa trước',
 'Gói vệ sinh máy giặt cửa trước hằng tháng, giữ lồng giặt luôn sạch và không mùi. Phù hợp gia đình giặt nhiều, có trẻ sơ sinh hoặc người có làn da nhạy cảm. Mỗi tháng 1 lần với đầy đủ hạng mục vệ sinh không tháo lồng; giảm 10% so với đặt lẻ.',
 'GoiThang', 'Gói 1 lần/tháng', 1, 90, 1, 315000, NULL),

(69, 'DV203', 2, 'Vệ sinh bình nóng lạnh',
 'Vệ sinh bình nóng lạnh dưới 20 lít: ngắt điện, xả cạn nước, súc rửa cặn vôi trong ruột bình, kiểm tra và vệ sinh thanh magie chống ăn mòn, kiểm tra rơ le nhiệt, van an toàn và dây tiếp đất. Giúp nước nóng nhanh hơn, tiết kiệm điện và giảm nguy cơ rò điện. Bình từ 20 lít trở lên được báo giá thêm; thay thanh magie (nếu cần) tính theo giá linh kiện.',
 'TheoLan', 'Máy (dưới 20 lít)', NULL, 60, 1, 300000, 'bình nóng lạnh'),
(NULL, 'DV203-T', 2, 'Vệ sinh bình nóng lạnh',
 'Gói kiểm tra và vệ sinh bình nóng lạnh hằng tháng, phù hợp khu vực nước nhiễm đá vôi hoặc nhà có người già, trẻ nhỏ cần đảm bảo an toàn điện. Mỗi tháng 1 lần với đầy đủ hạng mục vệ sinh và kiểm tra an toàn; giảm 10% so với đặt lẻ.',
 'GoiThang', 'Gói 1 lần/tháng', 1, 60, 1, 270000, NULL),

-- ===== 3. Chăm Sóc Sức Khỏe Và Sắc Đẹp =====
(73, 'DV301', 3, 'Trông trẻ tại nhà',
 'Bảo mẫu có kinh nghiệm trông trẻ từ 1 tuổi trở lên tại nhà trong 3 giờ: cho bé ăn, uống theo hướng dẫn của phụ huynh; chơi cùng bé, đọc truyện, hướng dẫn hoạt động phù hợp độ tuổi; vệ sinh cá nhân, thay tã và quần áo; dọn dẹp khu vực bé chơi. Bảo mẫu đã được kiểm tra lý lịch và có kiến thức sơ cứu cơ bản. Không bao gồm đưa đón bé ra ngoài và nấu ăn cho cả gia đình.',
 'TheoLan', 'Buổi (3 giờ)', NULL, 180, 1, 270000, 'Trông trẻ'),
(NULL, 'DV301-T', 3, 'Trông trẻ tại nhà',
 'Gói trông trẻ 12 buổi mỗi tháng (3 buổi/tuần), mỗi buổi 3 giờ, ưu tiên cố định một bảo mẫu để bé quen người và nếp sinh hoạt. Phụ huynh chọn các thứ trong tuần; bảo mẫu ghi chú tình hình ăn, ngủ, chơi của bé sau mỗi buổi. Tiết kiệm khoảng 8% so với đặt lẻ.',
 'GoiThang', 'Gói 12 buổi/tháng', 12, 180, 1, 2980000, NULL),

(79, 'DV302', 3, 'Chăm sóc người cao tuổi',
 'Điều dưỡng hoặc hộ lý chăm sóc người cao tuổi tại nhà, mỗi buổi tối đa 4 giờ: hỗ trợ ăn uống, nhắc và cho uống thuốc theo đơn; vệ sinh cá nhân, thay quần áo; hỗ trợ đi lại, vận động nhẹ, xoa bóp; đo huyết áp, đường huyết (nếu gia đình có máy) và ghi lại; trò chuyện, đồng hành cùng người cao tuổi. Không thực hiện thủ thuật y tế xâm lấn như tiêm, truyền dịch.',
 'TheoLan', 'Buổi (4 giờ)', NULL, 240, 1, 273000, 'người cao tuổi'),
(83, 'DV302-T', 3, 'Chăm sóc người cao tuổi',
 'Gói chăm sóc người cao tuổi 13 buổi mỗi tháng (khoảng 3 buổi/tuần), mỗi buổi tối đa 4 giờ với đầy đủ nội dung chăm sóc như đặt theo buổi. Ưu tiên cố định một người chăm sóc để người cao tuổi yên tâm; gia đình nhận nhật ký sức khoẻ sau mỗi buổi. Tiết kiệm khoảng 5% so với đặt lẻ.',
 'GoiThang', 'Gói 13 buổi/tháng', 13, 240, 1, 3380000, NULL),

(87, 'DV303', 3, 'Chăm sóc người bệnh tại bệnh viện',
 'Hộ lý chăm sóc người bệnh nằm viện mỗi buổi tối đa 4 giờ: hỗ trợ ăn uống, vệ sinh cá nhân, thay tã, xoay trở chống loét; theo dõi dịch truyền và báo điều dưỡng khi cần; đi lấy kết quả xét nghiệm, mua đồ dùng cá nhân; trò chuyện, động viên người bệnh. Thực hiện theo nội quy bệnh viện và chỉ định của nhân viên y tế.',
 'TheoLan', 'Buổi (4 giờ)', NULL, 240, 1, 294000, 'người bệnh'),
(91, 'DV303-T', 3, 'Chăm sóc người bệnh tại bệnh viện',
 'Gói chăm sóc người bệnh tại bệnh viện 13 buổi mỗi tháng, mỗi buổi tối đa 4 giờ, phù hợp người điều trị dài ngày cần người túc trực luân phiên với gia đình. Ưu tiên cố định một hộ lý; lịch theo các thứ gia đình chọn. Tiết kiệm khoảng 7% so với đặt lẻ.',
 'GoiThang', 'Gói 13 buổi/tháng', 13, 240, 1, 3531000, NULL),

-- ===== 4. Dịch Vụ Tiện Ích Nâng Cao =====
(98, 'DV401', 4, 'Giặt sấy quần áo',
 'Nhận giặt sấy quần áo hằng ngày tính theo kg (tối thiểu 3kg mỗi lần): phân loại đồ trắng và đồ màu, giặt máy với nước giặt, nước xả dịu nhẹ, sấy khô, gấp gọn và đóng túi. Nhân viên nhận và trả đồ tận nhà trong 24 giờ. Đồ len, lụa, vest, chăn mền, đồ cần giặt hấp hoặc tẩy vết bẩn đặc biệt tính phí riêng.',
 'TheoLan', 'Kg', NULL, NULL, 1, 17000, 'Giặt|giặt|Ủi|Tẩy'),
(NULL, 'DV401-T', 4, 'Giặt sấy quần áo',
 'Gói giặt sấy 4 lần mỗi tháng (1 lần/tuần), mỗi lần tối đa 5kg, đã gồm nhận và trả tận nhà. Phù hợp người đi làm bận rộn, sinh viên, gia đình nhỏ. Phần vượt 5kg tính theo đơn giá giặt sấy lẻ. Tiết kiệm khoảng 9% so với đặt từng lần.',
 'GoiThang', 'Gói 4 lần/tháng (≤5kg/lần)', 4, NULL, 1, 310000, NULL),

(123, 'DV402', 4, 'Nấu ăn gia đình',
 'Người nấu ăn đến tận nhà nấu một bữa cơm cho gia đình 2–4 người, gồm 2–3 món (món mặn, món rau hoặc xào, canh) theo thực đơn khách chọn hoặc người nấu gợi ý. Thời gian khoảng 2 giờ gồm sơ chế, nấu, bày món và rửa dọn bếp sau khi nấu. Chi phí nguyên liệu do khách hàng chi trả (khách tự chuẩn bị hoặc đặt thêm dịch vụ đi chợ hộ).',
 'TheoLan', 'Bữa (2–4 người)', NULL, 120, 1, 150000, 'Nấu ăn'),
(NULL, 'DV402-T', 4, 'Nấu ăn gia đình',
 'Gói nấu ăn 12 bữa mỗi tháng (3 bữa/tuần) cho gia đình 2–4 người, mỗi bữa 2–3 món. Ưu tiên cố định người nấu để nắm khẩu vị gia đình; thực đơn được lên trước theo tuần. Chưa gồm tiền nguyên liệu. Tiết kiệm khoảng 8% so với đặt lẻ.',
 'GoiThang', 'Gói 12 bữa/tháng', 12, 120, 1, 1650000, NULL),

(127, 'DV403', 4, 'Đi chợ hộ',
 'Nhân viên đi chợ hoặc siêu thị mua thực phẩm, đồ dùng theo danh sách khách gửi: chọn hàng tươi, kiểm tra hạn dùng, giữ hoá đơn và giao tận nhà trong khoảng 1 giờ. Tiền hàng thanh toán theo hoá đơn thực tế; đơn hàng trên 2 triệu đồng cần tạm ứng trước.',
 'TheoLan', 'Lần (tối đa 1 giờ)', NULL, 60, 1, 80000, 'chợ'),
(NULL, 'DV403-T', 4, 'Đi chợ hộ',
 'Gói đi chợ hộ 8 lần mỗi tháng (2 lần/tuần) theo lịch cố định, phù hợp gia đình bận rộn, người cao tuổi hoặc kết hợp với gói nấu ăn gia đình. Chưa gồm tiền hàng. Tiết kiệm khoảng 6% so với đặt lẻ.',
 'GoiThang', 'Gói 8 lần/tháng', 8, 60, 1, 600000, NULL),

-- ===== 5. Dịch Vụ Dành Cho Doanh Nghiệp =====
(129, 'DV501', 5, 'Vệ sinh văn phòng',
 'Vệ sinh văn phòng đến 100m² trong 2 giờ, có thể làm ngoài giờ hành chính: hút bụi, lau sàn; lau bàn làm việc, ghế, tủ hồ sơ (không xáo trộn giấy tờ); vệ sinh pantry, bồn rửa, lò vi sóng; cọ rửa nhà vệ sinh; lau cửa kính, tay nắm cửa; đổ rác và thay túi rác. Văn phòng lớn hơn được bố trí thêm nhân sự theo diện tích.',
 'TheoLan', 'Buổi (≤100m², 2 giờ)', NULL, 120, 1, 216000, 'Vệ sinh văn phòng'),
(NULL, 'DV501-T', 5, 'Vệ sinh văn phòng',
 'Gói vệ sinh văn phòng định kỳ 12 buổi mỗi tháng (3 buổi/tuần), mỗi buổi 2 giờ cho văn phòng đến 100m². Lịch cố định theo ngày doanh nghiệp chọn, có biên bản nghiệm thu hằng tháng và xuất hoá đơn VAT theo yêu cầu. Tiết kiệm khoảng 8% so với đặt lẻ.',
 'GoiThang', 'Gói 12 buổi/tháng', 12, 120, 1, 2390000, NULL),

(138, 'DV502', 5, 'Vệ sinh thảm văn phòng',
 'Giặt thảm trải sàn văn phòng dưới 50m² bằng máy giặt thảm phun hút công nghiệp: hút bụi sâu, xử lý vết cà phê, mực, kẹo cao su; giặt bằng hoá chất chuyên dụng an toàn với sợi thảm; hút khô và khử mùi. Thảm khô sau 4–6 giờ; có thể thực hiện cuối tuần để không ảnh hưởng giờ làm việc. Diện tích lớn hơn được báo giá theo m².',
 'TheoLan', 'Lần (dưới 50m²)', NULL, 180, 2, 950000, 'thảm văn phòng'),
(NULL, 'DV502-T', 5, 'Vệ sinh thảm văn phòng',
 'Gói giặt thảm văn phòng định kỳ mỗi tháng 1 lần cho diện tích dưới 50m², giữ thảm sạch bụi mịn và vi khuẩn, kéo dài tuổi thọ thảm. Lịch cố định theo ngày doanh nghiệp chọn; giảm khoảng 9% so với đặt lẻ.',
 'GoiThang', 'Gói 1 lần/tháng', 1, 180, 2, 860000, NULL),

(141, 'DV503', 5, 'Phun khử khuẩn',
 'Phun khử khuẩn văn phòng, nhà ở đến 70m² bằng máy phun ULV với dung dịch được Bộ Y tế cấp phép, diệt vi khuẩn, vi rút và nấm mốc trên bề mặt và trong không khí. Khu vực cần đóng kín khoảng 30 phút sau khi phun; dung dịch không ăn mòn thiết bị điện tử và không để lại mùi khó chịu. Diện tích lớn hơn được báo giá theo m².',
 'TheoLan', 'Lần (đến 70m²)', NULL, 60, 2, 550000, 'khử khuẩn'),
(NULL, 'DV503-T', 5, 'Phun khử khuẩn',
 'Gói phun khử khuẩn định kỳ 2 lần mỗi tháng cho diện tích đến 70m², phù hợp văn phòng đông người, phòng khám, lớp học, nhà có người bệnh. Có giấy xác nhận sau mỗi lần phun; giảm khoảng 10% so với đặt lẻ.',
 'GoiThang', 'Gói 2 lần/tháng', 2, 60, 2, 990000, NULL);

-- Nhóm dịch vụ: dòng theo lần (tlId) và gói tháng cùng mã (gtId)
DROP TEMPORARY TABLE IF EXISTS tmp_fam;
CREATE TEMPORARY TABLE tmp_fam (
    maTL VARCHAR(20) NOT NULL PRIMARY KEY,
    loai INT NOT NULL,
    mau  VARCHAR(200) NOT NULL,
    tlId INT NOT NULL,
    gtId INT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
INSERT INTO tmp_fam (maTL, loai, mau, tlId)
SELECT ma, loai, mau, id FROM tmp_dv WHERE loaiHinh = 'TheoLan';

-- ----------------------------------------------------------------------------
-- 2. Các dịch vụ cũ sẽ bị gộp/xoá và dịch vụ còn giữ nhận phần của chúng
-- ----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_map;
CREATE TEMPORARY TABLE tmp_map (
    oldId INT NOT NULL PRIMARY KEY,
    loai  INT NOT NULL,
    ten   VARCHAR(200) NOT NULL,
    newId INT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
INSERT INTO tmp_map (oldId, loai, ten)
SELECT d.id, d.loaiDichVuId, d.TenDichVu
FROM DichVu d
WHERE d.id NOT IN (SELECT id FROM tmp_dv WHERE id IS NOT NULL);

UPDATE tmp_map m
JOIN tmp_fam f ON f.loai = m.loai AND m.ten REGEXP f.mau
SET m.newId = f.tlId;

-- ----------------------------------------------------------------------------
-- 3. Chuyển tham chiếu sang dịch vụ còn giữ
-- ----------------------------------------------------------------------------
-- Đơn hàng: dịch vụ không thuộc nhóm nào thì chuyển sang nhóm đầu tiên cùng loại
UPDATE ChiTietDonDat c
JOIN tmp_map m ON m.oldId = c.dichVuId
SET c.dichVuId = COALESCE(m.newId,
        (SELECT f.tlId FROM tmp_fam f WHERE f.loai = m.loai ORDER BY f.maTL LIMIT 1)),
    c.bangGiaId = NULL;

-- Cộng tác viên đã đăng ký dịch vụ bị gộp -> đăng ký dịch vụ cùng nhóm (bỏ qua nếu đã có)
INSERT INTO DichVuCTV (MaDichVuCTV, congTacVienId, dichVuId, TrangThai, NgayBatDau)
SELECT CONCAT('DVCTV-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 10))), x.congTacVienId, x.newId, 'HoatDong', CURDATE()
FROM (SELECT DISTINCT c.congTacVienId, m.newId
      FROM DichVuCTV c JOIN tmp_map m ON m.oldId = c.dichVuId
      WHERE m.newId IS NOT NULL) x
WHERE NOT EXISTS (SELECT 1 FROM DichVuCTV e WHERE e.congTacVienId = x.congTacVienId AND e.dichVuId = x.newId);

DELETE FROM DichVuCTV    WHERE dichVuId IN (SELECT oldId FROM tmp_map);
DELETE FROM BangGiaDichVu WHERE dichVuId IN (SELECT oldId FROM tmp_map);
DELETE FROM DichVu        WHERE id       IN (SELECT oldId FROM tmp_map);

-- ----------------------------------------------------------------------------
-- 4. Cập nhật dịch vụ giữ lại, thêm gói tháng mới
-- ----------------------------------------------------------------------------
UPDATE DichVu d
JOIN tmp_dv t ON t.id = d.id
SET d.MaDichVu = t.ma,
    d.loaiDichVuId = t.loai,
    d.TenDichVu = t.ten,
    d.MoTaChiTiet = t.moTa,
    d.LoaiHinhDat = t.loaiHinh,
    d.DonViTinh = t.donVi,
    d.SoBuoi = t.soBuoi,
    d.ThoiGianThucHien = t.tg,
    d.SoNguoiThucHien = t.soNguoi,
    d.GiaHienTai = t.gia,
    d.TrangThai = 'HoatDong';

INSERT INTO DichVu (MaDichVu, loaiDichVuId, TenDichVu, MoTaChiTiet, ThoiGianThucHien, LoaiHinhDat,
                    DonViTinh, TrangThai, SoBuoi, SoNguoiThucHien, GiaHienTai)
SELECT ma, loai, ten, moTa, tg, loaiHinh, donVi, 'HoatDong', soBuoi, soNguoi, gia
FROM tmp_dv WHERE id IS NULL;

UPDATE tmp_dv t JOIN DichVu d ON d.MaDichVu = t.ma SET t.id = d.id WHERE t.id IS NULL;
UPDATE tmp_fam f JOIN tmp_dv t ON t.ma = CONCAT(f.maTL, '-T') SET f.gtId = t.id;

-- ----------------------------------------------------------------------------
-- 5. Bảng giá: mỗi dịch vụ một gói đang áp dụng khớp giá mới
-- ----------------------------------------------------------------------------
-- Lưu giá cũ trước (UPDATE nhiều bảng không đảm bảo thứ tự gán)
UPDATE BangGiaDichVu b JOIN tmp_dv t ON t.id = b.dichVuId
SET b.GiaCu = b.DonGia
WHERE b.DonGia <> t.gia;

UPDATE BangGiaDichVu b JOIN tmp_dv t ON t.id = b.dichVuId
SET b.MaBangGia = CONCAT('BG-', t.ma),
    b.LoaiHinhDat = t.loaiHinh,
    b.DonViTinh = t.donVi,
    b.DonGia = t.gia,
    b.khuVucId = NULL,
    b.NgayKetThuc = NULL,
    b.TrangThai = 'DangApDung',
    b.NguoiCapNhat = 'Chuẩn hoá dữ liệu';

INSERT INTO BangGiaDichVu (MaBangGia, dichVuId, khuVucId, LoaiHinhDat, DonViTinh, DonGia, NgayApDung, TrangThai, NguoiCapNhat)
SELECT CONCAT('BG-', t.ma), t.id, NULL, t.loaiHinh, t.donVi, t.gia, CURDATE(), 'DangApDung', 'Chuẩn hoá dữ liệu'
FROM tmp_dv t
WHERE NOT EXISTS (SELECT 1 FROM BangGiaDichVu b WHERE b.dichVuId = t.id);

-- Đơn hàng trỏ lại bảng giá của dịch vụ
UPDATE ChiTietDonDat c
JOIN BangGiaDichVu b ON b.dichVuId = c.dichVuId
SET c.bangGiaId = b.id
WHERE c.bangGiaId IS NULL;

-- ----------------------------------------------------------------------------
-- 6. CTV làm dịch vụ theo lần thì nhận luôn gói tháng cùng nhóm (và ngược lại)
-- ----------------------------------------------------------------------------
INSERT INTO DichVuCTV (MaDichVuCTV, congTacVienId, dichVuId, TrangThai, NgayBatDau)
SELECT CONCAT('DVCTV-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 10))), x.congTacVienId, x.gtId, 'HoatDong', CURDATE()
FROM (SELECT DISTINCT c.congTacVienId, f.gtId FROM DichVuCTV c JOIN tmp_fam f ON f.tlId = c.dichVuId) x
WHERE NOT EXISTS (SELECT 1 FROM DichVuCTV e WHERE e.congTacVienId = x.congTacVienId AND e.dichVuId = x.gtId);

INSERT INTO DichVuCTV (MaDichVuCTV, congTacVienId, dichVuId, TrangThai, NgayBatDau)
SELECT CONCAT('DVCTV-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 10))), x.congTacVienId, x.tlId, 'HoatDong', CURDATE()
FROM (SELECT DISTINCT c.congTacVienId, f.tlId FROM DichVuCTV c JOIN tmp_fam f ON f.gtId = c.dichVuId) x
WHERE NOT EXISTS (SELECT 1 FROM DichVuCTV e WHERE e.congTacVienId = x.congTacVienId AND e.dichVuId = x.tlId);

-- ----------------------------------------------------------------------------
-- 7. Kiểm tra kết quả
-- ----------------------------------------------------------------------------
SELECT l.TenLoaiDichVu,
       SUM(d.LoaiHinhDat = 'TheoLan')  AS soDichVu,
       SUM(d.LoaiHinhDat = 'GoiThang') AS soGoiThang
FROM LoaiDichVu l LEFT JOIN DichVu d ON d.loaiDichVuId = l.id
GROUP BY l.id, l.TenLoaiDichVu ORDER BY l.ThuTuHienThi;

SELECT d.MaDichVu, d.TenDichVu, d.LoaiHinhDat, d.DonViTinh, d.SoBuoi, d.GiaHienTai, b.MaBangGia, b.DonGia,
       (SELECT COUNT(*) FROM DichVuCTV c WHERE c.dichVuId = d.id) AS soCTV
FROM DichVu d LEFT JOIN BangGiaDichVu b ON b.dichVuId = d.id
ORDER BY d.MaDichVu;

DROP TEMPORARY TABLE IF EXISTS tmp_map;
DROP TEMPORARY TABLE IF EXISTS tmp_fam;
DROP TEMPORARY TABLE IF EXISTS tmp_dv;

COMMIT;
SET SQL_SAFE_UPDATES = @safe_updates_cu;
