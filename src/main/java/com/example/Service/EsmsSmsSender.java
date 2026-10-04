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
     * Loại tin nhắn eSMS (xem tài liệu eSMS, tuỳ gói tài khoản):
     *   2 = Brandname CSKH (cần brandname đã đăng ký, gửi kèm tham số Brandname)
     *   các loại đầu số cố định (4 / 8) không cần brandname.
     * Chỉnh bằng app.esms.sms-type cho đúng gói đã mua.
     */
    @Value("${app.esms.sms-type:4}")
    private int smsType;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String send(String soDienThoai, String noiDung) {
        // Chuẩn hoá số điện thoại: eSMS yêu cầu dạng 84xxxxxxxxx
        String phone = chuanHoaSoDienThoai(soDienThoai);
        java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;

        // Build URL GET với query params
        String url = ESMS_API_URL
                + "?ApiKey=" + java.net.URLEncoder.encode(apiKey, utf8)
                + "&SecretKey=" + java.net.URLEncoder.encode(secretKey, utf8)
                + "&Phone=" + phone
                + "&Content=" + java.net.URLEncoder.encode(noiDung, utf8)
                + "&SmsType=" + smsType;
        // Brandname chỉ áp dụng cho tin Brandname (SmsType=2); gửi kèm ở loại khác eSMS có thể từ chối
        if (smsType == 2 && brandName != null && !brandName.isBlank()) {
            url += "&Brandname=" + java.net.URLEncoder.encode(brandName, utf8);
        }

        Map<?, ?> body;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(java.net.URI.create(url), Map.class); // URI: tránh RestTemplate mã hoá lần 2
            body = response.getBody();
        } catch (Exception e) {
            log.error("[eSMS] Ngoại lệ khi gửi SMS tới {}: {}", soDienThoai, e.getMessage());
            throw new SmsSendException("Không kết nối được tới dịch vụ SMS. Vui lòng thử lại sau.", e);
        }

        String code = body != null && body.get("CodeResult") != null ? body.get("CodeResult").toString() : "";
        if (!"100".equals(code)) {
            Object loi = body != null ? body.get("ErrorMessage") : null;
            log.warn("[eSMS] Lỗi gửi SMS tới {}: code={}, message={}", soDienThoai, code, loi);
            throw new SmsSendException("Không gửi được tin nhắn tới số " + soDienThoai
                    + (loi != null ? " (" + loi + ")" : "") + ". Vui lòng thử lại sau.", null);
        }

        Object ref = body.get("SMSID");
        String maTin = "ESMS-" + (ref != null ? ref : UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        log.info("[eSMS {}] Gửi thành công tới {}", maTin, soDienThoai);
        return maTin;
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
