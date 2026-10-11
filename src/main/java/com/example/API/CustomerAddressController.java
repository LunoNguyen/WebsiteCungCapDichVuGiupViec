package com.example.API;

import com.example.DTO.request.AddressRequest;
import com.example.DTO.response.ApiResponse;
import com.example.Service.CustomerAddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Sổ địa chỉ của khách hàng trên app: một khách hàng có nhiều địa chỉ, một địa chỉ mặc định. */
@RestController
@RequestMapping("/v1/customers/{khachHangId}/addresses")
@CrossOrigin(origins = "*")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    public CustomerAddressController(CustomerAddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(@PathVariable Integer khachHangId) {
        return handle("Danh sách địa chỉ.", () -> addressService.danhSach(khachHangId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@PathVariable Integer khachHangId,
                                                                   @Valid @RequestBody AddressRequest req) {
        return handle("Đã thêm địa chỉ.", () -> addressService.them(khachHangId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable Integer khachHangId,
                                                                   @PathVariable Integer id,
                                                                   @Valid @RequestBody AddressRequest req) {
        return handle("Đã cập nhật địa chỉ.", () -> addressService.sua(khachHangId, id, req));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setDefault(@PathVariable Integer khachHangId,
                                                                       @PathVariable Integer id) {
        return handle("Đã đặt làm địa chỉ mặc định.", () -> addressService.chonMacDinh(khachHangId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer khachHangId,
                                                                   @PathVariable Integer id) {
        return handle("Đã xoá địa chỉ.", () -> {
            addressService.xoa(khachHangId, id);
            return Map.of("id", id);
        });
    }

    private static <T> ResponseEntity<ApiResponse<T>> handle(String message, Supplier<T> action) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(message, action.get()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi xử lý địa chỉ: " + e.getMessage()));
        }
    }
}
