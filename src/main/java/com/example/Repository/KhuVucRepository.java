package com.example.Repository;

import com.example.Model.KhuVuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhuVucRepository extends JpaRepository<KhuVuc, Integer> {
    Optional<KhuVuc> findByMaKhuVuc(String maKhuVuc);
    Optional<KhuVuc> findFirstByQuanHuyenAndTinhThanh(String quanHuyen, String tinhThanh);
    Optional<KhuVuc> findFirstByTenKhuVucAndTinhThanh(String tenKhuVuc, String tinhThanh);
    List<KhuVuc> findByTinhThanh(String tinhThanh);
}
