import sys
sys.stdout.reconfigure(encoding='utf-8')

js_path = r'd:\2026\HK1_Nam4\LuanAn\SourceCode\WebsiteCungCapDichVuGiupViec\src\main\resources\static\js\dang-ky-ctv.js'
with open(js_path, 'r', encoding='utf-8') as f:
    code = f.read()

# Let's inspect the sections we need to replace:
# 1. Lines 46-133:
dead_block_start = '  // ---- Chọn file: chỉ xem trước, chưa tải lên ----'
dead_block_end = '  // ---- Địa chỉ hiện tại: tỉnh (mới + cũ) và phường xã / quận huyện – /js/dia-chi.js ----'

if dead_block_start in code and dead_block_end in code:
    idx1 = code.find(dead_block_start)
    idx2 = code.find(dead_block_end)
    code = code[:idx1] + code[idx2:]
    print('Removed duplicate data-upload change listener and unused taiLen/guiGiayTo block.')
else:
    print('Warning: dead block not found as expected')

# 2. Update capNhatTienTrinhForm submitBtn handling:
old_step7 = """    // Bước 7: Tài liệu bắt buộc
    var duHoSo = coDichVu && uploaded.AnhChanDung && uploaded.CCCD_Mat_Truoc && uploaded.CCCD_Mat_Sau;
    if (submitBtn) {
      if (duHoSo && uploading === 0) {
        submitBtn.removeAttribute('disabled');
        submitBtn.classList.remove('is-locked');
        submitBtn.style.opacity = '1';
        submitBtn.style.cursor = 'pointer';
      } else {
        submitBtn.setAttribute('disabled', 'disabled');
        submitBtn.classList.add('is-locked');
        submitBtn.style.opacity = '0.5';
        submitBtn.style.cursor = 'not-allowed';
      }
    }"""

new_step7 = """    // Bước 7: Nút gửi hồ sơ luôn sẵn sàng để nhận click và thông báo lỗi rõ ràng nếu còn thiếu thông tin
    if (submitBtn) {
      submitBtn.removeAttribute('disabled');
      submitBtn.classList.remove('is-locked');
      submitBtn.style.opacity = '1';
      submitBtn.style.cursor = 'pointer';
    }"""

if old_step7 in code:
    code = code.replace(old_step7, new_step7, 1)
    print('Updated capNhatTienTrinhForm to keep submitBtn clickable.')
else:
    # Try with LF/CRLF normalization
    old_step7_norm = old_step7.replace('\r\n', '\n')
    code_norm = code.replace('\r\n', '\n')
    if old_step7_norm in code_norm:
        code = code_norm.replace(old_step7_norm, new_step7.replace('\r\n', '\n'), 1)
        print('Updated capNhatTienTrinhForm (normalized).')
    else:
        print('Warning: old_step7 not found')

# 3. Update datNut
old_datNut = """  function datNut(dangGui) {
    submitBtn.disabled = dangGui;
    submitBtn.textContent = dangGui ? 'Đang gửi hồ sơ…' : (giayToCho ? 'Gửi lại giấy tờ' : 'Gửi hồ sơ');
  }"""

new_datNut = """  function datNut(dangGui) {
    if (!submitBtn) return;
    submitBtn.disabled = !!dangGui;
    submitBtn.textContent = dangGui ? 'Đang gửi hồ sơ…' : 'Gửi hồ sơ';
  }"""

if old_datNut in code:
    code = code.replace(old_datNut, new_datNut, 1)
    print('Updated datNut.')
else:
    old_datNut_norm = old_datNut.replace('\r\n', '\n')
    code_norm = code.replace('\r\n', '\n')
    if old_datNut_norm in code_norm:
        code = code_norm.replace(old_datNut_norm, new_datNut.replace('\r\n', '\n'), 1)
        print('Updated datNut (normalized).')

# 4. Remove guiGiayToCho
old_cho = """  // Hồ sơ đã lưu: tải giấy tờ; lỗi thì giữ file + mã để bấm gửi lại
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
  }"""

code = code.replace(old_cho, '').replace(old_cho.replace('\r\n', '\n'), '')

