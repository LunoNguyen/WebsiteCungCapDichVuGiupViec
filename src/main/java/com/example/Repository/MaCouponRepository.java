package com.example.Repository;

import com.example.Model.MaCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaCouponRepository extends JpaRepository<MaCoupon, Integer> { }
