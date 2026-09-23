package com.example.Repository;

import com.example.Model.KhuVucCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuVucCTVRepository extends JpaRepository<KhuVucCTV, Integer> {
    List<KhuVucCTV> findByCongTacVien_Id(Integer congTacVienId);
}
