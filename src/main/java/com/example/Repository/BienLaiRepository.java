package com.example.Repository;

import com.example.Model.BienLai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BienLaiRepository extends JpaRepository<BienLai, Integer> { 
}