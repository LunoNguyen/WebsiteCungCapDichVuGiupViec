/* ==========================================================
   FILE-VIEWER.JS – Xem file đính kèm lưu trên MinIO (trang quản trị)

   Cách dùng: gắn data-files="<đường dẫn JSON>" vào nút bất kỳ, ví dụ
     <button data-files="/hcns/cong-tac-vien/12/tai-lieu">Hồ sơ</button>
   JSON trả về: { tieuDe, taiLieu: [ { nhom, ten, moTa, url, laAnh } ] }
   Ảnh được hiển thị qua /files/<object key> (máy chủ lấy từ MinIO xuống).
   ========================================================== */
(function () {
  'use strict';

  var overlay, titleEl, bodyEl;

  function el(tag, style, text) {
    var node = document.createElement(tag);
    if (style) node.style.cssText = style;
    if (text != null) node.textContent = text;
    return node;
  }

  function build() {
    overlay = el('div', 'position:fixed;inset:0;z-index:2000;background:rgba(6,13,24,.72);display:none;align-items:center;justify-content:center;padding:1rem');
    overlay.setAttribute('role', 'dialog');
    overlay.setAttribute('aria-modal', 'true');

    var box = el('div', 'width:min(920px,100%);max-height:90vh;display:flex;flex-direction:column;background:var(--bg-card,#fff);color:var(--text-primary,#0f172a);border:1px solid var(--border,#d9e7ea);border-radius:16px;overflow:hidden');
    var head = el('div', 'display:flex;align-items:center;justify-content:space-between;gap:1rem;padding:1rem 1.25rem;border-bottom:1px solid var(--border,#d9e7ea)');
    titleEl = el('h2', 'font-size:1.1rem;font-weight:700;margin:0');
    var close = el('button', 'border:0;background:none;font-size:1.25rem;cursor:pointer;color:inherit;padding:.25rem .5rem', '✕');
    close.type = 'button';
    close.setAttribute('aria-label', 'Đóng');
    close.addEventListener('click', hide);
    head.appendChild(titleEl);
    head.appendChild(close);

    bodyEl = el('div', 'padding:1.25rem;overflow:auto');
    box.appendChild(head);
    box.appendChild(bodyEl);
    overlay.appendChild(box);
    overlay.addEventListener('click', function (e) { if (e.target === overlay) hide(); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') hide(); });
    document.body.appendChild(overlay);
  }

  function hide() { if (overlay) overlay.style.display = 'none'; }

  function message(text) {
    bodyEl.innerHTML = '';
    bodyEl.appendChild(el('p', 'margin:0;padding:1.5rem;text-align:center;color:var(--text-muted,#475569)', text));
  }

  function render(data) {
    titleEl.textContent = data.tieuDe || 'Tài liệu đính kèm';
    var files = data.taiLieu || [];
    if (!files.length) {
      message('Chưa có file nào được tải lên.');
      return;
    }
    bodyEl.innerHTML = '';
    var groups = {};
    var order = [];
    files.forEach(function (f) {
      var key = f.nhom || 'Tài liệu';
      if (!groups[key]) { groups[key] = []; order.push(key); }
      groups[key].push(f);
    });

    order.forEach(function (name) {
      bodyEl.appendChild(el('h3', 'font-size:.95rem;font-weight:700;margin:0 0 .75rem', name));
      var grid = el('div', 'display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:1rem;margin-bottom:1.5rem');

      groups[name].forEach(function (f) {
        var card = el('div', 'border:1px solid var(--border,#d9e7ea);border-radius:12px;overflow:hidden;background:var(--bg-base,#f8fafc)');
        var preview = el(f.url ? 'a' : 'div', 'display:grid;place-items:center;aspect-ratio:4/3;background:#0b1628;color:#fff;text-decoration:none;font-weight:600;font-size:.9rem');
        if (f.url) {
          preview.href = f.url;
          preview.target = '_blank';
          preview.rel = 'noopener';
        }
        if (f.url && f.laAnh) {
          var img = el('img', 'width:100%;height:100%;object-fit:contain');
          img.src = f.url;
          img.alt = f.ten || '';
          img.loading = 'lazy';
          img.addEventListener('error', function () {
            preview.textContent = 'Không tải được ảnh từ MinIO';
          });
          preview.appendChild(img);
        } else {
          preview.textContent = f.url ? 'Mở file' : 'Chưa có file';
        }
        card.appendChild(preview);

        var info = el('div', 'padding:.75rem .9rem');
        info.appendChild(el('div', 'font-weight:600;font-size:.9rem', f.ten || 'Tài liệu'));
        if (f.moTa) info.appendChild(el('div', 'font-size:.8rem;color:var(--text-muted,#475569)', f.moTa));
        if (f.url) {
          var dl = el('a', 'display:inline-block;margin-top:.4rem;font-size:.8rem;font-weight:600', 'Tải về');
          dl.href = f.url + (f.url.indexOf('?') === -1 ? '?' : '&') + 'download=true';
          info.appendChild(dl);
        }
        card.appendChild(info);
        grid.appendChild(card);
      });
      bodyEl.appendChild(grid);
    });
  }

  document.addEventListener('click', function (e) {
    var trigger = e.target.closest ? e.target.closest('[data-files]') : null;
    if (!trigger) return;
    e.preventDefault();
    if (!overlay) build();
    titleEl.textContent = 'Tài liệu đính kèm';
    message('Đang tải danh sách file…');
    overlay.style.display = 'flex';

    fetch(trigger.getAttribute('data-files'), { headers: { 'Accept': 'application/json' } })
      .then(function (res) {
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
      })
      .then(render)
      .catch(function () { message('Không tải được danh sách file. Thử lại sau.'); });
  });
})();
