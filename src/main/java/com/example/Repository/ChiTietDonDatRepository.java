package com.example.Repository;

import com.example.Model.ChiTietDonDat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ChiTietDonDatRepository extends JpaRepository<ChiTietDonDat, Integer> {
    long countByDichVu_Id(Integer dichVuId);


    List<ChiTietDonDat> findByDonDat_Id(Integer donDatId);

    /** Lấy chi tiết của nhiều đơn một lần, kèm dịch vụ (tránh N+1 khi dựng danh sách) */
    @Query("SELECT ct FROM ChiTietDonDat ct JOIN FETCH ct.dichVu "
         + "WHERE ct.donDat.id IN :ids")
    List<ChiTietDonDat> findByDonDatIds(@Param("ids") Collection<Integer> ids);
}