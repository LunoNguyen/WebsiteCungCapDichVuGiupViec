package com.example.Repository;

import com.example.Model.PhanCongCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhanCongCTVRepository extends JpaRepository<PhanCongCTV, Integer> { }
