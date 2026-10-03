/* ==========================================================
   MAIN.JS – Shared JavaScript
   GiupViec Platform
   ========================================================== */

'use strict';

// ============================================================
// SIDEBAR
// ============================================================
const SidebarManager = {
  init() {
    const sidebar = document.querySelector('.sidebar');
    const toggleBtn = document.getElementById('sidebarToggle');
    const mobileToggle = document.getElementById('mobileSidebarToggle');
    const overlay = document.getElementById('sidebarOverlay');

    if (!sidebar) return;

    // Desktop toggle (collapse/expand)
    if (toggleBtn) {
      toggleBtn.addEventListener('click', () => {
        sidebar.classList.toggle('collapsed');
        localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed'));
      });
    }

    // Restore state
    if (localStorage.getItem('sidebarCollapsed') === 'true') {
      sidebar.classList.add('collapsed');
    }

    // Mobile toggle
    if (mobileToggle) {
      mobileToggle.addEventListener('click', () => {
        sidebar.classList.toggle('mobile-open');
        if (overlay) overlay.classList.toggle('active');
      });
    }

    // Đóng menu trượt: bấm lớp phủ, Esc, hoặc bấm một mục
    const closeMobile = () => {
      sidebar.classList.remove('mobile-open');
      if (overlay) overlay.classList.remove('active');
    };
    if (overlay) overlay.addEventListener('click', closeMobile);
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeMobile(); });
    sidebar.querySelectorAll('.sidebar-item').forEach(a => a.addEventListener('click', closeMobile));
  }
};

// ============================================================
// AVATAR: cùng một tên luôn ra cùng một tông pastel
// ============================================================
function initAvatarTones(root = document) {
  root.querySelectorAll('.avatar:not([data-tone])').forEach(el => {
    const inline = el.getAttribute('style') || '';
    if (/background/i.test(inline)) return; // avatar có ảnh hay màu riêng thì giữ
    const seed = (el.dataset.name || el.getAttribute('title') || el.textContent || '').trim();
    if (!seed) return;
    let hash = 0;
    for (let i = 0; i < seed.length; i++) hash = (hash * 31 + seed.charCodeAt(i)) | 0;
    el.dataset.tone = String(Math.abs(hash) % 6);
  });
}

