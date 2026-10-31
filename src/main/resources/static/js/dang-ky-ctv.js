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

  // Lưu trữ tài liệu bắt buộc đã tải lên
  var uploaded = {};
  // Danh sách chứng chỉ tải lên (nhiều file): [{ ten, objectName }]
  var uploadedChungChi = [];
  var uploading = 0;

  function show(box, message) {
    if (!box) return;
    box.textContent = message;
    box.classList.add('is-shown');
    box.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }
  function hide(box) { if (box) box.classList.remove('is-shown'); }

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
  // ==========================================================
  // 1. FORM TUẦN TỰ: ĐIỀN TỪ TRÊN XUỐNG
  // ==========================================================
  var hoTenInput = document.getElementById('hoTen');
  var sdtInput = document.getElementById('soDienThoai');
  var emailInput = document.getElementById('email');
  var matKhauInput = document.getElementById('matKhau');
  var xacNhanInput = document.getElementById('xacNhanMatKhau');
  var ngaySinhInput = document.getElementById('ngaySinh');
  var gioiTinhSelect = document.getElementById('gioiTinh');
  var tinhSelect = document.getElementById('tinhThanh');
  var phuongSelect = document.getElementById('phuongXa');
  var diaChiChiTietInput = document.getElementById('diaChiChiTiet');
  var fsDichVu = document.getElementById('fsDichVu');
  var fsTaiLieu = document.getElementById('fsTaiLieu');

  function setFieldLock(fieldEl, isLocked) {
    if (!fieldEl) return;
    fieldEl.classList.toggle('is-locked', isLocked);
    var controls = fieldEl.querySelectorAll('input, select, textarea, button:not([tabindex="-1"])');
    controls.forEach(function (ctrl) {
      if (isLocked) {
        ctrl.setAttribute('disabled', 'disabled');
      } else {
        ctrl.removeAttribute('disabled');
      }
    });
  }

  function setFieldsetLock(fsEl, isLocked) {
    if (!fsEl) return;
    fsEl.classList.toggle('is-locked', isLocked);
    var controls = fsEl.querySelectorAll('input, select, textarea, button');
    controls.forEach(function (ctrl) {
      if (isLocked) {
        ctrl.setAttribute('disabled', 'disabled');
      } else {
        ctrl.removeAttribute('disabled');
      }
    });
  }

  function du18Tuoi(value) {
    if (!value) return false;
    var sinh = new Date(value);
    var moc = new Date();
    moc.setFullYear(moc.getFullYear() - 18);
    return sinh <= moc;
  }

  // Khởi tạo ngày sinh max = đủ 18 tuổi
  if (ngaySinhInput) {
    var d = new Date();
    d.setFullYear(d.getFullYear() - 18);
    ngaySinhInput.max = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    if (window.Lich) Lich.dongBo(ngaySinhInput);
  }

  function kiemTraMatKhauHopLe(mk, sdt) {
    if (!mk) return false;
    if (window.QuyTacMatKhau) {
      return QuyTacMatKhau.kiemTra(mk, sdt).length === 0;
    }
    return mk.length >= 8 && /[A-Za-z]/.test(mk) && /[0-9]/.test(mk);
  }

  function capNhatTienTrinhForm() {
    // Bước 1: Họ và tên
    var hoTenOk = hoTenInput && hoTenInput.value.trim().length >= 2;
    var sdtField = document.querySelector('[data-step-field="soDienThoai"]');
    var emailField = document.querySelector('[data-step-field="email"]');
    setFieldLock(sdtField, !hoTenOk);
    setFieldLock(emailField, !hoTenOk);

    // Bước 2: Số điện thoại
    var sdtVal = sdtInput ? sdtInput.value.trim() : '';
    var sdtOk = hoTenOk && /^0[0-9]{9}$/.test(sdtVal);
    var mkField = document.querySelector('[data-step-field="matKhau"]');
    var xnField = document.querySelector('[data-step-field="xacNhanMatKhau"]');
    setFieldLock(mkField, !sdtOk);
    setFieldLock(xnField, !sdtOk);

    // Bước 3: Mật khẩu
    var mkVal = matKhauInput ? matKhauInput.value : '';
    var xnVal = xacNhanInput ? xacNhanInput.value : '';
    var mkOk = sdtOk && kiemTraMatKhauHopLe(mkVal, sdtVal);
    var xnOk = mkOk && xnVal === mkVal && xnVal.length > 0;
    var nsField = document.querySelector('[data-step-field="ngaySinh"]');
    var gtField = document.querySelector('[data-step-field="gioiTinh"]');
    setFieldLock(nsField, !xnOk);
    setFieldLock(gtField, !xnOk);

    // Bước 4: Ngày sinh
    var nsVal = ngaySinhInput ? ngaySinhInput.value : '';
    var nsOk = xnOk && du18Tuoi(nsVal);
    var dcHeader = document.querySelector('[data-step-field="diaChiHeader"]');
    var ttField = document.querySelector('[data-step-field="tinhThanh"]');
    var pxField = document.querySelector('[data-step-field="phuongXa"]');
    var dcctField = document.querySelector('[data-step-field="diaChiChiTiet"]');
    setFieldLock(dcHeader, !nsOk);
    setFieldLock(ttField, !nsOk);
    setFieldLock(pxField, !nsOk);
    setFieldLock(dcctField, !nsOk);

    // Bước 5: Địa chỉ
    var diaChiOk = nsOk &&
      tinhSelect && tinhSelect.value &&
      phuongSelect && phuongSelect.value &&
      diaChiChiTietInput && diaChiChiTietInput.value.trim().length >= 3;
    setFieldsetLock(fsDichVu, !diaChiOk);

    // Bước 6: Dịch vụ
    var coDichVu = diaChiOk && (
      form.querySelectorAll('input[name="dichVuItem"]:checked').length > 0 ||
      form.querySelectorAll('input.check-rep-dv:checked').length > 0 ||
      form.querySelectorAll('input[name="loaiDichVu"]:checked').length > 0
    );
    // Không khóa cứng phần tài liệu để ứng viên có thể tải ảnh chân dung/CCCD bất kỳ lúc nào
    // setFieldsetLock(fsTaiLieu, !coDichVu);

    // Bước 7: Tài liệu bắt buộc
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
    }
  }

  // Lắng nghe thay đổi trên toàn form để cập nhật trạng thái mở khóa tuần tự
  form.addEventListener('input', capNhatTienTrinhForm);
  form.addEventListener('change', capNhatTienTrinhForm);

  // ==========================================================
  // 2. KHỞI TẠO MENU DỊCH VỤ ĐA CẤP (ACCORDION & TÊN ĐẠI DIỆN)
  // ==========================================================
  function rutGonTenDichVu(ten) {
    if (!ten) return '';
    return ten.replace(/\s*\(.*?\)/g, '')
              .replace(/\s+(tối đa|dưới|trên|từ|nhỏ hơn|lớn hơn)\s.*$/gi, '')
              .replace(/\s+\d.*$/g, '')
              .trim();
  }

  var dmAccordionCards = document.querySelectorAll('.dm-accordion-card');
  dmAccordionCards.forEach(function (card, cardIdx) {
    var header = card.querySelector('.dm-accordion-header');
    var body = card.querySelector('.dm-accordion-body');
    var chevron = card.querySelector('.dm-accordion-chevron');
    var countBadge = card.querySelector('.dm-count-badge');
    var catId = header.getAttribute('data-cat-id');
    var rawSpans = card.querySelectorAll('.raw-dv-items span');
    var repContainer = card.querySelector('.dm-rep-list');

    // Gom nhóm dịch vụ con theo tên đại diện
    var repGroups = {};
    rawSpans.forEach(function (span) {
      var id = parseInt(span.getAttribute('data-id'), 10);
      var ten = span.getAttribute('data-ten') || '';
      var loai = span.getAttribute('data-loai') || '';
      var repName = rutGonTenDichVu(ten);
      if (!repGroups[repName]) {
        repGroups[repName] = { name: repName, items: [] };
      }
      repGroups[repName].items.push({ id: id, ten: ten, loai: loai });
    });

    var groupKeys = Object.keys(repGroups);
    if (countBadge) {
      countBadge.textContent = groupKeys.length + ' dịch vụ';
    }

    // Render danh sách dịch vụ đại diện (Cấp 2)
    if (repContainer) {
      repContainer.innerHTML = '';
      groupKeys.forEach(function (key) {
        var grp = repGroups[key];
        var allIds = grp.items.map(function (it) { return it.id; }).join(',');

        var repCard = document.createElement('div');
        repCard.className = 'rep-dv-card';
        repCard.style.cssText = 'border:1px solid #e2e8f0;border-radius:10px;padding:0.75rem;background:#f8fafc;display:flex;flex-direction:column;justify-content:flex-start;align-items:stretch;gap:6px;transition:all 0.2s';

        var topHtml = '<label class="check" style="font-weight:600;font-size:0.92rem;color:#1e293b;cursor:pointer;display:flex;align-items:flex-start;gap:8px;margin:0">' +
          '<input type="checkbox" class="check-rep-dv" data-nhom="' + catId + '" data-sub-ids="' + allIds + '" disabled style="margin-top:3px;cursor:pointer">' +
          '<span>' + grp.name + '</span>' +
          '</label>';

        var subToggleHtml = '';
        var subListHtml = '';
        if (grp.items.length > 1) {
          subToggleHtml = '<div class="sub-toggle-btn" style="margin-top:6px;font-size:0.78rem;color:var(--color-brand,#0d9488);cursor:pointer;display:inline-flex;align-items:center;gap:4px;user-select:none">' +
            '<span>Xem các gói cụ thể (' + grp.items.length + ')</span> <span class="sub-icon">▼</span>' +
            '</div>';
          var subItemsHtml = grp.items.map(function (it) {
            return '<label class="check sub-item-check" style="font-size:0.8rem;color:#475569;margin-bottom:4px;cursor:pointer;display:flex;align-items:center;gap:6px">' +
              '<input type="checkbox" class="check-dichvu" name="dichVuItem" value="' + it.id + '" data-nhom="' + catId + '" data-rep="' + grp.name + '" disabled style="cursor:pointer">' +
              '<span>' + it.ten + '</span>' +
              '</label>';
          }).join('');
          subListHtml = '<div class="sub-variants-list" style="display:none;margin-top:6px;padding:8px;background:#ffffff;border:1px solid #e2e8f0;border-radius:8px">' + subItemsHtml + '</div>';
        } else if (grp.items.length === 1) {
          subListHtml = '<input type="checkbox" class="check-dichvu" name="dichVuItem" value="' + grp.items[0].id + '" data-nhom="' + catId + '" data-rep="' + grp.name + '" style="display:none" disabled>';
        }

        repCard.innerHTML = topHtml + subToggleHtml + subListHtml;
        repContainer.appendChild(repCard);
      });
    }

    // Sự kiện mở/đóng Accordion Header
    header.addEventListener('click', function (e) {
      if (e.target.closest('input[type="checkbox"]')) return;
      var isHidden = body.style.display === 'none';
      body.style.display = isHidden ? 'block' : 'none';
      if (chevron) chevron.style.transform = isHidden ? 'rotate(180deg)' : 'none';
    });

    // Mở sẵn danh mục đầu tiên
    if (cardIdx === 0 && body) {
      body.style.display = 'block';
      if (chevron) chevron.style.transform = 'rotate(180deg)';
    }
  });

  // Toggle chi tiết biến thể con trong từng card đại diện
  document.addEventListener('click', function (e) {
    var toggleBtn = e.target.closest('.sub-toggle-btn');
    if (!toggleBtn) return;
    var card = toggleBtn.closest('.rep-dv-card');
    var subList = card ? card.querySelector('.sub-variants-list') : null;
    var subIcon = toggleBtn.querySelector('.sub-icon');
    if (subList) {
      var isHidden = subList.style.display === 'none';
      subList.style.display = isHidden ? 'block' : 'none';
      if (subIcon) subIcon.textContent = isHidden ? '▲' : '▼';
    }
  });

  // Tương tác checkbox đại diện (Cấp 2) -> chọn tất cả gói con bên trong
  document.addEventListener('change', function (e) {
    var repCheck = e.target.closest('.check-rep-dv');
    if (repCheck) {
      var card = repCheck.closest('.rep-dv-card');
      if (card) {
        var subChecks = card.querySelectorAll('.check-dichvu');
        subChecks.forEach(function (sc) { sc.checked = repCheck.checked; });
      }
      // Đồng bộ checkbox nhóm danh mục cha (Cấp 1)
      var catId = repCheck.getAttribute('data-nhom');
      var catCheck = form.querySelector('.check-nhom[value="' + catId + '"]');
      if (catCheck) {
        var allRep = form.querySelectorAll('.check-rep-dv[data-nhom="' + catId + '"]');
        var anyChecked = Array.prototype.some.call(allRep, function (c) { return c.checked; });
        catCheck.checked = anyChecked;
      }
      capNhatTienTrinhForm();
      return;
    }

    // Tương tác checkbox biến thể con (Cấp 3) -> đồng bộ ngược lại checkbox đại diện
    var dvCheck = e.target.closest('.check-dichvu');
    if (dvCheck) {
      var card = dvCheck.closest('.rep-dv-card');
      if (card) {
        var rep = card.querySelector('.check-rep-dv');
        var allSub = card.querySelectorAll('.check-dichvu');
        var anySubChecked = Array.prototype.some.call(allSub, function (c) { return c.checked; });
        if (rep) rep.checked = anySubChecked;
      }
      var catId = dvCheck.getAttribute('data-nhom');
      var catCheck = form.querySelector('.check-nhom[value="' + catId + '"]');
      if (catCheck) {
        var allCatSub = form.querySelectorAll('.check-dichvu[data-nhom="' + catId + '"]');
        var anyCatChecked = Array.prototype.some.call(allCatSub, function (c) { return c.checked; });
        catCheck.checked = anyCatChecked;
      }
      capNhatTienTrinhForm();
      return;
    }

    // Tương tác checkbox danh mục cha (Cấp 1) -> chọn tất cả dịch vụ đại diện và gói con
    var catCheck = e.target.closest('.check-nhom');
    if (catCheck) {
      var catId = catCheck.value;
      var allRep = form.querySelectorAll('.check-rep-dv[data-nhom="' + catId + '"]');
      allRep.forEach(function (r) { r.checked = catCheck.checked; });
      var allSub = form.querySelectorAll('.check-dichvu[data-nhom="' + catId + '"]');
      allSub.forEach(function (s) { s.checked = catCheck.checked; });
      capNhatTienTrinhForm();
    }
  });

  // ==========================================================
  // 3. LIGHTBOX XEM ẢNH PHÓNG TO & UPLOAD TÀI LIỆU
  // ==========================================================
  function openCtvLightbox(src, title) {
    var modal = document.getElementById('ctvLightbox');
    var img = document.getElementById('ctvLightboxImg');
    var titleEl = document.getElementById('ctvLightboxTitle');
    if (!modal || !img) {
      console.warn('Không tìm thấy modal ctvLightbox');
      return;
    }
    img.src = src;
    if (titleEl) titleEl.textContent = title || 'Chi tiết hình ảnh';
    modal.style.display = 'flex';
  }

  window.closeCtvLightbox = function () {
    var modal = document.getElementById('ctvLightbox');
    if (modal) modal.style.display = 'none';
  };

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') window.closeCtvLightbox();
  });

  // Render danh sách bằng cấp/chứng chỉ trong 1 ô lưới duy nhất
  var chungChiSharedBox = document.getElementById('chungChiSharedBox');
  var chungChiEmptyNote = document.getElementById('chungChiEmptyNote');
  var chungChiGrid = document.getElementById('chungChiGrid');
  var ccCountBadge = document.getElementById('ccCountBadge');
  var btnThemChungChi = document.getElementById('btnThemChungChi');
  var inputChungChiMulti = document.getElementById('inputChungChiMulti');

  function renderChungChiGrid() {
    if (!chungChiGrid) return;
    if (uploadedChungChi.length === 0) {
      if (chungChiEmptyNote) chungChiEmptyNote.style.display = 'block';
      chungChiGrid.style.display = 'none';
      if (ccCountBadge) ccCountBadge.textContent = '(Chưa có chứng chỉ)';
      return;
    }

    if (chungChiEmptyNote) chungChiEmptyNote.style.display = 'none';
    chungChiGrid.style.display = 'grid';
    if (ccCountBadge) ccCountBadge.textContent = '(Đã nộp ' + uploadedChungChi.length + ' chứng chỉ)';

    chungChiGrid.innerHTML = '';
    uploadedChungChi.forEach(function (cc, idx) {
      var card = document.createElement('div');
      card.className = 'chung-chi-thumb-card';

      var isPdf = (cc.ten && cc.ten.toLowerCase().endsWith('.pdf')) || (cc.fileType === 'application/pdf');
      var imgWrap = document.createElement('div');
      imgWrap.className = 'chung-chi-thumb-img-wrap';

      if (isPdf) {
        imgWrap.innerHTML = '<div style="width:100%;height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#ef4444;font-size:32px">📄<span style="font-size:11px;font-weight:600;color:#64748b">PDF</span></div>' +
          '<div class="chung-chi-thumb-overlay">Tài liệu PDF</div>';
      } else {
        var imgSrc = cc.previewUrl || ('/api/storage/file/' + cc.objectName);
        imgWrap.innerHTML = '<img src="' + imgSrc + '" alt="' + cc.ten + '">' +
          '<div class="chung-chi-thumb-overlay">🔍 Xem to</div>';
        imgWrap.addEventListener('click', function () {
          openCtvLightbox(imgSrc, cc.ten);
        });
      }

      var delBtn = document.createElement('button');
      delBtn.type = 'button';
      delBtn.className = 'btn-del-thumb';
      delBtn.innerHTML = '&times;';
      delBtn.title = 'Xóa chứng chỉ này';
      delBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        uploadedChungChi.splice(idx, 1);
        renderChungChiGrid();
        capNhatTienTrinhForm();
      });

      var nameLabel = document.createElement('div');
      nameLabel.className = 'chung-chi-thumb-name';
      nameLabel.textContent = cc.ten;
      nameLabel.title = cc.ten;

      card.appendChild(delBtn);
      card.appendChild(imgWrap);
      card.appendChild(nameLabel);
      chungChiGrid.appendChild(card);
    });
  }
  // Alias tương thích ngược để không bị lỗi bất kỳ đâu
  var renderChungChiList = renderChungChiGrid;

  // Nút Thêm chứng chỉ nằm ngoài ô
  if (btnThemChungChi && inputChungChiMulti) {
    btnThemChungChi.addEventListener('click', function (e) {
      e.preventDefault();
      inputChungChiMulti.click();
    });
  }

  // Bấm vào vùng ô chứa chứng chỉ cũng kích hoạt chọn file (nếu không bấm vào nút xóa hay ảnh)
  if (chungChiSharedBox && inputChungChiMulti) {
    chungChiSharedBox.addEventListener('click', function (e) {
      if (e.target.closest('.btn-del-thumb') || e.target.closest('.chung-chi-thumb-img-wrap') || e.target.closest('img')) return;
      inputChungChiMulti.click();
    });
  }

  if (inputChungChiMulti) {
    inputChungChiMulti.addEventListener('change', function () {
      var files = inputChungChiMulti.files;
      if (!files || files.length === 0) return;

      Array.prototype.forEach.call(files, function (file) {
        if (file.size > MAX_BYTES) {
          alert('File "' + file.name + '" lớn hơn 20 MB, không thể tải.');
          return;
        }
        uploading++;
        capNhatTienTrinhForm();

        var localUrl = (file.type.indexOf('image/') === 0) ? URL.createObjectURL(file) : '';
        var data = new FormData();
        data.append('file', file);

        fetch('/api/storage/upload/ctv-chung-chi', { method: 'POST', body: data })
          .then(function (res) { return res.json().catch(function () { return {}; }).then(function (b) { return { ok: res.ok, body: b }; }); })
          .then(function (r) {
            if (!r.ok || !r.body.success) {
              throw new Error(r.body.error || 'Lỗi tải ' + file.name);
            }
            uploadedChungChi.push({
              ten: file.name,
              objectName: r.body.objectName,
              previewUrl: localUrl,
              fileType: file.type
            });
            renderChungChiGrid();
          })
          .catch(function (err) {
            alert('Lỗi tải file ' + file.name + ': ' + (err.message || 'Thất bại'));
          })
          .then(function () {
            uploading--;
            capNhatTienTrinhForm();
          });
      });
      inputChungChiMulti.value = '';
    });
  }

  // Tải các tài liệu bắt buộc (AnhChanDung, CCCD) & Click xem phóng to
  document.querySelectorAll('[data-upload]').forEach(function (zone) {
    var input = zone.querySelector('input[type="file"]');
    var state = zone.querySelector('.state');
    var type = zone.getAttribute('data-upload');
    var wrap = zone.closest('.upload-wrap');
    var actionBox = wrap ? wrap.querySelector('.upload-actions') : null;

    function setState(cls, text) {
      zone.classList.remove('is-busy', 'is-done', 'is-failed');
      if (cls) zone.classList.add(cls);
      if (state) state.textContent = text || '';
    }

    // Bấm vào ảnh đã upload để xem to (Lightbox) hoặc bấm vào ô trống để tải ảnh
    zone.addEventListener('click', function (e) {
      if (e.target.closest('.btn-change-file')) return;

      if (zone.classList.contains('is-done')) {
        var img = zone.querySelector('img');
        if (img) {
          e.preventDefault();
          e.stopPropagation();
          var label = zone.querySelector('b') ? zone.querySelector('b').textContent.replace('*', '').trim() : 'Ảnh đính kèm';
          openCtvLightbox(img.src, label);
        }
        return;
      }

      // Chưa tải ảnh: kích hoạt input chọn file nếu người dùng nhấp vào chữ/biểu tượng
      if (e.target !== input) {
        e.preventDefault();
        input.click();
      }
    });

    var viewHint = zone.querySelector('.upload-view-hint');
    if (viewHint) {
      viewHint.addEventListener('click', function (e) {
        if (!zone.classList.contains('is-done')) return;
        e.preventDefault();
        e.stopPropagation();
        var currImg = zone.querySelector('img');
        if (currImg) {
          var label = zone.querySelector('b') ? zone.querySelector('b').textContent.replace('*', '').trim() : 'Ảnh đính kèm';
          openCtvLightbox(currImg.src, label);
        }
      });
    }

    // Nút "Đổi ảnh khác"
    if (actionBox) {
      var changeBtn = actionBox.querySelector('.btn-change-file');
      if (changeBtn) {
        changeBtn.addEventListener('click', function (e) {
          e.stopPropagation();
          delete uploaded[type];
          var old = zone.querySelector('img');
          if (old) old.remove();
          setState('', '');
          actionBox.style.display = 'none';
          input.value = '';
          capNhatTienTrinhForm();
          input.click();
        });
      }
    }

    input.addEventListener('change', function () {
      var files = input.files;
      if (!files || files.length === 0) return;

      var file = files[0];
      delete uploaded[type];
      if (file.size > MAX_BYTES) {
        setState('is-failed', 'File lớn hơn 20 MB. Bấm để chọn lại.');
        input.value = '';
        capNhatTienTrinhForm();
        return;
      }

      // Preview ảnh
      var old = zone.querySelector('img');
      if (old) old.remove();
      if (file.type.indexOf('image/') === 0) {
        var img = document.createElement('img');
        img.alt = '';
        img.src = URL.createObjectURL(file);
        zone.insertBefore(img, state);
      }

      uploading++;
      setState('is-busy', 'Đang tải lên…');
      if (actionBox) actionBox.style.display = 'none';
      capNhatTienTrinhForm();

      var data = new FormData();
      data.append('file', file);
      fetch(zone.getAttribute('data-endpoint'), { method: 'POST', body: data })
        .then(function (res) { return res.json().catch(function () { return {}; }).then(function (b) { return { ok: res.ok, body: b }; }); })
        .then(function (r) {
          if (!r.ok || !r.body.success) {
            throw new Error(r.body.error || 'Không tải được file. Bấm để thử lại.');
          }
          uploaded[type] = r.body.objectName;
          setState('is-done', 'Đã tải: ' + file.name);
          if (actionBox) actionBox.style.display = 'block';
        })
        .catch(function (err) {
          setState('is-failed', err.message || 'Tải lỗi. Bấm để thử lại');
          input.value = '';
        })
        .then(function () {
          uploading--;
          capNhatTienTrinhForm();
        });
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
    hide(errorBox);
    hide(okBox);

    if (uploading > 0) {
      show(errorBox, 'File đang được tải lên. Vui lòng chờ trong giây lát.');
      return;
    }

    // Thu thập danh sách dịch vụ CTV đã chọn
    var dichVuIds = [];
    form.querySelectorAll('input[name="dichVuItem"]:checked').forEach(function (cb) {
      var id = parseInt(cb.value, 10);
      if (id && dichVuIds.indexOf(id) === -1) dichVuIds.push(id);
    });
    // Thu thập từ các checkbox đại diện đã tích
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
      return;
    }

    if (!uploaded.AnhChanDung || !uploaded.CCCD_Mat_Truoc || !uploaded.CCCD_Mat_Sau) {
      show(errorBox, 'Vui lòng tải đủ Ảnh chân dung và 2 mặt Căn cước công dân.');
      return;
    }

    datNut(true);

    // Hồ sơ đã lưu ở lần trước, chỉ còn thiếu giấy tờ: không tạo hồ sơ mới
    if (giayToCho) {
      guiGiayToCho().then(function () { datNut(false); });
      return;
    }

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
    var payload = {
      hoTen: hoTenInput.value.trim(),
      soDienThoai: sdtInput.value.trim(),
      email: emailInput && emailInput.value.trim() ? emailInput.value.trim() : null,
      matKhau: matKhauInput ? matKhauInput.value : null,
      ngaySinh: ngaySinhInput.value,
      gioiTinh: gioiTinhSelect ? gioiTinhSelect.value : 'Khac',
      soNhaTenDuong: diaChiChiTietInput.value.trim(),
      // Ô tỉnh / ô thứ hai mang khoá "c74" / "q767": lấy mã số theo dòng đang chọn (quận cũ → maQuan)
      maTinh: diaChi.maTinh,
      maQuan: diaChi.maQuan,
      maPhuong: diaChi.maPhuong,
      danhSachDichVuId: dichVuIds,
      danhSachHoSo: hoSo,
      danhSachChungChi: danhSachChungChi
    };

    fetch('/v1/collaborators/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
      .then(function (res) { return res.json().catch(function () { return {}; }).then(function (body) { return { ok: res.ok, body: body }; }); })
      .then(function (r) {
        if (!r.ok || !r.body.success) {
          throw new Error(r.body.message || 'Chưa gửi được hồ sơ. Vui lòng kiểm tra lại thông tin.');
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
        show(errorBox, err.message);
      })
      .then(function () {
        submitBtn.textContent = 'Gửi hồ sơ';
        capNhatTienTrinhForm();
      });
  });

  // ==========================================================
  // 5. TRA CỨU KẾT QUẢ XÉT DUYỆT (BỎ CÁC CÂU DÀI, CHỈ GIỮ "HỒ SƠ ĐÃ ĐƯỢC DUYỆT.")
  // ==========================================================
  var traCuuForm = document.getElementById('traCuuForm');
  var ketQua = document.getElementById('traCuuKetQua');
  var TRANG_THAI = {
    ChoDuyet: 'Hồ sơ đang chờ xét duyệt.',
    HoatDong: 'Hồ sơ đã được duyệt.',
    TuChoi: 'Hồ sơ chưa đạt yêu cầu.',
    DinhChi: 'Tài khoản cộng tác viên đang bị đình chỉ.'
  };

  if (traCuuForm) {
    traCuuForm.addEventListener('submit', function (e) {
      e.preventDefault();
      var sdt = document.getElementById('traCuuSdt').value.trim();
      if (!sdt) {
        ketQua.textContent = 'Nhập số điện thoại đã đăng ký.';
        ketQua.style.color = 'var(--danger, #dc2626)';
        return;
      }
      ketQua.textContent = 'Đang tra cứu…';
      ketQua.style.color = 'var(--color-brand, #0d9488)';
      fetch('/v1/collaborators/application-status?soDienThoai=' + encodeURIComponent(sdt))
        .then(function (res) { return res.json().catch(function () { return {}; }).then(function (body) { return { ok: res.ok, body: body }; }); })
        .then(function (r) {
          if (!r.ok || !r.body.success) {
            ketQua.textContent = r.body.message || 'Không tìm thấy hồ sơ với số điện thoại này.';
            ketQua.style.color = 'var(--danger, #dc2626)';
            return;
          }
          var d = r.body.data || {};
          var statusText = TRANG_THAI[d.trangThai] || (d.trangThai === 'HoatDong' ? 'Hồ sơ đã được duyệt.' : d.message || 'Hồ sơ đã được duyệt.');
          ketQua.textContent = statusText;
          ketQua.style.color = d.trangThai === 'HoatDong' ? 'var(--success, #16a34a)' : '#1e293b';
        })
        .catch(function () {
          ketQua.textContent = 'Không kết nối được máy chủ. Thử lại sau.';
          ketQua.style.color = 'var(--danger, #dc2626)';
        });
    });
  }

  // Khởi động cập nhật tiến trình ban đầu
  capNhatTienTrinhForm();
})();
