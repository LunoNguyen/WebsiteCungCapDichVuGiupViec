package com.example.Repository;

import com.example.Model.PhanCongCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhanCongCTVRepository extends JpaRepository<PhanCongCTV, Integer> {
    List<PhanCongCTV> findByCongTacVien_IdOrderByThoiGianPhanCongDesc(Integer congTacVienId);
    List<PhanCongCTV> findByCongTacVien_IdAndTrangThaiOrderByThoiGianPhanCongDesc(Integer congTacVienId, String trangThai);
    Optional<PhanCongCTV> findByDonDat_IdAndCongTacVien_Id(Integer donDatId, Integer congTacVienId);
    Optional<PhanCongCTV> findByDonDat_Id(Integer donDatId);

    /** Kiểm tra xem có phân công active cho đơn không */
    boolean existsByDonDat_IdAndTrangThaiIn(Integer donDatId, java.util.List<String> trangThaiList);

    /** Mọi phân công của một đơn (một đơn có thể cần nhiều cộng tác viên). */
    List<PhanCongCTV> findAllByDonDat_Id(Integer donDatId);

    /**
     * Lấy bản ghi và KHÓA dòng đó trong CSDL (SELECT ... FOR UPDATE) cho tới khi giao dịch kết thúc.
     * Hai người cùng sửa một bản ghi sẽ lần lượt thực hiện: người sau thấy dữ liệu người trước đã lưu.
     * Chỉ gọi bên trong phương thức có @Transactional.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM PhanCongCTV e WHERE e.id = :id")
    Optional<PhanCongCTV> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
}