// ============================================================
// SELECT: dựng lại danh sách chọn. <select> gốc vẫn giữ giá trị,
// required, name và sự kiện change/onchange như cũ.
// Thêm data-native vào <select> nào muốn giữ bản gốc.
// ============================================================
const SelectEnhancer = {
  openMenu: null,
  init(root = document) {
    root.querySelectorAll('select.filter-select, select.form-control').forEach(sel => this.enhance(sel));
    if (this.bound) return;
    this.bound = true;
    document.addEventListener('click', (e) => {
      if (this.openMenu && !this.openMenu.menu.contains(e.target) && !this.openMenu.trigger.contains(e.target)) this.close();
    });
    document.addEventListener('scroll', (e) => {
      if (this.openMenu && !this.openMenu.menu.contains(e.target)) this.close();
    }, true);
    window.addEventListener('resize', () => this.close());
  },
  enhance(select) {
    if (select.multiple || select.size > 1 || select.hasAttribute('data-native') || select.closest('.ui-select')) return;
    const wrap = document.createElement('div');
    wrap.className = 'ui-select' + (select.classList.contains('form-control') ? ' is-block' : '');
    // Giữ bề rộng, flex của select gốc; bỏ phần tô màu cũ
    const inline = select.getAttribute('style');
    if (inline) {
      const keep = inline.split(';').filter(rule => /^\s*(width|min-width|max-width|flex|flex-[a-z]+|margin[a-z-]*)\s*:/i.test(rule));
      if (keep.length) wrap.setAttribute('style', keep.join(';'));
    }
    select.parentNode.insertBefore(wrap, select);
    wrap.appendChild(select);
    select.tabIndex = -1;
    select.setAttribute('aria-hidden', 'true');

    const trigger = document.createElement('button');
    trigger.type = 'button';
    trigger.className = 'ui-select-trigger';
    trigger.setAttribute('aria-haspopup', 'listbox');
    trigger.setAttribute('aria-expanded', 'false');
    const label = select.id ? document.querySelector(`label[for="${select.id}"]`) : null;
    if (label) {
      label.addEventListener('click', (e) => { e.preventDefault(); trigger.focus(); });
      trigger.setAttribute('aria-label', label.textContent.trim());
    } else if (select.getAttribute('aria-label')) {
      trigger.setAttribute('aria-label', select.getAttribute('aria-label'));
    }
    trigger.innerHTML = '<span class="ui-select-value"></span><i class="ti ti-chevron-down" aria-hidden="true"></i>';
    wrap.appendChild(trigger);

    const menu = document.createElement('div');
    menu.className = 'ui-select-menu';
    menu.setAttribute('role', 'listbox');
    document.body.appendChild(menu);

    const state = { select, trigger, menu };
    const sync = () => {
      const opt = select.options[select.selectedIndex];
      const valueEl = trigger.querySelector('.ui-select-value');
      valueEl.textContent = opt ? opt.textContent.trim() : '';
      valueEl.classList.toggle('is-placeholder', !!opt && opt.value === '' && opt.disabled);
      trigger.disabled = select.disabled;
    };
    state.sync = sync;

    // Gán giá trị bằng JS (mở form sửa, reset) cũng cập nhật nhãn
    const proto = HTMLSelectElement.prototype;
    ['value', 'selectedIndex'].forEach(prop => {
      const desc = Object.getOwnPropertyDescriptor(proto, prop);
      Object.defineProperty(select, prop, {
        configurable: true,
        get() { return desc.get.call(this); },
        set(v) { desc.set.call(this, v); sync(); }
      });
    });
    new MutationObserver(sync).observe(select, { childList: true, subtree: true, attributes: true, attributeFilter: ['disabled', 'selected'] });
    select.addEventListener('change', sync);
    if (select.form) select.form.addEventListener('reset', () => setTimeout(sync));

    trigger.addEventListener('click', () => (this.openMenu === state ? this.close() : this.open(state)));
    trigger.addEventListener('keydown', (e) => {
      if (this.openMenu !== state) {
        if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(e.key)) { e.preventDefault(); this.open(state); }
        return;
      }
      const opts = [...menu.querySelectorAll('.ui-select-option:not(:disabled)')];
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        const cur = opts.findIndex(o => o.classList.contains('is-active'));
        const next = e.key === 'ArrowDown' ? Math.min(cur + 1, opts.length - 1) : Math.max(cur - 1, 0);
        opts.forEach(o => o.classList.remove('is-active'));
        if (opts[next]) { opts[next].classList.add('is-active'); opts[next].scrollIntoView({ block: 'nearest' }); }
      } else if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        const act = menu.querySelector('.ui-select-option.is-active');
        if (act) act.click();
      } else if (e.key === 'Escape' || e.key === 'Tab') {
        this.close();
      }
    });
    sync();
  },
  open(state) {
    this.close();
    const { select, trigger, menu } = state;
    menu.innerHTML = '';
    [...select.options].forEach((opt, i) => {
      if (opt.hidden) return;
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'ui-select-option';
      b.setAttribute('role', 'option');
      b.setAttribute('aria-selected', String(i === select.selectedIndex));
      b.disabled = opt.disabled;
      const text = document.createElement('span');
      text.textContent = opt.textContent.trim();
      b.appendChild(text);
      if (i === select.selectedIndex) b.classList.add('is-active');
      b.addEventListener('click', () => {
        Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'selectedIndex').set.call(select, i);
        state.sync();
        select.dispatchEvent(new Event('input', { bubbles: true }));
        select.dispatchEvent(new Event('change', { bubbles: true }));
        this.close();
        trigger.focus();
      });
      menu.appendChild(b);
    });
    const r = trigger.getBoundingClientRect();
    menu.style.minWidth = r.width + 'px';
    menu.style.left = '0px';
    menu.style.top = '0px';
    menu.classList.add('open');
    const mw = menu.offsetWidth;
    const mh = menu.offsetHeight;
    const left = Math.max(8, Math.min(r.left, window.innerWidth - mw - 8));
    const below = window.innerHeight - r.bottom;
    const top = below < mh + 12 && r.top > mh + 12 ? r.top - mh - 4 : r.bottom + 4;
    menu.style.left = left + 'px';
    menu.style.top = top + 'px';
    trigger.setAttribute('aria-expanded', 'true');
    const act = menu.querySelector('.is-active');
    if (act) act.scrollIntoView({ block: 'nearest' });
    this.openMenu = state;
  },
  close() {
    if (!this.openMenu) return;
    this.openMenu.menu.classList.remove('open');
    this.openMenu.trigger.setAttribute('aria-expanded', 'false');
    this.openMenu = null;
  }
};


