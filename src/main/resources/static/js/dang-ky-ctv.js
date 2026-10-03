/* ==========================================================
   Đăng ký làm cộng tác viên (UC-KH02)
   1. Mỗi file được tải lên MinIO ngay khi chọn  → POST /api/storage/upload/...
      Máy chủ trả về objectName (object key) – giá trị này được gửi kèm hồ sơ.
   2. Gửi hồ sơ                                   → POST /v1/collaborators/register
   3. Tra cứu kết quả                             → GET  /v1/collaborators/application-status
   Tỉnh/thành và phường/xã lấy từ https://provinces.open-api.vn/api/v2/ (đơn vị hành chính 2 cấp).
   Máy chủ dựa vào tỉnh/thành + phường/xã để xếp cộng tác viên vào khu vực hoạt động.
   ========================================================== */
(function () {
  'use strict';

  var form = document.getElementById('ctvForm');
  if (!form) return;

  var errorBox = document.getElementById('ctvError');
  var okBox = document.getElementById('ctvOk');
  var submitBtn = document.getElementById('ctvSubmit');
  var MAX_BYTES = 20 * 1024 * 1024;

  // loại tài liệu → object key đã tải lên MinIO
  var uploaded = {};
  var uploading = 0;

  function show(box, message) {
    box.textContent = message;
    box.classList.add('is-shown');
    box.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }
  function hide(box) { box.classList.remove('is-shown'); }

  // ---- Tải file lên MinIO ----
  document.querySelectorAll('[data-upload]').forEach(function (zone) {
    var input = zone.querySelector('input[type="file"]');
    var state = zone.querySelector('.state');
    var type = zone.getAttribute('data-upload');

    function setState(cls, text) {
      zone.classList.remove('is-busy', 'is-done', 'is-failed');
      if (cls) zone.classList.add(cls);
      state.textContent = text || '';
    }

    input.addEventListener('change', function () {
      var file = input.files && input.files[0];
      if (!file) return;
      delete uploaded[type];

      if (file.size > MAX_BYTES) {
        setState('is-failed', 'File lớn hơn 20 MB');
        input.value = '';
        return;
      }

      // Xem trước ảnh ngay trên trình duyệt
      var old = zone.querySelector('img');
      if (old) old.remove();
      if (file.type.indexOf('image/') === 0) {
        var img = document.createElement('img');
        img.alt = '';
        img.src = URL.createObjectURL(file);
        zone.insertBefore(img, state);
      }

      var data = new FormData();
      data.append('file', file);
      uploading++;
      setState('is-busy', 'Đang tải lên…');

      fetch(zone.getAttribute('data-endpoint'), { method: 'POST', body: data })
        .then(function (res) { return res.json().catch(function () { return {}; }).then(function (body) { return { ok: res.ok, body: body }; }); })
        .then(function (r) {
          if (!r.ok || !r.body.success) {
            throw new Error(r.body.error || 'Không tải được file. Thử lại sau.');
          }
          uploaded[type] = r.body.objectName;
          setState('is-done', 'Đã tải lên: ' + file.name);
        })
        .catch(function (err) {
          setState('is-failed', err.message || 'Không tải được file');
          input.value = '';
        })
        .then(function () { uploading--; });
    });
  });

  // ---- Địa chỉ: tỉnh/thành → phường/xã ----
  var DIA_CHI_API = 'https://provinces.open-api.vn/api/v2';
  var tinhSelect = document.getElementById('tinhThanh');
  var phuongSelect = document.getElementById('phuongXa');
  var diaChiLoi = document.getElementById('diaChiLoi');

  function doDuLieu(select, items, placeholder) {
    select.innerHTML = '';
    var first = document.createElement('option');
    first.value = '';
    first.textContent = placeholder;
    select.appendChild(first);
    items.slice().sort(function (a, b) { return a.name.localeCompare(b.name, 'vi'); }).forEach(function (item) {
      var opt = document.createElement('option');
      opt.value = item.name;            // tên gửi lên máy chủ
      opt.setAttribute('data-code', item.code);
      opt.textContent = item.name;
      select.appendChild(opt);
    });
  }

  function layJson(url) {
    return fetch(url).then(function (res) {
      if (!res.ok) throw new Error('HTTP ' + res.status);
      return res.json();
    });
  }

  function taiTinhThanh() {
    diaChiLoi.classList.remove('is-shown');
    tinhSelect.disabled = true;
    tinhSelect.innerHTML = '<option value="">Đang tải danh sách…</option>';
    layJson(DIA_CHI_API + '/p/')
      .then(function (list) {
        doDuLieu(tinhSelect, list || [], 'Chọn tỉnh, thành phố');
        tinhSelect.disabled = false;
      })
      .catch(function () {
        tinhSelect.innerHTML = '<option value="">Chưa tải được</option>';
        diaChiLoi.classList.add('is-shown');
      });
  }

  function taiPhuongXa() {
    var chon = tinhSelect.options[tinhSelect.selectedIndex];
    var code = chon ? chon.getAttribute('data-code') : null;
    phuongSelect.disabled = true;
    if (!code) {
      phuongSelect.innerHTML = '<option value="">Chọn tỉnh, thành phố trước</option>';
      return;
    }
    phuongSelect.innerHTML = '<option value="">Đang tải danh sách…</option>';
    layJson(DIA_CHI_API + '/p/' + encodeURIComponent(code) + '?depth=2')
      .then(function (tinh) {
        // Đề phòng thao tác đổi tỉnh liên tiếp: chỉ nhận kết quả của tỉnh đang chọn
        var hienTai = tinhSelect.options[tinhSelect.selectedIndex];
        if (!hienTai || hienTai.getAttribute('data-code') !== code) return;
        doDuLieu(phuongSelect, (tinh && tinh.wards) || [], 'Chọn phường, xã');
        phuongSelect.disabled = false;
      })
      .catch(function () {
        phuongSelect.innerHTML = '<option value="">Chưa tải được</option>';
        diaChiLoi.classList.add('is-shown');
      });
  }

  if (tinhSelect && phuongSelect) {
    tinhSelect.addEventListener('change', taiPhuongXa);
    document.getElementById('diaChiTaiLai').addEventListener('click', function () {
      if (tinhSelect.value) { diaChiLoi.classList.remove('is-shown'); taiPhuongXa(); } else { taiTinhThanh(); }
    });
    taiTinhThanh();
  }

  // ---- Kiểm tra dữ liệu ----
  function mark(field, bad) {
    if (field) field.classList.toggle('has-error', bad);
    return bad;
  }
  function du18Tuoi(value) {
    if (!value) return false;
    var sinh = new Date(value);
    var moc = new Date();
    moc.setFullYear(moc.getFullYear() - 18);
    return sinh <= moc;
  }

  function validate() {
    var bad = false;
    ['hoTen', 'soDienThoai', 'email', 'diaChiChiTiet', 'matKhau'].forEach(function (id) {
      var input = document.getElementById(id);
      if (mark(input.closest('.field'), !input.checkValidity())) bad = true;
    });
    [tinhSelect, phuongSelect].forEach(function (select) {
      if (mark(select.closest('.field'), !select.value)) bad = true;
    });
    var ngaySinh = document.getElementById('ngaySinh');
    if (mark(ngaySinh.closest('.field'), !du18Tuoi(ngaySinh.value))) bad = true;

    var coDichVu = form.querySelectorAll('input[name="loaiDichVu"]:checked').length > 0;
    if (mark(document.getElementById('dichVuField'), !coDichVu)) bad = true;

    var duHoSo = uploaded.AnhChanDung && uploaded.CCCD_Mat_Truoc && uploaded.CCCD_Mat_Sau;
    if (mark(document.getElementById('hoSoField'), !duHoSo)) bad = true;
    return !bad;
  }

  // Sửa xong mục nào thì bỏ đánh dấu lỗi của mục đó ngay
  ['input', 'change'].forEach(function (evt) {
    form.addEventListener(evt, function (e) {
      var field = e.target.closest ? e.target.closest('.field') : null;
      if (field) field.classList.remove('has-error');
    });
  });

  function checkedValues(name) {
    return Array.prototype.map.call(form.querySelectorAll('input[name="' + name + '"]:checked'),
      function (el) { return parseInt(el.value, 10); });
  }

  // ---- Gửi hồ sơ ----
  form.addEventListener('submit', function (e) {
    e.preventDefault();
    hide(errorBox); hide(okBox);

    if (uploading > 0) {
      show(errorBox, 'File đang được tải lên. Chờ tải xong rồi gửi hồ sơ.');
      return;
    }
    if (!validate()) {
      show(errorBox, 'Hồ sơ còn thiếu thông tin. Xem các mục được đánh dấu bên dưới.');
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Đang gửi hồ sơ…';

    var loaiDaChon = checkedValues('loaiDichVu');

    // Lấy các dịch vụ thuộc nhóm đã chọn (API này được cache Redis)
    fetch('/v1/services')
      .then(function (res) { return res.json(); })
      .then(function (body) {
        var dichVuIds = (body.data || [])
          .filter(function (dv) { return loaiDaChon.indexOf(dv.loaiDichVuId) !== -1; })
          .map(function (dv) { return dv.id; });

        var hoSo = ['AnhChanDung', 'CCCD_Mat_Truoc', 'CCCD_Mat_Sau'].map(function (loai) {
          return { loaiTaiLieu: loai, duongDanFile: uploaded[loai] };
        });
        var chungChi = [];
        if (uploaded.ChungChi) {
          chungChi.push({
            loaiChungChi: 'ChungChi',
            tenChungChi: document.getElementById('tenChungChi').value.trim() || 'Chứng chỉ đính kèm',
            duongDanFile: uploaded.ChungChi
          });
        }

        var payload = {
          hoTen: document.getElementById('hoTen').value.trim(),
          soDienThoai: document.getElementById('soDienThoai').value.trim(),
          email: document.getElementById('email').value.trim() || null,
          matKhau: document.getElementById('matKhau').value,
          ngaySinh: document.getElementById('ngaySinh').value,
          gioiTinh: document.getElementById('gioiTinh').value,
          // Địa chỉ đầy đủ để hiển thị; tỉnh/thành + phường/xã để máy chủ xếp khu vực
          noiCuTru: [document.getElementById('diaChiChiTiet').value.trim(), phuongSelect.value, tinhSelect.value].join(', '),
          tinhThanh: tinhSelect.value,
          phuongXa: phuongSelect.value,
          danhSachDichVuId: dichVuIds,
          danhSachHoSo: hoSo,
          danhSachChungChi: chungChi
        };

        return fetch('/v1/collaborators/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      })
      .then(function (res) { return res.json().catch(function () { return {}; }).then(function (body) { return { ok: res.ok, body: body }; }); })
      .then(function (r) {
        if (!r.ok || !r.body.success) {
          throw new Error(r.body.message || 'Chưa gửi được hồ sơ. Kiểm tra lại thông tin rồi thử lại.');
        }
        var d = r.body.data || {};
        form.reset();
        taiPhuongXa();
        uploaded = {};
        document.querySelectorAll('[data-upload]').forEach(function (zone) {
          zone.classList.remove('is-busy', 'is-done', 'is-failed');
          var img = zone.querySelector('img');
          if (img) img.remove();
        });
        show(okBox, 'Đã gửi hồ sơ' + (d.maCongTacVien ? ' ' + d.maCongTacVien : '') +
          '. Thời gian xét duyệt dự kiến ' + (d.thoiGianXetDuyetDuKien || '1-3 ngày làm việc') +
          (d.khuVuc ? '. Khu vực nhận việc: ' + d.khuVuc : '') +
          '. Bạn có thể tra cứu kết quả bằng số điện thoại.');
      })
      .catch(function (err) { show(errorBox, err.message); })
      .then(function () {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Gửi hồ sơ';
      });
  });

  // ---- Tra cứu kết quả ----
  var traCuuForm = document.getElementById('traCuuForm');
  var ketQua = document.getElementById('traCuuKetQua');
  var TRANG_THAI = {
    ChoDuyet: 'Hồ sơ đang chờ xét duyệt.',
    HoatDong: 'Hồ sơ đã được duyệt. Bạn có thể đăng nhập ứng dụng cộng tác viên.',
    TuChoi: 'Hồ sơ chưa đạt yêu cầu.',
    DinhChi: 'Tài khoản cộng tác viên đang bị đình chỉ.'
  };
  if (traCuuForm) {
    traCuuForm.addEventListener('submit', function (e) {
      e.preventDefault();
      var sdt = document.getElementById('traCuuSdt').value.trim();
      if (!sdt) { ketQua.textContent = 'Nhập số điện thoại đã đăng ký.'; return; }
      ketQua.textContent = 'Đang tra cứu…';
      fetch('/v1/collaborators/application-status?soDienThoai=' + encodeURIComponent(sdt))
        .then(function (res) { return res.json().catch(function () { return {}; }).then(function (body) { return { ok: res.ok, body: body }; }); })
        .then(function (r) {
          if (!r.ok || !r.body.success) {
            ketQua.textContent = r.body.message || 'Không tìm thấy hồ sơ với số điện thoại này.';
            return;
          }
          var d = r.body.data || {};
          ketQua.textContent = TRANG_THAI[d.trangThai] || d.message || ('Trạng thái hồ sơ: ' + (d.trangThai || 'không rõ'));
        })
        .catch(function () { ketQua.textContent = 'Không kết nối được máy chủ. Thử lại sau.'; });
    });
  }
})();
