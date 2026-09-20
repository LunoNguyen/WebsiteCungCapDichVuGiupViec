package com.example.Repository;

import com.example.Model.BoiThuong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoiThuongRepository extends JpaRepository<BoiThuong, Integer> { }
