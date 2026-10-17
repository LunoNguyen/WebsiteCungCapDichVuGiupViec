-- ============================================================================
-- MÔ TẢ KHÁC BIỆT CHO TỪNG DỊCH VỤ + SỬA DỮ LIỆU BỊ LỖI MÃ HOÁ
--   Chạy SAU KhoiPhucBienThe_GiuNhomTheoDanhMuc.sql (CSDL có 66 dịch vụ, 10 nhóm).
--   1. Viết lại MoTaChiTiet cho cả 66 dịch vụ: mỗi mô tả nói rõ dịch vụ phù hợp với ai / nhà bao lớn,
--      thời lượng, số người làm, phạm vi công việc riêng và điểm khác so với gói kế bên trong cùng nhóm.
--   2. Sửa 23 dòng dữ liệu mẫu bị mất dấu thành '?' (vd 'Kh?ch h?ng r?t h?i l?ng') trong
--      DonDatDichVu.GhiChu, DonDatDichVu.YeuCauDacBiet, DiaChiKhachHang.LuuYDacBiet.
--      Dạng lỗi này mất hẳn ký tự gốc nên không chuyển ngược tự động được: khôi phục theo nội dung.
--
-- Không đổi cấu trúc bảng. Chạy lại nhiều lần không sao.
-- Sau khi chạy: xoá cache Redis
--   docker exec redis sh -c "redis-cli --scan --pattern 'neatify:*' | xargs -r redis-cli del"
-- ============================================================================

