/*
 * Form địa chỉ dùng chung (sổ địa chỉ, đặt dịch vụ, đăng ký CTV, CSKH thêm khách), khớp bảng DiaChiKhachHang
 * (một chuỗi địa chỉ + khuVucId):
 *   Ô 1 – tỉnh, thành: tỉnh sau sáp nhập (provinces.open-api.vn/api/v2) và trước sáp nhập (api/v1) gộp theo tên;
 *          tỉnh chỉ còn ở danh mục cũ ghi "(cũ)".
 *   Ô 2 – cấp ngay dưới tỉnh: phường, xã mới và quận, huyện cũ của tỉnh đó (quận ghi "(cũ)").
 *   Ô 3 – số nhà, tên đường.
 * Khu vực (quận, huyện phục vụ) suy ra từ ô 2. Gửi lên: phường mới → maTinh + maPhuong; quận cũ → maTinh (cũ) + maQuan.
 */
(function () {
  var MA_HCM = 79;
  var tinhPromise = null;
  var duoiPromise = {};

  function layJson(url) {
    return fetch(url, { headers: { Accept: 'application/json' } })
      .then(function (r) {
        return r.json().then(function (d) {
          if (!r.ok || d.success === false) throw new Error(d.message || 'Lỗi tải dữ liệu');
          return d.data;
        });
      });
  }

  // [{ma, ten, cu, maCu}]
  function dsTinh() {
    return tinhPromise || (tinhPromise = layJson('/v1/dia-gioi/tinh-gop').catch(function (e) {
      tinhPromise = null;
      throw e;
    }));
  }
  // khoa: "79" (tỉnh mới) hoặc "c74" (tỉnh chỉ còn ở danh mục cũ) → [{ma, ten, cu, maTinh, khuVuc}]
  function dsDuoi(khoa) {
    var cu = /^c/.test(khoa), ma = String(khoa).replace(/^c/, '');
    return duoiPromise[khoa] || (duoiPromise[khoa] = layJson('/v1/dia-gioi/tinh-gop/' + ma + '/duoi?cu=' + cu).catch(function (e) {
      delete duoiPromise[khoa];
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
    // Mở danh sách: hiện đủ mọi dòng (không lọc theo tên đang chọn); ô trống, tên đang chọn làm chữ mờ
    function mo() {
      if (o.disabled) return;
      o.placeholder = nhanChon() || goiY() || 'Gõ để tìm';
      o.value = '';
      ve('');
      ds.hidden = false;
      o.setAttribute('aria-expanded', 'true');
    }
    function dong() {
      ds.hidden = true;
      o.setAttribute('aria-expanded', 'false');
      o.placeholder = goiY() || 'Gõ để tìm';
      o.value = nhanChon();
    }
    function chon(op) {
      var doi = sel.value !== op.value;
      sel.value = op.value;
      dong();
      if (doi) sel.dispatchEvent(new Event('change', { bubbles: true }));
    }

    o.addEventListener('focus', mo);
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
      if (ds.hidden) {
        o.placeholder = goiY() || 'Gõ để tìm';
        o.value = nhanChon();
      } else {
        o.placeholder = nhanChon() || goiY() || 'Gõ để tìm';
        ve(o.value);
      }
    };
    sel.__dongBo();
    return sel.__dongBo;
  }

  function nhanPhuong(ten) {
    var m = /^(Phường|Xã|Đặc khu|Thị trấn) (.*)$/.exec(ten);
    return m ? m[2] + ' (' + m[1] + ')' : ten;
  }

  // Giá trị ô tỉnh: "79" = tỉnh sau sáp nhập (gộp với tỉnh cũ cùng tên), "c74" = tỉnh chỉ còn ở danh mục cũ.
  // Giá trị ô thứ hai: "26734" = phường, xã mới; "q767" = quận, huyện cũ (mã hai loại có thể trùng nhau).
  function khoaTinh(ma, cu) { return (cu ? 'c' : '') + ma; }
  function khoaDuoi(ma, cu) { return (cu ? 'q' : '') + ma; }

  function ganKhoi(khoi) {
    if (khoi.__diaChi) return;
    khoi.__diaChi = true;

    var selTinh = khoi.querySelector('select[name="maTinh"]');
    var selDuoi = khoi.querySelector('select[name="maPhuong"]'); // ô thứ hai: phường, xã hoặc quận, huyện
    var selKv = khoi.querySelector('select[name="khuVucId"]');
    var kvWrap = khoi.querySelector('[data-kv-wrap]');
    var note = khoi.querySelector('[data-kv-note]');
    var batBuoc = khoi.hasAttribute('data-bat-buoc-khu-vuc');

    // Ô quận riêng (form cũ 3 cấp) không còn dùng: quận, huyện nằm chung ô thứ hai
    var selQuanCu = khoi.querySelector('select[name="maQuan"]');
    if (selQuanCu) {
      selQuanCu.disabled = true;
      selQuanCu.required = false;
      var quanWrap = khoi.querySelector('[data-quan-wrap]') || selQuanCu.closest('.form-group') || selQuanCu.closest('.field');
      if (quanWrap) quanWrap.style.display = 'none';
    }
    var duoiWrap = selDuoi.closest('[data-phuong-wrap]') || selDuoi.closest('.form-group') || selDuoi.closest('.field');
    if (duoiWrap) duoiWrap.classList.add('col-full');

    var dongBoTinh = timChon(selTinh);
    var dongBoDuoi = timChon(selDuoi);
    var dsTinhDaNap = [];
    var duoiTheoMa = {};

    // Mã gửi lên theo dòng đang chọn ở ô thứ hai: phường mới → maTinh (mới) + maPhuong; quận cũ → maTinh (cũ) + maQuan
    function giaTriGui() {
      var x = duoiTheoMa[selDuoi.value];
      var tinh = selTinh.value ? parseInt(String(selTinh.value).replace(/^c/, ''), 10) : null;
      if (!x) return { maTinh: tinh, maPhuong: null, maQuan: null };
      return {
        maTinh: x.maTinh != null ? x.maTinh : tinh,
        maPhuong: x.cu ? null : x.ma,
        maQuan: x.cu ? x.ma : null
      };
    }

    // Khoá ô tỉnh của địa chỉ đã lưu: tỉnh mới theo mã; địa chỉ cũ (có quận) theo mã tỉnh cũ
    function khoaTinhDaLuu(maTinh, coQuan) {
      if (!maTinh) return String(MA_HCM);
      if (!coQuan) return String(maTinh);
      var n = Number(maTinh);
      var t = dsTinhDaNap.filter(function (t) { return t.cu ? t.ma === n : t.maCu === n; })[0];
      return t ? khoaTinh(t.ma, t.cu) : String(MA_HCM);
    }

    function baoKv(text, loi) {
      if (!note) return;
      note.textContent = text || '';
      note.style.color = loi ? 'var(--danger, #C70036)' : '#6b7280';
    }

    function capNhatKhuVuc(giuKv) {
      var x = duoiTheoMa[selDuoi.value];
      var kvs = x ? (x.khuVuc || []) : [];
      khoi.dispatchEvent(new CustomEvent('diachi:khuvuc', { detail: kvs, bubbles: true }));

      if (!selKv) {
        baoKv(!x ? '' : kvs.length ? 'Thuộc khu vực: ' + kvs.map(function (k) { return k.tenKhuVuc; }).join(' / ')
                                   : 'Nơi ở ngoài khu vực bTaskee đang phục vụ.', false);
        return;
      }
      var cu = giuKv || selKv.value;
      selKv.innerHTML = '';
      if (!x) { if (kvWrap) kvWrap.hidden = true; selKv.required = false; baoKv(''); return; }
      if (kvs.length === 0) {
        if (kvWrap) kvWrap.hidden = true; selKv.required = false;
        baoKv(batBuoc ? 'Địa chỉ này nằm ngoài khu vực phục vụ.'
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

    function napTinh() {
      selTinh.innerHTML = '';
      selTinh.appendChild(option('', 'Đang tải…'));
      dongBoTinh();
      dsTinh().then(function (ds) {
        dsTinhDaNap = ds;
        selTinh.innerHTML = '';
        selTinh.appendChild(option('', 'Chọn tỉnh, thành phố'));
        ds.forEach(function (t) {
          selTinh.appendChild(option(khoaTinh(t.ma, t.cu), t.cu ? t.ten + ' (cũ)' : t.ten));
        });
        var coQuan = !!khoi.dataset.maQuan;
        selTinh.value = khoaTinhDaLuu(khoi.dataset.maTinh, coQuan);
        if (!selTinh.value) selTinh.value = String(MA_HCM);
        dongBoTinh();
        napDuoi(coQuan ? khoaDuoi(khoi.dataset.maQuan, true) : khoi.dataset.maPhuong, khoi.dataset.khuVucId);
      }).catch(function () {
        selTinh.innerHTML = '';
        selTinh.appendChild(option('', 'Không tải được tỉnh'));
        dongBoTinh();
      });
    }

    function napDuoi(maChon, kvChon) {
      var khoa = selTinh.value;
      selDuoi.innerHTML = '';
      selDuoi.appendChild(option('', khoa ? 'Đang tải…' : 'Chọn tỉnh trước'));
      selDuoi.disabled = true;
      duoiTheoMa = {};
      dongBoDuoi();
      capNhatKhuVuc();
      if (!khoa) return Promise.resolve();
      return dsDuoi(khoa).then(function (ds) {
        if (selTinh.value !== khoa) return; // đã đổi tỉnh trong lúc tải
        selDuoi.innerHTML = '';
        selDuoi.appendChild(option('', 'Chọn phường, xã / quận, huyện'));
        ds.forEach(function (x) {
          var k = khoaDuoi(x.ma, x.cu);
          duoiTheoMa[k] = x;
          selDuoi.appendChild(option(k, x.cu ? x.ten + ' (cũ)' : nhanPhuong(x.ten)));
        });
        // Form theo bước (đăng ký CTV): ô còn bị khoá thì giữ khoá đến khi tới bước này
        selDuoi.disabled = !!selDuoi.closest('.is-locked');
        if (maChon && duoiTheoMa[maChon]) selDuoi.value = String(maChon);
        dongBoDuoi();
        capNhatKhuVuc(kvChon);
      }).catch(function () {
        selDuoi.innerHTML = '';
        selDuoi.appendChild(option('', 'Lỗi tải danh sách'));
        dongBoDuoi();
      });
    }

    selTinh.addEventListener('change', function () { napDuoi(); });
    selDuoi.addEventListener('change', function () { capNhatKhuVuc(); });

    // Gửi form: thay khoá trong ô ("c74", "q767") bằng mã số maTinh + maPhuong hoặc maQuan
    var form = khoi.closest('form');
    if (form) {
      form.addEventListener('formdata', function (e) {
        var g = giaTriGui();
        e.formData.delete('maPhuong');
        e.formData.delete('maQuan');
        if (g.maTinh != null) e.formData.set('maTinh', g.maTinh);
        if (g.maPhuong != null) e.formData.set('maPhuong', g.maPhuong);
        if (g.maQuan != null) e.formData.set('maQuan', g.maQuan);
      });
    }

    khoi.__giaTri = giaTriGui;

    // maQuan có = địa chỉ cũ đã lưu (maTinh là mã tỉnh cũ)
    khoi.__datGiaTri = function (maTinh, maPhuong, khuVucId, maQuan) {
      khoi.dataset.maTinh = maTinh || MA_HCM;
      khoi.dataset.maPhuong = maPhuong || '';
      khoi.dataset.maQuan = maQuan || '';
      khoi.dataset.khuVucId = khuVucId || '';
      napTinh();
    };

    // Địa chỉ cũ đã lưu do template truyền dạng data-ma-tinh="c79" + data-ma-phuong=<mã quận>
    if (/^c/.test(khoi.dataset.maTinh || '')) {
      khoi.dataset.maTinh = khoi.dataset.maTinh.slice(1);
      if (!khoi.dataset.maQuan) khoi.dataset.maQuan = khoi.dataset.maPhuong || '';
      khoi.dataset.maPhuong = '';
    }
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
    },
    /** {maTinh, maPhuong, maQuan} để gửi bằng JSON: quận cũ có maQuan, phường mới có maPhuong. */
    giaTri: function (khoi) {
      if (!khoi) return {};
      ganKhoi(khoi);
      return khoi.__giaTri ? khoi.__giaTri() : {};
    }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { ganTatCa(); });
  } else {
    ganTatCa();
  }
})();
