package com.example.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Nội dung chính sách của bTaskee (Điều khoản sử dụng, Quy trình khiếu nại, Chính sách bảo mật), dùng chung cho
 * trang web /chinh-sach và API /v1/chinh-sach của ứng dụng.
 *
 * Nguồn: resources/chinh-sach.json, chuyển từ tài liệu "tổng hợp nội dung … điều khoản, chính sách.docx".
 * Mỗi tài liệu: {ma, ten, moTa, noiDung: [khối]}; khối là {loai: tieuDe|tieuDeNho|doan, noiDung},
 * {loai: ds, muc: [..]} hoặc {loai: bang, cot: [..], dong: [[..]]}.
 */
@Service
public class ChinhSachService {

    private final List<Map<String, Object>> taiLieu;

    @SuppressWarnings("unchecked")
    public ChinhSachService() {
        try (InputStream in = ChinhSachService.class.getResourceAsStream("/chinh-sach.json")) {
            if (in == null) throw new IllegalStateException("Thiếu resources/chinh-sach.json");
            Map<String, Object> goc = new ObjectMapper().readValue(in, Map.class);
            this.taiLieu = List.copyOf((List<Map<String, Object>>) goc.get("taiLieu"));
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được nội dung chính sách", e);
        }
    }

    /** Ba tài liệu theo thứ tự hiển thị. */
    public List<Map<String, Object>> danhSach() {
        return taiLieu;
    }

    public Optional<Map<String, Object>> theoMa(String ma) {
        if (ma == null) return Optional.empty();
        return taiLieu.stream().filter(t -> ma.equals(t.get("ma"))).findFirst();
    }
}
