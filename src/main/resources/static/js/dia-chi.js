/*
 * Form địa chỉ dùng chung (sổ địa chỉ, đặt dịch vụ, đăng ký CTV, CSKH thêm khách):
 * Hỗ trợ song song cả 2 hệ thống địa chỉ:
 *   - Địa chỉ cũ (v1): 3 cấp (Tỉnh/TP → Quận/Huyện → Phường/Xã) từ https://provinces.open-api.vn/api/v1/
 *   - Địa chỉ mới (v2): 2 cấp (Tỉnh/TP → Phường/Xã sau sáp nhập) từ https://provinces.open-api.vn/api/v2/
 * Khu vực (quận, huyện nhận việc/phục vụ) tự động suy ra và đồng bộ chính xác.
 */
(function () {
  var MA_HCM = 79;
  var tinhPromiseV1 = null;
  var tinhPromiseV2 = null;
  var quanPromiseV1 = {};
  var phuongPromiseV1 = {};
  var phuongPromiseV2 = {};

  function layJson(url) {
    return fetch(url, { headers: { Accept: 'application/json' } })
      .then(function (r) {
        return r.json().then(function (d) {
          if (!r.ok || d.success === false) throw new Error(d.message || 'Lỗi tải dữ liệu');
          return d.data;
        });
      });
  }

  // API V1
  function dsTinhV1() {
    return tinhPromiseV1 || (tinhPromiseV1 = layJson('/v1/dia-gioi/v1/tinh').catch(function (e) {
      tinhPromiseV1 = null;
      throw e;
    }));
  }
  function dsQuanV1(maTinh) {
    return quanPromiseV1[maTinh] || (quanPromiseV1[maTinh] = layJson('/v1/dia-gioi/v1/tinh/' + maTinh + '/quan-huyen').catch(function (e) {
      delete quanPromiseV1[maTinh];
      throw e;
    }));
  }
  function dsPhuongV1(maQuan) {
    return phuongPromiseV1[maQuan] || (phuongPromiseV1[maQuan] = layJson('/v1/dia-gioi/v1/quan-huyen/' + maQuan + '/phuong-xa').catch(function (e) {
      delete phuongPromiseV1[maQuan];
      throw e;
    }));
  }

  // API V2
  function dsTinhV2() {
    return tinhPromiseV2 || (tinhPromiseV2 = layJson('/v1/dia-gioi/tinh').catch(function (e) {
      tinhPromiseV2 = null;
      throw e;
    }));
  }
  function dsPhuongV2(maTinh) {
    return phuongPromiseV2[maTinh] || (phuongPromiseV2[maTinh] = layJson('/v1/dia-gioi/tinh/' + maTinh + '/phuong-xa').catch(function (e) {
      delete phuongPromiseV2[maTinh];
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
      '.diachi-chedo-toggle{display:flex;background:#f1f5f9;padding:4px;border-radius:10px;margin-bottom:14px;gap:6px;grid-column:1 / -1;border:1px solid #cbd5e1;box-sizing:border-box;width:100%}' +
      '.diachi-chedo-btn{flex:1 1 0%;min-width:0;padding:8px 12px;border:none;background:transparent;border-radius:8px;font-size:13px;font-weight:600;cursor:pointer;color:#64748b;transition:all .2s;text-align:center;display:flex;align-items:center;justify-content:center;gap:6px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}' +
      '.diachi-chedo-btn:hover{color:#0f172a;background:rgba(255,255,255,0.7)}' +
      '.diachi-chedo-btn.active{background:#fff;color:#ea580c;box-shadow:0 1px 3px rgba(0,0,0,.1);font-weight:700}' +
      '@media (max-width: 440px){.diachi-chedo-toggle{flex-direction:column}.diachi-chedo-btn{width:100%;padding:8px 10px;font-size:13px}}' +
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
        if (op.value === sel.value) li.classList.add('dang-chon');
        li.addEventListener('mousedown', function (e) {
          e.preventDefault();
          chon(op);
        });
        ds.appendChild(li);
      });
      danhDau(hien.findIndex(function (op) {
        return op.value === sel.value;
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
      var doi = sel.value !== op.value;
      sel.value = op.value;
      dong();
      if (doi) sel.dispatchEvent(new Event('change', { bubbles: true }));
    }

    o.addEventListener('focus', function () {
      o.select();
      mo();
    });
    o.addEventListener('click', function () {
      if (ds.hidden) mo();
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

    var cheDo = 'v1'; // Mặc định v1: Địa chỉ cũ có quận huyện cho dễ nhận diện
    var selTinh = khoi.querySelector('select[name="maTinh"]');
    var selPhuong = khoi.querySelector('select[name="maPhuong"]');
    var selKv = khoi.querySelector('select[name="khuVucId"]');
    var kvWrap = khoi.querySelector('[data-kv-wrap]');
    var note = khoi.querySelector('[data-kv-note]');
    var batBuoc = khoi.hasAttribute('data-bat-buoc-khu-vuc');

    // Tạo ô Quận/Huyện nếu chưa có
    var quanWrap = khoi.querySelector('[data-quan-wrap]');
    var selQuan = khoi.querySelector('select[name="maQuan"]');
    if (!selQuan) {
      quanWrap = document.createElement('div');
      quanWrap.className = 'field';
      quanWrap.setAttribute('data-quan-wrap', '');
      var lbQuan = document.createElement('label');
      lbQuan.innerHTML = 'Quận, huyện <span class="req">*</span>';
      selQuan = document.createElement('select');
      selQuan.name = 'maQuan';
      selQuan.className = selTinh ? selTinh.className : 'select';
      selQuan.required = true;
      quanWrap.appendChild(lbQuan);
      quanWrap.appendChild(selQuan);

      var phuongWrap = selPhuong ? (selPhuong.closest('.field') || selPhuong.parentNode) : null;
      if (phuongWrap && phuongWrap.parentNode) {
        phuongWrap.parentNode.insertBefore(quanWrap, phuongWrap);
      } else {
        khoi.appendChild(quanWrap);
      }
    }

    // Tạo thanh Toggle Chế độ Địa chỉ Cũ / Mới
    var toggleWrap = document.createElement('div');
    toggleWrap.className = 'diachi-chedo-toggle';
    toggleWrap.innerHTML =
      '<button type="button" class="diachi-chedo-btn active" data-che-do="v1">📍 Địa chỉ cũ (Quận / Huyện)</button>' +
      '<button type="button" class="diachi-chedo-btn" data-che-do="v2">🏛️ Địa chỉ mới (Sau sáp nhập)</button>';
    khoi.insertBefore(toggleWrap, khoi.firstChild);

    var btnV1 = toggleWrap.querySelector('[data-che-do="v1"]');
    var btnV2 = toggleWrap.querySelector('[data-che-do="v2"]');

    var phuongTheoMa = {};
    var quanTheoMa = {};
    var ngoaiKhuVuc = false;

    var dongBoTinh = timChon(selTinh);
    var dongBoQuan = timChon(selQuan);
    var dongBoPhuong = timChon(selPhuong);

    function baoKv(text, loi) {
      if (!note) return;
      note.textContent = text || '';
      note.style.color = loi ? 'var(--danger, #C70036)' : '#6b7280';
    }

    var phuongWrap = selPhuong ? (selPhuong.closest('[data-phuong-wrap]') || selPhuong.closest('.form-group') || selPhuong.closest('.field') || selPhuong.parentNode) : null;

    function capNhatCheDo(moi) {
      cheDo = moi;
      if (cheDo === 'v1') {
        btnV1.classList.add('active');
        btnV2.classList.remove('active');
        quanWrap.style.display = '';
        selQuan.required = true;
        if (phuongWrap) phuongWrap.classList.add('col-full');
        if (kvWrap) kvWrap.hidden = true;
        baoKv('Đang chọn theo địa chỉ cũ (3 cấp: Tỉnh → Quận/Huyện → Phường/Xã).');
        napTinhV1();
      } else {
        btnV2.classList.add('active');
        btnV1.classList.remove('active');
        quanWrap.style.display = 'none';
        selQuan.required = false;
        if (phuongWrap) phuongWrap.classList.remove('col-full');
        baoKv('Đang chọn theo địa chỉ mới sau sắp xếp hành chính (2 cấp: Tỉnh → Phường/Xã).');
        napTinhV2();
      }
    }

    btnV1.addEventListener('click', function () { capNhatCheDo('v1'); });
    btnV2.addEventListener('click', function () { capNhatCheDo('v2'); });

    // ================= XỬ LÝ CHẾ ĐỘ V1 =================
    function napTinhV1() {
      selTinh.innerHTML = '';
      selTinh.appendChild(option('', 'Đang tải…'));
      dongBoTinh();
      dsTinhV1().then(function (ds) {
        selTinh.innerHTML = '';
        selTinh.appendChild(option('', 'Chọn tỉnh, thành phố'));
        ds.forEach(function (t) { selTinh.appendChild(option(t.ma, t.ten)); });
        selTinh.value = String(khoi.dataset.maTinh || MA_HCM);
        dongBoTinh();
        napQuanV1(selTinh.value, khoi.dataset.maQuan, khoi.dataset.maPhuong);
      }).catch(function (e) {
        selTinh.innerHTML = '';
        selTinh.appendChild(option('', 'Không tải được tỉnh'));
        dongBoTinh();
      });
    }

    function napQuanV1(maTinh, maQuanChon, maPhuongChon) {
      selQuan.innerHTML = '';
      selQuan.appendChild(option('', maTinh ? 'Đang tải quận, huyện…' : 'Chọn tỉnh trước'));
      selQuan.disabled = true;
      quanTheoMa = {};
      dongBoQuan();

      selPhuong.innerHTML = '';
      selPhuong.appendChild(option('', 'Chọn quận, huyện trước'));
      selPhuong.disabled = true;
      dongBoPhuong();

      if (!maTinh) return Promise.resolve();
      return dsQuanV1(maTinh).then(function (ds) {
        selQuan.innerHTML = '';
        selQuan.appendChild(option('', 'Chọn quận, huyện'));
        ds.forEach(function (q) {
          quanTheoMa[q.ma] = q;
          selQuan.appendChild(option(q.ma, q.ten));
        });
        selQuan.disabled = false;
        if (maQuanChon && quanTheoMa[maQuanChon]) {
          selQuan.value = String(maQuanChon);
        }
        dongBoQuan();
        if (selQuan.value) {
          napPhuongV1(selQuan.value, maPhuongChon);
        }
      }).catch(function (e) {
        selQuan.innerHTML = '';
        selQuan.appendChild(option('', 'Lỗi tải quận huyện'));
        dongBoQuan();
      });
    }

    function napPhuongV1(maQuan, maPhuongChon) {
      selPhuong.innerHTML = '';
      selPhuong.appendChild(option('', maQuan ? 'Đang tải phường, xã…' : 'Chọn quận trước'));
      selPhuong.disabled = true;
      dongBoPhuong();

      if (!maQuan) return Promise.resolve();
      var q = quanTheoMa[maQuan];
      if (q && q.khuVucId && selKv) {
        selKv.value = q.khuVucId;
      }

      return dsPhuongV1(maQuan).then(function (ds) {
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Chọn phường, xã'));
        ds.forEach(function (p) {
          selPhuong.appendChild(option(p.ma, nhanPhuong(p.ten)));
        });
        selPhuong.disabled = false;
        if (maPhuongChon) selPhuong.value = String(maPhuongChon);
        dongBoPhuong();
        if (q && q.tenKhuVuc) {
          baoKv('Khu vực phục vụ: ' + q.tenKhuVuc);
        }
      }).catch(function (e) {
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Lỗi tải phường xã'));
        dongBoPhuong();
      });
    }

    // ================= XỬ LÝ CHẾ ĐỘ V2 =================
    function napTinhV2() {
      selTinh.innerHTML = '';
      selTinh.appendChild(option('', 'Đang tải…'));
      dongBoTinh();
      dsTinhV2().then(function (ds) {
        selTinh.innerHTML = '';
        selTinh.appendChild(option('', 'Chọn tỉnh, thành phố'));
        ds.forEach(function (t) { selTinh.appendChild(option(t.ma, t.ten)); });
        selTinh.value = String(khoi.dataset.maTinh || MA_HCM);
        dongBoTinh();
        napPhuongV2(selTinh.value, khoi.dataset.maPhuong, khoi.dataset.khuVucId);
      }).catch(function (e) {
        selTinh.innerHTML = '';
        selTinh.appendChild(option('', 'Không tải được tỉnh'));
        dongBoTinh();
      });
    }

    function capNhatKhuVucV2(giuKv) {
      var p = phuongTheoMa[selPhuong.value];
      var kvs = p ? (p.khuVuc || []) : [];
      ngoaiKhuVuc = !!p && kvs.length === 0;
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

    function napPhuongV2(maTinh, maPhuongChon, kvChon) {
      selPhuong.innerHTML = '';
      selPhuong.appendChild(option('', maTinh ? 'Đang tải…' : 'Chọn tỉnh trước'));
      selPhuong.disabled = true;
      phuongTheoMa = {};
      dongBoPhuong();
      capNhatKhuVucV2();
      if (!maTinh) return Promise.resolve();
      return dsPhuongV2(maTinh).then(function (ds) {
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Chọn phường, xã'));
        ds.forEach(function (p) {
          phuongTheoMa[p.ma] = p;
          selPhuong.appendChild(option(p.ma, nhanPhuong(p.ten)));
        });
        selPhuong.disabled = false;
        if (maPhuongChon && phuongTheoMa[maPhuongChon]) selPhuong.value = String(maPhuongChon);
        dongBoPhuong();
        capNhatKhuVucV2(kvChon);
      }).catch(function (e) {
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Lỗi tải phường xã'));
        dongBoPhuong();
      });
    }

    selTinh.addEventListener('change', function () {
      if (cheDo === 'v1') napQuanV1(selTinh.value);
      else napPhuongV2(selTinh.value);
    });

    selQuan.addEventListener('change', function () {
      if (cheDo === 'v1') napPhuongV1(selQuan.value);
    });

    selPhuong.addEventListener('change', function () {
      if (cheDo === 'v2') capNhatKhuVucV2();
    });

    khoi.__datGiaTri = function (maTinh, maPhuong, khuVucId, maQuan) {
      khoi.dataset.maTinh = maTinh || MA_HCM;
      khoi.dataset.maPhuong = maPhuong || '';
      khoi.dataset.khuVucId = khuVucId || '';
      khoi.dataset.maQuan = maQuan || '';
      if (cheDo === 'v1') {
        selTinh.value = String(khoi.dataset.maTinh);
        dongBoTinh();
        napQuanV1(selTinh.value, khoi.dataset.maQuan, khoi.dataset.maPhuong);
      } else {
        selTinh.value = String(khoi.dataset.maTinh);
        dongBoTinh();
        napPhuongV2(selTinh.value, khoi.dataset.maPhuong, khoi.dataset.khuVucId);
      }
    };

    capNhatCheDo('v1');
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
