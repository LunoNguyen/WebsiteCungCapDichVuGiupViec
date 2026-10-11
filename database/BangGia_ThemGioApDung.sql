-- ============================================================================
-- BẢNG GIÁ: THÊM GIỜ ÁP DỤNG
--   Đổi kiểu 2 cột của bảng BangGiaDichVu từ DATE sang DATETIME (không thêm, không xoá cột):
--     NgayApDung   DATE -> DATETIME NOT NULL  (giá trị cũ thành 00:00 của ngày đó)
--     NgayKetThuc  DATE -> DATETIME NULL      (giá trị cũ thành 23:59:59, giữ nghĩa "hết ngày đó mới hết hạn")
--   Đi cùng phiên bản code dùng LocalDateTime cho BangGiaDichVu (ddl-auto=validate:
--   chạy code mới trên CSDL chưa đổi, hoặc ngược lại, ứng dụng sẽ không khởi động được).
--
-- Cách chạy:
--   MySQL Workbench: mở file, bấm ⚡ (Execute All)
--   Dòng lệnh: mysql -u root -p --default-character-set=utf8mb4 GiupViecDB < BangGia_ThemGioApDung.sql
-- Chạy lại lần nữa cũng không sao (ngày kết thúc đã là 23:59:59 vẫn giữ nguyên).
-- ============================================================================

SET @safe_updates_cu = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

ALTER TABLE BangGiaDichVu
    MODIFY COLUMN NgayApDung  DATETIME NOT NULL,
    MODIFY COLUMN NgayKetThuc DATETIME NULL;

-- Ngày kết thúc cũ (chỉ có ngày) nghĩa là áp dụng hết ngày đó
UPDATE BangGiaDichVu
SET NgayKetThuc = TIMESTAMP(DATE(NgayKetThuc), '23:59:59')
WHERE NgayKetThuc IS NOT NULL AND TIME(NgayKetThuc) = '00:00:00';

SET SQL_SAFE_UPDATES = @safe_updates_cu;

-- Kiểm tra
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BangGiaDichVu' AND COLUMN_NAME IN ('NgayApDung', 'NgayKetThuc');
