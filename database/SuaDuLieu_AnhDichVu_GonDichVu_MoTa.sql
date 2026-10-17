-- ============================================================================
-- SỬA DỮ LIỆU DỊCH VỤ
--   Chạy SAU ChiaBienTheDichVu.sql và BangGia_ThemGioApDung.sql.
--   1. Thêm cột HinhAnh (ảnh dịch vụ) vào bảng DichVu và gán ảnh cho từng nhóm.
--   2. Sửa chữ tiếng Việt bị lỗi mã hoá (dạng "TÃªn Ä‘Äƒng nháº­p") trong mọi cột chữ
--      của CSDL.
--   3. Gọn dữ liệu: mỗi nhóm dịch vụ (cùng MaDichVu / cùng tên) chỉ giữ 2 dịch vụ:
--      1 dịch vụ đặt theo lần + 1 gói tháng (nhóm không có gói tháng giữ 1).
--      Đơn hàng, bảng giá, đăng ký CTV của dịch vụ bị xoá được chuyển sang dịch vụ còn lại
--      cùng nhóm, nên không mất đơn hàng cũ.
--   4. Viết lại mô tả: mỗi dịch vụ có mô tả riêng, nêu rõ thời gian, diện tích, số người.
--
-- Nên sao lưu trước khi chạy (mysqldump GiupViecDB > backup.sql).
-- Chạy được nhiều lần (cột đã có thì bỏ qua, chữ đã sửa thì không sửa lại).
-- Sau khi chạy:
--   - xoá cache Redis:
--     docker exec redis sh -c "redis-cli --scan --pattern 'neatify:*' | xargs -r redis-cli del"
--   - khởi động lại ứng dụng (entity DichVu đã có trường hinhAnh -> cột HinhAnh phải có trước).
-- ============================================================================

SET NAMES utf8mb4;
SET @safe_updates_cu = @@SQL_SAFE_UPDATES;
SET @sql_mode_cu = @@SESSION.sql_mode;
SET SQL_SAFE_UPDATES = 0;

-- ----------------------------------------------------------------------------
-- 0. Thủ tục hỗ trợ (CREATE PROCEDURE / ALTER TABLE tự commit nên tạo trước transaction)
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_them_cot_hinh_anh;
DROP PROCEDURE IF EXISTS sp_sua_loi_ma_hoa;
DROP PROCEDURE IF EXISTS sp_chuyen_khoa_ngoai;

DELIMITER $$

-- Thêm cột HinhAnh nếu chưa có
CREATE PROCEDURE sp_them_cot_hinh_anh()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND LOWER(TABLE_NAME) = 'dichvu'
                     AND LOWER(COLUMN_NAME) = 'hinhanh') THEN
        ALTER TABLE DichVu ADD COLUMN HinhAnh VARCHAR(255) NULL AFTER MoTaChiTiet;
    END IF;
END$$

-- Sửa chữ bị mã hoá 2 lần (UTF-8 bị đọc thành cp1252 rồi lưu lại thành UTF-8) ở mọi cột chữ.
-- Chỉ cập nhật khi chuỗi chuyển ngược hợp lệ, ngắn hơn và không sinh ký tự '?'.
CREATE PROCEDURE sp_sua_loi_ma_hoa()
BEGIN
    DECLARE xong INT DEFAULT 0;
    DECLARE v_bang VARCHAR(64);
    DECLARE v_cot VARCHAR(64);
    DECLARE v_cs VARCHAR(32);
    DECLARE cur CURSOR FOR
        SELECT c.TABLE_NAME, c.COLUMN_NAME, c.CHARACTER_SET_NAME
        FROM information_schema.COLUMNS c
        JOIN information_schema.TABLES t
          ON t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME AND t.TABLE_TYPE = 'BASE TABLE'
        WHERE c.TABLE_SCHEMA = DATABASE()
          AND c.DATA_TYPE IN ('char', 'varchar', 'tinytext', 'text', 'mediumtext', 'longtext')
          AND c.CHARACTER_SET_NAME IN ('utf8mb4', 'utf8mb3', 'utf8')
          AND LOWER(c.COLUMN_NAME) NOT LIKE '%matkhau%'
          AND LOWER(c.COLUMN_NAME) NOT LIKE '%password%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET xong = 1;

    OPEN cur;
    lap: LOOP
        FETCH cur INTO v_bang, v_cot, v_cs;
        IF xong = 1 THEN LEAVE lap; END IF;
        IF v_cs = 'utf8' THEN SET v_cs = 'utf8mb3'; END IF;

        SET @moi = CONCAT('CONVERT(CAST(CONVERT(`', v_cot, '` USING latin1) AS BINARY) USING ', v_cs, ')');
        SET @bin = CONCAT('`', v_cot, '` COLLATE ', v_cs, '_bin');
        SET @sql = CONCAT(
            'UPDATE `', v_bang, '` SET `', v_cot, '` = ', @moi,
            ' WHERE (', @bin, ' LIKE ''%Ã%'' OR ', @bin, ' LIKE ''%Ä%'' OR ', @bin, ' LIKE ''%Æ%'''
            , ' OR ', @bin, ' LIKE ''%áº%'' OR ', @bin, ' LIKE ''%á»%'' OR ', @bin, ' LIKE ''%â€%'')',
            ' AND ', @moi, ' IS NOT NULL',
            ' AND CHAR_LENGTH(', @moi, ') < CHAR_LENGTH(`', v_cot, '`)',
            ' AND (LOCATE(''?'', ', @moi, ') = 0 OR LOCATE(''?'', `', v_cot, '`) > 0)');
        PREPARE st FROM @sql;
        EXECUTE st;
        DEALLOCATE PREPARE st;
    END LOOP;
    CLOSE cur;
