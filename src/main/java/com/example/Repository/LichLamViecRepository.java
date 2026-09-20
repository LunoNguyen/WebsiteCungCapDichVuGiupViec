package com.example.Repository;

import com.example.Model.LichLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, Integer> { }
