/*
 * Form địa chỉ dùng chung (sổ địa chỉ, đặt dịch vụ, đăng ký CTV, CSKH thêm khách): chỉ 2 ô
 * Tỉnh, thành phố → Phường, xã. Tỉnh gộp trước (v1) và sau (v2) sáp nhập theo mã; tỉnh có ở cả hai
 * thì ô phường tìm được cả phường mới lẫn phường cũ (kèm quận cũ). Chọn phường cũ thì gửi thêm maQuan.
 * Khu vực (quận, huyện nhận việc/phục vụ) tự động suy ra.
 */
(function () {
  var MA_HCM = 79;
  var tinhPromise = null;
  var phuongPromise = {};

  function layJson(url) {
    return fetch(url, { headers: { Accept: 'application/json' } })
      .then(function (r) {
        return r.json().then(function (d) {
          if (!r.ok || d.success === false) throw new Error(d.message || 'Lỗi tải dữ liệu');
          return d.data;
        });
      });
  }

  // Tỉnh gộp trước + sau sáp nhập theo mã; phường của tỉnh lấy từ cả hai nguồn
  function dsTinhGop() {
    return tinhPromise || (tinhPromise = layJson('/v1/dia-gioi/tinh-gop').catch(function (e) {
      tinhPromise = null;
      throw e;
    }));
  }
  function dsPhuongGop(maTinh) {
    return phuongPromise[maTinh] || (phuongPromise[maTinh] = layJson('/v1/dia-gioi/tinh/' + maTinh + '/phuong-xa-gop').catch(function (e) {
      delete phuongPromise[maTinh];
      throw e;
    }));
  }

  function option(value, text) {
    var o = document.createElement('option');
    o.value = value;
    o.textContent = text;
    return o;
  }

  function boDau(s) {
    return String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();
  }

  var daThemCss = false;
  function themCss() {
    if (daThemCss) return;
    daThemCss = true;
    var st = document.createElement('style');
    st.textContent =
      '.dia-chi-box{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px;margin-bottom:14px;box-sizing:border-box;width:100%}' +
      '.dia-chi-box .col-full{grid-column:1 / -1}' +
      '.dia-chi-box .form-group{margin-bottom:0}' +
      '.dia-chi-box .form-label{display:block;font-size:0.8125rem;font-weight:600;color:#334155;margin-bottom:6px}' +
      '.tim-chon{position:relative;width:100%}' +
      '.tim-chon>select{position:absolute;inset:0;opacity:0;pointer-events:none;z-index:-1}' +
      '.tim-chon>input{width:100%;height:38px;padding:0.45rem 2.25rem 0.45rem 0.75rem;font-size:0.875rem;border-radius:8px;border:1px solid #cbd5e1;background:#fff;color:#0f172a;box-sizing:border-box}' +
      '.tim-chon>input:focus{outline:none;border-color:#ea580c;box-shadow:0 0 0 3px rgba(234,88,12,0.15)}' +
      '.tim-chon-mui{position:absolute;right:.75rem;top:50%;transform:translateY(-50%);pointer-events:none;color:#64748b;font-size:0.75rem}' +
      '.tim-chon-ds{position:absolute;z-index:9999 !important;left:0;right:0;top:calc(100% + 4px);max-height:220px;overflow-y:auto;margin:0;padding:4px;list-style:none;' +
      'background:#ffffff !important;border:1px solid #cbd5e1;border-radius:8px;box-shadow:0 10px 25px -5px rgba(0,0,0,.15),0 8px 10px -6px rgba(0,0,0,.1)}' +
      '.tim-chon-ds li{padding:.5rem .75rem;border-radius:6px;cursor:pointer;line-height:1.35;font-size:0.875rem;color:#1e293b}' +
      '.tim-chon-ds li:hover,.tim-chon-ds li[aria-selected=true]{background:#fff1e6;color:#ea580c}' +
      '.tim-chon-ds li.dang-chon{font-weight:700;color:#ea580c}' +
      '.tim-chon-ds li.rong{cursor:default;opacity:.65}';
    document.head.appendChild(st);
  }

  var demTimChon = 0;
  function timChon(sel) {
    if (!sel) return function () {};
    sel.setAttribute('data-native', 'true');
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
    if (sel.id) {
      o.id = sel.id + '-tim';
      var lb = document.querySelector('label[for="' + sel.id + '"]');
      if (lb) lb.htmlFor = o.id;
    }
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
    function nhanChon() {
      var op = sel.options[sel.selectedIndex];
      return op && op.value ? op.textContent : '';
    }
    function goiY() {
      var op = sel.options[0];
      return op && !op.value ? op.textContent : '';
    }

    function ve(loc) {
      var k = boDau(loc);
      ds.innerHTML = '';
      hien = [].filter.call(sel.options, function (op) {
        return op.value && (!k || boDau(op.textContent).indexOf(k) !== -1);
      });
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
        if (op.index === sel.selectedIndex) li.classList.add('dang-chon');
        li.addEventListener('mousedown', function (e) {
          e.preventDefault();
          chon(op);
        });
        ds.appendChild(li);
      });
      danhDau(hien.findIndex(function (op) {
        return op.index === sel.selectedIndex;
      }));
    }
    function danhDau(i) {
      viTri = i;
      [].forEach.call(ds.children, function (li, j) {
        li.setAttribute('aria-selected', j === i ? 'true' : 'false');
      });
      var li = ds.children[i];
      if (li && li.id) {
        o.setAttribute('aria-activedescendant', li.id);
        li.scrollIntoView({ block: 'nearest' });
      } else o.removeAttribute('aria-activedescendant');
    }
    function mo() {
      if (o.disabled) return;
      ve('');
      ds.hidden = false;
      o.setAttribute('aria-expanded', 'true');
    }
    function dong() {
      ds.hidden = true;
      o.setAttribute('aria-expanded', 'false');
      o.value = nhanChon();
    }
    function chon(op) {
      var doi = sel.selectedIndex !== op.index;
      sel.selectedIndex = op.index;
      dong();
      if (doi) sel.dispatchEvent(new Event('change', { bubbles: true }));
    }

    o.addEventListener('focus', function () {
      o.select();
      mo();
    });
    o.addEventListener('click', function () {
      // Bấm lại ô đã chọn (vẫn đang focus): chọn hết chữ để gõ tìm lại từ đầu
      if (ds.hidden) { o.select(); mo(); }
    });
    o.addEventListener('input', function () {
      ds.hidden = false;
      o.setAttribute('aria-expanded', 'true');
      ve(o.value);
      if (hien.length) danhDau(0);
    });
    o.addEventListener('blur', dong);
    o.addEventListener('keydown', function (e) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        if (ds.hidden) {
          mo();
          return;
        }
        if (!hien.length) return;
        var n = viTri + (e.key === 'ArrowDown' ? 1 : -1);
        danhDau(Math.max(0, Math.min(hien.length - 1, n)));
      } else if (e.key === 'Enter') {
        if (!ds.hidden) {
          e.preventDefault();
          if (hien[viTri]) chon(hien[viTri]);
        }
      } else if (e.key === 'Escape') {
        if (!ds.hidden) {
          e.preventDefault();
          dong();
        }
      }
    });

    sel.addEventListener('invalid', function () {
      o.focus();
    });
    sel.addEventListener('change', function () {
      if (ds.hidden) o.value = nhanChon();
    });

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

  function nhanPhuong(ten) {
    var m = /^(Phường|Xã|Đặc khu|Thị trấn) (.*)$/.exec(ten);
    return m ? m[2] + ' (' + m[1] + ')' : ten;
  }

  function ganKhoi(khoi) {
    if (khoi.__diaChi) return;
    khoi.__diaChi = true;

    var selTinh = khoi.querySelector('select[name="maTinh"]');
    var selPhuong = khoi.querySelector('select[name="maPhuong"]');
    var selKv = khoi.querySelector('select[name="khuVucId"]');
    var kvWrap = khoi.querySelector('[data-kv-wrap]');
    var note = khoi.querySelector('[data-kv-note]');
    var batBuoc = khoi.hasAttribute('data-bat-buoc-khu-vuc');

    // Ô quận ẩn: chỉ mang maQuan khi khách chọn phường cũ (trước sáp nhập); phường mới thì tắt để không gửi.
    var selQuan = khoi.querySelector('select[name="maQuan"]');
    if (!selQuan) {
      selQuan = document.createElement('select');
      selQuan.name = 'maQuan';
      selQuan.hidden = true;
      khoi.appendChild(selQuan);
    } else {
      var quanWrap = selQuan.closest('[data-quan-wrap]') || selQuan.closest('.form-group') || selQuan.closest('.field');
      if (quanWrap && quanWrap !== khoi && !quanWrap.contains(selPhuong)) quanWrap.style.display = 'none';
      else selQuan.hidden = true;
    }
    selQuan.required = false;
    function datQuan(p) {
      selQuan.innerHTML = '';
      if (p && p.loai === 'cu') {
        selQuan.appendChild(option(p.maQuan, p.tenQuan || ''));
        selQuan.disabled = false;
      } else selQuan.disabled = true;
    }
    datQuan(null);

    var dongBoTinh = timChon(selTinh);
    var dongBoPhuong = timChon(selPhuong);
    var phuongTheoViTri = [];

    function baoKv(text, loi) {
      if (!note) return;
      note.textContent = text || '';
      note.style.color = loi ? 'var(--danger, #C70036)' : '#6b7280';
    }

    function napTinh() {
      selTinh.innerHTML = '';
      selTinh.appendChild(option('', 'Đang tải…'));
      dongBoTinh();
      dsTinhGop().then(function (ds) {
        selTinh.innerHTML = '';
        selTinh.appendChild(option('', 'Chọn tỉnh, thành phố'));
        ds.forEach(function (t) { selTinh.appendChild(option(t.ma, t.ten + (t.coMoi ? '' : ' (trước sáp nhập)'))); });
        selTinh.value = String(khoi.dataset.maTinh || MA_HCM);
        dongBoTinh();
        napPhuong(selTinh.value, khoi.dataset.maPhuong, khoi.dataset.maQuan, khoi.dataset.khuVucId);
      }).catch(function () {
        selTinh.innerHTML = '';
        selTinh.appendChild(option('', 'Không tải được tỉnh'));
        dongBoTinh();
      });
    }

    // Phường mới và phường cũ có thể trùng mã: nhận diện bằng vị trí option, không bằng value.
    function napPhuong(maTinh, maPhuongChon, maQuanChon, kvChon) {
      selPhuong.innerHTML = '';
      selPhuong.appendChild(option('', maTinh ? 'Đang tải phường, xã…' : 'Chọn tỉnh trước'));
      selPhuong.disabled = true;
      phuongTheoViTri = [null];
      datQuan(null);
      dongBoPhuong();
      capNhatKhuVuc();
      if (!maTinh) return Promise.resolve();
      return dsPhuongGop(maTinh).then(function (ds) {
        if (selTinh.value !== String(maTinh)) return; // khách đã đổi tỉnh trong lúc tải
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Gõ tên phường, xã (mới hoặc cũ)'));
        phuongTheoViTri = [null];
        var chon = 0;
        ds.forEach(function (p) {
          var nhan = nhanPhuong(p.ten) + (p.loai === 'cu' ? ' — ' + p.tenQuan + ' (cũ)' : ' — mới');
          selPhuong.appendChild(option(p.ma, nhan));
          phuongTheoViTri.push(p);
          var dung = String(p.ma) === String(maPhuongChon || '') &&
            (maQuanChon ? p.loai === 'cu' && String(p.maQuan) === String(maQuanChon) : p.loai === 'moi');
          if (dung && !chon) chon = phuongTheoViTri.length - 1;
        });
        selPhuong.disabled = false;
        selPhuong.selectedIndex = chon;
        datQuan(phuongTheoViTri[chon]);
        dongBoPhuong();
        capNhatKhuVuc(kvChon);
      }).catch(function () {
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Lỗi tải phường, xã'));
        dongBoPhuong();
      });
    }

    function capNhatKhuVuc(giuKv) {
      var p = phuongTheoViTri[selPhuong.selectedIndex] || null;
      var kvs = !p ? [] : p.loai === 'cu' ? (p.khuVucId ? [{ id: p.khuVucId, tenKhuVuc: p.tenKhuVuc }] : []) : (p.khuVuc || []);
      khoi.dispatchEvent(new CustomEvent('diachi:khuvuc', { detail: kvs, bubbles: true }));

      if (!selKv) {
        baoKv(!p ? '' : kvs.length ? 'Thuộc khu vực: ' + kvs.map(function (k) { return k.tenKhuVuc; }).join(' / ')
                                   : 'Nơi ở ngoài khu vực bTaskee đang phục vụ.', false);
        return;
      }
      var cu = giuKv || selKv.value;
      selKv.innerHTML = '';
      if (!p) { if (kvWrap) kvWrap.hidden = true; selKv.required = false; baoKv(''); return; }
      if (kvs.length === 0) {
        if (kvWrap) kvWrap.hidden = true; selKv.required = false;
        baoKv(batBuoc ? 'Phường, xã này nằm ngoài khu vực phục vụ.'
                      : 'Ngoài khu vực phục vụ: vẫn lưu được địa chỉ.', true);
        return;
      }
      kvs.forEach(function (k) { selKv.appendChild(option(k.id, k.tenKhuVuc)); });
      if (cu && kvs.some(function (k) { return String(k.id) === String(cu); })) selKv.value = cu;
      if (kvs.length === 1) {
        if (kvWrap) kvWrap.hidden = true; selKv.required = false;
        baoKv('Khu vực: ' + kvs[0].tenKhuVuc);
      } else {
        if (kvWrap) kvWrap.hidden = false; selKv.required = true;
        baoKv('Phường này thuộc nhiều quận cũ, hãy chọn quận đúng.');
      }
    }

    selTinh.addEventListener('change', function () { napPhuong(selTinh.value); });
    selPhuong.addEventListener('change', function () {
      datQuan(phuongTheoViTri[selPhuong.selectedIndex]);
      capNhatKhuVuc();
    });

    khoi.__datGiaTri = function (maTinh, maPhuong, khuVucId, maQuan) {
      khoi.dataset.maTinh = maTinh || MA_HCM;
      khoi.dataset.maPhuong = maPhuong || '';
      khoi.dataset.khuVucId = khuVucId || '';
      khoi.dataset.maQuan = maQuan || '';
      napTinh();
    };

    napTinh();
  }

  function ganTatCa(goc) {
    (goc || document).querySelectorAll('[data-dia-chi-form]').forEach(ganKhoi);
  }

  window.DiaChi = {
    gan: ganTatCa,
    datGiaTri: function (khoi, maTinh, maPhuong, khuVucId, maQuan) {
      if (!khoi) return;
      ganKhoi(khoi);
      if (khoi.__datGiaTri) khoi.__datGiaTri(maTinh, maPhuong, khuVucId, maQuan);
    }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { ganTatCa(); });
  } else {
    ganTatCa();
  }
})();
