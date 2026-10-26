package com.example.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Xác định quận, huyện cũ của TP.HCM từ toạ độ GPS: chọn quận có tâm gần nhất (file data/tam-quan-hcm.json).
 * Geocoder của điện thoại không còn trả tên quận sau sắp xếp hành chính 2025, nên khu vực nhận việc của CTV
 * dựa vào toạ độ. Điểm cách mọi tâm quá {@link #XA_NHAT_KM} km (VD vị trí mặc định của máy ảo ở Mỹ) thì bỏ qua.
 */
@Component
public class TamQuanHcm {

    static final String FILE = "data/tam-quan-hcm.json";
    /** Cần Giờ rộng nhất: điểm xa tâm huyện nhất vẫn dưới khoảng 25 km. */
    static final double XA_NHAT_KM = 30;

    public record Tam(String ten, double viDo, double kinhDo) { }

    private final List<Tam> ds = docMacDinh();

    /** Danh sách tâm các quận, huyện TP.HCM đọc từ data/tam-quan-hcm.json. */
    public List<Tam> danhSach() {
        return ds;
    }

    /** Tên quận (khớp KhuVuc.TenKhuVuc) chứa toạ độ, null nếu ngoài TP.HCM. */
    public String quanCua(double viDo, double kinhDo) {
        return quanGanNhat(ds, viDo, kinhDo);
    }

    static String quanGanNhat(List<Tam> ds, double viDo, double kinhDo) {
        Tam gan = null;
        double min = Double.MAX_VALUE;
        for (Tam t : ds) {
            double d = khoangCachKm(viDo, kinhDo, t.viDo(), t.kinhDo());
            if (d < min) {
                min = d;
                gan = t;
            }
        }
        return gan != null && min <= XA_NHAT_KM ? gan.ten() : null;
    }

    /** Khoảng cách theo công thức haversine (km). */
    static double khoangCachKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    static List<Tam> docMacDinh() {
        try (InputStream in = new ClassPathResource(FILE).getInputStream()) {
            List<Tam> kq = new ArrayList<>();
            for (JsonNode n : new ObjectMapper().readTree(in).path("quanHuyen")) {
                kq.add(new Tam(n.path("ten").asText(), n.path("viDo").asDouble(), n.path("kinhDo").asDouble()));
            }
            return List.copyOf(kq);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được " + FILE, e);
        }
    }
}