// ============================================================
// DROPDOWN
// ============================================================
const DropdownManager = {
  init() {
    document.addEventListener('click', (e) => {
      // Open clicked dropdown
      const trigger = e.target.closest('[data-dropdown]');
      if (trigger) {
        e.stopPropagation();
        const targetId = trigger.dataset.dropdown;
        const menu = document.getElementById(targetId);
        if (menu) {
          const isOpen = menu.classList.contains('active') || menu.classList.contains('show');
          this.closeAll();
          if (!isOpen) {
            menu.classList.add('active');
            menu.classList.add('show');
            // Menu trong bảng (khung cuộn) đặt fixed theo nút, để không bị khung cắt
            if (menu.classList.contains('dropdown-menu-fixed')) {
              const r = trigger.getBoundingClientRect();
              menu.style.position = 'fixed';
              menu.style.right = 'auto';
              const mw = menu.offsetWidth, mh = menu.offsetHeight;
              menu.style.left = Math.max(8, Math.min(r.right - mw, window.innerWidth - mw - 8)) + 'px';
              menu.style.top = (window.innerHeight - r.bottom < mh + 12 && r.top > mh + 12 ? r.top - mh - 4 : r.bottom + 4) + 'px';
            }
            const parent = menu.closest('.dropdown') || trigger.closest('.dropdown');
            if (parent) {
              parent.classList.add('open');
              parent.classList.add('active');
            }
          }
        }
        return;
      }

      // If clicking inside menu, don't close unless an action item (e.g. data-theme-set or link) was clicked
      if (e.target.closest('.ui-select-menu')) return;
      const insideMenu = e.target.closest('.dropdown-menu');
      if (insideMenu && !e.target.closest('[data-theme-set]') && !e.target.closest('a')) {
        return;
      }

      // Close all if clicking elsewhere
      this.closeAll();
    });
    document.addEventListener('scroll', (e) => {
      const open = document.querySelector('.dropdown-menu-fixed.active');
      if (open && !open.contains(e.target)) this.closeAll();
    }, true);
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') this.closeAll(); });
  },
  closeAll() {
    document.querySelectorAll('.dropdown-menu.active, .dropdown-menu.show').forEach(m => {
      m.classList.remove('active');
      m.classList.remove('show');
    });
    document.querySelectorAll('.dropdown.open, .dropdown.active').forEach(d => {
      d.classList.remove('open');
      d.classList.remove('active');
    });
  }
};

// ============================================================
// MODAL
// ============================================================
const ModalManager = {
  open(modalId) {
    const overlay = document.getElementById(modalId);
    if (overlay) {
      overlay.classList.add('active');
      document.body.style.overflow = 'hidden';
    }
  },
  close(modalId) {
    const overlay = document.getElementById(modalId);
    if (overlay) {
      overlay.classList.remove('active');
      document.body.style.overflow = '';
    }
  },
  init() {
    // Open by data-modal-open attribute
    document.addEventListener('click', (e) => {
      const opener = e.target.closest('[data-modal-open]');
      if (opener) this.open(opener.dataset.modalOpen);

      const closer = e.target.closest('[data-modal-close]');
      if (closer) this.close(closer.dataset.modalClose);

      // Close on overlay click
      if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('active');
        document.body.style.overflow = '';
      }
    });

    // Close on Escape
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay.active').forEach(m => {
          m.classList.remove('active');
          document.body.style.overflow = '';
        });
      }
    });
  }
};

