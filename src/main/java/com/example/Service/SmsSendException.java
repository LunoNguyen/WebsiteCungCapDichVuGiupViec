package com.example.Service;

/** Nhà cung cấp SMS từ chối hoặc không gửi được tin nhắn. Thông điệp có thể hiển thị cho người dùng. */
public class SmsSendException extends RuntimeException {
    public SmsSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
