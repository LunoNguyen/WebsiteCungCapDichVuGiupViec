package com.example.Repository;

import com.example.Model.ThongBaoNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThongBaoNguoiDungRepository extends JpaRepository<ThongBaoNguoiDung, Integer> { }
