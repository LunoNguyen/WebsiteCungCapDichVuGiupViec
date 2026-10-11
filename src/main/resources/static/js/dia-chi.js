/*
 * Form địa chỉ dùng chung (sổ địa chỉ, đặt dịch vụ, đăng ký CTV, CSKH thêm khách):
 *   Tỉnh, thành → Phường, xã (provinces.open-api.vn v2, qua /v1/dia-gioi) + số nhà, tên đường.
 * Khu vực (quận, huyện) tự suy ra từ phường: 1 khu vực thì chọn sẵn, nhiều khu vực thì cho chọn,
 * không có thì báo "ngoài khu vực phục vụ".
 *
 * Đánh dấu bằng [data-dia-chi-form], bên trong có:
 *   select[name=maTinh], select[name=maPhuong], select[name=khuVucId] (trong [data-kv-wrap]), [data-kv-note]
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
      capNhatKhuVuc();
      if (!maTinh) return Promise.resolve();
      return dsPhuong(maTinh).then(function (ds) {
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Chọn phường, xã'));
        ds.forEach(function (p) { phuongTheoMa[p.ma] = p; selPhuong.appendChild(option(p.ma, nhanPhuong(p.ten))); });
        selPhuong.disabled = false;
        if (maPhuongChon && phuongTheoMa[maPhuongChon]) selPhuong.value = String(maPhuongChon);
        capNhatKhuVuc(kvChon);
      }).catch(function (e) {
        selPhuong.innerHTML = '';
        selPhuong.appendChild(option('', 'Không tải được, chọn lại tỉnh để thử lại'));
        baoKv(e.message, true);
      });
    }

    selTinh.innerHTML = '';
    selTinh.appendChild(option('', 'Đang tải…'));
    dsTinh().then(function (ds) {
      selTinh.innerHTML = '';
      selTinh.appendChild(option('', 'Chọn tỉnh, thành phố'));
      ds.forEach(function (t) { selTinh.appendChild(option(t.ma, t.ten)); });
      var maTinh = khoi.dataset.maTinh || MA_HCM;
      selTinh.value = String(maTinh);
      napPhuong(selTinh.value, khoi.dataset.maPhuong, khoi.dataset.khuVucId);
    }).catch(function (e) {
      selTinh.innerHTML = '';
      selTinh.appendChild(option('', 'Không tải được danh sách tỉnh'));
      baoKv(e.message, true);
    });

    // Đặt lại giá trị (VD mở hộp sửa với dữ liệu của bản ghi khác)
    khoi.__datGiaTri = function (maTinh, maPhuong, khuVucId) {
      dsTinh().then(function () {
        selTinh.value = String(maTinh || MA_HCM);
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
          selPhuong.focus();
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
