package com.example.Service;

/**
 * Gửi tin nhắn SMS tới số điện thoại.
 * Bản đang dùng là {@link MockSmsSender} (mô phỏng, ghi ra log).
 * Khi tích hợp nhà cung cấp thật (eSMS, SpeedSMS...), chỉ cần thêm một
 * implementation khác và đánh dấu @Primary, không phải sửa nơi gọi.
 */
public interface SmsSender {

    /**
     * @return mã tham chiếu của tin đã gửi (để ghi nhật ký)
     */
    String send(String soDienThoai, String noiDung);
}
