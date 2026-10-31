/* ==========================================================
   Đăng ký làm cộng tác viên (UC-KH02)
   1. Chọn file: chỉ giữ tạm trên trình duyệt (xem trước), CHƯA tải lên MinIO
   2. Gửi hồ sơ (không kèm file)                  → POST /v1/collaborators/register  → maGuiGiayTo
   3. Hồ sơ lưu thành công mới tải file lên MinIO → POST /api/storage/upload/...      → objectName
   4. Gắn object key vào hồ sơ                    → POST /v1/collaborators/register/giay-to
      Bước 3-4 lỗi thì giữ file và mã, bấm "Gửi lại giấy tờ" để thử lại (không tạo hồ sơ mới).
   5. Tra cứu kết quả                             → GET  /v1/collaborators/application-status
   Địa chỉ hiện tại: phường/xã TP.HCM lấy từ danh sách khu vực phục vụ của hệ thống.
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

  // loại tài liệu → file đã chọn (giữ tạm trên trình duyệt, chưa tải lên MinIO)
  var chon = {};
  // Hồ sơ đã lưu nhưng giấy tờ chưa gửi xong: { ma, d } – lần bấm tiếp chỉ gửi lại giấy tờ
  var giayToCho = null;
  // loại tài liệu → object key đã tải lên (gửi lại giấy tờ thì không tải lại file đã lên)
  var daTaiLen = {};

  function show(box, message) {
    box.textContent = message;
    box.classList.add('is-shown');
    box.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }
  function hide(box) { box.classList.remove('is-shown'); }

  function setState(zone, cls, text) {
    zone.classList.remove('is-busy', 'is-done', 'is-failed');
    if (cls) zone.classList.add(cls);
    zone.querySelector('.state').textContent = text || '';
  }

  // ---- Chọn file: chỉ xem trước, chưa tải lên ----
  document.querySelectorAll('[data-upload]').forEach(function (zone) {
    var input = zone.querySelector('input[type="file"]');
    var type = zone.getAttribute('data-upload');

    input.addEventListener('change', function () {
      var file = input.files && input.files[0];
      delete chon[type];
      delete daTaiLen[type];
      if (!file) { setState(zone, null, ''); return; }

      if (file.size > MAX_BYTES) {
        setState(zone, 'is-failed', 'File lớn hơn 20 MB');
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
        zone.insertBefore(img, zone.querySelector('.state'));
      }
      chon[type] = file;
      setState(zone, 'is-done', 'Đã chọn: ' + file.name);
    });
  });

  function docJson(res) {
    return res.json().catch(function () { return {}; }).then(function (body) { return { ok: res.ok, body: body }; });
  }

  // Tải một file lên MinIO (chỉ gọi sau khi hồ sơ đã lưu thành công) → object key
  function taiLen(type) {
    var zone = form.querySelector('[data-upload="' + type + '"]');
    var data = new FormData();
    data.append('file', chon[type]);
    setState(zone, 'is-busy', 'Đang tải lên…');
    return fetch(zone.getAttribute('data-endpoint'), { method: 'POST', body: data })
      .then(docJson)
      .then(function (r) {
        if (!r.ok || !r.body.success) {
          throw new Error(r.body.error || 'Không tải được file ' + chon[type].name + '.');
        }
        setState(zone, 'is-done', 'Đã tải lên: ' + chon[type].name);
        return r.body.objectName;
      })
      .catch(function (err) {
        setState(zone, 'is-failed', err.message || 'Không tải được file');
        throw err;
      });
  }

  // Sau khi gửi hồ sơ thành công: tải lần lượt từng file rồi gắn vào hồ sơ bằng mã một lần
  function guiGiayTo(ma) {
    var loaiHoSo = ['AnhChanDung', 'CCCD_Mat_Truoc', 'CCCD_Mat_Sau'];
    var loai = loaiHoSo.concat(chon.ChungChi ? ['ChungChi'] : []);
    var keys = daTaiLen;
    return loai.reduce(function (p, type) {
      return p.then(function () {
        if (keys[type]) return null;
        return taiLen(type).then(function (key) { keys[type] = key; });
      });
    }, Promise.resolve())
      .then(function () {
        var chungChi = keys.ChungChi ? [{
          loaiChungChi: 'ChungChi',
          tenChungChi: document.getElementById('tenChungChi').value.trim() || 'Chứng chỉ đính kèm',
          duongDanFile: keys.ChungChi
        }] : [];
        return fetch('/v1/collaborators/register/giay-to', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            maGuiGiayTo: ma,
            danhSachHoSo: loaiHoSo.map(function (t) { return { loaiTaiLieu: t, duongDanFile: keys[t] }; }),
            danhSachChungChi: chungChi
          })
        });
      })
      .then(docJson)
      .then(function (r) {
        if (!r.ok || !r.body.success) throw new Error(r.body.message || 'Chưa lưu được giấy tờ vào hồ sơ.');
      });
  }

  // ---- Địa chỉ hiện tại: tỉnh (mới + cũ) và phường xã / quận huyện – /js/dia-chi.js ----
  var tinhSelect = document.getElementById('tinhThanh');
  var phuongSelect = document.getElementById('phuongXa');

  // ---- Kiểm tra dữ liệu ----
  function mark(field, bad) {
    if (field) field.classList.toggle('has-error', bad);
    return bad;
  }
  // Lịch chọn ngày sinh chỉ cho chọn người đủ 18 tuổi
  (function () {
    var el = document.getElementById('ngaySinh');
    if (!el) return;
    var d = new Date();
    d.setFullYear(d.getFullYear() - 18);
    el.max = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    if (window.Lich) Lich.dongBo(el); // lịch chọn ngày (lich.js) nhận mốc 18 tuổi
  })();

  function du18Tuoi(value) {
    if (!value) return false;
    var sinh = new Date(value);
    var moc = new Date();
    moc.setFullYear(moc.getFullYear() - 18);
    return sinh <= moc;
  }

  function validate() {
    var bad = false;
    ['hoTen', 'soDienThoai', 'email', 'diaChiChiTiet'].forEach(function (id) {
      var input = document.getElementById(id);
      if (mark(input.closest('.field'), !input.checkValidity())) bad = true;
    });
    [tinhSelect, phuongSelect].forEach(function (select) {
      if (mark(select.closest('.field'), !select.value)) bad = true;
    });
    var ngaySinh = document.getElementById('ngaySinh');
    if (mark(ngaySinh.closest('.field'), !du18Tuoi(ngaySinh.value))) bad = true;

    // Validate mật khẩu
    var matKhauInput = document.getElementById('matKhau');
    var xacNhanInput = document.getElementById('xacNhanMatKhau');
    var xacNhanErr = document.getElementById('xacNhanMatKhauError');
    if (matKhauInput) {
      var mk = matKhauInput.value;
      // Dùng chung tài khoản đã có: nhập mật khẩu hiện tại, không áp quy tắc mật khẩu mới
      var dungChung = document.getElementById('coTaiKhoanSan');
      var chuaDat = window.QuyTacMatKhau && !(dungChung && dungChung.checked)
        ? QuyTacMatKhau.kiemTra(mk, document.getElementById('soDienThoai').value).length > 0
        : false;
      if (!mk || chuaDat) {
        mark(matKhauInput.closest('.field'), true);
        bad = true;
      } else {
        mark(matKhauInput.closest('.field'), false);
      }
      if (xacNhanInput) {
        var xn = xacNhanInput.value;
        var khongKhop = !xn || xn !== mk;
        mark(xacNhanInput.closest('.field'), khongKhop);
        if (xacNhanErr) xacNhanErr.style.display = khongKhop ? '' : 'none';
        if (khongKhop) bad = true;
      }
    }

    var coDichVu = form.querySelectorAll('input[name="loaiDichVu"]:checked').length > 0;
    if (mark(document.getElementById('dichVuField'), !coDichVu)) bad = true;

    var duHoSo = chon.AnhChanDung && chon.CCCD_Mat_Truoc && chon.CCCD_Mat_Sau;
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

  function datNut(dangGui) {
    submitBtn.disabled = dangGui;
    submitBtn.textContent = dangGui ? 'Đang gửi hồ sơ…' : (giayToCho ? 'Gửi lại giấy tờ' : 'Gửi hồ sơ');
  }

  function xongHet(d) {
    giayToCho = null;
    form.reset();
    chon = {};
    daTaiLen = {};
    document.querySelectorAll('[data-upload]').forEach(function (zone) {
      setState(zone, null, '');
      var img = zone.querySelector('img');
      if (img) img.remove();
    });
    show(okBox, 'Đã gửi hồ sơ' + (d.maCongTacVien ? ' ' + d.maCongTacVien : '') +
      '. Thời gian xét duyệt dự kiến ' + (d.thoiGianXetDuyetDuKien || '1-3 ngày làm việc') +
      (d.khuVuc ? '. Khu vực nhận việc: ' + d.khuVuc : '') +
      '. Khi hồ sơ được duyệt, tài khoản sẽ được kích hoạt và bạn nhận tin nhắn thông báo. Đăng nhập bằng số điện thoại và mật khẩu đã đặt.');
  }

  // Hồ sơ đã lưu: tải giấy tờ; lỗi thì giữ file + mã để bấm gửi lại
  function guiGiayToCho() {
    var cho = giayToCho;
    if (!cho.ma) {
      giayToCho = null;
      show(errorBox, 'Đã gửi hồ sơ ' + (cho.d.maCongTacVien || '') +
        ' nhưng máy chủ chưa nhận được giấy tờ. Liên hệ bộ phận nhân sự để bổ sung ảnh chân dung và CCCD.');
      return Promise.resolve();
    }
    return guiGiayTo(cho.ma)
      .then(function () { xongHet(cho.d); })
      .catch(function (err) {
        show(errorBox, 'Đã gửi hồ sơ ' + (cho.d.maCongTacVien || '') + ' nhưng chưa tải được giấy tờ: ' +
          (err.message || 'lỗi kết nối') + ' Bấm "Gửi lại giấy tờ" để thử lại.');
      });
  }

  // ---- Gửi hồ sơ ----
  form.addEventListener('submit', function (e) {
    e.preventDefault();
    hide(errorBox); hide(okBox);

    if (!validate()) {
      show(errorBox, 'Hồ sơ còn thiếu thông tin. Xem các mục được đánh dấu bên dưới.');
      return;
    }

    datNut(true);

    // Hồ sơ đã lưu ở lần trước, chỉ còn thiếu giấy tờ: không tạo hồ sơ mới
    if (giayToCho) {
      guiGiayToCho().then(function () { datNut(false); });
      return;
    }

    var loaiDaChon = checkedValues('loaiDichVu');

    // Lấy các dịch vụ thuộc nhóm đã chọn (API này được cache Redis)
    fetch('/v1/services')
      .then(function (res) { return res.json(); })
      .then(function (body) {
        var dichVuIds = (body.data || [])
          .filter(function (dv) { return loaiDaChon.indexOf(dv.loaiDichVuId) !== -1; })
          .map(function (dv) { return dv.id; });

        // Giấy tờ chưa tải lên: chỉ tải sau khi hồ sơ lưu thành công
        var diaChi = DiaChi.giaTri(tinhSelect.closest('[data-dia-chi-form]'));
        var payload = {
          hoTen: document.getElementById('hoTen').value.trim(),
          soDienThoai: document.getElementById('soDienThoai').value.trim(),
          email: document.getElementById('email').value.trim() || null,
          matKhau: document.getElementById('matKhau') ? document.getElementById('matKhau').value : null,
          ngaySinh: document.getElementById('ngaySinh').value,
          gioiTinh: document.getElementById('gioiTinh').value,
          // Địa chỉ nơi ở: số nhà + tỉnh và phường xã (tỉnh mới) hoặc quận huyện (tỉnh cũ);
          // máy chủ ghép tên và lấy quận làm khu vực nhận việc ban đầu
          soNhaTenDuong: document.getElementById('diaChiChiTiet').value.trim(),
          maTinh: diaChi.maTinh,
          maQuan: diaChi.maQuan,
          maPhuong: diaChi.maPhuong,
          danhSachDichVuId: dichVuIds
        };

        return fetch('/v1/collaborators/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      })
      .then(docJson)
      .then(function (r) {
        if (!r.ok || !r.body.success) {
          throw new Error(r.body.message || 'Chưa gửi được hồ sơ. Kiểm tra lại thông tin rồi thử lại.');
        }
        var d = r.body.data || {};
        giayToCho = { ma: d.maGuiGiayTo, d: d };
        return guiGiayToCho();
      })
      .catch(function (err) { show(errorBox, err.message); })
      .then(function () { datNut(false); });
  });

  // ---- Tra cứu kết quả ----
  var traCuuForm = document.getElementById('traCuuForm');
  var ketQua = document.getElementById('traCuuKetQua');
  var TRANG_THAI = {
    ChoDuyet: 'Hồ sơ đang chờ xét duyệt.',
    HoatDong: 'Hồ sơ đã được duyệt, tài khoản đã được kích hoạt. Đăng nhập ứng dụng bằng số điện thoại và mật khẩu đã đặt.',
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