END$$

-- Chuyển mọi khoá ngoại trỏ tới bảng p_bang (trừ các bảng trong p_bo_qua) theo bảng ánh xạ
-- tmp_anh_xa(cu, moi). Dò khoá ngoại trong information_schema nên không bỏ sót bảng nào.
CREATE PROCEDURE sp_chuyen_khoa_ngoai(IN p_bang VARCHAR(64), IN p_bo_qua VARCHAR(255))
BEGIN
    DECLARE xong INT DEFAULT 0;
    DECLARE v_bang VARCHAR(64);
    DECLARE v_cot VARCHAR(64);
    DECLARE cur CURSOR FOR
        SELECT k.TABLE_NAME, k.COLUMN_NAME
        FROM information_schema.KEY_COLUMN_USAGE k
        WHERE k.TABLE_SCHEMA = DATABASE()
          AND LOWER(k.REFERENCED_TABLE_NAME) = LOWER(p_bang)
          AND FIND_IN_SET(LOWER(k.TABLE_NAME), LOWER(p_bo_qua)) = 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET xong = 1;

    OPEN cur;
    lap: LOOP
        FETCH cur INTO v_bang, v_cot;
        IF xong = 1 THEN LEAVE lap; END IF;
        SET @sql = CONCAT('UPDATE `', v_bang, '` x JOIN tmp_anh_xa m ON m.cu = x.`', v_cot,
                          '` SET x.`', v_cot, '` = m.moi');
        PREPARE st FROM @sql;
        EXECUTE st;
        DEALLOCATE PREPARE st;
    END LOOP;
    CLOSE cur;
END$$

DELIMITER ;

-- ----------------------------------------------------------------------------
-- 1. Cột ảnh dịch vụ
-- ----------------------------------------------------------------------------
CALL sp_them_cot_hinh_anh();

START TRANSACTION;

-- ----------------------------------------------------------------------------
-- 2. Sửa chữ tiếng Việt bị lỗi mã hoá trong toàn bộ CSDL
--    (tắt strict mode tạm thời để chuỗi không chuyển được chỉ sinh cảnh báo)
-- ----------------------------------------------------------------------------
SET SESSION sql_mode = '';
CALL sp_sua_loi_ma_hoa();
SET SESSION sql_mode = @sql_mode_cu;

