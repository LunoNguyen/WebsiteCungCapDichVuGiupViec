package com.example.Repository;

import com.example.Model.DonDatDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DonDatDichVuRepository extends JpaRepository<DonDatDichVu, Integer> { }
