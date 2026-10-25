/* ==========================================================
   CSKH-THONG-BAO.JS – thông báo đơn mới cho nhân viên CSKH (chỉ nạp ở layout CSKH).

   Nhận sự kiện DOM "realtime:don" do realtime.js phát lại từ kênh SSE (máy chủ chỉ gửi cho ROLE_CSKH):
     - thẻ thông báo nổi góc phải (xếp chồng, không tự tắt) với nút "Xem đơn" / "Đóng";
     - tiếng "ting" ngắn khi tab đang mở; thông báo của hệ điều hành khi tab đang ẩn (cần cấp quyền);
     - huy hiệu số đơn đang chờ cộng tác viên ở mục "Đơn đặt dịch vụ" (GET /cskh/don-dat-dich-vu/so-cho-nhan).
   ========================================================== */
(function () {
  'use strict';

  var TIEU_DE = { 'don-moi': 'Đơn mới', 'da-thanh-toan': 'Đơn vừa thanh toán' };

  // ---------- Giao diện ----------
  var css = document.createElement('style');
  css.textContent =
    '.tbd-chong{position:fixed;right:16px;bottom:16px;z-index:9999;display:flex;flex-direction:column;gap:10px;width:min(360px,calc(100vw - 32px))}' +
    '.tbd-the{background:var(--bg-card,#fff);color:var(--text-primary,#111);border:1px solid var(--border,#e5e7eb);border-left:4px solid var(--cskh-primary,#ff8228);' +
    'border-radius:12px;box-shadow:0 12px 28px rgba(0,0,0,.16);padding:12px 14px;animation:tbd-vao .2s ease-out}' +
    '.tbd-the.tt{border-left-color:var(--success,#16a34a)}' +
    '.tbd-dau{display:flex;align-items:center;gap:8px;font-weight:700;font-size:.9rem}' +
    '.tbd-dau i{font-size:1.15rem;color:var(--cskh-primary,#ff8228)}.tbd-the.tt .tbd-dau i{color:var(--success,#16a34a)}' +
    '.tbd-dau span{margin-left:auto;font-weight:500;font-size:.75rem;color:var(--text-muted,#6b7280)}' +
    '.tbd-than{margin:6px 0 10px;font-size:.84rem;line-height:1.5}.tbd-than b{display:block}' +
    '.tbd-than small{color:var(--text-muted,#6b7280)}' +
    '.tbd-nut{display:flex;gap:8px;justify-content:flex-end}' +
    '.tbd-quyen{position:fixed;left:16px;bottom:16px;z-index:9998}' +
    '.tbd-dem{margin-left:auto;min-width:20px;height:20px;padding:0 6px;border-radius:999px;background:var(--danger,#dc2626);color:#fff;' +
    'font-size:.7rem;font-weight:700;display:inline-flex;align-items:center;justify-content:center}' +
    '@keyframes tbd-vao{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none}}';
  document.head.appendChild(css);

  var chong = document.createElement('div');
  chong.className = 'tbd-chong';
  chong.setAttribute('role', 'region');
  chong.setAttribute('aria-label', 'Thông báo đơn mới');
  chong.setAttribute('aria-live', 'polite');
  document.body.appendChild(chong);

  function chu(s) { return String(s == null ? '' : s); }
  function phan(tag, cls, text) { var el = document.createElement(tag); if (cls) el.className = cls; if (text != null) el.textContent = text; return el; }

  function hienThe(d) {
    var tt = d.loai === 'da-thanh-toan';
    var the = phan('div', 'tbd-the' + (tt ? ' tt' : ''));
    var dau = phan('div', 'tbd-dau');
    var ic = phan('i', 'ti ' + (tt ? 'ti-credit-card' : 'ti-bell-ringing'));
    dau.appendChild(ic);
    dau.appendChild(phan('b', null, (TIEU_DE[d.loai] || 'Đơn') + ' ' + chu(d.maDonDat)));
    var gio = new Date();
    dau.appendChild(phan('span', null, String(gio.getHours()).padStart(2, '0') + ':' + String(gio.getMinutes()).padStart(2, '0')));
    var than = phan('div', 'tbd-than');
    than.appendChild(phan('b', null, chu(d.tenDichVu)));
    than.appendChild(phan('div', null, [d.ngayGio, d.khuVuc].filter(Boolean).join(' · ')));
    if (d.ghiChu) than.appendChild(phan('small', null, d.ghiChu));
    var nut = phan('div', 'tbd-nut');
    var dong = phan('button', 'btn btn-outline btn-sm', 'Đóng');
    dong.type = 'button';
    dong.addEventListener('click', function () { the.remove(); });
    var xem = phan('a', 'btn btn-primary btn-sm', 'Xem đơn');
    xem.href = d.url || '/cskh/don-dat-dich-vu';
    nut.appendChild(dong);
    nut.appendChild(xem);
    the.appendChild(dau);
    the.appendChild(than);
    the.appendChild(nut);
    chong.appendChild(the);
    // Giữ tối đa 5 thẻ, bỏ thẻ cũ nhất
    while (chong.children.length > 5) chong.firstChild.remove();
  }

  // ---------- Âm thanh ----------
  var am = null;
  function ting() {
    try {
      am = am || new (window.AudioContext || window.webkitAudioContext)();
      [880, 1320].forEach(function (tan, i) {
        var o = am.createOscillator(), g = am.createGain(), t0 = am.currentTime + i * 0.12;
        o.frequency.value = tan;
        o.type = 'sine';
        g.gain.setValueAtTime(0.0001, t0);
        g.gain.exponentialRampToValueAtTime(0.18, t0 + 0.02);
        g.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.25);
        o.connect(g); g.connect(am.destination);
        o.start(t0); o.stop(t0 + 0.26);
      });
    } catch (e) { /* trình duyệt chặn âm thanh khi chưa tương tác: bỏ qua */ }
  }

  // ---------- Thông báo của hệ điều hành ----------
  var coNotif = 'Notification' in window;
  function baoHeThong(d) {
    if (!coNotif || Notification.permission !== 'granted') return;
    try {
      var n = new Notification((TIEU_DE[d.loai] || 'Đơn') + ' ' + chu(d.maDonDat), {
        body: [d.tenDichVu, d.ngayGio, d.ghiChu].filter(Boolean).join('\n'),
        tag: d.loai + '-' + d.donId,   // nhiều tab CSKH cùng nhận: hệ điều hành chỉ hiện một
        icon: '/images/favicon.svg'
      });
      n.onclick = function () { window.focus(); location.href = d.url || '/cskh/don-dat-dich-vu'; n.close(); };
    } catch (e) { /* bỏ qua */ }
  }
  function nutXinQuyen() {
    if (!coNotif || Notification.permission !== 'default') return;
    var b = phan('button', 'btn btn-outline btn-sm tbd-quyen');
    b.type = 'button';
    b.innerHTML = '<i class="ti ti-bell"></i> Bật thông báo đơn mới trên máy';
    b.addEventListener('click', function () {
      Notification.requestPermission().then(function () { b.remove(); });
    });
    document.body.appendChild(b);
  }

  // ---------- Huy hiệu số đơn chờ ----------
  var muc = document.querySelector('a.sidebar-item[href="/cskh/don-dat-dich-vu"]');
  var dem = null;
  function capNhatDem() {
    if (!muc) return;
    fetch('/cskh/don-dat-dich-vu/so-cho-nhan', { headers: { Accept: 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (j) {
        if (!j) return;
        var so = Number(j.soChoNhan) || 0;
        if (!dem) { dem = phan('span', 'tbd-dem'); muc.appendChild(dem); }
        dem.textContent = so > 99 ? '99+' : String(so);
        dem.hidden = so === 0;
        dem.setAttribute('aria-label', so + ' đơn đang chờ cộng tác viên');
      })
      .catch(function () { /* mất mạng tạm thời */ });
  }

  // ---------- Nhận sự kiện ----------
  document.addEventListener('realtime:don', function (e) {
    var d = e.detail || {};
    if (!d.donId) return;
    hienThe(d);
    if (document.visibilityState === 'visible') ting(); else baoHeThong(d);
    capNhatDem();
  });
  var hen = null;
  document.addEventListener('realtime:thay-doi', function () {
    clearTimeout(hen);
    hen = setTimeout(capNhatDem, 800);   // gom nhiều thay đổi liên tiếp
  });

  nutXinQuyen();
  capNhatDem();
})();
