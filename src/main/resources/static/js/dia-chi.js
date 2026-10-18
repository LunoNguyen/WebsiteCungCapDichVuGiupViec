/*
 * Form địa chỉ dùng chung (sổ địa chỉ, đặt dịch vụ, đăng ký CTV, CSKH thêm khách):
 *   Tỉnh, thành → Phường, xã (provinces.open-api.vn v2, qua /v1/dia-gioi) + số nhà, tên đường.
 * Khu vực (quận, huyện) tự suy ra từ phường: 1 khu vực thì chọn sẵn, nhiều khu vực thì cho chọn,
 * không có thì báo "ngoài khu vực phục vụ".
 *
 * Đánh dấu bằng [data-dia-chi-form], bên trong có:
 *   select[name=maTinh], select[name=maPhuong], select[name=khuVucId] (trong [data-kv-wrap]), [data-kv-note]
 * Tỉnh và phường là ô gõ để tìm (lọc không dấu); select gốc ẩn đi nhưng vẫn gửi cùng form.
 * Thuộc tính: data-ma-tinh, data-ma-phuong, data-khu-vuc-id (giá trị sẵn có khi sửa),
 *             data-bat-buoc-khu-vuc (không cho gửi form nếu địa chỉ ngoài khu vực phục vụ).
 */
