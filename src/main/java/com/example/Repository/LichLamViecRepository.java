package com.example.Repository;

import com.example.Model.LichLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, Integer> {
    List<LichLamViec> findByCongTacVien_IdOrderByNgayLamAscGioBatDauAsc(Integer congTacVienId);
    List<LichLamViec> findByCongTacVien_IdAndTrangThaiOrderByNgayLamAscGioBatDauAsc(Integer congTacVienId, String trangThai);
    List<LichLamViec> findByCongTacVien_IdAndNgayLamBetweenOrderByNgayLamAscGioBatDauAsc(Integer congTacVienId, LocalDate from, LocalDate to);
    List<LichLamViec> findByPhanCong_DonDat_Id(Integer donDatId);

    // Dịch vụ của đơn nằm ở ChiTietDonDat (dd.chiTietList), DonDatDichVu không còn cột dichVuId
    @Query("SELECT DISTINCT l FROM LichLamViec l "
         + "JOIN FETCH l.congTacVien ctv "
         + "LEFT JOIN FETCH l.phanCong pc "
         + "LEFT JOIN FETCH pc.donDat dd "
         + "LEFT JOIN FETCH dd.khachHang kh "
         + "LEFT JOIN FETCH dd.diaChi dc "
         + "ORDER BY l.ngayLam ASC, l.gioBatDau ASC")
    List<LichLamViec> findAllWithDetails();

    @Query("SELECT DISTINCT l FROM LichLamViec l "
         + "JOIN FETCH l.congTacVien ctv "
         + "LEFT JOIN FETCH l.phanCong pc "
         + "LEFT JOIN FETCH pc.donDat dd "
         + "LEFT JOIN FETCH dd.khachHang kh "
         + "LEFT JOIN FETCH dd.diaChi dc "
         + "WHERE ctv.id = :ctvId "
         + "ORDER BY l.ngayLam ASC, l.gioBatDau ASC")
    List<LichLamViec> findByCongTacVienIdWithDetails(@org.springframework.data.repository.query.Param("ctvId") Integer ctvId);

    /**
     * Lấy bản ghi và KHÓA dòng đó trong CSDL (SELECT ... FOR UPDATE) cho tới khi giao dịch kết thúc.
     * Hai người cùng sửa một bản ghi sẽ lần lượt thực hiện: người sau thấy dữ liệu người trước đã lưu.
     * Chỉ gọi bên trong phương thức có @Transactional.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM LichLamViec e WHERE e.id = :id")
    Optional<LichLamViec> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);

    /** Số lịch (chưa hủy) của một CTV bị trùng giờ với khoảng [batDau, ketThuc) trong ngày. */
    @Query("SELECT COUNT(l) FROM LichLamViec l WHERE l.congTacVien.id = :ctvId AND l.ngayLam = :ngay "
         + "AND l.trangThai <> 'Huy' AND l.gioBatDau < :ketThuc "
         + "AND (l.gioKetThuc IS NULL OR l.gioKetThuc > :batDau)")
    long demLichTrungGio(@org.springframework.data.repository.query.Param("ctvId") Integer ctvId,
                         @org.springframework.data.repository.query.Param("ngay") LocalDate ngay,
                         @org.springframework.data.repository.query.Param("batDau") java.time.LocalTime batDau,
                         @org.springframework.data.repository.query.Param("ketThuc") java.time.LocalTime ketThuc);
}
