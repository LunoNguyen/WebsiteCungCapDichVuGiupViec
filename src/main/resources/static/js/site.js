/* ==========================================================
   SITE.JS – Tương tác cho website công khai bTaskee
   ========================================================== */
(function () {
  'use strict';

  // ---- Panel trượt trên điện thoại: bấm lớp phủ hoặc Esc thì đóng ----
  var menuBtn = document.querySelector('[data-menu-toggle]');
  function setDrawer(open) {
    document.body.classList.toggle('menu-open', open);
    if (menuBtn) menuBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
  }
  if (menuBtn) menuBtn.addEventListener('click', function () { setDrawer(!document.body.classList.contains('menu-open')); });
  document.querySelectorAll('[data-menu-close]').forEach(function (el) { el.addEventListener('click', function () { setDrawer(false); }); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') setDrawer(false); });

  // ---- Menu tài khoản trên header ----
  var accBtn = document.querySelector('[data-account-toggle]');
  var accMenu = document.querySelector('[data-account-menu]');
  function setAcc(open) {
    if (!accMenu) return;
    accMenu.classList.toggle('is-open', open);
    accBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
  }
  if (accBtn && accMenu) {
    accBtn.addEventListener('click', function (e) { e.stopPropagation(); setAcc(!accMenu.classList.contains('is-open')); });
    document.addEventListener('click', function (e) { if (!accMenu.contains(e.target)) setAcc(false); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') setAcc(false); });
  }

  // ---- Hiện / ẩn mật khẩu ----
  document.querySelectorAll('[data-pass-toggle]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var input = btn.parentElement.querySelector('input');
      var hidden = input.type === 'password';
      input.type = hidden ? 'text' : 'password';
      btn.setAttribute('aria-label', hidden ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
      btn.querySelector('use').setAttribute('href', hidden ? '#i-eye-off' : '#i-eye');
    });
  });

  // ---- Hộp xác nhận: nút có data-confirm="idDialog" mở dialog ----
  document.querySelectorAll('[data-open-dialog]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var dlg = document.getElementById(btn.getAttribute('data-open-dialog'));
      if (dlg && dlg.showModal) dlg.showModal();
    });
  });
  document.querySelectorAll('[data-close-dialog]').forEach(function (btn) {
    btn.addEventListener('click', function () { btn.closest('dialog').close(); });
  });

  // ---- Mega menu "Dịch vụ" ----
  var megaBtn = document.querySelector('[data-mega-toggle]');
  var mega = megaBtn ? document.getElementById(megaBtn.getAttribute('aria-controls')) : null;
  function setMega(open) {
    if (!mega) return;
    mega.classList.toggle('is-open', open);
    megaBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
  }
  if (megaBtn && mega) {
    megaBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      setMega(!mega.classList.contains('is-open'));
    });
    document.addEventListener('click', function (e) {
      if (!mega.contains(e.target)) setMega(false);
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && mega.classList.contains('is-open')) {
        setMega(false);
        megaBtn.focus();
      }
    });
  }

  // ---- Thông báo nhỏ ----
  var toastEl = document.getElementById('siteToast');
  var toastTimer;
  window.siteToast = function (message) {
    if (!toastEl) return;
    toastEl.textContent = message;
    toastEl.classList.add('is-shown');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toastEl.classList.remove('is-shown'); }, 2600);
  };

  // ---- Sao chép mã khuyến mãi ----
  document.querySelectorAll('[data-copy]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var code = btn.getAttribute('data-copy');
      var originalHtml = btn.innerHTML;
      
      var showSuccess = function () {
        btn.classList.add('copied');
        btn.innerHTML = '<span>✓ Đã chép!</span>';
        if (typeof window.siteToast === 'function') {
          window.siteToast('Đã sao chép mã ' + code);
        }
        setTimeout(function () {
          btn.classList.remove('copied');
          btn.innerHTML = originalHtml;
        }, 2000);
      };

      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(code).then(showSuccess, function () {
          showSuccess();
        });
      } else {
        showSuccess();
      }
    });
  });

  // ---- Bảng giá: tab "Theo lần / Gói tháng" + ô tìm kiếm ----
  var tabs = document.querySelectorAll('[data-tab]');
  var panels = document.querySelectorAll('[data-panel]');
  tabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      tabs.forEach(function (t) { t.setAttribute('aria-selected', t === tab ? 'true' : 'false'); });
      panels.forEach(function (p) { p.hidden = p.getAttribute('data-panel') !== tab.getAttribute('data-tab'); });
    });
  });

  function boDau(s) {
    return (s || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd');
  }
  function moRongChuoiHang(text) {
    var base = boDau(text);
    // Tìm các cụm số tiền dạng 216.000đ để bổ sung dạng 216000 và 216k
    var extra = '';
    var matches = base.match(/\d{1,3}(?:\.\d{3})+/g);
    if (matches) {
      matches.forEach(function (m) {
        var raw = parseInt(m.replace(/\./g, ''), 10);
        if (!isNaN(raw) && raw > 0) {
          extra += ' ' + raw + ' ' + Math.round(raw / 1000) + 'k';
          if (raw >= 1000000) extra += ' ' + (raw / 1000000) + 'tr';
        }
      });
    }
    return base + extra;
  }
  var searchInput = document.querySelector('[data-price-search]');
  if (searchInput) {
    searchInput.addEventListener('input', function () {
      var q = boDau(searchInput.value.trim());
      var tokens = q ? q.split(/\s+/) : [];
      panels.forEach(function (panel) {
        var shown = 0;
        panel.querySelectorAll('tbody tr').forEach(function (row) {
          var rowText = moRongChuoiHang(row.textContent);
          var match = !q || tokens.every(function (tk) { return rowText.indexOf(tk) !== -1; });
          row.hidden = !match;
          if (match) shown++;
        });
        var empty = panel.querySelector('[data-no-match]');
        if (empty) empty.hidden = shown !== 0;
      });
    });
  }
    // ---- Hiệu ứng nghiêng 3D Tilt & Glare (ThreeUI) ----
  function initThreeUITilt() {
    var cards = document.querySelectorAll('.tilt-card, .popular-card, .review-card, .hero-card');
    cards.forEach(function (card) {
      var glare = card.querySelector('.glare-overlay');
      if (!glare) {
        glare = document.createElement('div');
        glare.className = 'glare-overlay';
        glare.setAttribute('aria-hidden', 'true');
        card.appendChild(glare);
      }

      card.addEventListener('mousemove', function (e) {
        var rect = card.getBoundingClientRect();
        var x = e.clientX - rect.left;
        var y = e.clientY - rect.top;
        var centerX = rect.width / 2;
        var centerY = rect.height / 2;

        // Nghiêng từ 6 đến 8 độ tạo chiều sâu 3D tinh tế
        var rotateX = ((centerY - y) / centerY) * 7;
        var rotateY = ((x - centerX) / centerX) * 7;

        card.style.transform = 'perspective(1000px) rotateX(' + rotateX.toFixed(2) + 'deg) rotateY(' + rotateY.toFixed(2) + 'deg) scale3d(1.02, 1.02, 1.02)';
        glare.style.opacity = '1';
        glare.style.background = 'radial-gradient(circle at ' + ((x / rect.width) * 100).toFixed(1) + '% ' + ((y / rect.height) * 100).toFixed(1) + '%, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0) 65%)';
      });

      card.addEventListener('mouseleave', function () {
        card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
        glare.style.opacity = '0';
      });
    });
  }
  initThreeUITilt();

  /* ---------- Slider lướt ngang (đánh giá trang chủ): nút mũi tên, ẩn nút ở hai đầu ---------- */
  document.querySelectorAll('[data-slider]').forEach(function (slider) {
    var track = slider.querySelector('[data-slider-track]');
    var prev = slider.querySelector('[data-slider-prev]');
    var next = slider.querySelector('[data-slider-next]');
    if (!track || !prev || !next) return;

    function buoc() {
      var the = track.firstElementChild;
      var gap = parseFloat(getComputedStyle(track).columnGap) || 0;
      return the ? the.getBoundingClientRect().width + gap : track.clientWidth;
    }
    function capNhatNut() {
      var max = track.scrollWidth - track.clientWidth - 2;
      prev.disabled = track.scrollLeft <= 2;
      next.disabled = track.scrollLeft >= max;
    }
    prev.addEventListener('click', function () { track.scrollBy({ left: -buoc(), behavior: 'smooth' }); });
    next.addEventListener('click', function () { track.scrollBy({ left: buoc(), behavior: 'smooth' }); });
    track.addEventListener('scroll', capNhatNut, { passive: true });
    window.addEventListener('resize', capNhatNut);
    capNhatNut();
  });
})();

