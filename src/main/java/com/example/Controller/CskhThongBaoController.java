package com.example.Controller;

import com.example.Service.ThongBaoService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Thao tác trên trang Thông báo của CSKH (trang hiển thị: CSKHController#thongBao).
 *   POST /cskh/thong-bao/gui           gửi thông báo
 *   GET  /cskh/thong-bao/{id}          chi tiết (JSON)
 *   POST /cskh/thong-bao/{id}/xoa      xóa
 *   GET  /cskh/thong-bao/nguoi-nhan    tìm khách hàng / cộng tác viên để gửi riêng (JSON)
 */
@Controller
public class CskhThongBaoController {

    private static final Logger log = LoggerFactory.getLogger(CskhThongBaoController.class);

    private final ThongBaoService thongBaoService;

    public CskhThongBaoController(ThongBaoService thongBaoService) {
        this.thongBaoService = thongBaoService;
    }

    @PostMapping("/cskh/thong-bao/gui")
    public String gui(@RequestParam String nhomNhan,
                      @RequestParam(required = false) Integer taiKhoanNhanId,
                      @RequestParam String tieuDe,
                      @RequestParam String noiDung,
                      HttpSession session,
                      RedirectAttributes redirectAttributes) {
        try {
            Object ten = session.getAttribute("authenticatedFullName");
            thongBaoService.gui(tieuDe, noiDung, nhomNhan, taiKhoanNhanId, ten != null ? "CSKH: " + ten : "CSKH");
            redirectAttributes.addFlashAttribute("successMessage",
                    "CaNhan".equals(nhomNhan) ? "Đã gửi thông báo." : "Đã gửi thông báo. Danh sách người nhận đang được cập nhật.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Gửi thông báo thất bại", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Chưa gửi được thông báo. Thử lại sau.");
        }
        return "redirect:/cskh/thong-bao";
    }

    @GetMapping("/cskh/thong-bao/{id:\\d+}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chiTiet(@PathVariable Integer id) {
        Map<String, Object> thongBao = thongBaoService.chiTiet(id);
        return thongBao != null ? ResponseEntity.ok(thongBao) : ResponseEntity.notFound().build();
    }

    @PostMapping("/cskh/thong-bao/{id:\\d+}/xoa")
    public String xoa(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            thongBaoService.xoa(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thông báo.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Xóa thông báo thất bại", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Chưa xóa được thông báo. Thử lại sau.");
        }
        return "redirect:/cskh/thong-bao";
    }

    @GetMapping("/cskh/thong-bao/nguoi-nhan")
    @ResponseBody
    public List<Map<String, Object>> nguoiNhan(@RequestParam(defaultValue = "KhachHang") String loai,
                                               @RequestParam(defaultValue = "") String q) {
        return thongBaoService.timNguoiNhan(loai, q);
    }
}
