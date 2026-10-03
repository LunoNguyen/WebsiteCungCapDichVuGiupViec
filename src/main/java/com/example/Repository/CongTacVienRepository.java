package com.example.Repository;

import com.example.Model.CongTacVien;
import com.example.Model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CongTacVienRepository extends JpaRepository<CongTacVien, Integer> {
    Optional<CongTacVien> findByTaiKhoan(TaiKhoan taiKhoan);
    Optional<CongTacVien> findByTaiKhoan_Id(Integer taiKhoanId);

    /**
     * Lấy bản ghi và KHÓA dòng đó trong CSDL (SELECT ... FOR UPDATE) cho tới khi giao dịch kết thúc.
     * Hai người cùng sửa một bản ghi sẽ lần lượt thực hiện: người sau thấy dữ liệu người trước đã lưu.
     * Chỉ gọi bên trong phương thức có @Transactional.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM CongTacVien e WHERE e.id = :id")
    Optional<CongTacVien> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
}