/* Quay về trang tìm dịch vụ trước đó (giữ bộ lọc): trang /tim-dich-vu ghi lại địa chỉ của mình,
   các link [data-ve-tim-kiem] (đường dẫn "Dịch vụ" ở trang đặt, "Tìm dịch vụ" ở trang chi tiết) trỏ về đó. */
(function () {
  var KHOA = 'btaskee.timDichVuGanNhat';
  function ghi() {
    try { sessionStorage.setItem(KHOA, location.pathname + location.search); } catch (e) { /* bỏ qua */ }
  }
  if (location.pathname.indexOf('/tim-dich-vu') === 0) {
    ghi();
    window.addEventListener('pagehide', ghi);   // bộ lọc đổi URL bằng history: ghi lại lúc rời trang
    window.addEventListener('popstate', ghi);
  }
  var luu = null;
  try { luu = sessionStorage.getItem(KHOA); } catch (e) { /* bỏ qua */ }
  if (!luu || luu.indexOf('/tim-dich-vu') !== 0) return;
  document.querySelectorAll('[data-ve-tim-kiem]').forEach(function (a) { a.href = luu; });
})();


/* ============================================================
   GIỎ HÀNG LƯU TẠM (CLIENT-SIDE CACHE) - PHONG CÁCH TIKTOK/SHOPEE
   Không lưu CSDL MySQL. Không có nút tăng/giảm số lượng (- 0 +).
   ============================================================ */
