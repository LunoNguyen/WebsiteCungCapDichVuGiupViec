package com.example.Repository;

import com.example.Model.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {
    Optional<HoaDon> findByDonDat_Id(Integer donDatId);
    Optional<HoaDon> findByMaHoaDon(String maHoaDon);

    /**
     * Lấy bản ghi và KHÓA dòng đó trong CSDL (SELECT ... FOR UPDATE) cho tới khi giao dịch kết thúc.
     * Hai người cùng sửa một bản ghi sẽ lần lượt thực hiện: người sau thấy dữ liệu người trước đã lưu.
     * Chỉ gọi bên trong phương thức có @Transactional.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM HoaDon e WHERE e.id = :id")
    Optional<HoaDon> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM HoaDon e WHERE e.donDat.id = :donDatId")
    Optional<HoaDon> findByDonDatIdForUpdate(@org.springframework.data.repository.query.Param("donDatId") Integer donDatId);
}
