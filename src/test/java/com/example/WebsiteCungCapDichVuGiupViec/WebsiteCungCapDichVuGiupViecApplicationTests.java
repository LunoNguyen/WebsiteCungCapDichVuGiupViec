package com.example.WebsiteCungCapDichVuGiupViec;

import com.example.Service.AuthService;
import com.example.Service.ThongKeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WebsiteCungCapDichVuGiupViecApplicationTests {

	@Autowired
	private AuthService authService;

	@Autowired
	private ThongKeService thongKeService;

	@Autowired
	private com.example.Validation.AdminAuthenticationInterceptor interceptor;

	@Test
	void testRoleAuthenticationAndDatabaseData() {
		// 1. Kiem tra xac thuc Role Giam doc
		AuthService.AuthenticationResult gdRes = authService.authenticate("giamdoc", "123456");
		Assertions.assertTrue(gdRes.success(), "Dang nhap giamdoc phai thanh cong");
		Assertions.assertEquals("ROLE_GIAM_DOC", gdRes.role());
		Assertions.assertEquals("/giam-doc/dashboard", gdRes.redirectUrl());
		System.out.println("GD Auth OK: " + gdRes.fullName() + " -> " + gdRes.role() + " -> " + gdRes.redirectUrl());

		// 2. Kiem tra xac thuc Role HCNS
		AuthService.AuthenticationResult hcnsRes = authService.authenticate("hanhnt", "123456");
		Assertions.assertTrue(hcnsRes.success(), "Dang nhap hanhnt phai thanh cong");
		Assertions.assertEquals("ROLE_HCNS", hcnsRes.role());
		Assertions.assertEquals("/hcns/dashboard", hcnsRes.redirectUrl());
		System.out.println("HCNS Auth OK: " + hcnsRes.fullName() + " -> " + hcnsRes.role() + " -> " + hcnsRes.redirectUrl());

		// 3. Kiem tra xac thuc Role CSKH
		AuthService.AuthenticationResult cskhRes = authService.authenticate("dunght", "123456");
		Assertions.assertTrue(cskhRes.success(), "Dang nhap dunght phai thanh cong");
		Assertions.assertEquals("ROLE_CSKH", cskhRes.role());
		Assertions.assertEquals("/cskh/dashboard", cskhRes.redirectUrl());
		System.out.println("CSKH Auth OK: " + cskhRes.fullName() + " -> " + cskhRes.role() + " -> " + cskhRes.redirectUrl());

		// 4. Kiem tra xac thuc Role Marketing
		AuthService.AuthenticationResult mktRes = authService.authenticate("minhpv", "123456");
		Assertions.assertTrue(mktRes.success(), "Dang nhap minhpv phai thanh cong");
		Assertions.assertEquals("ROLE_MARKETING", mktRes.role());
		Assertions.assertEquals("/marketing/dashboard", mktRes.redirectUrl());
		System.out.println("MKT Auth OK: " + mktRes.fullName() + " -> " + mktRes.role() + " -> " + mktRes.redirectUrl());

		// 5. Kiem tra du lieu dong tu CSDL
		Assertions.assertTrue(thongKeService.getTongDonHang() > 0, "Tong don hang phai > 0 tu DB");
		Assertions.assertTrue(thongKeService.getTongKhachHang() > 0, "Tong khach hang phai > 0 tu DB");
		Assertions.assertTrue(thongKeService.getTongDichVu() > 0, "Tong dich vu phai > 0 tu DB");
		System.out.println("Thong ke Database OK: Tong don=" + thongKeService.getTongDonHang()
				+ ", KH=" + thongKeService.getTongKhachHang()
				+ ", Doanh thu=" + thongKeService.getDoanhThuTrieuDong() + " tr");
	}

	@Test
	void testUrlProtectionWithInterceptor() throws Exception {
		org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
		org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();

		// Case 1: Chua dang nhap go truc tiep /giam-doc/dashboard
		request.setRequestURI("/giam-doc/dashboard");
		boolean allowedWithoutLogin = interceptor.preHandle(request, response, new Object());
		Assertions.assertFalse(allowedWithoutLogin, "Chua dang nhap phai bi chan");
		Assertions.assertTrue(response.getRedirectedUrl().contains("/login?unauthorized=true"), "Phai redirect ve trang login");

		// Case 2: Role HCNS go truc tiep /giam-doc/dashboard (khong du quyen)
		request = new org.springframework.mock.web.MockHttpServletRequest();
		response = new org.springframework.mock.web.MockHttpServletResponse();
		request.setRequestURI("/giam-doc/dashboard");
		request.getSession(true).setAttribute("userRole", "ROLE_HCNS");
		boolean allowedWrongRole = interceptor.preHandle(request, response, new Object());
		Assertions.assertFalse(allowedWrongRole, "Sai role phai bi chan");
		Assertions.assertEquals("/hcns/dashboard?error=forbidden", response.getRedirectedUrl());

		// Case 3: Role GIAM_DOC go truc tiep /giam-doc/dashboard (dung quyen)
		request = new org.springframework.mock.web.MockHttpServletRequest();
		response = new org.springframework.mock.web.MockHttpServletResponse();
		request.setRequestURI("/giam-doc/dashboard");
		request.getSession(true).setAttribute("userRole", "ROLE_GIAM_DOC");
		boolean allowedAdmin = interceptor.preHandle(request, response, new Object());
		Assertions.assertTrue(allowedAdmin, "Dung role phai cho phep truy cap");

		// Case 4: Role GIAM_DOC sua URL sang /hcns/cong-tac-vien (khong cho phep chéo role)
		request = new org.springframework.mock.web.MockHttpServletRequest();
		response = new org.springframework.mock.web.MockHttpServletResponse();
		request.setRequestURI("/hcns/cong-tac-vien");
		request.getSession(true).setAttribute("userRole", "ROLE_GIAM_DOC");
		boolean gdToHcns = interceptor.preHandle(request, response, new Object());
		Assertions.assertFalse(gdToHcns, "Giam doc khong duoc vao /hcns");
		Assertions.assertEquals("/giam-doc/dashboard?error=forbidden", response.getRedirectedUrl());

		// Case 5: Role CSKH sua URL sang /marketing/khuyen-mai
		request = new org.springframework.mock.web.MockHttpServletRequest();
		response = new org.springframework.mock.web.MockHttpServletResponse();
		request.setRequestURI("/marketing/khuyen-mai");
		request.getSession(true).setAttribute("userRole", "ROLE_CSKH");
		boolean cskhToMkt = interceptor.preHandle(request, response, new Object());
		Assertions.assertFalse(cskhToMkt, "CSKH khong duoc vao /marketing");
		Assertions.assertEquals("/cskh/dashboard?error=forbidden", response.getRedirectedUrl());

		// Case 6: Role MARKETING sua URL sang /cskh/don-dat-dich-vu
		request = new org.springframework.mock.web.MockHttpServletRequest();
		response = new org.springframework.mock.web.MockHttpServletResponse();
		request.setRequestURI("/cskh/don-dat-dich-vu");
		request.getSession(true).setAttribute("userRole", "ROLE_MARKETING");
		boolean mktToCskh = interceptor.preHandle(request, response, new Object());
		Assertions.assertFalse(mktToCskh, "Marketing khong duoc vao /cskh");
		Assertions.assertEquals("/marketing/dashboard?error=forbidden", response.getRedirectedUrl());

		System.out.println("Security Interceptor Tests OK: All URL protections verified!");
	}
}
