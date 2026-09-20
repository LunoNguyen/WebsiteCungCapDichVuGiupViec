package com.example.Repository;

import com.example.Model.BangGiaDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BangGiaDichVuRepository extends JpaRepository<BangGiaDichVu, Integer> { }
