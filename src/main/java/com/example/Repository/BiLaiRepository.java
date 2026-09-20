package com.example.Repository;

import com.example.Model.BiLai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BiLaiRepository extends JpaRepository<BiLai, Integer> { }
