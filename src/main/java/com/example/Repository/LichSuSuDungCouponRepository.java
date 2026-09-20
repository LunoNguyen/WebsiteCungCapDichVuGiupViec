package com.example.Repository;

import com.example.Model.LichSuSuDungCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuSuDungCouponRepository extends JpaRepository<LichSuSuDungCoupon, Integer> { }