-- ----------------------------------------------------------------------------
-- 3. Chọn dịch vụ được giữ: mỗi (MaDichVu, LoaiHinhDat) giữ 1 dòng,
--    ưu tiên dòng có tên trong danh sách dưới (dịch vụ gốc), nếu không có thì giữ id nhỏ nhất.
-- ----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_mo_ta;
CREATE TEMPORARY TABLE tmp_mo_ta (
    ten   VARCHAR(200) NOT NULL PRIMARY KEY,
    anh   VARCHAR(255) NOT NULL,
    moTa  TEXT NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_mo_ta (ten, anh, moTa) VALUES
-- DV101 Dọn dẹp nhà theo giờ
('Dọn dẹp nhà theo giờ (2 giờ)', '/images/btaskee/don-dep-nha-cua-tron.webp',
 'Gói dọn dẹp 2 giờ do 1 nhân viên thực hiện, phù hợp căn hộ hoặc nhà phố dưới 55m² (1–2 phòng ngủ). Trong 2 giờ nhân viên quét và lau sàn toàn bộ các phòng, lau bụi bàn ghế, kệ tủ và cửa sổ trong tầm với, rửa bát và lau mặt bếp, cọ rửa 1 nhà vệ sinh, gom và đổ rác, sắp xếp lại đồ đạc gọn gàng. Nhân viên mang theo dụng cụ cơ bản (chổi, cây lau, khăn, nước lau sàn). Không bao gồm giặt rèm, sofa, nệm và vệ sinh trên cao quá 2m.'),
('Dọn dẹp nhà theo giờ (Gói 8 buổi/tháng, 2 giờ/buổi)', '/images/btaskee/don-dep-nha-cua-tron.webp',
 'Gói dọn dẹp định kỳ 8 buổi trong 1 tháng (2 buổi/tuần), mỗi buổi 2 giờ với 1 nhân viên, dành cho nhà dưới 55m². Mỗi buổi gồm lau sàn, lau bụi, vệ sinh bếp và nhà vệ sinh, đổ rác, sắp xếp đồ đạc. Khách chọn các thứ cố định trong tuần, hệ thống ưu tiên giữ một cộng tác viên để quen nếp nhà. Buổi bị huỷ khi báo trước 24 giờ được dời sang ngày khác trong tháng. Tổng 16 giờ làm việc/tháng, rẻ hơn khoảng 8% so với đặt lẻ.'),
-- DV102 Tổng vệ sinh nhà
('Tổng vệ sinh nhà (tối đa 100m², 3 người/3 giờ)', '/images/btaskee/tasker-nhom.webp',
 'Tổng vệ sinh nhà có diện tích đến 100m² (căn hộ 2–3 phòng ngủ hoặc nhà phố 1 trệt 1 lầu) với đội 3 nhân viên làm liên tục trong 3 giờ. Sử dụng máy hút bụi công nghiệp và hoá chất an toàn: hút bụi và lau toàn bộ sàn, chân tường; lau kính và khung cửa hai mặt trong tầm với an toàn; tẩy dầu mỡ bếp, máy hút mùi, mặt ngoài tủ bếp; tẩy cặn vòi sen, gạch ốp, bồn cầu; lau công tắc, tay nắm cửa, quạt; thu gom rác. Thích hợp trước Tết, sau sửa chữa nhỏ hoặc khi chuyển vào nhà mới.'),
('Tổng vệ sinh nhà (Gói 2 lần/tháng, tối đa 100m²)', '/images/btaskee/tasker-nhom.webp',
 'Gói tổng vệ sinh 2 lần mỗi tháng (cách nhau khoảng 2 tuần) cho nhà đến 100m². Mỗi lần 3 nhân viên làm trong 3 giờ (tổng 6 giờ/tháng) với đầy đủ hạng mục: sàn, kính, bếp, nhà vệ sinh, công tắc, quạt. Giữ nhà sạch sâu đều đặn mà không phải đặt lại mỗi lần; lịch cố định theo ngày khách chọn. Tiết kiệm khoảng 7% so với đặt lẻ 2 lần.'),
-- DV103 Vệ sinh sofa, nệm, thảm
('Vệ sinh sofa, nệm, thảm (sofa nỉ/vải 3 ghế)', '/images/btaskee/tasker-lam-viec-tron.webp',
 'Giặt sạch 1 bộ sofa nỉ hoặc vải loại 3 chỗ ngồi (dài khoảng 1,8–2,2m) tại nhà, 2 nhân viên thực hiện trong khoảng 2 giờ. Quy trình: hút bụi khô toàn bộ đệm ngồi và tựa lưng, xử lý vết bẩn cục bộ, phun dung dịch làm sạch, dùng máy phun hút lấy lại nước bẩn, khử mùi và diệt khuẩn bằng hơi nước nóng. Sofa khô hoàn toàn sau 4–6 giờ trong điều kiện thoáng gió. Sofa da, nệm, thảm vui lòng ghi chú để được báo giá riêng.'),
('Vệ sinh sofa, nệm, thảm (Gói 1 lần/tháng, sofa nỉ/vải 3 ghế)', '/images/btaskee/tasker-lam-viec-tron.webp',
 'Gói bảo dưỡng định kỳ mỗi tháng 1 lần cho 1 bộ sofa nỉ/vải 3 chỗ, mỗi lần 2 nhân viên làm khoảng 2 giờ với đầy đủ các bước hút bụi, giặt phun hút, khử mùi và diệt khuẩn. Giúp hạn chế bụi mịn, mạt bụi và mùi hôi tích tụ; phù hợp gia đình có trẻ nhỏ, thú cưng hoặc người bị dị ứng. Lịch cố định theo ngày khách chọn; rẻ hơn 10% so với đặt lẻ.'),
-- DV201 Vệ sinh máy lạnh
('Vệ sinh máy lạnh (treo tường)', '/images/btaskee/ve-sinh-may-lanh-tron.webp',
 'Vệ sinh 1 máy lạnh treo tường công suất 1–2,5 HP, 1 thợ thực hiện trong khoảng 45 phút. Thợ tháo và rửa lưới lọc, mặt nạ; xịt rửa dàn lạnh, máng nước, quạt lồng sóc bằng bơm áp lực; vệ sinh dàn nóng; thông ống thoát nước để máy không chảy nước; kiểm tra gas, dòng điện và nhiệt độ gió sau khi vệ sinh. Có bạt hứng nước, không làm bẩn tường và đồ đạc. Nên vệ sinh 3–6 tháng/lần để máy lạnh sâu và tiết kiệm điện.'),
('Vệ sinh máy lạnh (Gói 1 lần/tháng, treo tường)', '/images/btaskee/ve-sinh-may-lanh-tron.webp',
 'Gói bảo dưỡng hằng tháng cho 1 máy lạnh treo tường: mỗi tháng 1 lần, khoảng 45 phút/lần, gồm vệ sinh lưới lọc, dàn lạnh, kiểm tra gas và đường thoát nước. Phù hợp phòng chạy máy lạnh liên tục như phòng ngủ trẻ nhỏ, cửa hàng, phòng máy chủ. Phát hiện sớm hư hỏng trước khi máy ngừng chạy; rẻ hơn 10% so với đặt lẻ.'),
-- DV202 Vệ sinh máy giặt
('Vệ sinh máy giặt (cửa trước dưới 9kg, không tháo lồng)', '/images/btaskee/tasker-lam-viec-tron.webp',
 'Vệ sinh 1 máy giặt cửa trước (lồng ngang) khối lượng giặt dưới 9kg, không tháo lồng, 1 thợ làm trong khoảng 1 giờ 30 phút. Gồm chạy chu trình nước nóng với dung dịch tẩy cặn chuyên dụng, vệ sinh gioăng cao su cửa (nơi hay đọng rêu mốc), ngăn chứa bột giặt, bộ lọc xả và ống thoát, lau sạch vỏ máy. Loại bỏ mùi hôi và vết đen bám trên quần áo. Máy cửa trên hoặc cần tháo lồng vui lòng ghi chú để được báo giá riêng.'),
('Vệ sinh máy giặt (Gói 1 lần/tháng, cửa trước dưới 9kg)', '/images/btaskee/tasker-lam-viec-tron.webp',
 'Gói vệ sinh hằng tháng cho 1 máy giặt cửa trước dưới 9kg (không tháo lồng), mỗi tháng 1 lần khoảng 1 giờ 30 phút, giữ lồng giặt và gioăng cửa luôn sạch, không mùi. Phù hợp gia đình giặt nhiều, có trẻ sơ sinh hoặc người có làn da nhạy cảm; rẻ hơn 10% so với đặt lẻ.'),
-- DV203 Vệ sinh bình nóng lạnh
('Vệ sinh bình nóng lạnh (dưới 20 lít)', '/images/btaskee/tasker-lam-viec-tron.webp',
 'Vệ sinh 1 bình nóng lạnh gián tiếp dung tích dưới 20 lít (loại thường lắp cho 1 phòng tắm), 1 thợ làm trong khoảng 1 giờ. Thợ ngắt điện, xả cạn nước, súc rửa cặn vôi trong ruột bình, kiểm tra và vệ sinh thanh magie chống ăn mòn, kiểm tra rơ le nhiệt, van an toàn và dây tiếp đất. Giúp nước nóng nhanh hơn, tiết kiệm điện và giảm nguy cơ rò điện. Thay thanh magie (nếu cần) tính theo giá linh kiện.'),
('Vệ sinh bình nóng lạnh (Gói 1 lần/tháng, dưới 20 lít)', '/images/btaskee/tasker-lam-viec-tron.webp',
 'Gói kiểm tra và vệ sinh hằng tháng cho 1 bình nóng lạnh dưới 20 lít, mỗi lần khoảng 1 giờ với đầy đủ hạng mục súc rửa cặn và kiểm tra an toàn điện. Phù hợp khu vực nước nhiễm đá vôi hoặc nhà có người già, trẻ nhỏ; rẻ hơn 10% so với đặt lẻ.'),
-- DV301 Trông trẻ tại nhà
('Trông trẻ tại nhà (3 giờ)', '/images/btaskee/trong-tre-tron.webp',
 'Một bảo mẫu trông 1–2 bé từ 1 tuổi trở lên tại nhà trong 3 giờ liên tục, phù hợp khi bố mẹ đi làm nửa buổi, đi khám hoặc có việc riêng. Bảo mẫu cho bé ăn uống theo hướng dẫn của phụ huynh, chơi cùng bé, đọc truyện, hướng dẫn hoạt động phù hợp độ tuổi, vệ sinh cá nhân, thay tã và quần áo, dọn dẹp khu vực bé chơi. Bảo mẫu đã được kiểm tra lý lịch và có kiến thức sơ cứu cơ bản. Không bao gồm đưa đón bé ra ngoài và nấu ăn cho cả gia đình.'),
('Trông trẻ tại nhà (Gói 12 buổi/tháng, 3 giờ/buổi)', '/images/btaskee/trong-tre-tron.webp',
 'Gói trông trẻ 12 buổi mỗi tháng (3 buổi/tuần), mỗi buổi 3 giờ (tổng 36 giờ/tháng), ưu tiên cố định một bảo mẫu để bé quen người và nếp sinh hoạt. Phụ huynh chọn các thứ trong tuần; sau mỗi buổi bảo mẫu ghi chú tình hình ăn, ngủ, chơi của bé. Tiết kiệm khoảng 8% so với đặt lẻ.'),
-- DV302 Chăm sóc người cao tuổi
('Chăm sóc người cao tuổi (theo buổi, tối đa 4 giờ)', '/images/btaskee/cham-soc-nguoi-cao-tuoi.webp',
 'Một điều dưỡng hoặc hộ lý chăm sóc 1 người cao tuổi tại nhà trong 1 buổi tối đa 4 giờ (sáng hoặc chiều). Hỗ trợ ăn uống, nhắc và cho uống thuốc theo đơn; vệ sinh cá nhân, thay quần áo; hỗ trợ đi lại, vận động nhẹ, xoa bóp; đo huyết áp, đường huyết (nếu gia đình có máy) và ghi lại; trò chuyện, đồng hành cùng người cao tuổi. Không thực hiện thủ thuật y tế xâm lấn như tiêm, truyền dịch.'),
('Chăm sóc người cao tuổi (Gói 13 buổi/tháng)', '/images/btaskee/cham-soc-nguoi-cao-tuoi.webp',
 'Gói chăm sóc người cao tuổi tại nhà 13 buổi trong 1 tháng (khoảng 3 buổi/tuần), mỗi buổi tối đa 4 giờ, cùng nội dung chăm sóc như đặt theo buổi. Ưu tiên cố định một người chăm sóc để người cao tuổi yên tâm; gia đình nhận nhật ký sức khoẻ sau mỗi buổi. Lịch theo các thứ gia đình chọn; giá thấp hơn đặt lẻ.'),
-- DV303 Chăm sóc người bệnh tại bệnh viện
('Chăm sóc người bệnh tại bệnh viện (theo buổi, tối đa 4 giờ)', '/images/btaskee/cham-soc-nguoi-cao-tuoi.webp',
 'Một hộ lý túc trực chăm sóc 1 người bệnh nằm viện trong 1 buổi tối đa 4 giờ, giúp gia đình có thời gian nghỉ ngơi hoặc đi làm. Hỗ trợ ăn uống, vệ sinh cá nhân, thay tã, xoay trở chống loét; theo dõi dịch truyền và báo điều dưỡng khi cần; đi lấy kết quả xét nghiệm, mua đồ dùng cá nhân; trò chuyện, động viên người bệnh. Thực hiện theo nội quy bệnh viện và chỉ định của nhân viên y tế.'),
('Chăm sóc người bệnh tại bệnh viện (Gói 13 buổi/tháng)', '/images/btaskee/cham-soc-nguoi-cao-tuoi.webp',
 'Gói chăm sóc người bệnh tại bệnh viện 13 buổi trong 1 tháng, mỗi buổi tối đa 4 giờ, phù hợp người điều trị dài ngày cần người túc trực luân phiên với gia đình. Ưu tiên cố định một hộ lý để nắm rõ tình trạng người bệnh; lịch theo các thứ gia đình chọn; giá thấp hơn đặt lẻ.'),
-- DV401 Giặt sấy
('Giặt sấy (quần áo)', '/images/btaskee/tasker-lam-viec-tron.webp',
 'Giặt sấy quần áo tính theo kg, tối thiểu 3kg mỗi lần. Nhân viên nhận đồ tận nhà, phân loại đồ trắng và đồ màu, giặt máy với nước giặt và nước xả dịu nhẹ, sấy khô, gấp gọn và đóng túi, trả đồ tận nhà trong 24 giờ. Chăn, mền, drap, gối và đồ len, lụa, vest hoặc cần giặt hấp, tẩy vết bẩn đặc biệt vui lòng ghi chú để được báo giá riêng.'),
('Giặt sấy (Gói 4 lần/tháng, quần áo tối đa 5kg/lần)', '/images/btaskee/tasker-lam-viec-tron.webp',
 'Gói giặt sấy quần áo 4 lần mỗi tháng (1 lần/tuần), mỗi lần tối đa 5kg (tổng 20kg/tháng), đã gồm nhận và trả tận nhà trong 24 giờ. Phù hợp người đi làm bận rộn, sinh viên, gia đình nhỏ. Phần vượt 5kg mỗi lần tính theo đơn giá giặt sấy lẻ. Tiết kiệm khoảng 9% so với đặt từng lần.'),
-- DV402 Nấu ăn gia đình
('Nấu ăn gia đình (2–4 người, 2–3 món)', '/images/btaskee/nau-an-gia-dinh-tron.webp',
 'Một người nấu ăn đến tận nhà nấu 1 bữa cơm cho gia đình 2–4 người gồm 2–3 món (1 món mặn, 1 món rau hoặc xào, 1 món canh), làm trong khoảng 2 giờ kể cả sơ chế, nấu, bày món và rửa dọn bếp. Thực đơn theo yêu cầu của khách hoặc người nấu gợi ý. Chưa gồm chi phí nguyên liệu (khách tự chuẩn bị hoặc đặt thêm dịch vụ đi chợ hộ).'),
('Nấu ăn gia đình (Gói 12 bữa/tháng, 2–4 người, 2–3 món)', '/images/btaskee/nau-an-gia-dinh-tron.webp',
 'Gói nấu ăn 12 bữa mỗi tháng (3 bữa/tuần) cho gia đình 2–4 người, mỗi bữa 2–3 món, khoảng 2 giờ/bữa. Ưu tiên cố định người nấu để nắm khẩu vị gia đình; thực đơn được lên trước theo tuần. Chưa gồm tiền nguyên liệu. Tiết kiệm khoảng 8% so với đặt lẻ.'),
-- DV403 Đi chợ hộ
('Đi chợ hộ (tối đa 1 giờ)', '/images/btaskee/nau-an-gia-dinh-tron.webp',
 'Nhân viên đi chợ hoặc siêu thị gần nhà mua thực phẩm, đồ dùng theo danh sách khách gửi trong tối đa 1 giờ: chọn hàng tươi, kiểm tra hạn dùng, giữ hoá đơn và giao tận nhà. Tiền hàng thanh toán theo hoá đơn thực tế; đơn hàng trên 2 triệu đồng cần tạm ứng trước.'),
('Đi chợ hộ (Gói 8 lần/tháng)', '/images/btaskee/nau-an-gia-dinh-tron.webp',
 'Gói đi chợ hộ 8 lần mỗi tháng (2 lần/tuần) theo lịch cố định, mỗi lần tối đa 1 giờ, phù hợp gia đình bận rộn, người cao tuổi hoặc kết hợp với gói nấu ăn gia đình. Chưa gồm tiền hàng. Tiết kiệm khoảng 6% so với đặt lẻ.'),
-- DV501 Vệ sinh văn phòng
('Vệ sinh văn phòng (tối đa 100m², 1 người/2 giờ)', '/images/btaskee/ve-sinh-van-phong-tron.webp',
 'Vệ sinh văn phòng diện tích đến 100m² (khoảng 10–15 chỗ ngồi) với 1 nhân viên trong 2 giờ, có thể làm ngoài giờ hành chính. Gồm hút bụi, lau sàn; lau bàn làm việc, ghế, tủ hồ sơ (không xáo trộn giấy tờ); vệ sinh pantry, bồn rửa, lò vi sóng; cọ rửa nhà vệ sinh; lau cửa kính, tay nắm cửa; đổ rác và thay túi rác. Có biên bản nghiệm thu và xuất hoá đơn VAT theo yêu cầu.'),
('Vệ sinh văn phòng (Gói 12 buổi/tháng, tối đa 100m²)', '/images/btaskee/ve-sinh-van-phong-tron.webp',
 'Gói vệ sinh văn phòng định kỳ 12 buổi mỗi tháng (3 buổi/tuần), mỗi buổi 2 giờ với 1 nhân viên cho văn phòng đến 100m². Lịch cố định theo ngày doanh nghiệp chọn, có biên bản nghiệm thu hằng tháng và xuất hoá đơn VAT theo yêu cầu. Tiết kiệm khoảng 8% so với đặt lẻ.'),
-- DV502 Vệ sinh thảm văn phòng
('Vệ sinh thảm văn phòng (dưới 50m²)', '/images/btaskee/ve-sinh-van-phong-tron.webp',
 'Giặt thảm trải sàn văn phòng diện tích dưới 50m² (phòng họp, phòng giám đốc hoặc văn phòng nhỏ), 2 nhân viên làm trong khoảng 3 giờ bằng máy giặt thảm phun hút công nghiệp: hút bụi sâu, xử lý vết cà phê, mực, kẹo cao su; giặt bằng hoá chất an toàn với sợi thảm; hút khô và khử mùi. Thảm khô sau 4–6 giờ; có thể làm cuối tuần để không ảnh hưởng giờ làm việc.'),
('Vệ sinh thảm văn phòng (Gói 1 lần/tháng, dưới 50m²)', '/images/btaskee/ve-sinh-van-phong-tron.webp',
 'Gói giặt thảm văn phòng định kỳ mỗi tháng 1 lần cho diện tích dưới 50m², 2 nhân viên làm khoảng 3 giờ mỗi lần, giữ thảm sạch bụi mịn và vi khuẩn, kéo dài tuổi thọ thảm. Lịch cố định theo ngày doanh nghiệp chọn; rẻ hơn khoảng 9% so với đặt lẻ.'),
-- DV503 Phun khử khuẩn
('Phun khử khuẩn (đến 70m²)', '/images/btaskee/tasker-nhom.webp',
 'Phun khử khuẩn văn phòng hoặc nhà ở diện tích đến 70m², 2 nhân viên làm trong khoảng 1 giờ bằng máy phun ULV với dung dịch được Bộ Y tế cấp phép, diệt vi khuẩn, vi rút và nấm mốc trên bề mặt và trong không khí. Khu vực cần đóng kín khoảng 30 phút sau khi phun; dung dịch không ăn mòn thiết bị điện tử và không để lại mùi khó chịu.'),
('Phun khử khuẩn (Gói 2 lần/tháng, đến 70m²)', '/images/btaskee/tasker-nhom.webp',
 'Gói phun khử khuẩn định kỳ 2 lần mỗi tháng cho diện tích đến 70m², mỗi lần 2 nhân viên làm khoảng 1 giờ. Phù hợp văn phòng đông người, phòng khám, lớp học, nhà có người bệnh. Có giấy xác nhận sau mỗi lần phun; rẻ hơn khoảng 10% so với đặt lẻ.');

-- Ảnh mặc định theo nhóm (dùng khi dòng được giữ không có trong danh sách trên)
DROP TEMPORARY TABLE IF EXISTS tmp_anh_nhom;
CREATE TEMPORARY TABLE tmp_anh_nhom (
    ma  VARCHAR(20) NOT NULL PRIMARY KEY,
    anh VARCHAR(255) NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_anh_nhom (ma, anh) VALUES
('DV101', '/images/btaskee/don-dep-nha-cua-tron.webp'),
('DV102', '/images/btaskee/tasker-nhom.webp'),
('DV103', '/images/btaskee/tasker-lam-viec-tron.webp'),
('DV201', '/images/btaskee/ve-sinh-may-lanh-tron.webp'),
('DV202', '/images/btaskee/tasker-lam-viec-tron.webp'),
('DV203', '/images/btaskee/tasker-lam-viec-tron.webp'),
('DV301', '/images/btaskee/trong-tre-tron.webp'),
('DV302', '/images/btaskee/cham-soc-nguoi-cao-tuoi.webp'),
('DV303', '/images/btaskee/cham-soc-nguoi-cao-tuoi.webp'),
('DV401', '/images/btaskee/tasker-lam-viec-tron.webp'),
('DV402', '/images/btaskee/nau-an-gia-dinh-tron.webp'),
('DV403', '/images/btaskee/nau-an-gia-dinh-tron.webp'),
('DV501', '/images/btaskee/ve-sinh-van-phong-tron.webp'),
('DV502', '/images/btaskee/ve-sinh-van-phong-tron.webp'),
('DV503', '/images/btaskee/tasker-nhom.webp');

-- Dòng được giữ cho mỗi (MaDichVu, LoaiHinhDat)
DROP TEMPORARY TABLE IF EXISTS tmp_giu;
CREATE TEMPORARY TABLE tmp_giu (
    id       INT NOT NULL PRIMARY KEY,
    ma       VARCHAR(20) NOT NULL,
    loaiHinh VARCHAR(20) NOT NULL,
    KEY (ma, loaiHinh)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_giu (id, ma, loaiHinh)
SELECT x.id, x.MaDichVu, x.LoaiHinhDat
FROM (SELECT d.id, d.MaDichVu, d.LoaiHinhDat,
             ROW_NUMBER() OVER (PARTITION BY d.MaDichVu, d.LoaiHinhDat
                                ORDER BY (m.ten IS NOT NULL) DESC,
                                         (d.TrangThai = 'HoatDong') DESC,
                                         d.id) AS thuTu
      FROM DichVu d
      LEFT JOIN tmp_mo_ta m ON m.ten = d.TenDichVu) x
WHERE x.thuTu = 1;

-- MySQL không cho một bảng tạm xuất hiện 2 lần trong cùng câu lệnh -> tạo bản sao
DROP TEMPORARY TABLE IF EXISTS tmp_giu2;
CREATE TEMPORARY TABLE tmp_giu2 LIKE tmp_giu;
INSERT INTO tmp_giu2 SELECT * FROM tmp_giu;

DROP TEMPORARY TABLE IF EXISTS tmp_giu_nhom;
CREATE TEMPORARY TABLE tmp_giu_nhom (
    ma VARCHAR(20) NOT NULL PRIMARY KEY,
    id INT NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
INSERT INTO tmp_giu_nhom (ma, id) SELECT ma, MIN(id) FROM tmp_giu GROUP BY ma;

-- Ánh xạ dịch vụ bị xoá -> dịch vụ được giữ (cùng mã, ưu tiên cùng loại hình đặt)
DROP TEMPORARY TABLE IF EXISTS tmp_anh_xa;
CREATE TEMPORARY TABLE tmp_anh_xa (
    cu  INT NOT NULL PRIMARY KEY,
    moi INT NOT NULL
);

INSERT INTO tmp_anh_xa (cu, moi)
SELECT d.id, COALESCE(g.id, n.id)
FROM DichVu d
LEFT JOIN tmp_giu k       ON k.id = d.id
LEFT JOIN tmp_giu2 g      ON g.ma = d.MaDichVu AND g.loaiHinh = d.LoaiHinhDat
LEFT JOIN tmp_giu_nhom n  ON n.ma = d.MaDichVu
WHERE k.id IS NULL;

-- ----------------------------------------------------------------------------
-- 4. Chuyển dữ liệu liên quan sang dịch vụ được giữ
-- ----------------------------------------------------------------------------
-- 4a. Bảng giá: dòng chi tiết đơn đang trỏ tới bảng giá của dịch vụ bị xoá
--     -> bảng giá đang áp dụng mới nhất của dịch vụ được giữ (không có thì NULL)
DROP TEMPORARY TABLE IF EXISTS tmp_anh_xa_dv;
CREATE TEMPORARY TABLE tmp_anh_xa_dv LIKE tmp_anh_xa;
INSERT INTO tmp_anh_xa_dv SELECT * FROM tmp_anh_xa;

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
JOIN tmp_anh_xa_dv a ON a.cu = b.dichVuId;

UPDATE ChiTietDonDat c
JOIN tmp_anh_xa_bg m ON m.cu = c.bangGiaId
SET c.bangGiaId = m.moi;

-- 4b. Chi tiết đơn đặt chuyển sang dịch vụ được giữ (giữ nguyên đơn giá, thành tiền cũ)
UPDATE ChiTietDonDat c
JOIN tmp_anh_xa_dv a ON a.cu = c.dichVuId
SET c.dichVuId = a.moi;

-- Các bảng khác có khoá ngoại tới DichVu (nếu có), trừ các bảng xử lý riêng
CALL sp_chuyen_khoa_ngoai('DichVu', 'dichvuctv,banggiadichvu,chitietdondat');

-- 4c. CTV đã đăng ký dịch vụ bị xoá thì nhận dịch vụ được giữ cùng nhóm
INSERT INTO DichVuCTV (MaDichVuCTV, congTacVienId, dichVuId, TrangThai, NgayBatDau)
SELECT CONCAT('DVCTV-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 10))), x.congTacVienId, x.moi, 'HoatDong', CURDATE()
FROM (SELECT DISTINCT c.congTacVienId, a.moi
      FROM DichVuCTV c
      JOIN tmp_anh_xa_dv a ON a.cu = c.dichVuId) x
WHERE NOT EXISTS (SELECT 1 FROM DichVuCTV e WHERE e.congTacVienId = x.congTacVienId AND e.dichVuId = x.moi);

DELETE c FROM DichVuCTV c JOIN tmp_anh_xa_dv a ON a.cu = c.dichVuId;

-- 4d. Xoá bảng giá rồi xoá dịch vụ dư
DELETE b FROM BangGiaDichVu b JOIN tmp_anh_xa_dv a ON a.cu = b.dichVuId;
DELETE d FROM DichVu d JOIN tmp_anh_xa_dv a ON a.cu = d.id;

-- ----------------------------------------------------------------------------
-- 5. Mô tả riêng và ảnh cho từng dịch vụ còn lại
-- ----------------------------------------------------------------------------
UPDATE DichVu d
JOIN tmp_mo_ta m ON m.ten = d.TenDichVu
SET d.MoTaChiTiet = m.moTa,
    d.HinhAnh = COALESCE(NULLIF(d.HinhAnh, ''), m.anh);

-- Dịch vụ không có trong danh sách: ghép thông tin riêng (thời gian, số người, số buổi) vào mô tả
UPDATE DichVu d
LEFT JOIN tmp_mo_ta m ON m.ten = d.TenDichVu
SET d.MoTaChiTiet = CONCAT(
        d.TenDichVu, ': ',
        IF(d.SoBuoi IS NOT NULL, CONCAT(d.SoBuoi, ' buổi trong 1 tháng, '), ''),
        IF(d.ThoiGianThucHien IS NOT NULL,
           CONCAT('mỗi lần khoảng ',
                  IF(d.ThoiGianThucHien >= 60, CONCAT(FLOOR(d.ThoiGianThucHien / 60), ' giờ'), ''),
                  IF(d.ThoiGianThucHien % 60 > 0, CONCAT(IF(d.ThoiGianThucHien >= 60, ' ', ''), d.ThoiGianThucHien % 60, ' phút'), ''),
                  ', '),
           ''),
        COALESCE(d.SoNguoiThucHien, 1), ' nhân viên thực hiện, tính theo ', LOWER(d.DonViTinh), '. ',
        COALESCE(d.MoTaChiTiet, ''))
WHERE m.ten IS NULL
  AND (d.MoTaChiTiet IS NULL OR d.MoTaChiTiet NOT LIKE CONCAT(d.TenDichVu, ':%'));

UPDATE DichVu d
JOIN tmp_anh_nhom a ON a.ma = d.MaDichVu
SET d.HinhAnh = a.anh
WHERE d.HinhAnh IS NULL OR d.HinhAnh = '';

-- ----------------------------------------------------------------------------
-- 6. Kiểm tra kết quả
-- ----------------------------------------------------------------------------
SELECT d.MaDichVu,
       MIN(SUBSTRING_INDEX(d.TenDichVu, ' (', 1)) AS tenNhom,
       SUM(d.LoaiHinhDat = 'TheoLan')  AS soTheoLan,
       SUM(d.LoaiHinhDat = 'GoiThang') AS soGoiThang
FROM DichVu d
GROUP BY d.MaDichVu ORDER BY d.MaDichVu;

SELECT d.id, d.MaDichVu, d.TenDichVu, d.LoaiHinhDat, d.GiaHienTai, d.HinhAnh, LEFT(d.MoTaChiTiet, 80) AS moTa
FROM DichVu d
ORDER BY d.MaDichVu, d.LoaiHinhDat DESC;

-- Số dịch vụ có mô tả trùng nhau (mong đợi 0)
SELECT COUNT(*) AS soMoTaTrung
FROM (SELECT MoTaChiTiet FROM DichVu GROUP BY MoTaChiTiet HAVING COUNT(*) > 1) t;

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_anh_xa_bg;
DROP TEMPORARY TABLE IF EXISTS tmp_anh_xa_dv;
DROP TEMPORARY TABLE IF EXISTS tmp_anh_xa;
DROP TEMPORARY TABLE IF EXISTS tmp_giu_nhom;
DROP TEMPORARY TABLE IF EXISTS tmp_giu2;
DROP TEMPORARY TABLE IF EXISTS tmp_giu;
DROP TEMPORARY TABLE IF EXISTS tmp_anh_nhom;
DROP TEMPORARY TABLE IF EXISTS tmp_mo_ta;
DROP PROCEDURE IF EXISTS sp_them_cot_hinh_anh;
DROP PROCEDURE IF EXISTS sp_sua_loi_ma_hoa;
DROP PROCEDURE IF EXISTS sp_chuyen_khoa_ngoai;

SET SESSION sql_mode = @sql_mode_cu;
SET SQL_SAFE_UPDATES = @safe_updates_cu;
