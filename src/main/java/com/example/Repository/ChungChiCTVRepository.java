package com.example.Repository;

import com.example.Model.ChungChiCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChungChiCTVRepository extends JpaRepository<ChungChiCTV, Integer> {
    List<ChungChiCTV> findByCongTacVien_Id(Integer congTacVienId);
}