# 5. Replace submit handler with comprehensive step-by-step validation and clear error reporting
idx_submit = code.find("form.addEventListener('submit', function (e) {")
if idx_submit == -1:
    print('ERROR: submit listener not found')
else:
    # Find the end of submit listener (before section 5: Tra cuu ket qua)
    idx_tracuu = code.find("// ==========================================================\n  // 5. TRA CỨU KẾT QUẢ XÉT DUYỆT")
    if idx_tracuu == -1:
        idx_tracuu = code.find("// ==========================================================\r\n  // 5. TRA CỨU KẾT QUẢ XÉT DUYỆT")
    if idx_tracuu == -1:
        idx_tracuu = code.find("var traCuuForm = document.getElementById('traCuuForm');")

    new_submit_handler = """  // ---- Gửi hồ sơ ----
  form.addEventListener('submit', function (e) {
    e.preventDefault();
    hide(errorBox);
    hide(okBox);

    // 1. Họ và tên
    var hoTenVal = hoTenInput ? hoTenInput.value.trim() : '';
    if (hoTenVal.length < 2) {
      show(errorBox, 'Vui lòng nhập Họ và tên (tối thiểu 2 ký tự).');
      if (hoTenInput) { hoTenInput.focus(); }
      return;
    }

    // 2. Số điện thoại
    var sdtVal = sdtInput ? sdtInput.value.trim() : '';
    setFieldLock(document.querySelector('[data-step-field="soDienThoai"]'), false);
    if (!/^0[0-9]{9}$/.test(sdtVal)) {
      show(errorBox, 'Vui lòng nhập số điện thoại hợp lệ (10 chữ số, bắt đầu bằng số 0).');
      if (sdtInput) { sdtInput.focus(); }
      return;
    }

    // 3. Mật khẩu
    setFieldLock(document.querySelector('[data-step-field="matKhau"]'), false);
    var mkVal = matKhauInput ? matKhauInput.value : '';
    if (!kiemTraMatKhauHopLe(mkVal, sdtVal)) {
      var errs = window.QuyTacMatKhau ? QuyTacMatKhau.kiemTra(mkVal, sdtVal) : [];
      show(errorBox, errs.length > 0 ? errs[0] : 'Mật khẩu cần tối thiểu 8 ký tự, có cả chữ và số, không chứa khoảng trắng hay số điện thoại.');
      if (matKhauInput) { matKhauInput.focus(); }
      return;
    }

    // 4. Xác nhận mật khẩu
    setFieldLock(document.querySelector('[data-step-field="xacNhanMatKhau"]'), false);
    var xnVal = xacNhanInput ? xacNhanInput.value : '';
    if (xnVal !== mkVal) {
      show(errorBox, 'Mật khẩu xác nhận không khớp với mật khẩu đã nhập.');
      if (xacNhanInput) { xacNhanInput.focus(); }
      return;
    }

    // 5. Ngày sinh (đủ 18 tuổi)
    setFieldLock(document.querySelector('[data-step-field="ngaySinh"]'), false);
    setFieldLock(document.querySelector('[data-step-field="gioiTinh"]'), false);
    var nsVal = ngaySinhInput ? ngaySinhInput.value : '';
    if (!nsVal) {
      show(errorBox, 'Vui lòng chọn ngày tháng năm sinh.');
      if (ngaySinhInput) { ngaySinhInput.focus(); }
      return;
    }
    if (!du18Tuoi(nsVal)) {
      show(errorBox, 'Cộng tác viên phải từ đủ 18 tuổi trở lên. Vui lòng kiểm tra lại ngày sinh.');
      if (ngaySinhInput) { ngaySinhInput.focus(); }
      return;
    }

    // 6. Địa chỉ
    setFieldLock(document.querySelector('[data-step-field="diaChiHeader"]'), false);
    setFieldLock(document.querySelector('[data-step-field="tinhThanh"]'), false);
    setFieldLock(document.querySelector('[data-step-field="phuongXa"]'), false);
    setFieldLock(document.querySelector('[data-step-field="diaChiChiTiet"]'), false);

    if (!tinhSelect || !tinhSelect.value) {
      show(errorBox, 'Vui lòng chọn Tỉnh, thành phố nơi bạn đang sống.');
      if (tinhSelect) { tinhSelect.focus(); }
      return;
    }
    if (!phuongSelect || !phuongSelect.value) {
      show(errorBox, 'Vui lòng chọn Phường, xã hoặc Quận, huyện nơi bạn đang sống.');
      if (phuongSelect) { phuongSelect.focus(); }
      return;
    }
    var dcVal = diaChiChiTietInput ? diaChiChiTietInput.value.trim() : '';
    if (dcVal.length < 3) {
      show(errorBox, 'Vui lòng nhập số nhà, tên đường (tối thiểu 3 ký tự).');
      if (diaChiChiTietInput) { diaChiChiTietInput.focus(); }
      return;
    }

    // 7. Dịch vụ
    setFieldsetLock(fsDichVu, false);
    var dichVuIds = [];
    form.querySelectorAll('input[name="dichVuItem"]:checked').forEach(function (cb) {
      var id = parseInt(cb.value, 10);
      if (id && dichVuIds.indexOf(id) === -1) dichVuIds.push(id);
    });
    form.querySelectorAll('input.check-rep-dv:checked').forEach(function (rep) {
      var subIdsStr = rep.getAttribute('data-sub-ids');
      if (subIdsStr) {
        subIdsStr.split(',').forEach(function (sid) {
          var id = parseInt(sid.trim(), 10);
          if (id && dichVuIds.indexOf(id) === -1) dichVuIds.push(id);
        });
      }
    });

    if (dichVuIds.length === 0) {
      show(errorBox, 'Vui lòng chọn ít nhất một dịch vụ bạn có thể làm.');
      if (fsDichVu) { fsDichVu.scrollIntoView({ block: 'center', behavior: 'smooth' }); }
      return;
    }

    // 8. Đang tải file lên MinIO
    if (uploading > 0) {
      show(errorBox, 'Hình ảnh đang được tải lên máy chủ. Vui lòng chờ vài giây rồi bấm Gửi hồ sơ.');
      return;
    }

    // 9. Tài liệu bắt buộc
    if (!uploaded.AnhChanDung) {
      show(errorBox, 'Vui lòng tải lên Ảnh chân dung.');
      var z = form.querySelector('[data-upload="AnhChanDung"]');
      if (z) { z.scrollIntoView({ block: 'center', behavior: 'smooth' }); }
      return;
    }
    if (!uploaded.CCCD_Mat_Truoc) {
      show(errorBox, 'Vui lòng tải lên ảnh Căn cước công dân (mặt trước).');
      var z = form.querySelector('[data-upload="CCCD_Mat_Truoc"]');
      if (z) { z.scrollIntoView({ block: 'center', behavior: 'smooth' }); }
      return;
    }
    if (!uploaded.CCCD_Mat_Sau) {
      show(errorBox, 'Vui lòng tải lên ảnh Căn cước công dân (mặt sau).');
      var z = form.querySelector('[data-upload="CCCD_Mat_Sau"]');
      if (z) { z.scrollIntoView({ block: 'center', behavior: 'smooth' }); }
      return;
    }

    datNut(true);

    var hoSo = ['AnhChanDung', 'CCCD_Mat_Truoc', 'CCCD_Mat_Sau'].map(function (loai) {
      return { loaiTaiLieu: loai, duongDanFile: uploaded[loai] };
    });

    // Danh sách chứng chỉ kèm ghi chú kỹ năng (nếu có)
    var danhSachChungChi = [];
    uploadedChungChi.forEach(function (cc) {
      danhSachChungChi.push({
        loaiChungChi: 'ChungChi',
        tenChungChi: cc.ten,
        noiCap: 'Tải lên từ biểu mẫu',
        duongDanFile: cc.objectName
      });
    });

    var tenChungChiNhap = document.getElementById('tenChungChi');
    if (tenChungChiNhap && tenChungChiNhap.value.trim()) {
      danhSachChungChi.push({
        loaiChungChi: 'ChungChi',
        tenChungChi: tenChungChiNhap.value.trim(),
        noiCap: 'Tự khai',
        duongDanFile: uploadedChungChi.length > 0 ? uploadedChungChi[0].objectName : ''
      });
    }

    var ghiChuInput = document.getElementById('ghiChuDichVu');
    if (ghiChuInput && ghiChuInput.value.trim()) {
      danhSachChungChi.push({
        loaiChungChi: 'ChungChi',
        tenChungChi: 'Kỹ năng / ghi chú: ' + ghiChuInput.value.trim(),
        noiCap: 'Tự khai',
        duongDanFile: ''
      });
    }

    var diaChi = window.DiaChi ? DiaChi.giaTri(tinhSelect.closest('[data-dia-chi-form]')) : {};
    var tinhText = (tinhSelect && tinhSelect.options && tinhSelect.selectedIndex >= 0) ? tinhSelect.options[tinhSelect.selectedIndex].text : '';
    var phuongText = (phuongSelect && phuongSelect.options && phuongSelect.selectedIndex >= 0) ? phuongSelect.options[phuongSelect.selectedIndex].text : '';
    var fullDiaChi = dcVal + (phuongText ? ', ' + phuongText : '') + (tinhText ? ', ' + tinhText : '');

    var payload = {
      hoTen: hoTenVal,
      soDienThoai: sdtVal,
      email: emailInput && emailInput.value.trim() ? emailInput.value.trim() : null,
      matKhau: mkVal,
      ngaySinh: nsVal,
      gioiTinh: gioiTinhSelect ? gioiTinhSelect.value : 'Khac',
      soNhaTenDuong: dcVal,
      tinhThanh: tinhText,
      phuongXa: phuongText,
      noiCuTru: fullDiaChi,
      maTinh: (diaChi && diaChi.maTinh != null) ? diaChi.maTinh : (tinhSelect && tinhSelect.value ? parseInt(String(tinhSelect.value).replace(/^c/, ''), 10) : null),
      maQuan: diaChi ? diaChi.maQuan : null,
      maPhuong: diaChi ? diaChi.maPhuong : null,
      danhSachDichVuId: dichVuIds,
      danhSachHoSo: hoSo,
      danhSachChungChi: danhSachChungChi
    };

    fetch('/v1/collaborators/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
      .then(function (res) {
        return res.json().catch(function () { return {}; }).then(function (body) { return { ok: res.ok, status: res.status, body: body }; });
      })
      .then(function (r) {
        if (!r.ok || !r.body.success) {
          throw new Error(r.body.message || r.body.error || 'Chưa gửi được hồ sơ (mã ' + r.status + '). Vui lòng kiểm tra lại thông tin.');
        }
        var d = r.body.data || {};
        form.reset();
        uploaded = {};
        uploadedChungChi = [];
        renderChungChiGrid();
        document.querySelectorAll('[data-upload]').forEach(function (zone) {
          zone.classList.remove('is-busy', 'is-done', 'is-failed');
          var img = zone.querySelector('img');
          if (img) img.remove();
          var state = zone.querySelector('.state');
          if (state) state.textContent = '';
        });
        document.querySelectorAll('.upload-actions').forEach(function (act) { act.style.display = 'none'; });

        show(okBox, 'Đã gửi hồ sơ thành công' + (d.maCongTacVien ? ' (Mã: ' + d.maCongTacVien + ')' : '') +
          '. Thời gian xét duyệt dự kiến: ' + (d.thoiGianXetDuyetDuKien || '1-3 ngày làm việc') + '.');

        capNhatTienTrinhForm();
      })
      .catch(function (err) {
        show(errorBox, err.message || 'Lỗi kết nối máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.');
      })
      .then(function () {
        datNut(false);
        capNhatTienTrinhForm();
      });
  });

  """
    code = code[:idx_submit] + new_submit_handler + code[idx_tracuu:]
    print('Replaced submit listener successfully.')

with open(js_path, 'w', encoding='utf-8') as f:
    f.write(code)
print('Updated dang-ky-ctv.js successfully.')