// ============================================================
// DETAIL PANEL
// ============================================================
const PanelManager = {
  open(panelId) {
    const panel = document.getElementById(panelId);
    const overlay = document.getElementById('panelOverlay');
    if (panel) {
      panel.classList.add('open');
      if (overlay) overlay.classList.add('active');
      document.body.style.overflow = 'hidden';
    }
  },
  close(panelId) {
    const panel = document.getElementById(panelId);
    const overlay = document.getElementById('panelOverlay');
    if (panel) {
      panel.classList.remove('open');
      if (overlay) overlay.classList.remove('active');
      document.body.style.overflow = '';
    }
  },
  init() {
    document.addEventListener('click', (e) => {
      const opener = e.target.closest('[data-panel-open]');
      if (opener) this.open(opener.dataset.panelOpen);

      const closer = e.target.closest('[data-panel-close]');
      if (closer) this.close(closer.dataset.panelClose);
    });

    const overlay = document.getElementById('panelOverlay');
    if (overlay) {
      overlay.addEventListener('click', () => {
        document.querySelectorAll('.detail-panel.open').forEach(p => p.classList.remove('open'));
        overlay.classList.remove('active');
        document.body.style.overflow = '';
      });
    }
  }
};

// ============================================================
// TOAST NOTIFICATIONS
// ============================================================
const Toast = {
  container: null,
  init() {
    this.container = document.getElementById('toastContainer');
    if (!this.container) {
      this.container = document.createElement('div');
      this.container.className = 'toast-container';
      this.container.id = 'toastContainer';
      document.body.appendChild(this.container);
    }
  },
  show(message, type = 'success', duration = 3500) {
    const icons = { success: 'circle-check', error: 'alert-circle', warning: 'alert-triangle', info: 'info-circle' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.setAttribute('role', type === 'error' ? 'alert' : 'status');
    toast.innerHTML = `
      <span style="display:inline-flex;font-size:1.125rem"><i class="ti ti-${icons[type] || 'info-circle'}"></i></span>
      <span style="flex:1;font-size:0.875rem">${message}</span>
      <button type="button" class="icon-btn" aria-label="Đóng" onclick="this.parentElement.remove()" style="width:28px;height:28px;font-size:1rem"><i class="ti ti-x"></i></button>
    `;
    this.container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }
};

// ============================================================
// TABLE SEARCH & FILTER
// ============================================================
const TableFilter = {
  init() {
    const searchInputs = document.querySelectorAll('[data-table-search]');
    searchInputs.forEach(input => {
      const tableId = input.dataset.tableSearch;
      input.addEventListener('input', () => this.filter(tableId, input.value));
    });
  },
  filter(tableId, query) {
    const table = document.getElementById(tableId);
    if (!table) return;
    const rows = table.querySelectorAll('tbody tr');
    const q = query.toLowerCase().trim();
    rows.forEach(row => {
      const text = row.textContent.toLowerCase();
      row.style.display = (!q || text.includes(q)) ? '' : 'none';
    });
  }
};

// ============================================================
// TABS
// ============================================================
const TabManager = {
  init() {
    document.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-tab]');
      if (!btn) return;
      const group = btn.closest('[data-tab-group]') || btn.parentElement;
      const target = btn.dataset.tab;

      // Deactivate all in group
      group.querySelectorAll('[data-tab]').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      // Show/hide panels
      const container = btn.closest('[data-tabs-container]') || document;
      container.querySelectorAll('[data-tab-panel]').forEach(panel => {
        panel.style.display = panel.dataset.tabPanel === target ? '' : 'none';
      });
    });
  }
};

// ============================================================
// FORM VALIDATION
// ============================================================
const FormValidator = {
  validate(formEl) {
    let valid = true;
    const required = formEl.querySelectorAll('[required]');
    required.forEach(field => {
      field.classList.remove('error');
      if (!field.value.trim()) {
        field.classList.add('error');
        field.style.borderColor = 'var(--danger)';
        valid = false;
      } else {
        field.style.borderColor = '';
      }
    });
    return valid;
  }
};

// ============================================================
// CONFIRM DIALOG
// ============================================================
function confirmAction(message, onConfirm) {
  const overlay = document.getElementById('confirmModal');
  if (!overlay) return onConfirm();
  document.getElementById('confirmMessage').textContent = message;
  overlay.classList.add('active');
  document.getElementById('confirmOk').onclick = () => {
    overlay.classList.remove('active');
    onConfirm();
  };
  document.getElementById('confirmCancel').onclick = () => {
    overlay.classList.remove('active');
  };
}

