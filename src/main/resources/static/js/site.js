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
      var done = function () { window.siteToast('Đã sao chép mã ' + code); };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(code).then(done, function () { window.siteToast('Mã: ' + code); });
      } else {
        window.siteToast('Mã: ' + code);
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
  var searchInput = document.querySelector('[data-price-search]');
  if (searchInput) {
    searchInput.addEventListener('input', function () {
      var q = boDau(searchInput.value.trim());
      panels.forEach(function (panel) {
        var shown = 0;
        panel.querySelectorAll('tbody tr').forEach(function (row) {
          var match = !q || boDau(row.textContent).indexOf(q) !== -1;
          row.hidden = !match;
          if (match) shown++;
        });
        var empty = panel.querySelector('[data-no-match]');
        if (empty) empty.hidden = shown !== 0;
      });
    });
  }
})();
