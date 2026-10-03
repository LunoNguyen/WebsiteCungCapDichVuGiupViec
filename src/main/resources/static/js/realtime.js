/* ==========================================================
   REALTIME.JS – Cập nhật dữ liệu theo thời gian thực cho trang quản trị

   Cách hoạt động:
   1. Trình duyệt mở kết nối Server-Sent Events tới /realtime/stream (một kết nối dùng chung cho mọi tab).
   2. Khi ai đó ghi dữ liệu (nhân viên trên web, khách hàng/CTV trên ứng dụng), máy chủ gửi
      sự kiện "du-lieu-thay-doi".
   3. Trang tải lại nội dung của chính nó ở chế độ nền và so với nội dung đang hiển thị:
        - không khác gì  → bỏ qua (thay đổi không liên quan tới trang này)
        - có khác        → tự làm mới trang, giữ nguyên vị trí cuộn
      Nếu người dùng đang mở hộp thoại hoặc đang nhập dở, trang KHÔNG tự làm mới mà hiện
      thông báo "Có dữ liệu mới" kèm nút Cập nhật, để không làm mất nội dung đang nhập.
   ========================================================== */
(function () {
  'use strict';
  if (!window.EventSource || !window.DOMParser) return;

  var VUNG = 'main';                 // vùng nội dung của trang quản trị
  var KHOA_CUON = 'neatify_rt_scroll';
  var KHOA_VUA_CAP_NHAT = 'neatify_rt_done';

  function layNoiDung(doc) {
    var main = doc.querySelector(VUNG);
    return main ? main.innerHTML.replace(/\s+/g, ' ').trim() : null;
  }

  // Ảnh chụp nội dung do máy chủ trả về, lấy ngay khi script chạy (trước khi JS khác chỉnh sửa giao diện)
  var banDau = layNoiDung(document);
  if (banDau === null) return;

  // ---- Khôi phục vị trí cuộn sau khi tự làm mới ----
  try {
    var cuon = sessionStorage.getItem(KHOA_CUON);
    if (cuon !== null) {
      sessionStorage.removeItem(KHOA_CUON);
      window.addEventListener('load', function () { window.scrollTo(0, parseInt(cuon, 10) || 0); });
    }
    if (sessionStorage.getItem(KHOA_VUA_CAP_NHAT)) {
      sessionStorage.removeItem(KHOA_VUA_CAP_NHAT);
      window.addEventListener('load', function () {
        if (window.Toast && Toast.show) Toast.show('Dữ liệu vừa được cập nhật', 'info', 2500);
      });
    }
  } catch (e) { /* sessionStorage bị chặn: bỏ qua */ }

  // ---- Người dùng có đang thao tác dở không? ----
  var daNhap = false;
  document.addEventListener('input', function (e) {
    var t = e.target;
    // Ô tìm kiếm/lọc không tính là "đang nhập dở"
    if (t && t.type !== 'search' && !(t.closest && t.closest('[data-rt-ignore]'))) daNhap = true;
  }, true);

  function dangBan() {
    if (daNhap) return true;
    if (document.querySelector('.modal-overlay.active, .modal-overlay.show, .modal-overlay.open, .detail-panel.active, .detail-panel.open')) return true;
    var a = document.activeElement;
    return !!(a && (a.tagName === 'TEXTAREA' || (a.tagName === 'INPUT' && a.type !== 'search' && a.type !== 'checkbox')));
  }

  // ---- Thông báo khi không thể tự làm mới ----
  var thongBao;
  function hienThongBao() {
    if (thongBao) return;
    thongBao = document.createElement('div');
    thongBao.setAttribute('role', 'status');
    thongBao.style.cssText = 'position:fixed;right:1.25rem;bottom:1.25rem;z-index:3000;display:flex;align-items:center;gap:.75rem;' +
      'padding:.75rem 1rem;border-radius:12px;background:#0E2A33;color:#fff;font-size:.875rem;font-weight:600;' +
      'box-shadow:0 10px 30px rgba(0,0,0,.25)';
    var chu = document.createElement('span');
    chu.textContent = 'Có dữ liệu mới';
    var nut = document.createElement('button');
    nut.type = 'button';
    nut.textContent = 'Cập nhật';
    nut.style.cssText = 'border:0;border-radius:999px;padding:.4rem .9rem;background:#FFC533;color:#0E2A33;font-weight:700;cursor:pointer';
    nut.addEventListener('click', lamMoi);
    thongBao.appendChild(chu);
    thongBao.appendChild(nut);
    document.body.appendChild(thongBao);
  }

  function lamMoi() {
    try {
      sessionStorage.setItem(KHOA_CUON, String(window.scrollY || 0));
      sessionStorage.setItem(KHOA_VUA_CAP_NHAT, '1');
    } catch (e) { /* bỏ qua */ }
    window.location.reload();
  }

  // ---- Kiểm tra trang này có dữ liệu mới không ----
  var henGio, dangKiemTra = false, canKiemTraLai = false;

  function kiemTra() {
    if (document.hidden) { canKiemTraLai = true; return; }   // tab đang ẩn: đợi khi người dùng quay lại
    if (dangKiemTra) { canKiemTraLai = true; return; }
    dangKiemTra = true;
    fetch(window.location.href, { headers: { 'X-Requested-With': 'realtime-check' }, credentials: 'same-origin', cache: 'no-store' })
      .then(function (res) {
        if (!res.ok || res.redirected) throw new Error('bo-qua');   // hết phiên đăng nhập...
        return res.text();
      })
      .then(function (html) {
        var moi = layNoiDung(new DOMParser().parseFromString(html, 'text/html'));
        if (moi === null || moi === banDau) return;
        if (dangBan()) hienThongBao(); else lamMoi();
      })
      .catch(function () { /* mất mạng tạm thời: lần sự kiện sau sẽ kiểm tra lại */ })
      .then(function () {
        dangKiemTra = false;
        if (canKiemTraLai) { canKiemTraLai = false; lenLich(); }
      });
  }

  function lenLich() {
    clearTimeout(henGio);
    henGio = setTimeout(kiemTra, 600);   // gom nhiều sự kiện liên tiếp thành một lần kiểm tra
  }

  document.addEventListener('visibilitychange', function () {
    if (!document.hidden && canKiemTraLai) { canKiemTraLai = false; lenLich(); }
  });

  // ---- Nhận sự kiện từ máy chủ ----
  // Sau khi rớt mạng và nối lại ("san-sang" lần thứ hai trở đi), kiểm tra một lần vì có thể đã lỡ sự kiện
  var daTungMo = false;
  function nhan(loai) {
    if (loai === 'du-lieu-thay-doi') {
      lenLich();
    } else if (loai === 'san-sang') {
      if (daTungMo) lenLich();
      daTungMo = true;
    }
  }

  if (window.SharedWorker) {
    // Một kết nối SSE dùng chung cho mọi tab (xem realtime-worker.js)
    try {
      var worker = new SharedWorker('/js/realtime-worker.js', 'neatify-realtime');
      worker.port.onmessage = function (e) { nhan(e.data); };
      worker.port.start();
      window.addEventListener('pagehide', function () { worker.port.postMessage('dong'); });
      return;
    } catch (e) { /* không tạo được worker: dùng kết nối riêng bên dưới */ }
  }

  // Trình duyệt không hỗ trợ SharedWorker: mỗi tab tự mở kết nối (tự kết nối lại khi rớt mạng)
  var nguon = new EventSource('/realtime/stream');
  nguon.addEventListener('du-lieu-thay-doi', function () { nhan('du-lieu-thay-doi'); });
  nguon.addEventListener('san-sang', function () { nhan('san-sang'); });
  window.addEventListener('pagehide', function () { nguon.close(); });
})();
