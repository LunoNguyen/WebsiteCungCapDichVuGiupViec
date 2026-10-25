-- Đơn nhiều dịch vụ: mỗi phân công ghi CTV làm dịch vụ (dòng ChiTietDonDat) nào.
-- NULL = phân công cả đơn (đơn một dịch vụ và mọi dữ liệu cũ), nên không cần sửa dữ liệu hiện có.
ALTER TABLE PhanCongCTV ADD COLUMN chiTietDonDatId INT NULL;
ALTER TABLE PhanCongCTV ADD CONSTRAINT fk_phancong_chitiet
    FOREIGN KEY (chiTietDonDatId) REFERENCES ChiTietDonDat(id);