// ============================================================
// SELECT ALL CHECKBOX
// ============================================================
function initSelectAll() {
  const masterCb = document.getElementById('selectAll');
  if (!masterCb) return;
  masterCb.addEventListener('change', () => {
    document.querySelectorAll('.row-checkbox').forEach(cb => {
      cb.checked = masterCb.checked;
    });
  });
}

// ============================================================
// LANDING PAGE SPECIFIC
// ============================================================
const LandingPage = {
  init() {
    if (!document.querySelector('.landing-nav')) return;

    // Sticky nav
    const nav = document.querySelector('.landing-nav');
    window.addEventListener('scroll', () => {
      nav.classList.toggle('scrolled', window.scrollY > 50);
    });

    // Scroll to top button
    const scrollBtn = document.getElementById('scrollTop');
    if (scrollBtn) {
      window.addEventListener('scroll', () => {
        scrollBtn.classList.toggle('visible', window.scrollY > 400);
      });
      scrollBtn.addEventListener('click', () => {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      });
    }

    // Mobile menu
    const hamburger = document.getElementById('hamburger');
    const mobileMenu = document.getElementById('mobileMenu');
    const mobileClose = document.getElementById('mobileMenuClose');
    if (hamburger && mobileMenu) {
      hamburger.addEventListener('click', () => mobileMenu.classList.add('active'));
      if (mobileClose) mobileClose.addEventListener('click', () => mobileMenu.classList.remove('active'));
    }

    // Smooth scrolling for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(link => {
      link.addEventListener('click', (e) => {
        const target = document.querySelector(link.getAttribute('href'));
        if (target) {
          e.preventDefault();
          target.scrollIntoView({ behavior: 'smooth', block: 'start' });
          if (mobileMenu) mobileMenu.classList.remove('active');
        }
      });
    });

    // Intersection Observer for animations
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('animate-slideUp');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.1 });

    document.querySelectorAll('.service-card, .step-card, .why-card, .testimonial-card, .pricing-card').forEach(el => {
      el.style.opacity = '0';
      observer.observe(el);
    });
  }
};

// ============================================================
// COUNTER ANIMATION
// ============================================================
function animateCounter(el, target, duration = 1500) {
  const start = 0;
  const startTime = performance.now();
  const isDecimal = target % 1 !== 0;

  function update(currentTime) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3); // ease out cubic
    const current = start + (target - start) * eased;
    el.textContent = isDecimal
      ? current.toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
      : Math.round(current).toLocaleString('vi-VN');
    if (progress < 1) requestAnimationFrame(update);
  }
  requestAnimationFrame(update);
}

function initCounters() {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const el = entry.target;
        const target = parseFloat(el.dataset.count);
        animateCounter(el, target);
        observer.unobserve(el);
      }
    });
  }, { threshold: 0.5 });

  document.querySelectorAll('[data-count]').forEach(el => observer.observe(el));
}

// ============================================================
// CHART PERIOD TABS
// ============================================================
function initChartPeriodTabs() {
  document.querySelectorAll('.chart-period-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const group = btn.closest('.chart-period-tabs');
      group.querySelectorAll('.chart-period-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    });
  });
}

