-- ============================================================================
-- CHIA DỊCH VỤ THÀNH CÁC BIẾN THỂ CÙNG MÃ
--   Chạy SAU ChuanHoaDichVu.sql (đang có 15 nhóm DV101..DV503, mỗi nhóm 1 dòng theo lần
--   + 1 gói tháng mã DVxxx-T).
--   Kết quả: mọi dịch vụ cùng tên dùng chung một mã nhóm, VD DV101 gồm
--     Dọn dẹp nhà theo giờ (2 giờ) / (3 giờ) / (4 giờ) / (Gói 8 buổi/tháng, 2 giờ/buổi)
--   Mỗi biến thể là một dòng DichVu riêng (giá, đơn vị, thời lượng riêng) và có bảng giá riêng.
--   Cột MaDichVu trong CSDL không có ràng buộc UNIQUE nên không phải đổi cấu trúc bảng.
--   Giá, đơn vị các biến thể lấy theo dữ liệu dịch vụ gốc trước khi chuẩn hoá.
--
-- Script tự dừng (lỗi "Column 'x' cannot be null") nếu CSDL không ở trạng thái mong đợi,
-- ví dụ đã chạy script này rồi. Nên sao lưu trước khi chạy.
-- Sau khi chạy: xoá cache Redis
--   docker exec redis sh -c "redis-cli --scan --pattern 'neatify:*' | xargs -r redis-cli del"
-- ============================================================================

SET NAMES utf8mb4;
SET @safe_updates_cu = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;
START TRANSACTION;

