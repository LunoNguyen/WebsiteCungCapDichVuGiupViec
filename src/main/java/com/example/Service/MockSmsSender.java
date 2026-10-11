package com.example.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SMS mô phỏng: không gửi ra ngoài, chỉ ghi nội dung vào log ứng dụng.
 * Được dùng khi KHÔNG có nhà cung cấp SMS thật nào được kích hoạt.
 * Để dùng eSMS thật, cấu hình: app.sms.provider=esms trong application.properties.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public String send(String soDienThoai, String noiDung) {
        String maTin = "SMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[SMS mô phỏng {}] Gửi tới {}: {}", maTin, soDienThoai, noiDung);
        return maTin;
    }

    @Override
    public boolean laMoPhong() {
        return true;
    }
}
