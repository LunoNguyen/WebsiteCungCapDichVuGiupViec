package com.example.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Gửi SMS thật qua eSMS.vn REST API.
 * Kích hoạt khi property: app.sms.provider=esms
 * Cần cấu hình: app.esms.api-key, app.esms.secret-key, app.esms.brand-name
 *
 * Tài liệu API: https://esms.vn/api-document/gui-tin-nhan-theo-dau-so-dien-thoai
 *
 * Cách đăng ký:
 *   1. Vào https://esms.vn → Đăng ký tài khoản
 *   2. Mua gói SMS OTP (có gói thử miễn phí)
 *   3. Lấy API Key và Secret Key ở phần "Thông tin tài khoản"
 *   4. Đăng ký Brandname (tên hiển thị của SMS) hoặc dùng "Brandname" mặc định
 *   5. Điền vào application.properties:
 *      app.sms.provider=esms
 *      app.esms.api-key=YOUR_API_KEY
 *      app.esms.secret-key=YOUR_SECRET_KEY
 *      app.esms.brand-name=YOUR_BRAND
 */
@Component("esmsSmsSender")
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "esms")
public class EsmsSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(EsmsSmsSender.class);
    private static final String ESMS_API_URL = "https://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_get";

    @Value("${app.esms.api-key}")
    private String apiKey;

    @Value("${app.esms.secret-key}")
    private String secretKey;

    @Value("${app.esms.brand-name:Neatify}")
    private String brandName;

    /**
     * Loại tin nhắn eSMS:
     *   2 = SMS quảng cáo (cần đăng ký brandname)
     *   4 = SMS chăm sóc khách hàng / OTP (không cần brandname, đầu số cố định)
     *   8 = SMS đầu số ngẫu nhiên
     * Dùng loại 4 cho OTP vì phù hợp nhất và không cần đăng ký brandname trước.
     */
    @Value("${app.esms.sms-type:4}")
    private int smsType;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String send(String soDienThoai, String noiDung) {
        try {
            // Chuẩn hoá số điện thoại: eSMS yêu cầu dạng 84xxxxxxxxx
            String phone = chuanHoaSoDienThoai(soDienThoai);

            // Build URL GET với query params
            String url = ESMS_API_URL
                    + "?ApiKey=" + apiKey
                    + "&SecretKey=" + secretKey
                    + "&Phone=" + phone
                    + "&Content=" + java.net.URLEncoder.encode(noiDung, java.nio.charset.StandardCharsets.UTF_8)
                    + "&SmsType=" + smsType
                    + "&Brandname=" + java.net.URLEncoder.encode(brandName, java.nio.charset.StandardCharsets.UTF_8);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<?, ?> body = response.getBody();
                Object codeObj = body.get("CodeResult");
                String code = codeObj != null ? codeObj.toString() : "";
                if ("100".equals(code)) {
                    Object ref = body.get("SMSID");
                    String maTin = ref != null ? "ESMS-" + ref : "ESMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    log.info("[eSMS {}] Gửi thành công tới {}", maTin, soDienThoai);
                    return maTin;
                } else {
                    log.warn("[eSMS] Lỗi gửi SMS tới {}: code={}, message={}", soDienThoai, code, body.get("ErrorMessage"));
                }
            }
        } catch (Exception e) {
            log.error("[eSMS] Ngoại lệ khi gửi SMS tới {}: {}", soDienThoai, e.getMessage(), e);
        }

        // Fallback: log nội dung ra console nếu gửi thất bại (tránh crash nghiệp vụ)
        String fallbackId = "ESMS-FAIL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        log.warn("[eSMS Fallback {}] Không gửi được SMS tới {}. Nội dung: {}", fallbackId, soDienThoai, noiDung);
        return fallbackId;
    }

    /**
     * Chuẩn hoá số điện thoại Việt Nam sang dạng 84xxxxxxxxx mà eSMS yêu cầu.
     * Hỗ trợ cả dạng 0xxxxxxxxx và +84xxxxxxxxx
     */
    private String chuanHoaSoDienThoai(String sdt) {
        if (sdt == null) return "";
        String s = sdt.trim().replaceAll("[^0-9+]", "");
        if (s.startsWith("+84")) return s.substring(1); // +84... → 84...
        if (s.startsWith("84")) return s;               // 84... giữ nguyên
        if (s.startsWith("0")) return "84" + s.substring(1); // 0... → 84...
        return "84" + s;
    }
}
