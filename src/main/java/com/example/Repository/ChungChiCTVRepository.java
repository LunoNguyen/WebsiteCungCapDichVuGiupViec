package com.example.Repository;

import com.example.Model.ChungChiCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChungChiCTVRepository extends JpaRepository<ChungChiCTV, Integer> { }