-- ----------------------------------------------------------------------------
-- 1. Mô tả chi tiết theo nhóm (dùng chung cho mọi biến thể; chi tiết biến thể nằm ở tên)
-- ----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_mota;
CREATE TEMPORARY TABLE tmp_mota (
    ma       VARCHAR(20) NOT NULL,
    loaiHinh VARCHAR(20) NOT NULL,
    moTa     TEXT NOT NULL,
    PRIMARY KEY (ma, loaiHinh)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_mota (ma, loaiHinh, moTa) VALUES
('DV101', 'TheoLan', 'Nhân viên dọn dẹp đến tận nhà làm sạch các khu vực sinh hoạt hằng ngày trong thời lượng khách chọn: quét và lau sàn; lau bụi bàn ghế, kệ tủ, cửa sổ trong tầm với; vệ sinh bồn rửa, bếp nấu, mặt bếp; cọ rửa nhà vệ sinh, bồn cầu, lavabo; gom và đổ rác; sắp xếp đồ đạc gọn gàng. Gợi ý: 2 giờ cho căn hộ dưới 55m², 3 giờ cho 55–85m², 4 giờ cho 85–105m². Nhân viên mang theo dụng cụ cơ bản. Không bao gồm giặt rèm, sofa, nệm và vệ sinh trên cao quá 2m.'),
('DV101', 'GoiThang', 'Gói dọn dẹp định kỳ 8 buổi trong 1 tháng (2 buổi/tuần), mỗi buổi 2 giờ với cùng nội dung như dọn theo giờ: lau sàn, lau bụi, vệ sinh bếp, nhà vệ sinh, đổ rác, sắp xếp đồ đạc. Khách chọn các thứ cố định trong tuần, hệ thống xếp lịch và ưu tiên cùng một cộng tác viên để quen nếp nhà. Buổi bị huỷ khi khách báo trước 24 giờ được dời sang ngày khác trong tháng. Tiết kiệm khoảng 8% so với đặt lẻ từng buổi.'),
('DV102', 'TheoLan', 'Đội nhân viên tổng vệ sinh toàn bộ ngôi nhà bằng máy hút bụi công nghiệp và hoá chất chuyên dụng an toàn cho gia đình; diện tích, số người và thời gian của từng gói ghi trong tên. Bao gồm: hút bụi và lau toàn bộ sàn, chân tường; lau cửa kính, khung cửa, cửa sổ hai mặt trong tầm với an toàn; tẩy dầu mỡ bếp, máy hút mùi, mặt ngoài tủ bếp; tẩy cặn vòi sen, gạch ốp, bồn cầu; lau công tắc, tay nắm cửa, quạt; thu gom rác. Thích hợp trước Tết, sau sửa chữa nhỏ hoặc khi chuyển vào nhà mới.'),
('DV102', 'GoiThang', 'Gói tổng vệ sinh 2 lần mỗi tháng (cách nhau khoảng 2 tuần) cho nhà đến 100m². Mỗi lần 3 nhân viên làm trong 3 giờ với đầy đủ hạng mục tổng vệ sinh: sàn, kính, bếp, nhà vệ sinh, công tắc, quạt. Giữ nhà sạch sâu đều đặn mà không cần đặt lại mỗi lần. Tiết kiệm khoảng 7% so với đặt lẻ.'),
('DV103', 'TheoLan', 'Giặt sạch sofa, nệm và thảm tại nhà bằng máy phun hút chuyên dụng: hút bụi khô, xử lý vết bẩn cục bộ, phun dung dịch làm sạch, hút lại nước bẩn, khử mùi và diệt khuẩn bằng hơi nước nóng. Sofa da được làm sạch và dưỡng bằng dung dịch riêng, không làm khô nứt da. Giá theo loại và kích thước ghi trong tên gói. Đồ khô hoàn toàn sau 4–6 giờ trong điều kiện thoáng gió.'),
('DV103', 'GoiThang', 'Gói bảo dưỡng định kỳ mỗi tháng 1 lần cho 1 bộ sofa nỉ/vải 3 ghế (hoặc nệm, thảm tương đương), giúp hạn chế bụi mịn, mạt bụi và mùi hôi tích tụ. Phù hợp gia đình có trẻ nhỏ, thú cưng hoặc người bị dị ứng. Lịch cố định theo ngày khách chọn; giá rẻ hơn 10% so với đặt lẻ.'),
('DV201', 'TheoLan', 'Vệ sinh máy lạnh bằng bơm áp lực: tháo và rửa lưới lọc, mặt nạ; xịt rửa dàn lạnh, máng nước, quạt; vệ sinh dàn nóng; thông ống thoát nước để máy không chảy nước; kiểm tra gas, dòng điện và nhiệt độ gió sau khi vệ sinh. Thợ dùng bạt hứng nước, không làm bẩn tường và đồ đạc. Giá theo loại máy ghi trong tên gói. Nên vệ sinh 3–6 tháng/lần để máy lạnh sâu và tiết kiệm điện.'),
('DV201', 'GoiThang', 'Gói bảo dưỡng máy lạnh treo tường hằng tháng: vệ sinh lưới lọc, dàn lạnh, kiểm tra gas và đường thoát nước mỗi tháng. Phù hợp phòng chạy máy lạnh liên tục như phòng ngủ trẻ nhỏ, cửa hàng, phòng máy chủ. Phát hiện sớm hư hỏng trước khi máy ngừng chạy; giảm 10% so với đặt lẻ.'),
('DV202', 'TheoLan', 'Vệ sinh máy giặt cửa trên hoặc cửa trước: chạy chu trình nước nóng với dung dịch tẩy cặn chuyên dụng, vệ sinh gioăng cửa, ngăn chứa bột giặt, bộ lọc xả và ống thoát, lau sạch vỏ máy. Gói tháo lồng còn tháo rời lồng giặt để chà sạch rêu mốc, cặn bẩn bám mặt ngoài lồng rồi lắp lại và chạy thử. Loại bỏ mùi hôi và vết đen trên quần áo. Giá theo loại máy, khối lượng và có tháo lồng hay không (ghi trong tên gói).'),
('DV202', 'GoiThang', 'Gói vệ sinh máy giặt cửa trước dưới 9kg hằng tháng (không tháo lồng), giữ lồng giặt luôn sạch và không mùi. Phù hợp gia đình giặt nhiều, có trẻ sơ sinh hoặc người có làn da nhạy cảm; giảm 10% so với đặt lẻ.'),
('DV203', 'TheoLan', 'Vệ sinh bình nóng lạnh: ngắt điện, xả cạn nước, súc rửa cặn vôi trong ruột bình, kiểm tra và vệ sinh thanh magie chống ăn mòn, kiểm tra rơ le nhiệt, van an toàn và dây tiếp đất. Giúp nước nóng nhanh hơn, tiết kiệm điện và giảm nguy cơ rò điện. Giá theo dung tích bình ghi trong tên gói; thay thanh magie (nếu cần) tính theo giá linh kiện.'),
('DV203', 'GoiThang', 'Gói kiểm tra và vệ sinh bình nóng lạnh dưới 20 lít hằng tháng, phù hợp khu vực nước nhiễm đá vôi hoặc nhà có người già, trẻ nhỏ cần đảm bảo an toàn điện. Mỗi tháng 1 lần với đầy đủ hạng mục vệ sinh và kiểm tra an toàn; giảm 10% so với đặt lẻ.'),
('DV301', 'TheoLan', 'Bảo mẫu có kinh nghiệm trông trẻ từ 1 tuổi trở lên tại nhà theo số giờ khách chọn: cho bé ăn uống theo hướng dẫn của phụ huynh; chơi cùng bé, đọc truyện, hướng dẫn hoạt động phù hợp độ tuổi; vệ sinh cá nhân, thay tã và quần áo; dọn dẹp khu vực bé chơi. Bảo mẫu đã được kiểm tra lý lịch và có kiến thức sơ cứu cơ bản. Không bao gồm đưa đón bé ra ngoài và nấu ăn cho cả gia đình.'),
('DV301', 'GoiThang', 'Gói trông trẻ 12 buổi mỗi tháng (3 buổi/tuần), mỗi buổi 3 giờ, ưu tiên cố định một bảo mẫu để bé quen người và nếp sinh hoạt. Phụ huynh chọn các thứ trong tuần; bảo mẫu ghi chú tình hình ăn, ngủ, chơi của bé sau mỗi buổi. Tiết kiệm khoảng 8% so với đặt lẻ.'),
('DV302', 'TheoLan', 'Điều dưỡng hoặc hộ lý chăm sóc người cao tuổi tại nhà theo buổi (tối đa 4 giờ) hoặc theo ngày (8 giờ): hỗ trợ ăn uống, nhắc và cho uống thuốc theo đơn; vệ sinh cá nhân, thay quần áo; hỗ trợ đi lại, vận động nhẹ, xoa bóp; đo huyết áp, đường huyết (nếu gia đình có máy) và ghi lại; trò chuyện, đồng hành cùng người cao tuổi. Không thực hiện thủ thuật y tế xâm lấn như tiêm, truyền dịch.'),
('DV302', 'GoiThang', 'Gói chăm sóc người cao tuổi theo tháng với số buổi (tối đa 4 giờ/buổi) hoặc số ngày (8 giờ/ngày) ghi trong tên gói, cùng nội dung chăm sóc như đặt lẻ. Ưu tiên cố định một người chăm sóc để người cao tuổi yên tâm; gia đình nhận nhật ký sức khoẻ sau mỗi buổi. Lịch theo các thứ gia đình chọn; giá thấp hơn đặt lẻ.'),
('DV303', 'TheoLan', 'Hộ lý chăm sóc người bệnh nằm viện theo buổi (tối đa 4 giờ) hoặc theo ngày (8 giờ): hỗ trợ ăn uống, vệ sinh cá nhân, thay tã, xoay trở chống loét; theo dõi dịch truyền và báo điều dưỡng khi cần; đi lấy kết quả xét nghiệm, mua đồ dùng cá nhân; trò chuyện, động viên người bệnh. Thực hiện theo nội quy bệnh viện và chỉ định của nhân viên y tế.'),
('DV303', 'GoiThang', 'Gói chăm sóc người bệnh tại bệnh viện theo tháng với số buổi (tối đa 4 giờ/buổi) hoặc số ngày (8 giờ/ngày) ghi trong tên gói, phù hợp người điều trị dài ngày cần người túc trực luân phiên với gia đình. Ưu tiên cố định một hộ lý; lịch theo các thứ gia đình chọn; giá thấp hơn đặt lẻ.'),
('DV401', 'TheoLan', 'Nhận giặt sấy tận nhà: phân loại đồ trắng và đồ màu, giặt máy với nước giặt và nước xả dịu nhẹ, sấy khô, gấp gọn và đóng túi; chăn, mền, drap, topper và gối được giặt riêng bằng máy cỡ lớn. Nhân viên nhận và trả đồ tận nhà trong 24 giờ; hàng tính theo kg cần tối thiểu 3kg mỗi lần. Đồ len, lụa, vest hoặc cần giặt hấp, tẩy vết bẩn đặc biệt tính phí riêng.'),
('DV401', 'GoiThang', 'Gói giặt sấy quần áo 4 lần mỗi tháng (1 lần/tuần), mỗi lần tối đa 5kg, đã gồm nhận và trả tận nhà. Phù hợp người đi làm bận rộn, sinh viên, gia đình nhỏ. Phần vượt 5kg tính theo đơn giá giặt sấy lẻ. Tiết kiệm khoảng 9% so với đặt từng lần.'),
('DV402', 'TheoLan', 'Người nấu ăn đến tận nhà nấu một bữa cơm gia đình theo số người và số món ghi trong tên gói (món mặn, món rau hoặc xào, canh; gói 4 món thêm một món mặn hoặc tráng miệng), theo thực đơn khách chọn hoặc người nấu gợi ý. Bao gồm sơ chế, nấu, bày món và rửa dọn bếp sau khi nấu. Chi phí nguyên liệu do khách hàng chi trả (khách tự chuẩn bị hoặc đặt thêm dịch vụ đi chợ hộ).'),
('DV402', 'GoiThang', 'Gói nấu ăn 12 bữa mỗi tháng (3 bữa/tuần) cho gia đình 2–4 người, mỗi bữa 2–3 món. Ưu tiên cố định người nấu để nắm khẩu vị gia đình; thực đơn được lên trước theo tuần. Chưa gồm tiền nguyên liệu. Tiết kiệm khoảng 8% so với đặt lẻ.'),
('DV403', 'TheoLan', 'Nhân viên đi chợ hoặc siêu thị mua thực phẩm, đồ dùng theo danh sách khách gửi: chọn hàng tươi, kiểm tra hạn dùng, giữ hoá đơn và giao tận nhà trong khoảng 1 giờ. Tiền hàng thanh toán theo hoá đơn thực tế; đơn hàng trên 2 triệu đồng cần tạm ứng trước.'),
('DV403', 'GoiThang', 'Gói đi chợ hộ 8 lần mỗi tháng (2 lần/tuần) theo lịch cố định, phù hợp gia đình bận rộn, người cao tuổi hoặc kết hợp với gói nấu ăn gia đình. Chưa gồm tiền hàng. Tiết kiệm khoảng 6% so với đặt lẻ.'),
('DV501', 'TheoLan', 'Vệ sinh văn phòng theo diện tích, số người và thời gian ghi trong tên gói, có thể làm ngoài giờ hành chính: hút bụi, lau sàn; lau bàn làm việc, ghế, tủ hồ sơ (không xáo trộn giấy tờ); vệ sinh pantry, bồn rửa, lò vi sóng; cọ rửa nhà vệ sinh; lau cửa kính, tay nắm cửa; đổ rác và thay túi rác. Có biên bản nghiệm thu và xuất hoá đơn VAT theo yêu cầu.'),
('DV501', 'GoiThang', 'Gói vệ sinh văn phòng định kỳ 12 buổi mỗi tháng (3 buổi/tuần), mỗi buổi 2 giờ cho văn phòng đến 100m². Lịch cố định theo ngày doanh nghiệp chọn, có biên bản nghiệm thu hằng tháng và xuất hoá đơn VAT theo yêu cầu. Tiết kiệm khoảng 8% so với đặt lẻ.'),
('DV502', 'TheoLan', 'Giặt thảm trải sàn văn phòng bằng máy giặt thảm phun hút công nghiệp: hút bụi sâu, xử lý vết cà phê, mực, kẹo cao su; giặt bằng hoá chất chuyên dụng an toàn với sợi thảm; hút khô và khử mùi. Thảm khô sau 4–6 giờ; có thể thực hiện cuối tuần để không ảnh hưởng giờ làm việc. Giá theo diện tích ghi trong tên gói.'),
('DV502', 'GoiThang', 'Gói giặt thảm văn phòng định kỳ mỗi tháng 1 lần cho diện tích dưới 50m², giữ thảm sạch bụi mịn và vi khuẩn, kéo dài tuổi thọ thảm. Lịch cố định theo ngày doanh nghiệp chọn; giảm khoảng 9% so với đặt lẻ.'),
('DV503', 'TheoLan', 'Phun khử khuẩn văn phòng, nhà ở bằng máy phun ULV với dung dịch được Bộ Y tế cấp phép, diệt vi khuẩn, vi rút và nấm mốc trên bề mặt và trong không khí. Khu vực cần đóng kín khoảng 30 phút sau khi phun; dung dịch không ăn mòn thiết bị điện tử và không để lại mùi khó chịu. Giá theo diện tích ghi trong tên gói.'),
('DV503', 'GoiThang', 'Gói phun khử khuẩn định kỳ 2 lần mỗi tháng cho diện tích đến 70m², phù hợp văn phòng đông người, phòng khám, lớp học, nhà có người bệnh. Có giấy xác nhận sau mỗi lần phun; giảm khoảng 10% so với đặt lẻ.');

-- ----------------------------------------------------------------------------
-- 2. Các biến thể. giuMa = mã của dòng hiện có được dùng lại (giữ id cho đơn hàng cũ),
--    NULL = thêm mới.
-- ----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_bt;
CREATE TEMPORARY TABLE tmp_bt (
    stt      INT AUTO_INCREMENT PRIMARY KEY,
    id       INT NULL,
    ma       VARCHAR(20)  NOT NULL,
    giuMa    VARCHAR(20)  NULL,
    loai     INT          NOT NULL,
    ten      VARCHAR(200) NOT NULL,
    loaiHinh VARCHAR(20)  NOT NULL,
    donVi    VARCHAR(30)  NOT NULL,
    soBuoi   INT NULL,
    tg       INT NULL,
    soNguoi  INT NULL,
    gia      DECIMAL(12,0) NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_bt (ma, giuMa, loai, ten, loaiHinh, donVi, soBuoi, tg, soNguoi, gia) VALUES
-- DV101 Dọn dẹp nhà theo giờ
('DV101', 'DV101',   1, 'Dọn dẹp nhà theo giờ (2 giờ)', 'TheoLan', 'Buổi', NULL, 120, 1, 216000),
('DV101', NULL,      1, 'Dọn dẹp nhà theo giờ (3 giờ)', 'TheoLan', 'Buổi', NULL, 180, 1, 270000),
('DV101', NULL,      1, 'Dọn dẹp nhà theo giờ (4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 1, 342000),
('DV101', 'DV101-T', 1, 'Dọn dẹp nhà theo giờ (Gói 8 buổi/tháng, 2 giờ/buổi)', 'GoiThang', 'Gói 8 buổi/tháng', 8, 120, 1, 1590000),
-- DV102 Tổng vệ sinh nhà
('DV102', NULL,      1, 'Tổng vệ sinh nhà (tối đa 60m², 2 người/3 giờ)',  'TheoLan', 'Lần', NULL, 180, 2, 660000),
('DV102', NULL,      1, 'Tổng vệ sinh nhà (tối đa 80m², 2 người/4 giờ)',  'TheoLan', 'Lần', NULL, 240, 2, 880000),
('DV102', 'DV102',   1, 'Tổng vệ sinh nhà (tối đa 100m², 3 người/3 giờ)', 'TheoLan', 'Lần', NULL, 180, 3, 990000),
('DV102', NULL,      1, 'Tổng vệ sinh nhà (tối đa 150m², 3 người/4 giờ)', 'TheoLan', 'Lần', NULL, 240, 3, 1320000),
('DV102', NULL,      1, 'Tổng vệ sinh nhà (tối đa 200m², 4 người/4 giờ)', 'TheoLan', 'Lần', NULL, 240, 4, 1760000),
('DV102', NULL,      1, 'Tổng vệ sinh nhà (tối đa 400m², 4 người/8 giờ)', 'TheoLan', 'Lần', NULL, 480, 4, 3784000),
('DV102', 'DV102-T', 1, 'Tổng vệ sinh nhà (Gói 2 lần/tháng, tối đa 100m²)', 'GoiThang', 'Gói 2 lần/tháng', 2, 180, 3, 1850000),
-- DV103 Vệ sinh sofa, nệm, thảm
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (sofa nỉ/vải đơn)',   'TheoLan', 'Ghế', NULL, 60, 1, 200000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (sofa nỉ/vải 1 ghế)', 'TheoLan', 'Ghế', NULL, 60, 1, 360000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (sofa nỉ/vải 2 ghế)', 'TheoLan', 'Bộ',  NULL, 90, 2, 450000),
('DV103', 'DV103',   1, 'Vệ sinh sofa, nệm, thảm (sofa nỉ/vải 3 ghế)', 'TheoLan', 'Bộ',  NULL, 120, 2, 600000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (sofa da đơn)',       'TheoLan', 'Ghế', NULL, 60, 1, 250000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (sofa da 1 ghế)',     'TheoLan', 'Ghế', NULL, 60, 1, 400000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (sofa da 2 ghế)',     'TheoLan', 'Bộ',  NULL, 90, 2, 500000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (sofa da 3 ghế)',     'TheoLan', 'Bộ',  NULL, 120, 2, 650000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (nệm nhỏ hơn 1.5m)',  'TheoLan', 'Cái', NULL, 60, 1, 300000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (nệm 1.5m–1.8m)',     'TheoLan', 'Cái', NULL, 90, 2, 350000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (nệm lớn hơn 2m)',    'TheoLan', 'Cái', NULL, 90, 2, 430000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (thảm nhỏ)',          'TheoLan', 'Cái', NULL, 60, 1, 300000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (thảm cỡ vừa)',       'TheoLan', 'Cái', NULL, 60, 1, 350000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (thảm cỡ lớn)',       'TheoLan', 'Cái', NULL, 90, 2, 450000),
('DV103', NULL,      1, 'Vệ sinh sofa, nệm, thảm (thảm cỡ đại)',       'TheoLan', 'Cái', NULL, 120, 2, 550000),
('DV103', 'DV103-T', 1, 'Vệ sinh sofa, nệm, thảm (Gói 1 lần/tháng, sofa nỉ/vải 3 ghế)', 'GoiThang', 'Gói 1 lần/tháng', 1, 120, 2, 540000),
-- DV201 Vệ sinh máy lạnh
('DV201', 'DV201',   2, 'Vệ sinh máy lạnh (treo tường)', 'TheoLan', 'Máy', NULL, 45, 1, 216000),
('DV201', NULL,      2, 'Vệ sinh máy lạnh (tủ đứng)',    'TheoLan', 'Máy', NULL, 60, 1, 360000),
('DV201', NULL,      2, 'Vệ sinh máy lạnh (âm trần)',    'TheoLan', 'Máy', NULL, 90, 2, 432000),
('DV201', NULL,      2, 'Vệ sinh máy lạnh (áp trần)',    'TheoLan', 'Máy', NULL, 90, 2, 660000),
('DV201', NULL,      2, 'Vệ sinh máy lạnh (giấu trần)',  'TheoLan', 'Máy', NULL, 90, 2, 240000),
('DV201', 'DV201-T', 2, 'Vệ sinh máy lạnh (Gói 1 lần/tháng, treo tường)', 'GoiThang', 'Gói 1 lần/tháng', 1, 45, 1, 195000),
-- DV202 Vệ sinh máy giặt
('DV202', NULL,      2, 'Vệ sinh máy giặt (cửa trên dưới 9kg, không tháo lồng)',  'TheoLan', 'Máy', NULL, 60, 1, 350000),
('DV202', NULL,      2, 'Vệ sinh máy giặt (cửa trên trên 9kg, không tháo lồng)',  'TheoLan', 'Máy', NULL, 60, 1, 350000),
('DV202', 'DV202',   2, 'Vệ sinh máy giặt (cửa trước dưới 9kg, không tháo lồng)', 'TheoLan', 'Máy', NULL, 90, 1, 350000),
('DV202', NULL,      2, 'Vệ sinh máy giặt (cửa trước trên 9kg, không tháo lồng)', 'TheoLan', 'Máy', NULL, 90, 1, 450000),
('DV202', NULL,      2, 'Vệ sinh máy giặt (cửa trên dưới 9kg, tháo lồng)',        'TheoLan', 'Máy', NULL, 120, 1, 450000),
('DV202', NULL,      2, 'Vệ sinh máy giặt (cửa trên trên 9kg, tháo lồng)',        'TheoLan', 'Máy', NULL, 120, 1, 550000),
('DV202', NULL,      2, 'Vệ sinh máy giặt (cửa trước dưới 9kg, tháo lồng)',       'TheoLan', 'Máy', NULL, 150, 2, 650000),
('DV202', NULL,      2, 'Vệ sinh máy giặt (cửa trước trên 9kg, tháo lồng)',       'TheoLan', 'Máy', NULL, 150, 2, 750000),
('DV202', 'DV202-T', 2, 'Vệ sinh máy giặt (Gói 1 lần/tháng, cửa trước dưới 9kg)', 'GoiThang', 'Gói 1 lần/tháng', 1, 90, 1, 315000),
-- DV203 Vệ sinh bình nóng lạnh
('DV203', 'DV203',   2, 'Vệ sinh bình nóng lạnh (dưới 20 lít)', 'TheoLan', 'Máy', NULL, 60, 1, 300000),
('DV203', NULL,      2, 'Vệ sinh bình nóng lạnh (20–30 lít)',   'TheoLan', 'Máy', NULL, 60, 1, 350000),
('DV203', NULL,      2, 'Vệ sinh bình nóng lạnh (trên 30 lít)', 'TheoLan', 'Máy', NULL, 90, 1, 400000),
('DV203', 'DV203-T', 2, 'Vệ sinh bình nóng lạnh (Gói 1 lần/tháng, dưới 20 lít)', 'GoiThang', 'Gói 1 lần/tháng', 1, 60, 1, 270000),
-- DV301 Trông trẻ tại nhà
('DV301', NULL,      3, 'Trông trẻ tại nhà (2 giờ)', 'TheoLan', 'Buổi', NULL, 120, 1, 216000),
('DV301', 'DV301',   3, 'Trông trẻ tại nhà (3 giờ)', 'TheoLan', 'Buổi', NULL, 180, 1, 270000),
('DV301', NULL,      3, 'Trông trẻ tại nhà (4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 1, 338000),
('DV301', NULL,      3, 'Trông trẻ tại nhà (5 giờ)', 'TheoLan', 'Buổi', NULL, 300, 1, 394000),
('DV301', NULL,      3, 'Trông trẻ tại nhà (6 giờ)', 'TheoLan', 'Buổi', NULL, 360, 1, 473000),
('DV301', NULL,      3, 'Trông trẻ tại nhà (7 giờ)', 'TheoLan', 'Buổi', NULL, 420, 1, 551000),
('DV301', NULL,      3, 'Trông trẻ tại nhà (8 giờ)', 'TheoLan', 'Buổi', NULL, 480, 1, 630000),
('DV301', 'DV301-T', 3, 'Trông trẻ tại nhà (Gói 12 buổi/tháng, 3 giờ/buổi)', 'GoiThang', 'Gói 12 buổi/tháng', 12, 180, 1, 2980000),
-- DV302 Chăm sóc người cao tuổi
('DV302', 'DV302',   3, 'Chăm sóc người cao tuổi (theo buổi, tối đa 4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 1, 273000),
('DV302', NULL,      3, 'Chăm sóc người cao tuổi (theo ngày, 8 giờ)',       'TheoLan', 'Ngày', NULL, 480, 1, 605000),
('DV302', NULL,      3, 'Chăm sóc người cao tuổi (Gói 4 buổi/tháng)',  'GoiThang', 'Gói 4 buổi/tháng',  4,  240, 1, 1040000),
('DV302', NULL,      3, 'Chăm sóc người cao tuổi (Gói 9 buổi/tháng)',  'GoiThang', 'Gói 9 buổi/tháng',  9,  240, 1, 2340000),
('DV302', 'DV302-T', 3, 'Chăm sóc người cao tuổi (Gói 13 buổi/tháng)', 'GoiThang', 'Gói 13 buổi/tháng', 13, 240, 1, 3380000),
('DV302', NULL,      3, 'Chăm sóc người cao tuổi (Gói 4 ngày/tháng)',  'GoiThang', 'Gói 4 ngày/tháng',  4,  480, 1, 2038000),
('DV302', NULL,      3, 'Chăm sóc người cao tuổi (Gói 9 ngày/tháng)',  'GoiThang', 'Gói 9 ngày/tháng',  9,  480, 1, 4586000),
('DV302', NULL,      3, 'Chăm sóc người cao tuổi (Gói 13 ngày/tháng)', 'GoiThang', 'Gói 13 ngày/tháng', 13, 480, 1, 6625000),
-- DV303 Chăm sóc người bệnh tại bệnh viện
('DV303', 'DV303',   3, 'Chăm sóc người bệnh tại bệnh viện (theo buổi, tối đa 4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 1, 294000),
('DV303', NULL,      3, 'Chăm sóc người bệnh tại bệnh viện (theo ngày, 8 giờ)',       'TheoLan', 'Ngày', NULL, 480, 1, 651000),
('DV303', NULL,      3, 'Chăm sóc người bệnh tại bệnh viện (Gói 4 buổi/tháng)',  'GoiThang', 'Gói 4 buổi/tháng',  4,  240, 1, 1087000),
('DV303', NULL,      3, 'Chăm sóc người bệnh tại bệnh viện (Gói 9 buổi/tháng)',  'GoiThang', 'Gói 9 buổi/tháng',  9,  240, 1, 2445000),
('DV303', 'DV303-T', 3, 'Chăm sóc người bệnh tại bệnh viện (Gói 13 buổi/tháng)', 'GoiThang', 'Gói 13 buổi/tháng', 13, 240, 1, 3531000),
('DV303', NULL,      3, 'Chăm sóc người bệnh tại bệnh viện (Gói 18 buổi/tháng)', 'GoiThang', 'Gói 18 buổi/tháng', 18, 240, 1, 4889000),
('DV303', NULL,      3, 'Chăm sóc người bệnh tại bệnh viện (Gói 4 ngày/tháng)',  'GoiThang', 'Gói 4 ngày/tháng',  4,  480, 1, 2173000),
('DV303', NULL,      3, 'Chăm sóc người bệnh tại bệnh viện (Gói 9 ngày/tháng)',  'GoiThang', 'Gói 9 ngày/tháng',  9,  480, 1, 4889000),
('DV303', NULL,      3, 'Chăm sóc người bệnh tại bệnh viện (Gói 13 ngày/tháng)', 'GoiThang', 'Gói 13 ngày/tháng', 13, 480, 1, 7062000),
('DV303', NULL,      3, 'Chăm sóc người bệnh tại bệnh viện (Gói 18 ngày/tháng)', 'GoiThang', 'Gói 18 ngày/tháng', 18, 480, 1, 9374000),
-- DV401 Giặt sấy
('DV401', 'DV401',   4, 'Giặt sấy (quần áo)',                'TheoLan', 'Kg',  NULL, NULL, 1, 17000),
('DV401', NULL,      4, 'Giặt sấy (chăn, mền, mùng, drap)',  'TheoLan', 'Kg',  NULL, NULL, 1, 35000),
('DV401', NULL,      4, 'Giặt sấy (topper – tấm phủ nệm)',   'TheoLan', 'Kg',  NULL, NULL, 1, 55000),
('DV401', NULL,      4, 'Giặt sấy (gối)',                    'TheoLan', 'Cái', NULL, NULL, 1, 100000),
('DV401', 'DV401-T', 4, 'Giặt sấy (Gói 4 lần/tháng, quần áo tối đa 5kg/lần)', 'GoiThang', 'Gói 4 lần/tháng', 4, NULL, 1, 310000),
-- DV402 Nấu ăn gia đình
('DV402', 'DV402',   4, 'Nấu ăn gia đình (2–4 người, 2–3 món)', 'TheoLan', 'Bữa', NULL, 120, 1, 150000),
('DV402', NULL,      4, 'Nấu ăn gia đình (2–4 người, 4 món)',   'TheoLan', 'Bữa', NULL, 150, 1, 188000),
('DV402', NULL,      4, 'Nấu ăn gia đình (5–8 người, 2–3 món)', 'TheoLan', 'Bữa', NULL, 150, 1, 225000),
('DV402', NULL,      4, 'Nấu ăn gia đình (5–8 người, 4 món)',   'TheoLan', 'Bữa', NULL, 180, 1, 263000),
('DV402', 'DV402-T', 4, 'Nấu ăn gia đình (Gói 12 bữa/tháng, 2–4 người, 2–3 món)', 'GoiThang', 'Gói 12 bữa/tháng', 12, 120, 1, 1650000),
-- DV403 Đi chợ hộ
('DV403', 'DV403',   4, 'Đi chợ hộ (tối đa 1 giờ)', 'TheoLan', 'Lần', NULL, 60, 1, 80000),
('DV403', 'DV403-T', 4, 'Đi chợ hộ (Gói 8 lần/tháng)', 'GoiThang', 'Gói 8 lần/tháng', 8, 60, 1, 600000),
-- DV501 Vệ sinh văn phòng
('DV501', 'DV501',   5, 'Vệ sinh văn phòng (tối đa 100m², 1 người/2 giờ)', 'TheoLan', 'Buổi', NULL, 120, 1, 216000),
('DV501', NULL,      5, 'Vệ sinh văn phòng (tối đa 150m², 1 người/3 giờ)', 'TheoLan', 'Buổi', NULL, 180, 1, 270000),
('DV501', NULL,      5, 'Vệ sinh văn phòng (tối đa 200m², 1 người/4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 1, 335000),
('DV501', NULL,      5, 'Vệ sinh văn phòng (tối đa 250m², 1 người/5 giờ)', 'TheoLan', 'Buổi', NULL, 300, 1, 419000),
('DV501', NULL,      5, 'Vệ sinh văn phòng (tối đa 300m², 2 người/3 giờ)', 'TheoLan', 'Buổi', NULL, 180, 2, 540000),
('DV501', NULL,      5, 'Vệ sinh văn phòng (tối đa 400m², 2 người/4 giờ)', 'TheoLan', 'Buổi', NULL, 240, 2, 670000),
('DV501', NULL,      5, 'Vệ sinh văn phòng (tối đa 500m², 2 người/5 giờ)', 'TheoLan', 'Buổi', NULL, 300, 2, 837000),
('DV501', NULL,      5, 'Vệ sinh văn phòng (tối đa 750m², 3 người/5 giờ)', 'TheoLan', 'Buổi', NULL, 300, 3, 1256000),
('DV501', NULL,      5, 'Vệ sinh văn phòng (tối đa 900m², 3 người/6 giờ)', 'TheoLan', 'Buổi', NULL, 360, 3, 1561000),
('DV501', 'DV501-T', 5, 'Vệ sinh văn phòng (Gói 12 buổi/tháng, tối đa 100m²)', 'GoiThang', 'Gói 12 buổi/tháng', 12, 120, 1, 2390000),
-- DV502 Vệ sinh thảm văn phòng
('DV502', 'DV502',   5, 'Vệ sinh thảm văn phòng (dưới 50m²)', 'TheoLan', 'Lần', NULL, 180, 2, 950000),
('DV502', NULL,      5, 'Vệ sinh thảm văn phòng (51–75m²)',   'TheoLan', 'Lần', NULL, 210, 2, 1100000),
('DV502', NULL,      5, 'Vệ sinh thảm văn phòng (76–100m²)',  'TheoLan', 'Lần', NULL, 240, 2, 1200000),
('DV502', 'DV502-T', 5, 'Vệ sinh thảm văn phòng (Gói 1 lần/tháng, dưới 50m²)', 'GoiThang', 'Gói 1 lần/tháng', 1, 180, 2, 860000),
-- DV503 Phun khử khuẩn
('DV503', 'DV503',   5, 'Phun khử khuẩn (đến 70m²)',    'TheoLan', 'Lần', NULL, 60, 2, 550000),
('DV503', NULL,      5, 'Phun khử khuẩn (71–100m²)',    'TheoLan', 'Lần', NULL, 75, 2, 700000),
('DV503', NULL,      5, 'Phun khử khuẩn (101–200m²)',   'TheoLan', 'Lần', NULL, 90, 2, 900000),
('DV503', 'DV503-T', 5, 'Phun khử khuẩn (Gói 2 lần/tháng, đến 70m²)', 'GoiThang', 'Gói 2 lần/tháng', 2, 60, 2, 990000);

-- ----------------------------------------------------------------------------
-- 3. Kiểm tra trạng thái CSDL: mọi dòng giuMa phải tìm thấy dịch vụ hiện có
-- ----------------------------------------------------------------------------
UPDATE tmp_bt t JOIN DichVu d ON d.MaDichVu = t.giuMa SET t.id = d.id;

DROP TEMPORARY TABLE IF EXISTS tmp_kiem_tra;
CREATE TEMPORARY TABLE tmp_kiem_tra (khong_dung_trang_thai_chay_ChuanHoaDichVu_truoc INT NOT NULL);
-- Có dòng không tìm thấy -> chèn NULL vào cột NOT NULL -> lỗi, script dừng
INSERT INTO tmp_kiem_tra SELECT NULL FROM tmp_bt WHERE giuMa IS NOT NULL AND id IS NULL;
INSERT INTO tmp_kiem_tra SELECT NULL FROM DichVu d
WHERE d.id NOT IN (SELECT id FROM tmp_bt WHERE id IS NOT NULL);

-- ----------------------------------------------------------------------------
-- 4. Cập nhật dòng dùng lại, thêm biến thể mới (mã tạm để lấy id, rồi gán mã nhóm)
-- ----------------------------------------------------------------------------
UPDATE DichVu d
JOIN tmp_bt t ON t.id = d.id
JOIN tmp_mota m ON m.ma = t.ma AND m.loaiHinh = t.loaiHinh
SET d.MaDichVu = t.ma,
    d.loaiDichVuId = t.loai,
    d.TenDichVu = t.ten,
    d.MoTaChiTiet = m.moTa,
    d.LoaiHinhDat = t.loaiHinh,
    d.DonViTinh = t.donVi,
    d.SoBuoi = t.soBuoi,
    d.ThoiGianThucHien = t.tg,
    d.SoNguoiThucHien = t.soNguoi,
    d.GiaHienTai = t.gia,
    d.TrangThai = 'HoatDong';

INSERT INTO DichVu (MaDichVu, loaiDichVuId, TenDichVu, MoTaChiTiet, ThoiGianThucHien, LoaiHinhDat,
                    DonViTinh, TrangThai, SoBuoi, SoNguoiThucHien, GiaHienTai)
SELECT CONCAT('TMP-', t.stt), t.loai, t.ten, m.moTa, t.tg, t.loaiHinh, t.donVi, 'HoatDong', t.soBuoi, t.soNguoi, t.gia
FROM tmp_bt t
JOIN tmp_mota m ON m.ma = t.ma AND m.loaiHinh = t.loaiHinh
WHERE t.id IS NULL;

UPDATE tmp_bt t JOIN DichVu d ON d.MaDichVu = CONCAT('TMP-', t.stt) SET t.id = d.id WHERE t.id IS NULL;
UPDATE DichVu d JOIN tmp_bt t ON t.id = d.id SET d.MaDichVu = t.ma WHERE d.MaDichVu LIKE 'TMP-%';

-- ----------------------------------------------------------------------------
-- 5. Bảng giá: mỗi biến thể một gói đang áp dụng (mã gói giá vẫn duy nhất)
-- ----------------------------------------------------------------------------
UPDATE BangGiaDichVu b JOIN tmp_bt t ON t.id = b.dichVuId
SET b.GiaCu = b.DonGia
WHERE b.DonGia <> t.gia;

UPDATE BangGiaDichVu b JOIN tmp_bt t ON t.id = b.dichVuId
SET b.MaBangGia = CONCAT('BG-', t.ma, '-', LPAD(t.stt, 3, '0')),
    b.LoaiHinhDat = t.loaiHinh,
    b.DonViTinh = t.donVi,
    b.DonGia = t.gia,
    b.khuVucId = NULL,
    b.NgayKetThuc = NULL,
    b.TrangThai = 'DangApDung',
    b.NguoiCapNhat = 'Chia biến thể dịch vụ';

INSERT INTO BangGiaDichVu (MaBangGia, dichVuId, khuVucId, LoaiHinhDat, DonViTinh, DonGia, NgayApDung, TrangThai, NguoiCapNhat)
SELECT CONCAT('BG-', t.ma, '-', LPAD(t.stt, 3, '0')), t.id, NULL, t.loaiHinh, t.donVi, t.gia, CURDATE(), 'DangApDung', 'Chia biến thể dịch vụ'
FROM tmp_bt t
WHERE NOT EXISTS (SELECT 1 FROM BangGiaDichVu b WHERE b.dichVuId = t.id);

-- ----------------------------------------------------------------------------
-- 6. CTV đã đăng ký một dịch vụ trong nhóm thì nhận mọi biến thể cùng mã
-- ----------------------------------------------------------------------------
INSERT INTO DichVuCTV (MaDichVuCTV, congTacVienId, dichVuId, TrangThai, NgayBatDau)
SELECT CONCAT('DVCTV-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 10))), x.congTacVienId, x.dichVuId, 'HoatDong', CURDATE()
FROM (SELECT DISTINCT c.congTacVienId, d2.id AS dichVuId
      FROM DichVuCTV c
      JOIN DichVu d1 ON d1.id = c.dichVuId
      JOIN DichVu d2 ON d2.MaDichVu = d1.MaDichVu) x
WHERE NOT EXISTS (SELECT 1 FROM DichVuCTV e WHERE e.congTacVienId = x.congTacVienId AND e.dichVuId = x.dichVuId);

-- ----------------------------------------------------------------------------
-- 7. Kiểm tra kết quả
-- ----------------------------------------------------------------------------
SELECT d.MaDichVu,
       MIN(SUBSTRING_INDEX(d.TenDichVu, ' (', 1)) AS tenNhom,
       SUM(d.LoaiHinhDat = 'TheoLan')  AS soTheoLan,
       SUM(d.LoaiHinhDat = 'GoiThang') AS soGoiThang,
       MIN(d.GiaHienTai) AS giaTu
FROM DichVu d
GROUP BY d.MaDichVu ORDER BY d.MaDichVu;

SELECT d.id, d.MaDichVu, d.TenDichVu, d.LoaiHinhDat, d.DonViTinh, d.GiaHienTai, b.MaBangGia
FROM DichVu d LEFT JOIN BangGiaDichVu b ON b.dichVuId = d.id
ORDER BY d.MaDichVu, d.LoaiHinhDat DESC, d.GiaHienTai;

DROP TEMPORARY TABLE IF EXISTS tmp_kiem_tra;
DROP TEMPORARY TABLE IF EXISTS tmp_bt;
DROP TEMPORARY TABLE IF EXISTS tmp_mota;

COMMIT;
SET SQL_SAFE_UPDATES = @safe_updates_cu;
