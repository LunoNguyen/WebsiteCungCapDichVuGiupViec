package com.example.Service;

import com.example.DTO.request.SePayWebhookRequest;
import com.example.Model.DonDatDichVu;
import com.example.Model.HoaDon;
import com.example.Repository.DonDatDichVuRepository;
import com.example.Repository.HoaDonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xử lý thanh toán tự động qua SePay Webhook và VietQR.
 */
@Service
public class SePayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(SePayPaymentService.class);

    private final DonDatDichVuRepository donDatDichVuRepository;
    private final HoaDonRepository hoaDonRepository;
    private final CustomerApiService customerApiService;
    private final RealtimeService realtimeService;

    @Value("${sepay.bank.account-no:0336676173}")
    private String configuredAccountNo;

    @Value("${sepay.transfer.prefix:BTK}")
    private String transferPrefix;

    @Value("${sepay.api-token:}")
    private String apiToken;

    public SePayPaymentService(DonDatDichVuRepository donDatDichVuRepository,
                               HoaDonRepository hoaDonRepository,
                               CustomerApiService customerApiService,
                               RealtimeService realtimeService) {
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.hoaDonRepository = hoaDonRepository;
        this.customerApiService = customerApiService;
        this.realtimeService = realtimeService;
    }

    /**
     * Xác thực API Token từ SePay (nếu cấu hình)
     */
    public boolean verifyToken(String authHeader) {
        if (apiToken == null || apiToken.isBlank()) {
            return true; // Không cấu hình token thì chấp nhận webhook
        }
        if (authHeader == null || authHeader.isBlank()) {
            return false;
        }
        String token = authHeader.replace("Apikey ", "").replace("Bearer ", "").trim();
        return apiToken.equals(token);
    }

    /**
     * Xử lý webhook từ SePay khi có biến động số dư tài khoản
     */
    public Map<String, Object> processWebhook(SePayWebhookRequest req) {
        log.info("[SePay Webhook] Nhận thông báo giao dịch: ID={}, Gateway={}, Amount={}, Content={}",
                req.getId(), req.getGateway(), req.getTransferAmount(), req.getContent());

        // 1. Chỉ xử lý tiền vào ("in")
        if (req.getTransferType() != null && !"in".equalsIgnoreCase(req.getTransferType())) {
            log.info("[SePay Webhook] Bỏ qua vì đây là giao dịch tiền ra (transferType={})", req.getTransferType());
            return Map.of("success", true, "message", "Ignored: out transfer");
        }

        // 2. Tìm đơn hàng qua nội dung chuyển khoản
        String content = req.getContent();
        if (content == null || content.isBlank()) {
            log.warn("[SePay Webhook] Nội dung chuyển khoản trống!");
            return Map.of("success", false, "message", "Empty transfer content");
        }

        Integer donDatId = extractDonDatId(content);
        if (donDatId == null) {
            log.warn("[SePay Webhook] Không tìm thấy mã đơn đặt từ nội dung: '{}'", content);
            return Map.of("success", false, "message", "Cannot extract order ID from content: " + content);
        }

        // 3. Tiến hành xác nhận thanh toán (tạo biên lai, phát hành đơn sang CTV)
        String nguon = "SePay Auto (" + (req.getReferenceCode() != null ? req.getReferenceCode() : "GD-" + req.getId()) + ")";
        Map<String, Object> receipt = customerApiService.xacNhanThanhToanChuyenKhoan(donDatId, nguon);

        // 4. Phát tín hiệu realtime
        realtimeService.phatSuKien("/tai-khoan/don-hang/" + donDatId);
        realtimeService.phatSuKien("/cskh/don-dat-dich-vu");

        log.info("[SePay Webhook] Đã xác nhận thanh toán thành công cho đơn đặt ID={}!", donDatId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Thanh toán thành công");
        response.put("donDatId", donDatId);
        response.put("receipt", receipt);
        return response;
    }

    /**
     * API giả lập thanh toán (Dành cho việc test chức năng miễn phí, không tốn quota SePay)
     */
    public Map<String, Object> mockPayment(Integer donDatId) {
        log.info("[Mock Payment] Yêu cầu giả lập thanh toán cho đơn đặt ID={}", donDatId);
        DonDatDichVu don = donDatDichVuRepository.findById(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt ID: " + donDatId));

        String nguon = "Giả lập thanh toán (Test không tốn phí)";
        Map<String, Object> receipt = customerApiService.xacNhanThanhToanChuyenKhoan(donDatId, nguon);

        realtimeService.phatSuKien("/tai-khoan/don-hang/" + donDatId);
        realtimeService.phatSuKien("/cskh/don-dat-dich-vu");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Giả lập thanh toán thành công cho đơn " + don.getMaDonDat());
        response.put("donDatId", donDatId);
        response.put("maDonDat", don.getMaDonDat());
        response.put("receipt", receipt);
        return response;
    }

    /**
     * Trích xuất donDatId từ nội dung chuyển khoản
     * Hỗ trợ:
     * - BTK30, BTK 30
     * - DD-ABCDE (mã đơn đặt)
     * - HD-123 (mã hoá đơn)
     */
    public Integer extractDonDatId(String content) {
        if (content == null) return null;
        String normalized = content.toUpperCase().trim();

        // 1. Khớp cú pháp cấu hình (mặc định BTK...)
        Pattern btkPattern = Pattern.compile("(?i)(?:" + Pattern.quote(transferPrefix) + "|DONHANG|DON)\\s*(\\d+)");
        Matcher btkMatcher = btkPattern.matcher(normalized);
        if (btkMatcher.find()) {
            try {
                return Integer.parseInt(btkMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // 2. Khớp mã hóa đơn HD-xxx hoặc THANHTOAN HDxxx
        Pattern hdPattern = Pattern.compile("(?i)HD[-_]?([A-Za-z0-9]+)");
        Matcher hdMatcher = hdPattern.matcher(normalized);
        if (hdMatcher.find()) {
            String maHd = hdMatcher.group(0).toUpperCase();
            HoaDon hd = hoaDonRepository.findByMaHoaDon(maHd)
                    .or(() -> hoaDonRepository.findByMaHoaDon("HD-" + hdMatcher.group(1).toUpperCase()))
                    .orElse(null);
            if (hd != null && hd.getDonDat() != null) {
                return hd.getDonDat().getId();
            }
        }

        // 3. Khớp mã đơn đặt DD-xxx
        Pattern ddPattern = Pattern.compile("(?i)DD[-_]?([A-Za-z0-9]+)");
        Matcher ddMatcher = ddPattern.matcher(normalized);
        if (ddMatcher.find()) {
            String maDon = ddMatcher.group(0).toUpperCase();
            DonDatDichVu don = donDatDichVuRepository.findByMaDonDat(maDon)
                    .or(() -> donDatDichVuRepository.findByMaDonDat("DD-" + ddMatcher.group(1).toUpperCase()))
                    .orElse(null);
            if (don != null) {
                return don.getId();
            }
        }

        return null;
    }
}
