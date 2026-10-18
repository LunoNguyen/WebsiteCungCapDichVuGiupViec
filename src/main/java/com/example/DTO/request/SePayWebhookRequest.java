package com.example.DTO.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload SePay gửi đến Webhook khi có biến động số dư ngân hàng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayWebhookRequest {
    private Long id;                  // ID giao dịch trên SePay
    private String gateway;           // Tên ngân hàng (MBBank)
    private String transactionDate;   // Thời gian giao dịch (yyyy-MM-dd HH:mm:ss)
    private String accountNumber;     // Số tài khoản ngân hàng nhận tiền
    private String code;              // Mã code thanh toán (nếu SePay nhận diện)
    private String content;           // Nội dung chuyển khoản (chứa BTK{id})
    private String transferType;      // "in" (tiền vào) hoặc "out" (tiền ra)
    private BigDecimal transferAmount;// Số tiền nhận được
    private BigDecimal accumulated;   // Số dư sau giao dịch
    private String subAccount;        // Tài khoản phụ (nếu có)
    private String referenceCode;     // Mã giao dịch ngân hàng (FT...)
    private String description;       // Mô tả chi tiết giao dịch
}
