package com.example.Repository;

import com.example.Model.OTPXacThuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OTPXacThucRepository extends JpaRepository<OTPXacThuc, Integer> { }
