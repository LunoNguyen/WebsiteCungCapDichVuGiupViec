/* ==========================================================
   LICH.JS – Lịch chọn ngày dùng chung cho toàn web (flatpickr)

   - Tự gắn lịch vào mọi <input type="date">: tiếng Việt, tuần bắt đầu thứ Hai,
     hiển thị dd/mm/yyyy, vẫn gửi lên máy chủ dạng yyyy-mm-dd như trước.
   - Đọc min/max của ô (DateRules trong main.js cập nhật lại khi đổi ngày bắt đầu…).
   - Ô ngày ẩn của HCNS (aria-hidden, mở bằng nút lịch): lịch bật cạnh nút bấm.
   - Bỏ qua ô có data-lich="off".
   Cần nạp trước: flatpickr.min.js và l10n/vn.js.
   ========================================================== */
(function () {
  'use strict';
  if (!window.flatpickr) return;
  if (flatpickr.l10ns && flatpickr.l10ns.vn) flatpickr.localize(flatpickr.l10ns.vn);
  // Tên tháng dạng số ("Tháng 5" thay cho "Tháng năm") cho dễ đọc
  var thang = [];
  for (var t = 1; t <= 12; t++) thang.push('Tháng ' + t);
  flatpickr.localize({ months: { shorthand: thang.map(function (_, i) { return 'Th' + (i + 1); }), longhand: thang }, firstDayOfWeek: 1 });

  function iso(d) {
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
  }

  function gan(input) {
    if (!input || input._flatpickr || input.dataset.lich === 'off') return null;
    var an = input.getAttribute('aria-hidden') === 'true';
    var ngaySinh = !!input.dataset.minAge;
    var opts = {
      dateFormat: 'Y-m-d',
      altInput: !an,
      altFormat: 'd/m/Y',
      altInputClass: (input.className || '') + ' lich-alt',
      allowInput: !an,
      disableMobile: true,
      monthSelectorType: 'dropdown',
      minDate: input.min || null,
      maxDate: input.max || null,
      defaultDate: input.value || null,
      // Ngày sinh: mở ở năm gần mốc tối đa (đủ tuổi) thay vì tháng hiện tại
      onOpen: function (sel, str, fp) {
        if (!sel.length && ngaySinh && fp.config.maxDate) fp.jumpToDate(fp.config.maxDate);
      }
    };
    if (an) {
      // Ô ẩn 1px: hiện lịch cạnh nút mở lịch đi kèm
      var nut = input.parentElement && input.parentElement.querySelector('button');
      if (nut) opts.positionElement = nut;
    }
    var fp = flatpickr(input, opts);
    if (fp.altInput) {
      fp.altInput.placeholder = input.placeholder || 'dd/mm/yyyy';
      if (input.getAttribute('aria-label')) fp.altInput.setAttribute('aria-label', input.getAttribute('aria-label'));
      if (input.title) fp.altInput.title = input.title;
      // Giữ id cho <label for> trỏ vào ô hiển thị
      if (input.id && document.querySelector('label[for="' + input.id + '"]')) {
        document.querySelectorAll('label[for="' + input.id + '"]').forEach(function (l) { l.htmlFor = input.id + '-hien'; });
        fp.altInput.id = input.id + '-hien';
      }
    }
    return fp;
  }

  /** Đồng bộ lịch với ô gốc sau khi code khác đổi value/min/max của ô. */
  function dongBo(input) {
    var fp = input && input._flatpickr;
    if (!fp) return;
    // Giữ giá trị cần đặt trước: đổi minDate/maxDate làm flatpickr ghi ngày cũ đang chọn đè lên ô gốc
    var muon = input.value || '';
    var min = input.min || null, max = input.max || null;
    if ((fp.config._minDate ? iso(fp.config._minDate) : null) !== min) fp.set('minDate', min);
    if ((fp.config._maxDate ? iso(fp.config._maxDate) : null) !== max) fp.set('maxDate', max);
    var dangChon = fp.selectedDates[0] ? iso(fp.selectedDates[0]) : '';
    if (dangChon !== muon) fp.setDate(muon || null, false);
    if ((input.value || '') !== muon) input.value = muon;
  }

  function ganTatCa(root) {
    (root || document).querySelectorAll('input[type="date"]').forEach(gan);
  }

  // form.reset() chỉ đặt lại ô gốc: cho lịch đi theo
  document.addEventListener('reset', function (e) {
    setTimeout(function () {
      e.target.querySelectorAll('input').forEach(function (i) { if (i._flatpickr) dongBo(i); });
    }, 0);
  }, true);

  window.Lich = { gan: gan, ganTatCa: ganTatCa, dongBo: dongBo, iso: iso };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', function () { ganTatCa(); });
  else ganTatCa();
})();
