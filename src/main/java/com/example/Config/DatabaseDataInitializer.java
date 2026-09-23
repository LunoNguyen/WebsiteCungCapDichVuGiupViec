package com.example.Config;

import com.example.Model.*;
import com.example.Repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Khoi tao du lieu mau phong phu vao MySQL neu cac bang nghiep vu chua co du
 * lieu.
 * Dam bao he thong goi du lieu dong 100% tu Database cho cac trang quan tri.
 */
@Component
public class DatabaseDataInitializer implements CommandLineRunner {

        private final LoaiDichVuRepository loaiDichVuRepository;
        private final DichVuRepository dichVuRepository;
        private final BangGiaDichVuRepository bangGiaDichVuRepository;
        private final KhachHangRepository khachHangRepository;
        private final DiaChiKhachHangRepository diaChiKhachHangRepository;
        private final CongTacVienRepository congTacVienRepository;
        private final ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository;
        private final MaKhuyenMaiRepository maKhuyenMaiRepository;
        private final DonDatDichVuRepository donDatDichVuRepository;
        private final KhieuNaiRepository khieuNaiRepository;
        private final DanhGiaRepository danhGiaRepository;
        private final ThongBaoRepository thongBaoRepository;
        private final NhanVienRepository nhanVienRepository;

        public DatabaseDataInitializer(
                        LoaiDichVuRepository loaiDichVuRepository,
                        DichVuRepository dichVuRepository,
                        BangGiaDichVuRepository bangGiaDichVuRepository,
                        KhachHangRepository khachHangRepository,
                        DiaChiKhachHangRepository diaChiKhachHangRepository,
                        CongTacVienRepository congTacVienRepository,
                        ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository,
                        MaKhuyenMaiRepository maKhuyenMaiRepository,
                        DonDatDichVuRepository donDatDichVuRepository,
                        KhieuNaiRepository khieuNaiRepository,
                        DanhGiaRepository danhGiaRepository,
                        ThongBaoRepository thongBaoRepository,
                        NhanVienRepository nhanVienRepository) {
                this.loaiDichVuRepository = loaiDichVuRepository;
                this.dichVuRepository = dichVuRepository;
                this.bangGiaDichVuRepository = bangGiaDichVuRepository;
                this.khachHangRepository = khachHangRepository;
                this.diaChiKhachHangRepository = diaChiKhachHangRepository;
                this.congTacVienRepository = congTacVienRepository;
                this.chuongTrinhKhuyenMaiRepository = chuongTrinhKhuyenMaiRepository;
                this.maKhuyenMaiRepository = maKhuyenMaiRepository;
                this.donDatDichVuRepository = donDatDichVuRepository;
                this.khieuNaiRepository = khieuNaiRepository;
                this.danhGiaRepository = danhGiaRepository;
                this.thongBaoRepository = thongBaoRepository;
                this.nhanVienRepository = nhanVienRepository;
        }

        @Override
        public void run(String... args) throws Exception {
                // Sẵn sàng cho việc nạp dữ liệu mẫu
        }
}