// ============================================================
// THEME MANAGER (LIGHT, DARK, SYSTEM)
// ============================================================
const ThemeManager = {
  STORAGE_KEY: 'neatify_theme',

  init() {
    const saved = localStorage.getItem(this.STORAGE_KEY) || 'light';
    this.apply(saved);

    try {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      mediaQuery.addEventListener('change', () => {
        if (this.getCurrentMode() === 'system') {
          this.apply('system');
        }
      });
    } catch (err) {}

    document.addEventListener('click', (e) => {
      const item = e.target.closest('[data-theme-set]');
      if (item) {
        e.preventDefault();
        const mode = item.getAttribute('data-theme-set');
        this.set(mode);
        if (typeof DropdownManager !== 'undefined' && DropdownManager.closeAll) {
          DropdownManager.closeAll();
        } else {
          const parentMenu = item.closest('.dropdown-menu');
          if (parentMenu) parentMenu.classList.remove('active');
        }
        const modeLabel = mode === 'light' ? 'Chế độ Sáng' : (mode === 'dark' ? 'Chế độ Tối' : 'Theo Hệ Thống');
        if (typeof Toast !== 'undefined' && Toast.show) {
          Toast.show(`Đã chuyển sang ${modeLabel}`, 'info', 2500);
        }
        return;
      }

      const directToggle = e.target.closest('.theme-direct-toggle');
      if (directToggle) {
        e.preventDefault();
        const curr = this.getCurrentMode();
        const next = curr === 'dark' ? 'light' : (curr === 'light' ? 'system' : 'dark');
        this.set(next);
        const nextLabel = next === 'light' ? 'Chế độ Sáng' : (next === 'dark' ? 'Chế độ Tối' : 'Theo Hệ Thống');
        if (typeof Toast !== 'undefined' && Toast.show) {
          Toast.show(`Đã chuyển sang ${nextLabel}`, 'info', 2500);
        }
      }
    });
  },

  getCurrentMode() {
    return localStorage.getItem(this.STORAGE_KEY) || 'light';
  },

  set(mode) {
    localStorage.setItem(this.STORAGE_KEY, mode);
    this.apply(mode);
  },

  apply(mode) {
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    const isDark = mode === 'dark' || (mode === 'system' && prefersDark);
    
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
    document.documentElement.setAttribute('data-theme-mode', mode);

    document.querySelectorAll('[data-theme-set]').forEach(el => {
      el.classList.toggle('active', el.getAttribute('data-theme-set') === mode);
    });

  }
};

// ============================================================
// INIT ALL
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  ThemeManager.init();
  SidebarManager.init();
  DropdownManager.init();
  ModalManager.init();
  PanelManager.init();
  Toast.init();
  TableFilter.init();
  TabManager.init();
  initSelectAll();
  LandingPage.init();
  initCounters();
  initChartPeriodTabs();
  initAvatarTones();
  SelectEnhancer.init();

  // Show forbidden toast if redirected due to unauthorized role access
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('error') === 'forbidden') {
    Toast.show('Bạn không có quyền truy cập vào đường dẫn yêu cầu! Hệ thống đã đưa bạn về trang thuộc quyền hạn của bạn.', 'error', 6000);
  }
});

// ============================================================
// BIỂU ĐỒ RỖNG: thay vùng vẽ bằng một câu báo, không để trục trống
// ChartEmpty.isEmpty(values) -> true khi không có giá trị nào khác 0
// ChartEmpty.show(canvasId, 'Chưa có dữ liệu ...')
// ============================================================
const ChartEmpty = {
  isEmpty(values) {
    const list = Array.isArray(values) ? values : Object.values(values || {});
    return !list.some(v => Number(v) > 0);
  },
  show(canvasId, message) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || !canvas.parentElement) return;
    const box = document.createElement('div');
    box.className = 'chart-empty';
    box.textContent = message || 'Chưa có dữ liệu trong kỳ này';
    canvas.replaceWith(box);
  }
};
window.ChartEmpty = ChartEmpty;

// Màu biểu đồ lấy từ token (main.css), tự đổi theo dark mode
const ChartTones = {
  css(name) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim(); },
  series(n) {
    const list = ['--primary', '--tone-indigo', '--tone-sky', '--tone-violet', '--tone-rose'];
    return Array.from({ length: n }, (_, i) => this.css(list[i % list.length]));
  },
  // 5 sao -> 1 sao
  rating() { return ['--success', '--primary', '--warning', '--tone-rose', '--danger'].map(v => this.css(v)); },
  // Hoàn thành, Đang thực hiện, Đã xác nhận, Chờ duyệt, Đã huỷ
  orderStatus() { return ['--success', '--tone-indigo', '--tone-sky', '--warning', '--danger'].map(v => this.css(v)); }
};
window.ChartTones = ChartTones;

// Add form error style
const style = document.createElement('style');
style.textContent = `.form-control.error { border-color: var(--danger) !important; animation: shake 0.3s ease; }
@keyframes shake { 0%,100%{transform:translateX(0)} 25%{transform:translateX(-4px)} 75%{transform:translateX(4px)} }`;
document.head.appendChild(style);
