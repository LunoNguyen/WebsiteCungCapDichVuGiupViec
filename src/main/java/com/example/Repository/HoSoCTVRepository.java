package com.example.Repository;

import com.example.Model.HoSoCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoSoCTVRepository extends JpaRepository<HoSoCTV, Integer> { }
