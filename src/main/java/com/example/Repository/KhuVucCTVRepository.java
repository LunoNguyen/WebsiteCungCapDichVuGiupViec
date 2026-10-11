package com.example.Repository;

import com.example.Model.KhuVucCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuVucCTVRepository extends JpaRepository<KhuVucCTV, Integer> {
    List<KhuVucCTV> findByCongTacVien_Id(Integer congTacVienId);

    /** CTV đăng ký nhận việc tại một khu vực (thông báo đơn mới trong khu vực). */
    List<KhuVucCTV> findByKhuVuc_IdAndTrangThai(Integer khuVucId, String trangThai);

    /** Số cộng tác viên đang hoạt động nhận việc tại một quận, huyện. */
    long countByKhuVuc_IdAndTrangThaiAndCongTacVien_TrangThai(Integer khuVucId, String trangThai, String trangThaiCtv);
}
