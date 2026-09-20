package com.example.Repository;

import com.example.Model.KhieuNai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KhieuNaiRepository extends JpaRepository<KhieuNai, Integer> { }
