/* ==========================================================
   SITE.JS – Tương tác cho website công khai Neatify
   ========================================================== */
(function () {
  'use strict';

  // ---- Menu trên điện thoại ----
  var nav = document.getElementById('mainNav');
  var menuBtn = document.querySelector('[data-menu-toggle]');
  if (nav && menuBtn) {
    menuBtn.addEventListener('click', function () {
      var open = nav.classList.toggle('is-open');
      menuBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  }

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
