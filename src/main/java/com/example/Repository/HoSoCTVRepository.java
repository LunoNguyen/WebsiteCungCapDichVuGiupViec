package com.example.Repository;

import com.example.Model.HoSoCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoSoCTVRepository extends JpaRepository<HoSoCTV, Integer> {
    List<HoSoCTV> findByCongTacVien_Id(Integer congTacVienId);
}
