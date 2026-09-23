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
 * Khoi tao du lieu mau phong phu vao MySQL neu cac bang nghiep vu chua co du lieu.
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
    @Transactional
    public void run(String... args) {
        if (dichVuRepository.count() > 0) {
            return; // Da co du lieu
        }

        System.out.println(">>> DANG KHOI TAO DU LIEU NGHIEP VU MAU VAO DATABASE...");

        // 1. LoaiDichVu
        LoaiDichVu ldv1 = loaiDichVuRepository.save(LoaiDichVu.builder()
                .maLoaiDichVu("LDV-01").tenLoaiDichVu("Dọn dẹp vệ sinh").moTa("Dịch vụ làm sạch và sắp xếp không gian sống").thuTuHienThi(1).trangThai("HienThi").build());
        LoaiDichVu ldv2 = loaiDichVuRepository.save(LoaiDichVu.builder()
                .maLoaiDichVu("LDV-02").tenLoaiDichVu("Nấu ăn gia đình").moTa("Nấu các món ăn chuẩn vị gia đình ấm cúng").thuTuHienThi(2).trangThai("HienThi").build());
        LoaiDichVu ldv3 = loaiDichVuRepository.save(LoaiDichVu.builder()
                .maLoaiDichVu("LDV-03").tenLoaiDichVu("Giặt ủi & Chăm sóc vải").moTa("Giặt hấp, là ủi quần áo cao cấp").thuTuHienThi(3).trangThai("HienThi").build());
        LoaiDichVu ldv4 = loaiDichVuRepository.save(LoaiDichVu.builder()
                .maLoaiDichVu("LDV-04").tenLoaiDichVu("Chăm sóc người thân").moTa("Trông trẻ và hỗ trợ người cao tuổi").thuTuHienThi(4).trangThai("HienThi").build());

        // 2. DichVu
        DichVu dv1 = dichVuRepository.save(DichVu.builder()
                .maDichVu("DV-01").loaiDichVu(ldv1).tenDichVu("Dọn dẹp nhà theo giờ").moTaChiTiet("Quét dọn, lau sàn, lau bụi nội thất, dọn phòng khách và bếp.").donViTinh("Giờ").trangThai("HoatDong").build());
        DichVu dv2 = dichVuRepository.save(DichVu.builder()
                .maDichVu("DV-02").loaiDichVu(ldv1).tenDichVu("Tổng vệ sinh nhà cửa").moTaChiTiet("Vệ sinh chuyên sâu toàn diện căn hộ hoặc nhà phố nhiều tầng.").donViTinh("Gói").trangThai("HoatDong").build());
        DichVu dv3 = dichVuRepository.save(DichVu.builder()
                .maDichVu("DV-03").loaiDichVu(ldv2).tenDichVu("Nấu ăn theo bữa").moTaChiTiet("Đi chợ, sơ chế thực phẩm và nấu bữa ăn ngon 3-4 món.").donViTinh("Bữa").trangThai("HoatDong").build());
        DichVu dv4 = dichVuRepository.save(DichVu.builder()
                .maDichVu("DV-04").loaiDichVu(ldv3).tenDichVu("Giặt sấy & Ủi đồ").moTaChiTiet("Phân loại, giặt thơm tho và ủi phẳng phiu từng bộ quần áo.").donViTinh("Kg").trangThai("HoatDong").build());
        DichVu dv5 = dichVuRepository.save(DichVu.builder()
                .maDichVu("DV-05").loaiDichVu(ldv4).tenDichVu("Trông trẻ tại nhà").moTaChiTiet("Trông giữ, vui chơi cùng bé và cho bé ăn đúng giờ.").donViTinh("Giờ").trangThai("HoatDong").build());

        // 3. BangGiaDichVu
        bangGiaDichVuRepository.save(BangGiaDichVu.builder()
                .maBangGia("BG-01").dichVu(dv1).loaiHinhDat("TheoLan").donViTinh("Giờ").donGia(new BigDecimal("80000")).ngayApDung(LocalDate.of(2026, 1, 1)).trangThai("DangApDung").build());
        bangGiaDichVuRepository.save(BangGiaDichVu.builder()
                .maBangGia("BG-02").dichVu(dv2).loaiHinhDat("TheoLan").donViTinh("Gói").donGia(new BigDecimal("700000")).ngayApDung(LocalDate.of(2026, 1, 1)).trangThai("DangApDung").build());
        bangGiaDichVuRepository.save(BangGiaDichVu.builder()
                .maBangGia("BG-03").dichVu(dv3).loaiHinhDat("TheoLan").donViTinh("Bữa").donGia(new BigDecimal("180000")).ngayApDung(LocalDate.of(2026, 1, 1)).trangThai("DangApDung").build());
        bangGiaDichVuRepository.save(BangGiaDichVu.builder()
                .maBangGia("BG-04").dichVu(dv4).loaiHinhDat("TheoLan").donViTinh("Kg").donGia(new BigDecimal("120000")).ngayApDung(LocalDate.of(2026, 1, 1)).trangThai("DangApDung").build());
        bangGiaDichVuRepository.save(BangGiaDichVu.builder()
                .maBangGia("BG-05").dichVu(dv5).loaiHinhDat("TheoLan").donViTinh("Giờ").donGia(new BigDecimal("150000")).ngayApDung(LocalDate.of(2026, 1, 1)).trangThai("DangApDung").build());

        // 4. KhachHang & DiaChiKhachHang
        KhachHang kh1 = khachHangRepository.save(KhachHang.builder().maKhachHang("KH-001").hoTen("Nguyễn Thị Hoa").soDienThoai("0901234567").email("hoa.nguyen@gmail.com").ngayDangKy(LocalDate.of(2026, 1, 15)).trangThai("HoatDong").build());
        KhachHang kh2 = khachHangRepository.save(KhachHang.builder().maKhachHang("KH-002").hoTen("Trần Văn Minh").soDienThoai("0912345678").email("minh.tran@gmail.com").ngayDangKy(LocalDate.of(2026, 2, 10)).trangThai("HoatDong").build());
        KhachHang kh3 = khachHangRepository.save(KhachHang.builder().maKhachHang("KH-003").hoTen("Phạm Thị Lan").soDienThoai("0923456789").email("lan.pham@gmail.com").ngayDangKy(LocalDate.of(2026, 3, 5)).trangThai("HoatDong").build());
        KhachHang kh4 = khachHangRepository.save(KhachHang.builder().maKhachHang("KH-004").hoTen("Lê Thị Thu").soDienThoai("0934567890").email("thu.le@gmail.com").ngayDangKy(LocalDate.of(2026, 4, 18)).trangThai("HoatDong").build());
        KhachHang kh5 = khachHangRepository.save(KhachHang.builder().maKhachHang("KH-005").hoTen("Nguyễn Văn Đức").soDienThoai("0945678901").email("duc.nguyen@gmail.com").ngayDangKy(LocalDate.of(2026, 5, 22)).trangThai("HoatDong").build());
        KhachHang kh6 = khachHangRepository.save(KhachHang.builder().maKhachHang("KH-006").hoTen("Vũ Thị Hạnh").soDienThoai("0956789012").email("hanh.vu@gmail.com").ngayDangKy(LocalDate.of(2026, 6, 8)).trangThai("HoatDong").build());

        DiaChiKhachHang dc1 = diaChiKhachHangRepository.save(DiaChiKhachHang.builder().maDiaChi("DC-001").khachHang(kh1).diaChiChiTiet("123 Lê Lợi, P. Bến Nghé, Quận 1, TP.HCM").laMacDinh(true).trangThai("HoatDong").build());
        DiaChiKhachHang dc2 = diaChiKhachHangRepository.save(DiaChiKhachHang.builder().maDiaChi("DC-002").khachHang(kh2).diaChiChiTiet("45 Cộng Hòa, P. 4, Q. Tân Bình, TP.HCM").laMacDinh(true).trangThai("HoatDong").build());
        DiaChiKhachHang dc3 = diaChiKhachHangRepository.save(DiaChiKhachHang.builder().maDiaChi("DC-003").khachHang(kh3).diaChiChiTiet("78 Xô Viết Nghệ Tĩnh, Q. Bình Thạnh, TP.HCM").laMacDinh(true).trangThai("HoatDong").build());
        DiaChiKhachHang dc4 = diaChiKhachHangRepository.save(DiaChiKhachHang.builder().maDiaChi("DC-004").khachHang(kh4).diaChiChiTiet("234 Nguyễn Thị Minh Khai, Quận 3, TP.HCM").laMacDinh(true).trangThai("HoatDong").build());
        DiaChiKhachHang dc5 = diaChiKhachHangRepository.save(DiaChiKhachHang.builder().maDiaChi("DC-005").khachHang(kh5).diaChiChiTiet("56 Nguyễn Thị Thập, Quận 7, TP.HCM").laMacDinh(true).trangThai("HoatDong").build());
        DiaChiKhachHang dc6 = diaChiKhachHangRepository.save(DiaChiKhachHang.builder().maDiaChi("DC-006").khachHang(kh6).diaChiChiTiet("89 Phan Xích Long, Q. Phú Nhuận, TP.HCM").laMacDinh(true).trangThai("HoatDong").build());

        // 5. CongTacVien
        CongTacVien ctv1 = congTacVienRepository.save(CongTacVien.builder().maCongTacVien("CTV-001").hoTen("Trần Thị Mai").soDienThoai("0981112233").noiCuTru("Quận 1, TP.HCM").diemDanhGia(new BigDecimal("4.9")).capDo("UuTu").trangThai("HoatDong").ngayDangKy(LocalDate.of(2026, 1, 10)).build());
        CongTacVien ctv2 = congTacVienRepository.save(CongTacVien.builder().maCongTacVien("CTV-002").hoTen("Lê Văn Hùng").soDienThoai("0982223344").noiCuTru("Tân Bình, TP.HCM").diemDanhGia(new BigDecimal("4.8")).capDo("Thuong").trangThai("HoatDong").ngayDangKy(LocalDate.of(2026, 2, 5)).build());
        CongTacVien ctv3 = congTacVienRepository.save(CongTacVien.builder().maCongTacVien("CTV-003").hoTen("Nguyễn Thị Kim").soDienThoai("0983334455").noiCuTru("Bình Thạnh, TP.HCM").diemDanhGia(new BigDecimal("4.7")).capDo("Thuong").trangThai("HoatDong").ngayDangKy(LocalDate.of(2026, 3, 1)).build());
        CongTacVien ctv4 = congTacVienRepository.save(CongTacVien.builder().maCongTacVien("CTV-004").hoTen("Phạm Thị Nga").soDienThoai("0984445566").noiCuTru("Quận 3, TP.HCM").diemDanhGia(new BigDecimal("5.0")).capDo("UuTu").trangThai("HoatDong").ngayDangKy(LocalDate.of(2026, 3, 20)).build());
        CongTacVien ctv5 = congTacVienRepository.save(CongTacVien.builder().maCongTacVien("CTV-005").hoTen("Đặng Văn Long").soDienThoai("0985556677").noiCuTru("Quận 7, TP.HCM").diemDanhGia(new BigDecimal("4.5")).capDo("Moi").trangThai("HoatDong").ngayDangKy(LocalDate.of(2026, 5, 12)).build());

        // 6. ChuongTrinhKhuyenMai & MaCoupon
        ChuongTrinhKhuyenMai km1 = chuongTrinhKhuyenMaiRepository.save(ChuongTrinhKhuyenMai.builder()
                .maChuongTrinh("KM-2026-01").tenChuongTrinh("Chào đón khách hàng mới Neatify").moTa("Giảm ngay 20% cho đơn đặt dịch vụ đầu tiên").loaiGiam("PhanTram").giaTriGiam(new BigDecimal("20.00")).giaTriGiamToiDa(new BigDecimal("100000")).dieuKienToiThieu(new BigDecimal("200000")).ngayBatDau(LocalDate.of(2026, 1, 1)).ngayKetThuc(LocalDate.of(2026, 12, 31)).trangThai("DangHoatDong").build());
        ChuongTrinhKhuyenMai km2 = chuongTrinhKhuyenMaiRepository.save(ChuongTrinhKhuyenMai.builder()
                .maChuongTrinh("KM-2026-02").tenChuongTrinh("Ưu đãi cuối tuần thảnh thơi").moTa("Giảm trực tiếp 50.000đ cho đơn cuối tuần").loaiGiam("SoTienCoDinh").giaTriGiam(new BigDecimal("50000")).dieuKienToiThieu(new BigDecimal("300000")).ngayBatDau(LocalDate.of(2026, 6, 1)).ngayKetThuc(LocalDate.of(2026, 11, 30)).trangThai("DangHoatDong").build());
        ChuongTrinhKhuyenMai km3 = chuongTrinhKhuyenMaiRepository.save(ChuongTrinhKhuyenMai.builder()
                .maChuongTrinh("KM-2026-03").tenChuongTrinh("Mùa tựu trường - Gia đình an tâm").moTa("Giảm 15% gói trông trẻ và nấu ăn").loaiGiam("PhanTram").giaTriGiam(new BigDecimal("15.00")).giaTriGiamToiDa(new BigDecimal("80000")).dieuKienToiThieu(new BigDecimal("250000")).ngayBatDau(LocalDate.of(2026, 8, 15)).ngayKetThuc(LocalDate.of(2026, 10, 15)).trangThai("DangHoatDong").build());

        maKhuyenMaiRepository.save(MaKhuyenMai.builder().maKhuyenMai("CP-001").chuongTrinhKhuyenMai(km1).codeKhuyenMai("NEAT20").soLuotToiDa(500).soLuotDaDung(128).trangThai("HoatDong").build());
        maKhuyenMaiRepository.save(MaKhuyenMai.builder().maKhuyenMai("CP-002").chuongTrinhKhuyenMai(km2).codeKhuyenMai("WEEKEND50").soLuotToiDa(300).soLuotDaDung(95).trangThai("HoatDong").build());
        maKhuyenMaiRepository.save(MaKhuyenMai.builder().maKhuyenMai("CP-003").chuongTrinhKhuyenMai(km3).codeKhuyenMai("FAMILY15").soLuotToiDa(200).soLuotDaDung(42).trangThai("HoatDong").build());

        // 7. DonDatDichVu
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElse(null);

        DonDatDichVu d1 = donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260920-01").khachHang(kh1).diaChi(dc1).nhanVienTiepNhan(nv).dichVu(dv1)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 9, 20)).gioBatDau(LocalTime.of(8, 0)).gioKetThuc(LocalTime.of(11, 0))
                .chiPhiGoc(new BigDecimal("240000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("240000")).trangThai("HoanThanh")
                .ngayTao(LocalDateTime.of(2026, 9, 19, 14, 30)).build());

        DonDatDichVu d2 = donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260920-02").khachHang(kh2).diaChi(dc2).nhanVienTiepNhan(nv).dichVu(dv3)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 9, 20)).gioBatDau(LocalTime.of(16, 0)).gioKetThuc(LocalTime.of(18, 0))
                .chiPhiGoc(new BigDecimal("180000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("180000")).trangThai("DangThucHien")
                .ngayTao(LocalDateTime.of(2026, 9, 20, 8, 15)).build());

        DonDatDichVu d3 = donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260921-01").khachHang(kh3).diaChi(dc3).nhanVienTiepNhan(nv).dichVu(dv5)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 9, 21)).gioBatDau(LocalTime.of(9, 0)).gioKetThuc(LocalTime.of(12, 0))
                .chiPhiGoc(new BigDecimal("450000")).soTienGiam(new BigDecimal("90000")).thanhTien(new BigDecimal("360000")).trangThai("DaXacNhan")
                .ngayTao(LocalDateTime.of(2026, 9, 20, 10, 0)).build());

        DonDatDichVu d4 = donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260919-01").khachHang(kh4).diaChi(dc4).nhanVienTiepNhan(nv).dichVu(dv4)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 9, 19)).gioBatDau(LocalTime.of(14, 0)).gioKetThuc(LocalTime.of(16, 0))
                .chiPhiGoc(new BigDecimal("120000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("120000")).trangThai("HoanThanh")
                .ngayTao(LocalDateTime.of(2026, 9, 18, 16, 45)).build());

        DonDatDichVu d5 = donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260922-01").khachHang(kh5).diaChi(dc5).nhanVienTiepNhan(nv).dichVu(dv2)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 9, 22)).gioBatDau(LocalTime.of(8, 0)).gioKetThuc(LocalTime.of(14, 0))
                .chiPhiGoc(new BigDecimal("700000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("700000")).trangThai("ChoDuyet")
                .ngayTao(LocalDateTime.of(2026, 9, 20, 19, 20)).build());

        // Them cac don hang cac thang truoc de tinh doanh thu bieu do
        donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260415-01").khachHang(kh1).diaChi(dc1).dichVu(dv1)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 4, 15)).gioBatDau(LocalTime.of(8, 0))
                .chiPhiGoc(new BigDecimal("520000000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("520000000")).trangThai("HoanThanh")
                .ngayTao(LocalDateTime.of(2026, 4, 14, 9, 0)).build());
        donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260518-01").khachHang(kh2).diaChi(dc2).dichVu(dv2)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 5, 18)).gioBatDau(LocalTime.of(8, 0))
                .chiPhiGoc(new BigDecimal("680000000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("680000000")).trangThai("HoanThanh")
                .ngayTao(LocalDateTime.of(2026, 5, 17, 10, 0)).build());
        donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260620-01").khachHang(kh3).diaChi(dc3).dichVu(dv1)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 6, 20)).gioBatDau(LocalTime.of(8, 0))
                .chiPhiGoc(new BigDecimal("910000000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("910000000")).trangThai("HoanThanh")
                .ngayTao(LocalDateTime.of(2026, 6, 19, 11, 0)).build());
        donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260722-01").khachHang(kh4).diaChi(dc4).dichVu(dv3)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 7, 22)).gioBatDau(LocalTime.of(8, 0))
                .chiPhiGoc(new BigDecimal("1240000000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("1240000000")).trangThai("HoanThanh")
                .ngayTao(LocalDateTime.of(2026, 7, 21, 14, 0)).build());
        donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260825-01").khachHang(kh5).diaChi(dc5).dichVu(dv5)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 8, 25)).gioBatDau(LocalTime.of(8, 0))
                .chiPhiGoc(new BigDecimal("1620000000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("1620000000")).trangThai("HoanThanh")
                .ngayTao(LocalDateTime.of(2026, 8, 24, 15, 0)).build());
        donDatDichVuRepository.save(DonDatDichVu.builder()
                .maDonDat("DD-20260910-01").khachHang(kh6).diaChi(dc6).dichVu(dv2)
                .loaiHinhDat("TheoLan").ngayThucHien(LocalDate.of(2026, 9, 10)).gioBatDau(LocalTime.of(8, 0))
                .chiPhiGoc(new BigDecimal("1850000000")).soTienGiam(BigDecimal.ZERO).thanhTien(new BigDecimal("1850000000")).trangThai("HoanThanh")
                .ngayTao(LocalDateTime.of(2026, 9, 9, 16, 0)).build());

        // 8. KhieuNai
        khieuNaiRepository.save(KhieuNai.builder()
                .maKhieuNai("KN-20260920-001").donDat(d1).khachHang(kh1).loaiVanDe("TaiSanHuHong")
                .noiDung("Khách hàng phản ánh bình sứ trang trí bị vỡ sau khi CTV dọn dẹp. Khách hàng không đồng ý mức hỗ trợ 500k của CSKH và yêu cầu ban Giám đốc xem xét bồi thường 2.000.000đ.")
                .trangThai("LeoCao").ngayGui(LocalDateTime.of(2026, 9, 20, 11, 30)).build());

        khieuNaiRepository.save(KhieuNai.builder()
                .maKhieuNai("KN-20260918-002").donDat(d4).khachHang(kh4).loaiVanDe("ChatLuongDichVu")
                .noiDung("Khách hàng phản ánh quần áo sau khi giặt ủi còn sót một vài vết ố trên áo sơ mi trắng.")
                .trangThai("DangXuLy").ngayGui(LocalDateTime.of(2026, 9, 19, 18, 0)).build());

        khieuNaiRepository.save(KhieuNai.builder()
                .maKhieuNai("KN-20260915-003").donDat(d2).khachHang(kh2).loaiVanDe("ThaiFDoPhucVu")
                .noiDung("CTV đến muộn 20 phút so với giờ hẹn nhưng chưa chủ động gọi điện báo trước cho khách hàng.")
                .trangThai("DaGiaiQuyet").ngayGui(LocalDateTime.of(2026, 9, 15, 17, 0)).ngayGiaiQuyet(LocalDateTime.of(2026, 9, 16, 10, 0)).build());

        // 9. DanhGia
        danhGiaRepository.save(DanhGia.builder()
                .maDanhGia("DG-001").donDat(d1).khachHang(kh1).congTacVien(ctv1)
                .diemChatLuong(5).diemThaiDo(5).nhanXet("Chị Mai làm việc rất nhanh nhẹn, cẩn thận, dọn dẹp sạch bóng từng góc nhà. Rất hài lòng!")
                .trangThai("HienThi").ngayDanhGia(LocalDateTime.of(2026, 9, 20, 12, 0)).build());

        danhGiaRepository.save(DanhGia.builder()
                .maDanhGia("DG-002").donDat(d4).khachHang(kh4).congTacVien(ctv4)
                .diemChatLuong(5).diemThaiDo(5).nhanXet("Quần áo ủi rất phẳng, gấp gọn gàng thơm tho. Đúng giờ và lễ phép.")
                .trangThai("HienThi").ngayDanhGia(LocalDateTime.of(2026, 9, 19, 17, 30)).build());

        // 10. ThongBao
        thongBaoRepository.save(ThongBao.builder()
                .maThongBao("TB-20260920-01").tieuDe("Triển khai quy trình an toàn lao động và bảo quản tài sản khách hàng")
                .noiDung("Tất cả CTV bắt buộc kiểm tra tài sản dễ vỡ trước khi tiến hành dọn dẹp và báo cáo ngay nếu phát hiện đồ vật có giá trị cao.")
                .nguoiGui("Hoàng Thị Dung").nhomNhan("CongTacVien").trangThai("DaGui").thoiGianGui(LocalDateTime.of(2026, 9, 20, 8, 0)).build());

        thongBaoRepository.save(ThongBao.builder()
                .maThongBao("TB-20260918-02").tieuDe("Khởi động chiến dịch khuyến mại chào đón mùa lễ hội quý 4/2026")
                .noiDung("Phòng Marketing ra mắt chương trình ưu đãi tri ân khách hàng thân thiết với mã giảm giá lên đến 20%.")
                .nguoiGui("Phạm Văn Minh").nhomNhan("KhachHang").trangThai("DaGui").thoiGianGui(LocalDateTime.of(2026, 9, 18, 9, 30)).build());

        thongBaoRepository.save(ThongBao.builder()
                .maThongBao("TB-20260915-03").tieuDe("Kế hoạch đào tạo nghiệp vụ nâng cao cho CTV tháng 10/2026")
                .noiDung("Lịch đào tạo kỹ năng nấu ăn tiêu chuẩn gia đình và sử dụng thiết bị gia dụng thông minh.")
                .nguoiGui("Nguyễn Thị Hạnh").nhomNhan("NhanVien").trangThai("DaGui").thoiGianGui(LocalDateTime.of(2026, 9, 15, 14, 0)).build());

        System.out.println(">>> KHOI TAO DU LIEU NGHIEP VU DATABASE THANH CONG!");
    }
}
