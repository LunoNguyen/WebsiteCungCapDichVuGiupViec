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

    // Close on overlay click
    if (overlay) {
      overlay.addEventListener('click', () => {
        sidebar.classList.remove('mobile-open');
        overlay.classList.remove('active');
      });
    }
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
          const isOpen = menu.classList.contains('active');
          this.closeAll();
          if (!isOpen) menu.classList.add('active');
        }
        return;
      }
      // Close all if clicking elsewhere
      this.closeAll();
    });
  },
  closeAll() {
    document.querySelectorAll('.dropdown-menu.active').forEach(m => m.classList.remove('active'));
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
    const icons = { success: '✓', error: '✕', warning: '⚠' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
      <span style="font-size:1.1rem">${icons[type] || '●'}</span>
      <span style="flex:1;font-size:0.875rem">${message}</span>
      <button onclick="this.parentElement.remove()" style="background:none;border:none;color:var(--text-muted);cursor:pointer;font-size:1rem">✕</button>
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
    el.textContent = isDecimal ? current.toFixed(1) : Math.round(current).toLocaleString('vi-VN');
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
    const saved = localStorage.getItem(this.STORAGE_KEY) || 'system';
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
        const parentMenu = item.closest('.dropdown-menu');
        if (parentMenu) parentMenu.classList.remove('active');
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
    return localStorage.getItem(this.STORAGE_KEY) || 'system';
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

    document.querySelectorAll('.theme-toggle-btn i').forEach(icon => {
      if (isDark) {
        icon.className = 'ti ti-bulb theme-bulb-icon';
      } else {
        icon.className = 'ti ti-bulb-filled theme-bulb-icon';
      }
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

  // Show forbidden toast if redirected due to unauthorized role access
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('error') === 'forbidden') {
    Toast.show('Bạn không có quyền truy cập vào đường dẫn yêu cầu! Hệ thống đã đưa bạn về trang thuộc quyền hạn của bạn.', 'error', 6000);
  }
});

// Add form error style
const style = document.createElement('style');
style.textContent = `.form-control.error { border-color: var(--danger) !important; animation: shake 0.3s ease; }
@keyframes shake { 0%,100%{transform:translateX(0)} 25%{transform:translateX(-4px)} 75%{transform:translateX(4px)} }`;
document.head.appendChild(style);
