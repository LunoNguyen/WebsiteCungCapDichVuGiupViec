package com.example.Repository;

import com.example.Model.DichVuCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DichVuCTVRepository extends JpaRepository<DichVuCTV, Integer> {
    List<DichVuCTV> findByCongTacVien_Id(Integer congTacVienId);
    List<DichVuCTV> findByDichVu_Id(Integer dichVuId);
}