(function () {
  var MA_HCM = 79;
  var tinhPromise = null;
  var phuongPromise = {};

  function layJson(url) {
    return fetch(url, { headers: { Accept: 'application/json' } })
      .then(function (r) { return r.json().then(function (d) { if (!r.ok || d.success === false) throw new Error(d.message || 'Lỗi tải dữ liệu'); return d.data; }); });
  }
  function dsTinh() { return tinhPromise || (tinhPromise = layJson('/v1/dia-gioi/tinh').catch(function (e) { tinhPromise = null; throw e; })); }
  function dsPhuong(ma) {
    return phuongPromise[ma] || (phuongPromise[ma] = layJson('/v1/dia-gioi/tinh/' + ma + '/phuong-xa').catch(function (e) { delete phuongPromise[ma]; throw e; }));
  }
  function option(value, text) { var o = document.createElement('option'); o.value = value; o.textContent = text; return o; }
  // ---- Ô chọn có tìm kiếm: gõ để lọc (không dấu cũng được), select gốc vẫn nằm trong form ----
  function boDau(s) {
    return String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();
  }
  var daThemCss = false;
  function themCss() {
    if (daThemCss) return;
    daThemCss = true;
    var st = document.createElement('style');
    st.textContent =
      '.tim-chon{position:relative}' +
      '.tim-chon>select{position:absolute;inset:0;opacity:0;pointer-events:none}' +
      '.tim-chon>input{width:100%;padding-right:2.25rem}' +
      '.tim-chon-mui{position:absolute;right:.75rem;top:50%;transform:translateY(-50%);pointer-events:none;opacity:.6}' +
      '.tim-chon-ds{position:absolute;z-index:60;left:0;right:0;top:calc(100% + 4px);max-height:260px;overflow:auto;margin:0;padding:4px;list-style:none;' +
      'background:var(--surface,var(--bg-card,#fff));border:1px solid var(--border,#e4e4e7);border-radius:10px;box-shadow:0 10px 30px rgba(0,0,0,.12)}' +
      '.tim-chon-ds li{padding:.5rem .75rem;border-radius:8px;cursor:pointer;line-height:1.35}' +
      '.tim-chon-ds li[aria-selected=true]{background:var(--brand-50,#fff1e6);color:inherit}' +
      '.tim-chon-ds li.dang-chon{font-weight:700}' +
      '.tim-chon-ds li.rong{cursor:default;opacity:.65}';
    document.head.appendChild(st);
  }

  var demTimChon = 0;
  /** Biến một select thành ô gõ để tìm; trả hàm đồng bộ lại khi select được nạp lại hoặc đổi giá trị bằng mã. */
  function timChon(sel) {
    if (sel.__dongBo) return sel.__dongBo;
    themCss();
    var id = 'tim-chon-' + (++demTimChon);
    var boc = document.createElement('div');
    boc.className = 'tim-chon';
    sel.parentNode.insertBefore(boc, sel);
    boc.appendChild(sel);
    sel.tabIndex = -1;
    sel.setAttribute('aria-hidden', 'true');

    var o = document.createElement('input');
    o.type = 'text';
    o.className = sel.className.replace(/\bselect\b/, 'input') || 'input';
    o.autocomplete = 'off';
    o.setAttribute('role', 'combobox');
    o.setAttribute('aria-autocomplete', 'list');
    o.setAttribute('aria-expanded', 'false');
    o.setAttribute('aria-controls', id);
    if (sel.id) { o.id = sel.id + '-tim'; var lb = document.querySelector('label[for="' + sel.id + '"]'); if (lb) lb.htmlFor = o.id; }
    boc.insertBefore(o, sel);
    var mui = document.createElement('span');
    mui.className = 'tim-chon-mui';
    mui.setAttribute('aria-hidden', 'true');
    mui.textContent = '▾';
    boc.appendChild(mui);
    var ds = document.createElement('ul');
    ds.className = 'tim-chon-ds';
    ds.id = id;
    ds.setAttribute('role', 'listbox');
    ds.hidden = true;
    boc.appendChild(ds);

    var hien = [], viTri = -1;
    function nhanChon() { var op = sel.options[sel.selectedIndex]; return op && op.value ? op.textContent : ''; }
    function goiY() { var op = sel.options[0]; return op && !op.value ? op.textContent : ''; }

    function ve(loc) {
      var k = boDau(loc);
      ds.innerHTML = '';
      hien = [].filter.call(sel.options, function (op) { return op.value && (!k || boDau(op.textContent).indexOf(k) !== -1); });
      if (!hien.length) {
        var li0 = document.createElement('li');
        li0.className = 'rong';
        li0.textContent = 'Không tìm thấy “' + loc + '”';
        ds.appendChild(li0);
      }
      hien.forEach(function (op, i) {
        var li = document.createElement('li');
        li.id = id + '-' + i;
        li.setAttribute('role', 'option');
        li.textContent = op.textContent;
        if (op.value === sel.value) li.classList.add('dang-chon');
        li.addEventListener('mousedown', function (e) { e.preventDefault(); chon(op); });
        ds.appendChild(li);
      });
      danhDau(hien.findIndex(function (op) { return op.value === sel.value; }));
    }
    function danhDau(i) {
      viTri = i;
      [].forEach.call(ds.children, function (li, j) { li.setAttribute('aria-selected', j === i ? 'true' : 'false'); });
      var li = ds.children[i];
      if (li && li.id) { o.setAttribute('aria-activedescendant', li.id); li.scrollIntoView({ block: 'nearest' }); }
      else o.removeAttribute('aria-activedescendant');
    }
    function mo() { if (o.disabled) return; ve(''); ds.hidden = false; o.setAttribute('aria-expanded', 'true'); }
    function dong() { ds.hidden = true; o.setAttribute('aria-expanded', 'false'); o.value = nhanChon(); }
    function chon(op) {
      var doi = sel.value !== op.value;
      sel.value = op.value;
      dong();
      if (doi) sel.dispatchEvent(new Event('change', { bubbles: true }));
    }

    o.addEventListener('focus', function () { o.select(); mo(); });
    o.addEventListener('click', function () { if (ds.hidden) mo(); });
    o.addEventListener('input', function () { ds.hidden = false; o.setAttribute('aria-expanded', 'true'); ve(o.value); if (hien.length) danhDau(0); });
    o.addEventListener('blur', dong);
    o.addEventListener('keydown', function (e) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        if (ds.hidden) { mo(); return; }
        if (!hien.length) return;
        var n = viTri + (e.key === 'ArrowDown' ? 1 : -1);
        danhDau(Math.max(0, Math.min(hien.length - 1, n)));
      } else if (e.key === 'Enter') {
        if (!ds.hidden) { e.preventDefault(); if (hien[viTri]) chon(hien[viTri]); }
      } else if (e.key === 'Escape') {
        if (!ds.hidden) { e.preventDefault(); dong(); }
      }
    });
    // Form báo thiếu (select required, đang ẩn): đưa con trỏ về ô gõ
    sel.addEventListener('invalid', function () { o.focus(); });
    sel.addEventListener('change', function () { if (ds.hidden) o.value = nhanChon(); });

    sel.__dongBo = function () {
      o.disabled = sel.disabled;
      o.required = false;
      o.placeholder = goiY() || 'Gõ để tìm';
      o.value = nhanChon();
      if (!ds.hidden) ve(o.value === nhanChon() ? '' : o.value);
    };
    sel.__dongBo();
    return sel.__dongBo;
  }

  function nhanPhuong(ten) { var m = /^(Phường|Xã|Đặc khu|Thị trấn) (.*)$/.exec(ten); return m ? m[2] + ' (' + m[1] + ')' : ten; }

  function ganKhoi(khoi) {
    if (khoi.__diaChi) return;
    khoi.__diaChi = true;
    var selTinh = khoi.querySelector('select[name="maTinh"]');
    var selPhuong = khoi.querySelector('select[name="maPhuong"]');
    var selKv = khoi.querySelector('select[name="khuVucId"]');
    var kvWrap = khoi.querySelector('[data-kv-wrap]');
    var note = khoi.querySelector('[data-kv-note]');
    var batBuoc = khoi.hasAttribute('data-bat-buoc-khu-vuc');
    var phuongTheoMa = {};
    var ngoaiKhuVuc = false;
    var dongBoTinh = timChon(selTinh);
    var dongBoPhuong = timChon(selPhuong);

    function baoKv(text, loi) {
      if (!note) return;
      note.textContent = text || '';
      note.style.color = loi ? 'var(--danger, #C70036)' : '';
    }

    function capNhatKhuVuc(giuKv) {
      var p = phuongTheoMa[selPhuong.value];
      var kvs = p ? (p.khuVuc || []) : [];
      ngoaiKhuVuc = !!p && kvs.length === 0;
      // Báo cho trang (VD đăng ký CTV tự chọn sẵn khu vực nhận việc gần nhà)
      khoi.dispatchEvent(new CustomEvent('diachi:khuvuc', { detail: kvs, bubbles: true }));
      if (!selKv) {
        // Chỉ hiện khu vực để tham khảo (địa chỉ nơi ở của CTV)
        baoKv(!p ? '' : kvs.length ? 'Thuộc khu vực: ' + kvs.map(function (k) { return k.tenKhuVuc; }).join(' / ')
                                   : 'Nơi ở ngoài khu vực bTaskee đang phục vụ.', false);
        return;
      }
      var cu = giuKv || selKv.value;
      selKv.innerHTML = '';
      if (!p) { kvWrap.hidden = true; selKv.required = false; baoKv(''); return; }
      if (kvs.length === 0) {
        kvWrap.hidden = true; selKv.required = false;
        baoKv(batBuoc ? 'Phường, xã này nằm ngoài khu vực bTaskee đang phục vụ (các quận, huyện TP. Hồ Chí Minh).'
                      : 'Ngoài khu vực phục vụ: vẫn lưu được địa chỉ nhưng chưa đặt dịch vụ tại đây được.', true);
        return;
      }
      kvs.forEach(function (k) { selKv.appendChild(option(k.id, k.tenKhuVuc)); });
      if (cu && kvs.some(function (k) { return String(k.id) === String(cu); })) selKv.value = cu;
      if (kvs.length === 1) {
        kvWrap.hidden = true; selKv.required = false;
        baoKv('Khu vực: ' + kvs[0].tenKhuVuc);
      } else {
        kvWrap.hidden = false; selKv.required = true;
        baoKv('Phường, xã này thuộc nhiều quận cũ, hãy chọn khu vực đúng với địa chỉ.');
      }
    }

    function napPhuong(maTinh, maPhuongChon, kvChon) {
      selPhuong.innerHTML = '';
      selPhuong.appendChild(option('', maTinh ? 'Đang tải…' : 'Chọn tỉnh, thành trước'));
      selPhuong.disabled = true;
      phuongTheoMa = {};
      dongBoPhuong();
      capNhatKhuVuc();
      if (!maTinh) return Promise.resolve();
      return dsPhuong(maTinh).then(function (ds) {
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Chọn phường, xã'));
        ds.forEach(function (p) { phuongTheoMa[p.ma] = p; selPhuong.appendChild(option(p.ma, nhanPhuong(p.ten))); });
        selPhuong.disabled = false;
        if (maPhuongChon && phuongTheoMa[maPhuongChon]) selPhuong.value = String(maPhuongChon);
        dongBoPhuong();
        capNhatKhuVuc(kvChon);
      }).catch(function (e) {
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Không tải được, chọn lại tỉnh để thử lại'));
        dongBoPhuong();
        baoKv(e.message, true);
      });
    }

    selTinh.innerHTML = '';
    selTinh.appendChild(option('', 'Đang tải…'));
    dongBoTinh();
    dsTinh().then(function (ds) {
      selTinh.innerHTML = '';
      selTinh.appendChild(option('', 'Chọn tỉnh, thành phố'));
      ds.forEach(function (t) { selTinh.appendChild(option(t.ma, t.ten)); });
      var maTinh = khoi.dataset.maTinh || MA_HCM;
      selTinh.value = String(maTinh);
      dongBoTinh();
      napPhuong(selTinh.value, khoi.dataset.maPhuong, khoi.dataset.khuVucId);
    }).catch(function (e) {
      selTinh.innerHTML = '';
      selTinh.appendChild(option('', 'Không tải được danh sách tỉnh'));
      dongBoTinh();
      baoKv(e.message, true);
    });

    // Đặt lại giá trị (VD mở hộp sửa với dữ liệu của bản ghi khác)
    khoi.__datGiaTri = function (maTinh, maPhuong, khuVucId) {
      dsTinh().then(function () {
        selTinh.value = String(maTinh || MA_HCM);
        dongBoTinh();
        napPhuong(selTinh.value, maPhuong, khuVucId);
      });
    };

    selTinh.addEventListener('change', function () { napPhuong(selTinh.value); });
    selPhuong.addEventListener('change', function () { capNhatKhuVuc(); });

    var form = khoi.closest('form');
    if (form && batBuoc) {
      form.addEventListener('submit', function (e) {
        if (!khoi.isConnected || khoi.closest('[hidden]') || khoi.offsetParent === null) return;
        if (ngoaiKhuVuc) {
          e.preventDefault();
          e.stopImmediatePropagation();
          (selPhuong.previousElementSibling || selPhuong).focus();
        }
      }, true);
    }
  }

  function ganTatCa(goc) { (goc || document).querySelectorAll('[data-dia-chi-form]').forEach(ganKhoi); }
  window.DiaChi = {
    gan: ganTatCa,
    /** Đặt tỉnh, phường, khu vực cho một khối [data-dia-chi-form] đã gắn. */
    datGiaTri: function (khoi, maTinh, maPhuong, khuVucId) {
      ganKhoi(khoi);
      if (khoi.__datGiaTri) khoi.__datGiaTri(maTinh, maPhuong, khuVucId);
    }
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', function () { ganTatCa(); });
  else ganTatCa();
})();