SET NAMES utf8mb4;
SET @safe_updates_cu = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_mo_ta;
CREATE TEMPORARY TABLE tmp_mo_ta (
    ten  VARCHAR(200) NOT NULL PRIMARY KEY,
    moTa TEXT NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_mo_ta (ten, moTa) VALUES
-- ---------------------------------------------------------------- DV101 Dọn dẹp nhà theo giờ
('Dọn dẹp nhà theo giờ (2 giờ)',
 'Gói ngắn nhất, dành cho căn hộ nhỏ dưới 55m² (studio, 1–2 phòng ngủ, 1 nhà vệ sinh) cần dọn hằng ngày hoặc cuối tuần. 1 nhân viên làm 2 giờ, tập trung vào việc thiết yếu: quét và lau sàn các phòng, lau bụi bề mặt bàn ghế, rửa bát và lau mặt bếp, cọ 1 nhà vệ sinh, đổ rác. Không đủ thời gian cho lau kính, ban công hay sắp xếp tủ đồ — nên chọn gói 3 giờ nếu cần thêm các việc này.'),
('Dọn dẹp nhà theo giờ (3 giờ)',
 'Dành cho căn hộ hoặc nhà phố 55–85m² (2–3 phòng ngủ, 1–2 nhà vệ sinh). 1 nhân viên làm 3 giờ: ngoài phần việc của gói 2 giờ (sàn, bụi, bếp, rác) còn cọ rửa 2 nhà vệ sinh, lau kỹ tủ bếp mặt ngoài, thay ga gối nếu khách chuẩn bị sẵn và sắp xếp lại tủ quần áo, kệ sách. Là gói được đặt nhiều nhất cho gia đình 3–4 người.'),
('Dọn dẹp nhà theo giờ (4 giờ)',
 'Dành cho nhà 85–105m² (3–4 phòng ngủ, 2–3 nhà vệ sinh) hoặc nhà lâu ngày chưa dọn. 1 nhân viên làm 4 giờ, đủ thời gian cho toàn bộ việc của gói 3 giờ cộng thêm: lau cửa kính và khung cửa trong tầm với, vệ sinh ban công, sân phơi, lau quạt, công tắc, tay nắm cửa. Không phải tổng vệ sinh (không dùng máy, không tẩy cặn sâu) — cần làm sạch sâu hãy chọn Tổng vệ sinh nhà.'),
('Dọn dẹp nhà theo giờ (Gói 8 buổi/tháng, 2 giờ/buổi)',
 'Gói định kỳ cho căn hộ dưới 55m² muốn nhà luôn gọn mà không phải đặt lại: 8 buổi trong 1 tháng (2 buổi/tuần, vd T3 và T6), mỗi buổi 2 giờ, 1 nhân viên — tổng 16 giờ làm/tháng. Mỗi buổi gồm lau sàn, lau bụi, vệ sinh bếp và nhà vệ sinh, đổ rác. Hệ thống ưu tiên giữ một cộng tác viên cố định để quen nếp nhà; buổi bị huỷ khi báo trước 24 giờ được dời sang ngày khác trong tháng. Rẻ hơn khoảng 8% so với đặt lẻ 8 buổi 2 giờ.'),
-- ---------------------------------------------------------------- DV102 Tổng vệ sinh nhà
('Tổng vệ sinh nhà (tối đa 60m², 2 người/3 giờ)',
 'Gói tổng vệ sinh nhỏ nhất, cho studio hoặc căn hộ 1–2 phòng ngủ đến 60m². Đội 2 người làm 3 giờ với máy hút bụi công nghiệp và hoá chất chuyên dụng: hút bụi và lau toàn bộ sàn, chân tường; tẩy dầu mỡ bếp và máy hút mùi; tẩy cặn vòi sen, gạch ốp, bồn cầu; lau kính hai mặt trong tầm với. Khác dọn dẹp theo giờ ở chỗ làm sạch sâu từng góc, phù hợp trước khi dọn vào ở hoặc trả nhà thuê.'),
('Tổng vệ sinh nhà (tối đa 80m², 2 người/4 giờ)',
 'Cho căn hộ 60–80m² (2 phòng ngủ, 2 nhà vệ sinh). Vẫn đội 2 người như gói 60m² nhưng làm 4 giờ để đủ thời gian cho nhà vệ sinh thứ hai, tủ bếp trên – dưới và ban công. Gồm hút bụi, lau sàn, tẩy dầu mỡ bếp, tẩy cặn nhà vệ sinh, lau kính, lau công tắc, quạt và thu gom rác sau vệ sinh.'),
('Tổng vệ sinh nhà (tối đa 100m², 3 người/3 giờ)',
 'Cho căn hộ 2–3 phòng ngủ hoặc nhà phố 1 trệt 1 lầu đến 100m². Tăng lên đội 3 người để xong trong 3 giờ — nhanh hơn gói 80m² dù diện tích lớn hơn, hợp với nhà cần làm gọn trong một buổi sáng. Đầy đủ hạng mục: sàn, chân tường, kính, bếp, máy hút mùi, nhà vệ sinh, công tắc, tay nắm cửa, quạt. Thích hợp trước Tết hoặc sau sửa chữa nhỏ.'),
('Tổng vệ sinh nhà (tối đa 150m², 3 người/4 giờ)',
 'Cho nhà phố 1 trệt 2 lầu hoặc căn hộ 3–4 phòng ngủ 100–150m². Đội 3 người làm 4 giờ, chia theo tầng để làm đồng thời; thêm vệ sinh cầu thang, tay vịn và sân trước so với gói 100m². Bao gồm toàn bộ hạng mục làm sạch sâu bếp, nhà vệ sinh, kính và sàn các tầng.'),
('Tổng vệ sinh nhà (tối đa 200m², 4 người/4 giờ)',
 'Cho nhà phố nhiều tầng hoặc căn hộ penthouse 150–200m². Đội 4 người (1 trưởng nhóm) làm 4 giờ, mang thêm thang chữ A và cây lau kính nối dài cho cửa kính lớn, giếng trời. Phạm vi như gói 150m² cộng thêm sân thượng và khu giặt phơi.'),
('Tổng vệ sinh nhà (tối đa 400m², 4 người/8 giờ)',
 'Gói lớn nhất, cho biệt thự hoặc nhà phố lớn có sân vườn 200–400m². Đội 4 người làm trọn ngày 8 giờ, chia ca sáng – chiều (có nghỉ trưa), làm lần lượt từng tầng từ trên xuống. Gồm mọi hạng mục làm sạch sâu trong nhà, sân thượng, sân trước, cổng và nhà xe. Nên đặt trước ít nhất 2 ngày để xếp đủ đội.'),
('Tổng vệ sinh nhà (Gói 2 lần/tháng, tối đa 100m²)',
 'Gói định kỳ 2 lần tổng vệ sinh mỗi tháng (cách nhau khoảng 2 tuần) cho nhà đến 100m², mỗi lần đội 3 người làm 3 giờ — cùng phạm vi với gói lẻ 100m². Hợp với gia đình có trẻ nhỏ, thú cưng hoặc nhà mặt đường nhiều bụi cần làm sạch sâu thường xuyên. Lịch cố định theo ngày khách chọn, rẻ hơn khoảng 7% so với đặt lẻ 2 lần.'),
-- ---------------------------------------------------------------- DV201 Vệ sinh máy lạnh
('Vệ sinh máy lạnh (treo tường)',
 'Cho máy lạnh treo tường gia đình 1–2,5 HP (loại phổ biến nhất trong phòng ngủ, phòng khách nhỏ). 1 thợ làm khoảng 45 phút, không cần tháo máy khỏi tường: rửa lưới lọc, mặt nạ; xịt rửa dàn lạnh và quạt lồng sóc bằng bơm áp lực có bạt hứng nước; vệ sinh dàn nóng ngoài trời; thông ống thoát nước; đo gas và nhiệt độ gió sau khi làm.'),
('Vệ sinh máy lạnh (giấu trần)',
 'Cho máy lạnh giấu trần nối ống gió (dàn lạnh nằm trên trần thạch cao, gió thổi qua các miệng gió). 2 thợ làm khoảng 1 giờ 30 phút: mở cửa thăm trần, vệ sinh dàn lạnh và lưới lọc hồi gió, lau các miệng gió trong phòng, thông máng nước. Giá thấp hơn máy âm trần vì không tháo mặt nạ, nhưng nhà cần có sẵn cửa thăm trần.'),
('Vệ sinh máy lạnh (tủ đứng)',
 'Cho máy lạnh tủ đứng 2–5 HP đặt sàn ở phòng khách, sảnh, cửa hàng. 1 thợ làm khoảng 1 giờ: tháo mặt trước, rửa lưới lọc và dàn lạnh dạng đứng, vệ sinh quạt ly tâm, thông máng nước đáy tủ, vệ sinh dàn nóng. Khác máy treo tường ở chỗ dàn lạnh lớn hơn, thợ phải che chắn sàn quanh tủ để không đọng nước.'),
('Vệ sinh máy lạnh (âm trần)',
 'Cho máy lạnh âm trần cassette (mặt thổi 4 hướng, thường ở văn phòng, nhà hàng, phòng khách trần cao). 2 thợ làm khoảng 1 giờ 30 phút với thang và bạt hứng nước chuyên dụng: tháo mặt nạ, rửa lưới lọc, xịt rửa dàn lạnh và bơm nước xả, kiểm tra phao báo nước. Cần 2 người vì phải đỡ mặt nạ và thao tác trên cao.'),
('Vệ sinh máy lạnh (áp trần)',
 'Cho máy lạnh áp trần (treo sát trần, thổi một hướng dài, công suất lớn 3–5 HP cho phòng rộng). 2 thợ làm khoảng 1 giờ 30 phút; dàn lạnh dài và nặng nhất trong các loại nên cần tháo vỏ hai bên để rửa hết chiều dài dàn và quạt. Giá cao nhất nhóm do khối lượng việc lớn.'),
('Vệ sinh máy lạnh (Gói 1 lần/tháng, treo tường)',
 'Gói bảo dưỡng định kỳ mỗi tháng 1 lần cho 1 máy lạnh treo tường, mỗi lần khoảng 45 phút, cùng phạm vi với vệ sinh lẻ máy treo tường và thêm ghi nhận áp suất gas, dòng điện hằng tháng để phát hiện sớm hao gas, hỏng tụ. Dành cho phòng chạy máy lạnh gần như liên tục: phòng trẻ nhỏ, cửa hàng, phòng máy chủ. Rẻ hơn 10% so với đặt lẻ.'),
-- ---------------------------------------------------------------- DV202 Vệ sinh máy giặt
('Vệ sinh máy giặt (cửa trên dưới 9kg, không tháo lồng)',
 'Cho máy giặt cửa trên (lồng đứng) dưới 9kg của gia đình 1–3 người, vệ sinh định kỳ khi máy mới có mùi nhẹ. 1 thợ làm khoảng 1 giờ, không tháo lồng: chạy chu trình nước nóng với dung dịch tẩy cặn, vệ sinh ngăn bột giặt, lưới lọc xơ vải và ống thoát, lau vỏ máy. Máy đã có vết đen bám quần áo nên chọn gói tháo lồng.'),
('Vệ sinh máy giặt (cửa trên trên 9kg, không tháo lồng)',
 'Cho máy giặt cửa trên từ 9kg trở lên (gia đình đông người). Phạm vi và thời gian 1 giờ như loại dưới 9kg, nhưng dùng lượng dung dịch tẩy cặn gấp đôi và ngâm lâu hơn cho lồng cỡ lớn. Không tháo lồng nên chỉ làm sạch được mặt trong lồng và đường nước.'),
('Vệ sinh máy giặt (cửa trước dưới 9kg, không tháo lồng)',
 'Cho máy giặt cửa trước (lồng ngang) dưới 9kg. 1 thợ làm khoảng 1 giờ 30 phút — lâu hơn máy cửa trên vì phải lau kỹ gioăng cao su cửa (nơi đọng nước, rêu mốc nhiều nhất) và tháo vệ sinh bơm xả ở chân máy. Gồm chạy chu trình tẩy cặn nước nóng, vệ sinh ngăn bột giặt và ống thoát.'),
('Vệ sinh máy giặt (cửa trên dưới 9kg, tháo lồng)',
 'Vệ sinh triệt để cho máy giặt cửa trên dưới 9kg đã có mùi hôi rõ hoặc vết đen bám quần áo. 1 thợ làm khoảng 2 giờ: tháo mâm giặt và lồng giặt ra khỏi thùng, chà sạch rêu mốc, cặn bột giặt bám mặt ngoài lồng và đáy thùng (phần không chạm tới được khi không tháo), lắp lại và chạy thử.'),
('Vệ sinh máy giặt (cửa trước trên 9kg, không tháo lồng)',
 'Cho máy giặt cửa trước từ 9kg trở lên, không tháo lồng. 1 thợ làm khoảng 1 giờ 30 phút như loại dưới 9kg nhưng gioăng cửa và lồng lớn hơn, cần nhiều dung dịch và chu trình xả lâu hơn. Phù hợp vệ sinh định kỳ 3–6 tháng/lần.'),
('Vệ sinh máy giặt (cửa trên trên 9kg, tháo lồng)',
 'Vệ sinh tháo lồng cho máy giặt cửa trên từ 9kg trở lên. 1 thợ làm khoảng 2 giờ; lồng cỡ lớn nặng hơn nên thợ dùng cảo chuyên dụng để tháo mâm giặt. Làm sạch rêu mốc mặt ngoài lồng, đáy thùng và trục, sau đó lắp lại, cân bằng máy và chạy thử.'),
('Vệ sinh máy giặt (cửa trước dưới 9kg, tháo lồng)',
 'Vệ sinh tháo lồng cho máy giặt cửa trước dưới 9kg. Cần 2 thợ, khoảng 2 giờ 30 phút, vì máy cửa trước phải tháo vỏ trên, mặt trước, cụm giảm chấn và trục lồng mới lấy được lồng ra. Làm sạch phần rêu mốc phía sau lồng mà gói không tháo lồng không chạm tới được.'),
('Vệ sinh máy giặt (cửa trước trên 9kg, tháo lồng)',
 'Gói vệ sinh kỹ nhất nhóm, cho máy giặt cửa trước từ 9kg trở lên. 2 thợ làm khoảng 2 giờ 30 phút: tháo vỏ, cụm giảm chấn và lồng giặt cỡ lớn; chà sạch lồng, thùng ngoài, gioăng cửa; kiểm tra bạc đạn và phớt trục; lắp lại và chạy thử đủ chu trình.'),
('Vệ sinh máy giặt (Gói 1 lần/tháng, cửa trước dưới 9kg)',
 'Gói vệ sinh định kỳ mỗi tháng 1 lần cho máy giặt cửa trước dưới 9kg, không tháo lồng, khoảng 1 giờ 30 phút/lần. Giữ gioăng cửa và lồng giặt luôn sạch để không cần tới gói tháo lồng; phù hợp nhà có trẻ sơ sinh hoặc người da nhạy cảm. Rẻ hơn 10% so với đặt lẻ.'),
-- ---------------------------------------------------------------- DV301 Trông trẻ tại nhà
('Trông trẻ tại nhà (2 giờ)',
 'Ca trông trẻ ngắn nhất: 1 bảo mẫu trông 1–2 bé từ 1 tuổi trong 2 giờ, khi bố mẹ đi chợ, đi họp ngắn hoặc cần nghỉ ngơi. Gồm chơi cùng bé, đọc truyện, cho bé ăn bữa phụ phụ huynh chuẩn bị sẵn, thay tã. Không gồm bữa chính và giấc ngủ trưa.'),
('Trông trẻ tại nhà (3 giờ)',
 '1 bảo mẫu trông 1–2 bé trong 3 giờ, vừa đủ cho một buổi khám bệnh, làm giấy tờ hay buổi hẹn tối của bố mẹ. Thêm so với ca 2 giờ: cho bé ăn 1 bữa chính theo hướng dẫn và tắm, thay đồ cho bé trước khi bố mẹ về.'),
('Trông trẻ tại nhà (4 giờ)',
 '1 bảo mẫu trông bé nửa ngày 4 giờ (sáng 8h–12h hoặc chiều 13h–17h). Bao gồm 1 bữa ăn, 1 bữa phụ và giấc ngủ trưa của bé; bảo mẫu dọn dẹp khu vực bé chơi và ghi chú lại giờ ăn, giờ ngủ cho phụ huynh.'),
('Trông trẻ tại nhà (5 giờ)',
 '1 bảo mẫu trông bé 5 giờ, thường là ca chiều từ lúc bé ngủ trưa dậy đến khi bố mẹ đi làm về (khoảng 13h–18h). Gồm bữa phụ chiều, tắm và cho bé ăn bữa tối sớm; có hoạt động tô màu, xếp hình phù hợp độ tuổi.'),
('Trông trẻ tại nhà (6 giờ)',
 '1 bảo mẫu trông bé 6 giờ, đủ cho ngày bố mẹ làm việc ngắn hoặc cuối tuần có việc. Gồm 2 bữa ăn, giấc ngủ trưa và hoạt động chơi – học buổi sáng; bảo mẫu giặt tay đồ bé thay trong ngày.'),
('Trông trẻ tại nhà (7 giờ)',
 '1 bảo mẫu trông bé 7 giờ, gần trọn giờ hành chính (vd 8h–15h, đón bé đi học về tại nhà). Gồm 2 bữa ăn, 1 bữa phụ, giấc ngủ trưa; bảo mẫu nghỉ giữa ca 30 phút khi bé ngủ.'),
('Trông trẻ tại nhà (8 giờ)',
 'Ca dài nhất: 1 bảo mẫu trông bé trọn ngày 8 giờ (thường 8h–16h) cho bố mẹ đi làm cả ngày. Gồm toàn bộ bữa ăn, giấc ngủ trưa, hoạt động sáng – chiều, tắm cho bé cuối ca và bàn giao nhật ký trong ngày. Bảo mẫu nghỉ giữa ca 30 phút khi bé ngủ.'),
('Trông trẻ tại nhà (Gói 12 buổi/tháng, 3 giờ/buổi)',
 'Gói trông trẻ cố định 12 buổi trong 1 tháng (3 buổi/tuần), mỗi buổi 3 giờ — tổng 36 giờ/tháng. Ưu tiên giữ cùng một bảo mẫu suốt tháng để bé quen người và nếp sinh hoạt; sau mỗi buổi bảo mẫu ghi chú ăn, ngủ, chơi của bé. Rẻ hơn khoảng 8% so với đặt lẻ 12 ca 3 giờ.'),
-- ---------------------------------------------------------------- DV302 Chăm sóc người cao tuổi
('Chăm sóc người cao tuổi (theo buổi, tối đa 4 giờ)',
 '1 điều dưỡng hoặc hộ lý chăm sóc người cao tuổi tại nhà trong 1 buổi tối đa 4 giờ (sáng hoặc chiều), phù hợp khi người nhà vắng nửa ngày. Gồm hỗ trợ 1 bữa ăn, nhắc và cho uống thuốc theo đơn, vệ sinh cá nhân, đo huyết áp và trò chuyện, đi dạo nhẹ. Không thực hiện tiêm, truyền dịch.'),
('Chăm sóc người cao tuổi (theo ngày, 8 giờ)',
 '1 điều dưỡng hoặc hộ lý ở cùng người cao tuổi trọn ngày 8 giờ (thường 8h–16h), phù hợp người cần theo dõi liên tục hoặc đi lại khó khăn. So với chăm theo buổi: gồm 2 bữa ăn, giấc ngủ trưa, các lần uống thuốc trong ngày, tập vận động nhẹ và xoa bóp chống cứng khớp; cuối ngày bàn giao nhật ký sức khoẻ.'),
('Chăm sóc người cao tuổi (Gói 4 buổi/tháng)',
 'Gói nhẹ nhất: 4 buổi trong 1 tháng (khoảng 1 buổi/tuần), mỗi buổi tối đa 4 giờ. Dành cho gia đình chỉ cần người hỗ trợ vào một ngày cố định trong tuần, vd ngày người nhà đi làm xa hoặc đưa ông bà đi tái khám. Cùng nội dung chăm sóc như buổi lẻ, ưu tiên cố định một người chăm sóc.'),
('Chăm sóc người cao tuổi (Gói 4 ngày/tháng)',
 '4 ngày trong 1 tháng (1 ngày/tuần), mỗi ngày 8 giờ — giúp người nhà được nghỉ trọn một ngày cố định mỗi tuần. Mỗi ngày gồm đầy đủ nội dung của chăm theo ngày: bữa ăn, thuốc, vệ sinh, vận động, nhật ký sức khoẻ.'),
('Chăm sóc người cao tuổi (Gói 9 buổi/tháng)',
 '9 buổi trong 1 tháng (khoảng 2 buổi/tuần), mỗi buổi tối đa 4 giờ. Phù hợp người cao tuổi còn tự sinh hoạt được nhưng cần người nhắc thuốc, đo huyết áp và trò chuyện vài lần mỗi tuần. Ưu tiên một người chăm sóc cố định, giá thấp hơn đặt lẻ.'),
('Chăm sóc người cao tuổi (Gói 13 buổi/tháng)',
 '13 buổi trong 1 tháng (khoảng 3 buổi/tuần), mỗi buổi tối đa 4 giờ. Phù hợp người cao tuổi cần hỗ trợ cách ngày, vd sau xuất viện hoặc bệnh mạn tính cần theo dõi huyết áp, đường huyết đều đặn. Gia đình nhận nhật ký sức khoẻ sau mỗi buổi.'),
('Chăm sóc người cao tuổi (Gói 9 ngày/tháng)',
 '9 ngày trong 1 tháng (khoảng 2 ngày/tuần), mỗi ngày 8 giờ. Dành cho gia đình đi làm cả ngày, cần người ở cùng ông bà vài ngày cố định mỗi tuần. Cùng nội dung chăm theo ngày, ưu tiên giữ một người chăm sóc để người cao tuổi yên tâm.'),
('Chăm sóc người cao tuổi (Gói 13 ngày/tháng)',
 'Gói nhiều nhất: 13 ngày trong 1 tháng (khoảng 3 ngày/tuần), mỗi ngày 8 giờ. Dành cho người cao tuổi đi lại khó khăn hoặc cần theo dõi thường xuyên; người chăm sóc cố định nắm rõ thói quen ăn uống, thuốc men và báo ngay cho gia đình khi có dấu hiệu bất thường.'),
-- ---------------------------------------------------------------- DV401 Giặt sấy
('Giặt sấy (quần áo)',
 'Giặt sấy quần áo mặc hằng ngày, tính theo kg (tối thiểu 3kg mỗi lần). Nhân viên nhận đồ tận nhà, phân loại đồ trắng – đồ màu, giặt máy với nước giặt dịu nhẹ, sấy khô, gấp gọn, đóng túi và trả trong 24 giờ. Đơn giá thấp nhất nhóm vì giặt chung chế độ thường; đồ len, lụa, vest cần ghi chú để báo giá riêng.'),
('Giặt sấy (chăn, mền, mùng, drap)',
 'Giặt sấy đồ giường cỡ lớn — chăn, mền, mùng, drap — tính theo kg. Khác giặt quần áo: dùng máy cỡ lớn 15–20kg giặt riêng từng khách để đồ không bị cuộn, sấy ở nhiệt độ vừa để vải không co rút, gấp vuông và đóng túi lớn. Trả đồ trong 24 giờ.'),
('Giặt sấy (topper – tấm phủ nệm)',
 'Giặt sấy topper (tấm phủ nệm) tính theo kg. Đơn giá cao hơn chăn mền vì ruột bông dày: hút bụi và xử lý vết bẩn trước, giặt máy cỡ lớn chế độ nhẹ, sấy nhiệt thấp nhiều đợt và đánh tơi để ruột khô đều, không vón cục. Trả đồ trong 24–48 giờ tuỳ độ dày.'),
('Giặt sấy (gối)',
 'Giặt sấy gối ngủ, gối ôm tính theo cái (không tính kg). Giặt cả vỏ và ruột gối bông hoặc lông vũ, sấy kèm bóng sấy để ruột phồng trở lại, khử mùi và diệt mạt bụi. Gối cao su non, gối thảo dược không giặt được bằng máy — vui lòng ghi chú.'),
('Giặt sấy (Gói 4 lần/tháng, quần áo tối đa 5kg/lần)',
 'Gói giặt sấy quần áo 4 lần trong 1 tháng (1 lần/tuần), mỗi lần tối đa 5kg — tổng 20kg/tháng, đã gồm nhận và trả tận nhà trong 24 giờ. Phù hợp người đi làm bận rộn, sinh viên, gia đình 2–3 người. Phần vượt 5kg mỗi lần tính theo đơn giá giặt quần áo lẻ. Rẻ hơn khoảng 9% so với đặt từng lần.'),
-- ---------------------------------------------------------------- DV402 Nấu ăn gia đình
('Nấu ăn gia đình (2–4 người, 2–3 món)',
 'Bữa cơm thường ngày cho gia đình 2–4 người: 1 người nấu đến nhà làm trong khoảng 2 giờ, gồm 1 món mặn, 1 món rau hoặc xào và 1 món canh. Bao gồm sơ chế, nấu, bày món và rửa dọn bếp. Chưa gồm tiền nguyên liệu (khách tự chuẩn bị hoặc đặt thêm Đi chợ hộ).'),
('Nấu ăn gia đình (2–4 người, 4 món)',
 'Bữa cơm đầy đặn hơn cho 2–4 người: 4 món, thêm 1 món mặn hoặc tráng miệng so với gói 2–3 món; thời gian khoảng 2 giờ 30 phút. Hợp với bữa cuối tuần hoặc khi nhà có khách nhỏ. Bao gồm sơ chế, nấu, bày món và rửa dọn bếp; chưa gồm tiền nguyên liệu.'),
('Nấu ăn gia đình (5–8 người, 2–3 món)',
 'Bữa cơm thường ngày cho gia đình đông người, 5–8 người: vẫn 2–3 món nhưng khẩu phần gấp đôi, thời gian khoảng 2 giờ 30 phút. Phù hợp gia đình nhiều thế hệ ăn chung hằng ngày. Bao gồm sơ chế, nấu, bày món và rửa dọn bếp; chưa gồm tiền nguyên liệu.'),
('Nấu ăn gia đình (5–8 người, 4 món)',
 'Gói nấu lớn nhất: 4 món cho 5–8 người, khoảng 3 giờ. Dành cho bữa cơm cuối tuần, giỗ nhỏ, sinh nhật hoặc tiếp khách tại nhà; người nấu có thể gợi ý thực đơn theo dịp. Bao gồm sơ chế, nấu, bày món và rửa dọn bếp; chưa gồm tiền nguyên liệu.'),
('Nấu ăn gia đình (Gói 12 bữa/tháng, 2–4 người, 2–3 món)',
 'Gói nấu ăn định kỳ 12 bữa trong 1 tháng (3 bữa/tuần) cho gia đình 2–4 người, mỗi bữa 2–3 món, khoảng 2 giờ/bữa. Ưu tiên giữ một người nấu để quen khẩu vị; thực đơn lên trước theo tuần để gia đình chuẩn bị nguyên liệu. Chưa gồm tiền nguyên liệu. Rẻ hơn khoảng 8% so với đặt lẻ.'),
-- ---------------------------------------------------------------- DV501 Vệ sinh văn phòng
('Vệ sinh văn phòng (tối đa 100m², 1 người/2 giờ)',
 'Cho văn phòng nhỏ đến 100m² (khoảng 10–15 chỗ ngồi, 1 phòng họp). 1 nhân viên làm 2 giờ, thường trước hoặc sau giờ làm: hút bụi, lau sàn, lau bàn làm việc (không xáo trộn giấy tờ), vệ sinh pantry, nhà vệ sinh, đổ rác. Có biên bản nghiệm thu và hoá đơn VAT theo yêu cầu.'),
('Vệ sinh văn phòng (tối đa 150m², 1 người/3 giờ)',
 'Cho văn phòng 100–150m² (khoảng 15–20 chỗ ngồi). 1 nhân viên làm 3 giờ: phạm vi như gói 100m² và thêm lau cửa kính vách ngăn, lau ghế, vệ sinh lò vi sóng và tủ lạnh pantry mặt ngoài.'),
('Vệ sinh văn phòng (tối đa 200m², 1 người/4 giờ)',
 'Cho văn phòng 150–200m² (khoảng 20–30 chỗ ngồi, 1–2 phòng họp). 1 nhân viên làm 4 giờ, đủ cho khu làm việc chung, phòng họp, pantry và 2 khu vệ sinh; thêm lau tủ hồ sơ mặt ngoài và tay nắm cửa.'),
('Vệ sinh văn phòng (tối đa 250m², 1 người/5 giờ)',
 'Cho văn phòng 200–250m² (khoảng 30–35 chỗ ngồi). Vẫn 1 nhân viên nhưng làm 5 giờ — phù hợp làm trong giờ hành chính, vừa dọn vừa duy trì vệ sinh pantry và nhà vệ sinh suốt ca.'),
('Vệ sinh văn phòng (tối đa 300m², 2 người/3 giờ)',
 'Cho văn phòng 250–300m² (khoảng 35–45 chỗ ngồi). Tăng lên 2 nhân viên chia khu vực để xong trong 3 giờ — chọn gói này nếu cần dọn nhanh ngoài giờ làm thay vì 1 người làm cả buổi.'),
('Vệ sinh văn phòng (tối đa 400m², 2 người/4 giờ)',
 'Cho văn phòng 300–400m² (khoảng 45–60 chỗ ngồi, nhiều phòng họp). 2 nhân viên làm 4 giờ: toàn bộ khu làm việc, phòng họp, phòng giám đốc, pantry, nhà vệ sinh và khu lễ tân; lau kính vách ngăn hai mặt.'),
('Vệ sinh văn phòng (tối đa 500m², 2 người/5 giờ)',
 'Cho văn phòng 400–500m² (khoảng 60–80 chỗ ngồi). 2 nhân viên làm 5 giờ, có thêm thời gian hút bụi thảm khu làm việc và lau thiết bị dùng chung (máy in, máy chấm công).'),
('Vệ sinh văn phòng (tối đa 750m², 3 người/5 giờ)',
 'Cho văn phòng nguyên sàn 500–750m² (khoảng 80–120 chỗ ngồi). Đội 3 nhân viên (1 trưởng nhóm) làm 5 giờ, chia theo khu: làm việc, phòng họp – lễ tân, pantry – vệ sinh. Có biên bản nghiệm thu theo từng khu.'),
('Vệ sinh văn phòng (tối đa 900m², 3 người/6 giờ)',
 'Gói lớn nhất cho văn phòng 750–900m² (khoảng 120–150 chỗ ngồi). Đội 3 nhân viên làm 6 giờ; phạm vi như gói 750m² cộng thêm hành lang, thang bộ nội bộ và khu để xe nhân viên.'),
('Vệ sinh văn phòng (Gói 12 buổi/tháng, tối đa 100m²)',
 'Gói vệ sinh văn phòng định kỳ 12 buổi trong 1 tháng (3 buổi/tuần), mỗi buổi 1 nhân viên làm 2 giờ cho văn phòng đến 100m². Lịch cố định theo ngày doanh nghiệp chọn, ưu tiên giữ một nhân viên quen văn phòng; biên bản nghiệm thu và hoá đơn VAT hằng tháng. Rẻ hơn khoảng 8% so với đặt lẻ.'),
-- ---------------------------------------------------------------- DV502 Vệ sinh thảm văn phòng
('Vệ sinh thảm văn phòng (dưới 50m²)',
 'Giặt thảm trải sàn dưới 50m² (phòng họp, phòng giám đốc, văn phòng nhỏ). 2 nhân viên làm khoảng 3 giờ bằng máy giặt thảm phun hút công nghiệp: hút bụi sâu, xử lý vết cà phê, mực, kẹo cao su, giặt bằng hoá chất an toàn với sợi thảm, hút khô và khử mùi. Thảm khô sau 4–6 giờ.'),
('Vệ sinh thảm văn phòng (51–75m²)',
 'Giặt thảm 51–75m² (văn phòng 10–20 chỗ ngồi hoặc phòng họp lớn). 2 nhân viên làm khoảng 3 giờ 30 phút; cần dời bàn ghế theo từng cụm nên nên làm cuối tuần. Quy trình như gói dưới 50m².'),
('Vệ sinh thảm văn phòng (76–100m²)',
 'Giặt thảm 76–100m² (văn phòng mở 20–30 chỗ ngồi). 2 nhân viên làm khoảng 4 giờ, giặt theo từng ô để khu đã giặt kịp khô trong ngày; có quạt sấy thảm công nghiệp để rút ngắn thời gian khô.'),
('Vệ sinh thảm văn phòng (Gói 1 lần/tháng, dưới 50m²)',
 'Gói giặt thảm định kỳ mỗi tháng 1 lần cho thảm dưới 50m², mỗi lần 2 nhân viên làm khoảng 3 giờ. Giữ thảm sạch bụi mịn và vi khuẩn, hạn chế vết bẩn bám lâu khó tẩy, kéo dài tuổi thọ thảm. Lịch cố định theo ngày doanh nghiệp chọn; rẻ hơn khoảng 9% so với đặt lẻ.');

UPDATE DichVu d JOIN tmp_mo_ta m ON m.ten = d.TenDichVu SET d.MoTaChiTiet = m.moTa;

-- ----------------------------------------------------------------------------
-- 2. Dữ liệu mẫu bị mất dấu thành '?' (so khớp nhị phân để không nhầm với chuỗi khác)
-- ----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_sua_chu;
CREATE TEMPORARY TABLE tmp_sua_chu (
    loi    VARCHAR(255) NOT NULL,
    dung   VARCHAR(255) NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

INSERT INTO tmp_sua_chu (loi, dung) VALUES
-- DiaChiKhachHang.LuuYDacBiet
('Nh? c? nu?i m?o, c?n th?n c?a ra v?o', 'Nhà có nuôi mèo, cẩn thận cửa ra vào'),
('B?m chu?ng c?a t?ng 2',                'Bấm chuông cửa tầng 2'),
('G?i xe d??i h?m chung c?',             'Gửi xe dưới hầm chung cư'),
-- DonDatDichVu.GhiChu
('Kh?ch h?ng r?t h?i l?ng',              'Khách hàng rất hài lòng'),
('?? ho?n th?nh ??ng gi?',               'Đã hoàn thành đúng giờ'),
('?ang th?c hi?n ca s?ng',               'Đang thực hiện ca sáng'),
('?ang th?c hi?n ca chi?u',              'Đang thực hiện ca chiều'),
('?? x?c nh?n v?i kh?ch',                'Đã xác nhận với khách'),
('Kh?ch d?n mang g?ng tay ri?ng',        'Khách dặn mang găng tay riêng'),
('Kh?ch h?ng th?n thi?t',                'Khách hàng thân thiết'),
('L?ch ??nh k?',                         'Lịch định kỳ'),
('C?n ?i?u ph?i CTV ca s?ng',            'Cần điều phối CTV ca sáng'),
('C?n ?i?u ph?i CTV tay ngh? cao',       'Cần điều phối CTV tay nghề cao'),
-- DonDatDichVu.YeuCauDacBiet
('D?n ph?ng kh?ch v? lau s?n g?',        'Dọn phòng khách và lau sàn gỗ'),
('T?ng v? sinh b?p v? r?a ch?n',         'Tổng vệ sinh bếp và rửa chén'),
('D?n nh? v? g?p qu?n ?o',               'Dọn nhà và gấp quần áo'),
('Lau c?a k?nh v? h?t b?i th?m',         'Lau cửa kính và hút bụi thảm'),
('V? sinh ph?ng t?m v? ph?ng ng?',       'Vệ sinh phòng tắm và phòng ngủ'),
('V? sinh b?p k? l??ng',                 'Vệ sinh bếp kỹ lưỡng'),
('D?n d?p t?ng th? c?n h?',              'Dọn dẹp tổng thể căn hộ'),
('Lau d?n ??nh k? tu?n',                 'Lau dọn định kỳ tuần'),
('D?n nh? ??n ng??i th?n',               'Dọn nhà đón người thân'),
('T?ng v? sinh cu?i tu?n',               'Tổng vệ sinh cuối tuần');

UPDATE DiaChiKhachHang x JOIN tmp_sua_chu s ON BINARY x.LuuYDacBiet = BINARY s.loi SET x.LuuYDacBiet = s.dung;
UPDATE DonDatDichVu x JOIN tmp_sua_chu s ON BINARY x.GhiChu = BINARY s.loi SET x.GhiChu = s.dung;
UPDATE DonDatDichVu x JOIN tmp_sua_chu s ON BINARY x.YeuCauDacBiet = BINARY s.loi SET x.YeuCauDacBiet = s.dung;
-- Hoá đơn sao chép yêu cầu đặc biệt của đơn
UPDATE ChiTietHoaDon x JOIN tmp_sua_chu s ON BINARY x.YeuCauDacBiet = BINARY s.loi SET x.YeuCauDacBiet = s.dung;

-- ----------------------------------------------------------------------------
-- 3. Kiểm tra (mong đợi: 66 dịch vụ có mô tả mới, 0 mô tả trùng, 0 dòng còn dấu '?')
-- ----------------------------------------------------------------------------
SELECT COUNT(*) AS soDichVuCoMoTaMoi FROM DichVu d JOIN tmp_mo_ta m ON m.ten = d.TenDichVu;
SELECT d.id, d.TenDichVu AS dichVuChuaCoMoTaMoi FROM DichVu d LEFT JOIN tmp_mo_ta m ON m.ten = d.TenDichVu WHERE m.ten IS NULL;
SELECT COUNT(*) AS soMoTaTrung FROM (SELECT MoTaChiTiet FROM DichVu GROUP BY MoTaChiTiet HAVING COUNT(*) > 1) t;
SELECT (SELECT COUNT(*) FROM DiaChiKhachHang WHERE LuuYDacBiet REGEXP '[[:alpha:]][?][[:alpha:]]')
     + (SELECT COUNT(*) FROM DonDatDichVu WHERE GhiChu REGEXP '[[:alpha:]][?][[:alpha:]]' OR YeuCauDacBiet REGEXP '[[:alpha:]][?][[:alpha:]]')
       AS soDongConLoiDau;

DROP TEMPORARY TABLE IF EXISTS tmp_sua_chu;
DROP TEMPORARY TABLE IF EXISTS tmp_mo_ta;

COMMIT;
SET SQL_SAFE_UPDATES = @safe_updates_cu;
