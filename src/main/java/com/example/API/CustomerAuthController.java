package com.example.API;

import com.example.DTO.request.*;
import com.example.DTO.response.ApiResponse;
import com.example.Service.CustomerApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * UC-KH01: Đăng ký tài khoản, xác thực OTP, đăng nhập Mobile
 */
@RestController
@RequestMapping("/v1/auth")
@CrossOrigin(origins = "*")
public class CustomerAuthController {

    private final CustomerApiService customerApiService;

    public CustomerAuthController(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;
    }

    /**
     * Dòng sự kiện 1-6: Khách hàng nhập form đăng ký, hệ thống kiểm tra và tạo OTP
     */
    @PostMapping("/customer/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody CustomerRegisterRequest request) {
        try {
            Map<String, Object> data = customerApiService.registerCustomer(request);
            return ResponseEntity.ok(ApiResponse.ok("Đăng ký thành công. Vui lòng xác thực mã OTP.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi xử lý đăng ký: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 7-9: Xác thực mã OTP và kích hoạt tài khoản
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        try {
            Map<String, Object> data = customerApiService.verifyOtp(request);
            return ResponseEntity.ok(ApiResponse.ok("Xác thực mã OTP thành công.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi xác thực OTP: " + e.getMessage()));
        }
    }

    /**
     * Gửi lại mã OTP
     */
    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        try {
            Map<String, Object> data = customerApiService.sendOtp(request);
            return ResponseEntity.ok(ApiResponse.ok("Mã OTP đã được gửi.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi gửi OTP: " + e.getMessage()));
        }
    }

    /**
     * Đăng nhập chuẩn cho Mobile
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> data = customerApiService.login(request);
            return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi đăng nhập: " + e.getMessage()));
        }
    }

    /**
     * Biến thể: Đăng ký / Đăng nhập qua tài khoản mạng xã hội (Google / Facebook)
     */
    @PostMapping("/social-login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        try {
            Map<String, Object> data = customerApiService.socialLogin(request);
            return ResponseEntity.ok(ApiResponse.ok("Đăng nhập mạng xã hội thành công.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi đăng nhập mạng xã hội: " + e.getMessage()));
        }
    }
}
