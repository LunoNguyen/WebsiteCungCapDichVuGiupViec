/*
 * Quy tắc mật khẩu mới: cùng tiêu chí và câu báo lỗi với com.example.Service.QuyTacMatKhau (server mới là nơi quyết định).
 * Dùng: <input type="password" data-quy-tac-mat-khau
 *              data-sdt="#soDienThoai"        (ô số điện thoại trên cùng form) hoặc data-sdt-value="0912345678"
 *              data-tuy-chon                   (bỏ trống được, ví dụ ô "mật khẩu mới" khi sửa tài khoản)
 *              data-bo-qua="#coTaiKhoanSan">   (ô đánh dấu dùng chung tài khoản đã có: đang nhập mật khẩu cũ)
 * Script chèn danh sách tiêu chí ngay dưới ô, tiêu chí đạt chuyển xanh, và chặn gửi form khi chưa đạt.
 */
(function () {
  'use strict';

  var DO_DAI_TOI_THIEU = 8;
  var TIEU_CHI = [
    { ma: 'doDai', nhan: 'Ít nhất ' + DO_DAI_TOI_THIEU + ' ký tự', loi: 'Mật khẩu cần ít nhất ' + DO_DAI_TOI_THIEU + ' ký tự.' },
    { ma: 'chuCai', nhan: 'Có chữ cái', loi: 'Mật khẩu cần có ít nhất một chữ cái.' },
    { ma: 'chuSo', nhan: 'Có chữ số', loi: 'Mật khẩu cần có ít nhất một chữ số.' },
    { ma: 'khoangTrang', nhan: 'Không có khoảng trắng', loi: 'Mật khẩu không được chứa khoảng trắng.' },
    { ma: 'soDienThoai', nhan: 'Không chứa số điện thoại', loi: 'Mật khẩu không được chứa số điện thoại.' }
  ];
  var LA_CHU_CAI = (function () {
    try { return new RegExp('\\p{L}', 'u'); } catch (e) { return /[A-Za-zÀ-ỹ]/; }
  })();

  function chuaSoDienThoai(mk, soDienThoai) {
    var sdt = String(soDienThoai || '').replace(/\D/g, '');
    if (sdt.indexOf('84') === 0 && sdt.length >= 11) sdt = '0' + sdt.substring(2);
    if (sdt.length < 6) return false;
    if (mk.indexOf(sdt) >= 0) return true;
    return sdt.charAt(0) === '0' && mk.indexOf(sdt.substring(1)) >= 0;
  }

  /** { doDai: true/false, ... } — true là đạt. */
  function danhGia(matKhau, soDienThoai) {
    var mk = matKhau || '';
    return {
      doDai: mk.length >= DO_DAI_TOI_THIEU,
      chuCai: LA_CHU_CAI.test(mk),
      chuSo: /[0-9]/.test(mk),
      khoangTrang: !/\s/.test(mk),
      soDienThoai: !chuaSoDienThoai(mk, soDienThoai)
    };
  }

  /** Câu báo lỗi của các tiêu chí chưa đạt (rỗng khi hợp lệ). */
  function kiemTra(matKhau, soDienThoai) {
    var kq = danhGia(matKhau, soDienThoai);
    return TIEU_CHI.filter(function (t) { return !kq[t.ma]; }).map(function (t) { return t.loi; });
  }

  function chenCss() {
    if (document.getElementById('quy-tac-mat-khau-css')) return;
    var st = document.createElement('style');
    st.id = 'quy-tac-mat-khau-css';
    st.textContent =
      '.qtmk{list-style:none;margin:6px 0 0;padding:0;display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:2px 12px;font-size:.82rem;line-height:1.5}' +
      '.qtmk li{display:flex;align-items:center;gap:6px;color:#6b7280}' +
      '.qtmk li::before{content:"";flex:0 0 auto;width:14px;height:14px;border-radius:50%;border:1.5px solid currentColor;box-sizing:border-box}' +
      '.qtmk li.dat{color:#15803d}' +
      '.qtmk li.dat::before{border:0;background:#15803d url("data:image/svg+xml,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27%3E%3Cpath d=%27M4 8.5l2.5 2.5L12 5.5%27 fill=%27none%27 stroke=%27white%27 stroke-width=%272%27 stroke-linecap=%27round%27 stroke-linejoin=%27round%27/%3E%3C/svg%3E") center/12px no-repeat}' +
      '.qtmk.bao-loi li:not(.dat){color:#b91c1c}' +
      '@media (prefers-color-scheme: dark){.qtmk li{color:#9ca3af}.qtmk li.dat{color:#4ade80}.qtmk.bao-loi li:not(.dat){color:#f87171}}';
    document.head.appendChild(st);
  }

  function layNutGui(form) {
    var nut = Array.prototype.slice.call(form.querySelectorAll('button[type=submit], button:not([type]), input[type=submit]'));
    if (form.id) {
      nut = nut.concat(Array.prototype.slice.call(document.querySelectorAll('[type=submit][form="' + form.id + '"]')));
    }
    return nut;
  }

  function ganO(input) {
    if (input.dataset.qtmkDaGan) return;
    input.dataset.qtmkDaGan = '1';
    var form = input.form;
    var tuyChon = input.hasAttribute('data-tuy-chon');
    input.removeAttribute('minlength');

    var ds = document.createElement('ul');
    ds.className = 'qtmk';
    ds.setAttribute('aria-live', 'polite');
    ds.id = (input.id || 'mk') + '-quy-tac';
    TIEU_CHI.forEach(function (t) {
      var li = document.createElement('li');
      li.dataset.ma = t.ma;
      li.textContent = t.nhan;
      ds.appendChild(li);
    });
    // Đặt dưới khung chứa ô (pass-wrap có nút hiện mật khẩu) để không vỡ bố cục
    var moc = input.closest('.pass-wrap, .input-group, .password-wrap')
      || (input.parentNode.querySelector('button') ? input.parentNode : input);
    moc.parentNode.insertBefore(ds, moc.nextSibling);
    input.setAttribute('aria-describedby', ((input.getAttribute('aria-describedby') || '') + ' ' + ds.id).trim());

    function soDienThoai() {
      if (input.dataset.sdtValue) return input.dataset.sdtValue;
      var o = timO(input.dataset.sdt);
      return o ? o.value : '';
    }

    function timO(sel) {
      return sel ? ((form && form.querySelector(sel)) || document.querySelector(sel)) : null;
    }
    // Ô đánh dấu "đã có tài khoản" (đăng ký dùng chung): nhập mật khẩu hiện tại nên không áp quy tắc
    var oBoQua = timO(input.dataset.boQua);
    function boQua() {
      return !!(oBoQua && oBoQua.checked);
    }

    function hopLe() {
      return boQua() || (tuyChon && input.value === '') || kiemTra(input.value, soDienThoai()).length === 0;
    }

    function capNhat() {
      var kq = danhGia(input.value, soDienThoai());
      Array.prototype.forEach.call(ds.children, function (li) {
        li.classList.toggle('dat', !!kq[li.dataset.ma]);
      });
      ds.hidden = boQua() || (tuyChon && input.value === '');
      var ok = hopLe();
      input.setCustomValidity(ok ? '' : kiemTra(input.value, soDienThoai())[0]);
      if (ok) ds.classList.remove('bao-loi');
      if (form) {
        layNutGui(form).forEach(function (b) {
          if (ok) {
            if (b.dataset.qtmkKhoa) { b.disabled = false; delete b.dataset.qtmkKhoa; }
          } else if (!b.disabled) {
            b.disabled = true;
            b.dataset.qtmkKhoa = '1';
          }
        });
      }
    }

    input.addEventListener('input', capNhat);
    var oSdt = timO(input.dataset.sdt);
    if (oSdt) oSdt.addEventListener('input', capNhat);
    if (oBoQua) oBoQua.addEventListener('change', capNhat);
    if (form) {
      // Chạy trước các xử lý submit khác (kể cả gửi bằng fetch) để chặn khi chưa đạt
      form.addEventListener('submit', function (e) {
        if (hopLe()) return;
        e.preventDefault();
        e.stopImmediatePropagation();
        ds.classList.add('bao-loi');
        input.focus();
      }, true);
      form.addEventListener('reset', function () { setTimeout(capNhat, 0); });
    }
    input._qtmkCapNhat = capNhat;
    capNhat();
  }

  function ganTatCa(goc) {
    chenCss();
    (goc || document).querySelectorAll('input[data-quy-tac-mat-khau]').forEach(ganO);
  }

  window.QuyTacMatKhau = {
    kiemTra: kiemTra,
    danhGia: danhGia,
    ganTatCa: ganTatCa,
    /** Gọi sau khi đổi giá trị ô bằng JS (ví dụ mở lại modal). */
    capNhat: function (input) { if (input && input._qtmkCapNhat) input._qtmkCapNhat(); }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { ganTatCa(); });
  } else {
    ganTatCa();
  }
})();
