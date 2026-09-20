package com.example.Repository;

import com.example.Model.BieuMauTaiSan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BieuMauTaiSanRepository extends JpaRepository<BieuMauTaiSan, Integer> { }