(function () {
  var CART_KEY = 'btaskee_cart_items_v2';

  window.CartCache = {
    getCart: function () {
      try {
        var raw = localStorage.getItem(CART_KEY);
        var items = raw ? JSON.parse(raw) : [];
        return Array.isArray(items) ? items : [];
      } catch (e) {
        return [];
      }
    },

    saveCart: function (items) {
      try {
        localStorage.setItem(CART_KEY, JSON.stringify(items || []));
      } catch (e) {}
      this.updateBadge();
    },

    addItem: function (item) {
      var items = this.getCart();
      var existing = items.find(function (x) { return x.id === item.id; });
      if (existing) {
        existing.checked = true;
      } else {
        item.checked = true;
        items.push(item);
      }
      this.saveCart(items);
      if (typeof window.siteToast === 'function') {
        window.siteToast(existing ? 'Dịch vụ này đã có trong giỏ hàng' : 'Đã thêm "' + item.tenDichVu + '" vào giỏ hàng');
      }
    },

    removeItem: function (id) {
      var items = this.getCart().filter(function (x) { return x.id !== id; });
      this.saveCart(items);
    },

    /** Bỏ các dịch vụ vừa đặt xong khỏi giỏ. */
    removeMany: function (ids) {
      var bo = ids.map(Number);
      this.saveCart(this.getCart().filter(function (x) { return bo.indexOf(Number(x.id)) === -1; }));
    },

    /** Gói tháng không đặt chung với dịch vụ khác; item cũ chưa có loaiHinhDat thì suy từ nhãn. */
    laGoi: function (item) {
      return item.loaiHinhDat === 'GoiThang' || (!item.loaiHinhDat && item.donViTinh === 'Gói tháng');
    },

    /** Thời lượng (phút) của một dịch vụ trong giỏ: trường phut, hoặc đọc từ "2 giờ 30 phút"; không rõ thì 120. */
    phut: function (item) {
      if (Number(item.phut) > 0) return Number(item.phut);
      var t = String(item.thoiGianText || ''), g = t.match(/(\d+)\s*giờ/), p = t.match(/(\d+)\s*phút/);
      var tong = (g ? Number(g[1]) * 60 : 0) + (p ? Number(p[1]) : 0);
      return tong > 0 ? tong : 120;
    },

    toggleSelect: function (id) {
      var items = this.getCart();
      var found = items.find(function (x) { return x.id === id; });
      if (found) {
        found.checked = found.checked === false; // chưa có cờ = đang chọn
        this.saveCart(items);
      }
    },

    selectAll: function (checked) {
      var items = this.getCart();
      items.forEach(function (x) { x.checked = checked; });
      this.saveCart(items);
    },

    // Nút giỏ hàng nổi chỉ hiện khi giỏ có dịch vụ
    updateBadge: function () {
      var btn = document.getElementById('floatingCartBtn');
      var badge = document.getElementById('floatingCartBadge');
      var count = this.getCart().length;
      if (btn) btn.hidden = count === 0;
      if (badge) badge.textContent = count > 99 ? '99+' : count;
    }
  };

  // Nút "Thêm vào giỏ" (trang chi tiết dịch vụ, trang đặt dịch vụ): dữ liệu dịch vụ nằm ở data-*
  document.addEventListener('click', function (e) {
    var nut = e.target.closest('[data-them-gio]');
    if (!nut) return;
    e.preventDefault();
    var d = nut.dataset;
    var phut = Number(d.phut) || 0;
    window.CartCache.addItem({
      id: Number(d.id),
      tenDichVu: d.ten,
      donGia: Number(d.gia) || 0,
      thoiGianText: d.thoiGian || (phut ? (phut % 60 === 0 ? (phut / 60) + ' giờ' : phut + ' phút') : ''),
      donViTinh: d.goi === 'true' ? 'Gói tháng' : 'Theo lần',
      loaiHinhDat: d.goi === 'true' ? 'GoiThang' : 'TheoLan',
      phut: phut || null,
      danhMucId: Number(d.danhMuc) || null,
      hinhAnh: d.anh || ''
    });
  });

  // Cập nhật huy hiệu khi load trang; vừa đặt xong từ giỏ (?xoaGio=1,25) thì bỏ các dịch vụ đó khỏi giỏ
  document.addEventListener('DOMContentLoaded', function () {
    try {
      var url = new URL(location.href), xoa = url.searchParams.get('xoaGio');
      if (xoa) {
        window.CartCache.removeMany(xoa.split(','));
        url.searchParams.delete('xoaGio');
        history.replaceState(null, '', url.pathname + url.search + url.hash);
      }
    } catch (e) { /* trình duyệt cũ: bỏ qua */ }
    window.CartCache.updateBadge();
  });
})();

/* Chuông thông báo trên header: hiện số thông báo chưa đọc ở góc phải trên */
(function () {
  document.addEventListener('DOMContentLoaded', function () {
    var bell = document.querySelector('[data-bell]');
    var badge = document.querySelector('[data-bell-count]');
    if (!bell || !badge) return;
    function capNhat() {
      fetch('/tai-khoan/thong-bao/so-chua-doc', { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (d) {
          if (!d) return;
          var so = Number(d.soChuaDoc) || 0;
          badge.hidden = so <= 0;
          badge.textContent = so > 99 ? '99+' : String(so);
          bell.setAttribute('aria-label', so > 0 ? 'Thông báo (' + so + ' chưa đọc)' : 'Thông báo');
        })
        .catch(function () { /* mất mạng: giữ số cũ */ });
    }
    capNhat();
    // Thông báo mới tới khi trang đang mở: hỏi lại mỗi phút, và ngay khi khách quay lại tab
    setInterval(function () { if (document.visibilityState === 'visible') capNhat(); }, 60000);
    document.addEventListener('visibilitychange', function () { if (document.visibilityState === 'visible') capNhat(); });
  });
})();
