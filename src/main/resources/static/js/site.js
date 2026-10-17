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
