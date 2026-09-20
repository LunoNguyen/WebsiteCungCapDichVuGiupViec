/* ==========================================================
   CHARTS.JS – Chart.js Configurations
   Neatify – Home Cleaning & Tasks
   ========================================================== */

'use strict';

// Chart.js global defaults
if (typeof Chart !== 'undefined') {
  Chart.defaults.color = '#A8C4C8';
  Chart.defaults.borderColor = 'rgba(13,155,163,0.10)';
  Chart.defaults.font.family = "'Inter', sans-serif";
  Chart.defaults.plugins.legend.labels.usePointStyle = true;
  Chart.defaults.plugins.legend.labels.pointStyleWidth = 8;
  Chart.defaults.plugins.tooltip.backgroundColor = 'rgba(6, 13, 18, 0.96)';
  Chart.defaults.plugins.tooltip.borderColor = 'rgba(13,155,163,0.35)';
  Chart.defaults.plugins.tooltip.borderWidth = 1;
  Chart.defaults.plugins.tooltip.padding = 12;
  Chart.defaults.plugins.tooltip.titleFont = { size: 13, weight: '700' };
  Chart.defaults.plugins.tooltip.bodyFont = { size: 12 };
}

// ============================================================
// GRADIENT HELPER
// ============================================================
function createGradient(ctx, colorStart, colorEnd) {
  const gradient = ctx.createLinearGradient(0, 0, 0, 300);
  gradient.addColorStop(0, colorStart);
  gradient.addColorStop(1, colorEnd);
  return gradient;
}

// ============================================================
// REVENUE CHART – Line
// ============================================================
function initRevenueChart(canvasId, data = null) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;
  const ctx = canvas.getContext('2d');

  const labels = data?.labels || ['T1','T2','T3','T4','T5','T6','T7','T8','T9','T10','T11','T12'];
  const values = data?.values || [42,58,75,62,90,110,98,125,142,118,160,185];

  const gradient = createGradient(ctx, 'rgba(13,155,163,0.45)', 'rgba(13,155,163,0.02)');

  return new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [{
        label: 'Doanh thu (triệu đồng)',
        data: values,
        fill: true,
        backgroundColor: gradient,
        borderColor: '#0D9BA3',
        borderWidth: 2.5,
        pointBackgroundColor: '#0D9BA3',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
        pointRadius: 4,
        pointHoverRadius: 7,
        tension: 0.4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => ` ${ctx.parsed.y.toLocaleString('vi-VN')} triệu đồng`
          }
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { font: { size: 11 } }
        },
        y: {
          grid: { color: 'rgba(13,155,163,0.06)' },
          ticks: {
            font: { size: 11 },
            callback: (val) => val + ' tr'
          }
        }
      }
    }
  });
}

// ============================================================
// ORDERS BY STATUS – Bar Chart (thay thế Doughnut)
// ============================================================
function initOrdersDonut(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;
  const ctx = canvas.getContext('2d');

  return new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Hoàn thành', 'Đang thực hiện', 'Chờ duyệt', 'Đã hủy'],
      datasets: [{
        label: 'Số đơn',
        data: [848, 224, 112, 63],
        backgroundColor: [
          'rgba(56,212,138,0.85)',
          'rgba(13,155,163,0.85)',
          'rgba(245,158,11,0.85)',
          'rgba(239,68,68,0.85)'
        ],
        borderRadius: 8,
        borderSkipped: false
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: ctx => ` ${ctx.parsed.y} đơn`
          }
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { font: { size: 11 } }
        },
        y: {
          grid: { color: 'rgba(13,155,163,0.06)' },
          ticks: { font: { size: 11 } }
        }
      }
    }
  });
}

// ============================================================
// SERVICES BAR CHART
// ============================================================
function initServicesBarChart(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;
  const ctx = canvas.getContext('2d');

  const gradients = [
    createGradient(ctx, 'rgba(13,155,163,0.9)', 'rgba(13,155,163,0.5)'),
    createGradient(ctx, 'rgba(21,133,192,0.9)', 'rgba(21,133,192,0.5)'),
    createGradient(ctx, 'rgba(56,212,138,0.9)', 'rgba(56,212,138,0.5)'),
    createGradient(ctx, 'rgba(245,158,11,0.9)', 'rgba(245,158,11,0.5)'),
    createGradient(ctx, 'rgba(93,217,224,0.9)', 'rgba(93,217,224,0.5)'),
    createGradient(ctx, 'rgba(10,123,130,0.9)', 'rgba(10,123,130,0.5)'),
  ];

  return new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Dọn dẹp nhà', 'Giặt ủi', 'Nấu ăn', 'Chăm sóc NCT', 'Trông trẻ', 'Tổng VS'],
      datasets: [{
        label: 'Số đơn',
        data: [345, 218, 187, 124, 156, 89],
        backgroundColor: gradients,
        borderRadius: 8,
        borderSkipped: false
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { font: { size: 11 } }
        },
        y: {
          grid: { color: 'rgba(13,155,163,0.06)' },
          ticks: { font: { size: 11 } }
        }
      }
    }
  });
}

// ============================================================
// RATING DISTRIBUTION – Horizontal Bar
// ============================================================
function initRatingChart(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;

  return new Chart(canvas, {
    type: 'bar',
    data: {
      labels: ['5 ★', '4 ★', '3 ★', '2 ★', '1 ★'],
      datasets: [{
        data: [58, 25, 10, 5, 2],
        backgroundColor: ['#43E97B', '#6C63FF', '#FFC107', '#FF6584', '#FF4757'],
        borderRadius: 6,
        borderSkipped: false,
      }]
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: { label: ctx => ` ${ctx.parsed.x}%` }
        }
      },
      scales: {
        x: {
          grid: { color: 'rgba(255,255,255,0.04)' },
          max: 100,
          ticks: { callback: v => v + '%', font: { size: 11 } }
        },
        y: {
          grid: { display: false },
          ticks: { font: { size: 12, weight: '600' } }
        }
      }
    }
  });
}

// ============================================================
// COMPLAINT STATUS – Horizontal Bar Chart (thay thế Pie)
// ============================================================
function initComplaintChart(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;

  return new Chart(canvas, {
    type: 'bar',
    data: {
      labels: ['Đã giải quyết', 'Đang xử lý', 'Mới', 'Chờ xác minh', 'Leo thang'],
      datasets: [{
        label: 'Số khiếu nại',
        data: [52, 24, 10, 9, 5],
        backgroundColor: [
          'rgba(56,212,138,0.85)',
          'rgba(13,155,163,0.85)',
          'rgba(245,158,11,0.85)',
          'rgba(20,191,201,0.85)',
          'rgba(239,68,68,0.85)'
        ],
        borderRadius: 6,
        borderSkipped: false
      }]
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: { label: ctx => ` ${ctx.parsed.x} khiếu nại` }
        }
      },
      scales: {
        x: {
          grid: { color: 'rgba(13,155,163,0.06)' },
          ticks: { font: { size: 11 } }
        },
        y: {
          grid: { display: false },
          ticks: { font: { size: 11, weight: '600' } }
        }
      }
    }
  });
}

// ============================================================
// CTV PERFORMANCE – Radar
// ============================================================
function initCtvRadarChart(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;

  return new Chart(canvas, {
    type: 'radar',
    data: {
      labels: ['Chất lượng', 'Thái độ', 'Đúng giờ', 'Giao tiếp', 'Chuyên nghiệp'],
      datasets: [{
        label: 'Điểm TB',
        data: [4.5, 4.8, 4.2, 4.6, 4.4],
        borderColor: '#6C63FF',
        backgroundColor: 'rgba(108,99,255,0.15)',
        borderWidth: 2,
        pointBackgroundColor: '#6C63FF',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
        pointRadius: 4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        r: {
          min: 0, max: 5,
          ticks: { stepSize: 1, font: { size: 10 } },
          grid: { color: 'rgba(255,255,255,0.06)' },
          angleLines: { color: 'rgba(255,255,255,0.06)' },
          pointLabels: { font: { size: 12 } }
        }
      },
      plugins: { legend: { display: false } }
    }
  });
}

// ============================================================
// MONTHLY COMPARISON – Grouped Bar
// ============================================================
function initComparisonChart(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;

  return new Chart(canvas, {
    type: 'bar',
    data: {
      labels: ['T7', 'T8', 'T9'],
      datasets: [
        {
          label: 'Năm nay',
          data: [118, 160, 185],
          backgroundColor: 'rgba(13,155,163,0.85)',
          borderRadius: 6,
          borderSkipped: false
        },
        {
          label: 'Năm trước',
          data: [95, 130, 142],
          backgroundColor: 'rgba(21,133,192,0.45)',
          borderRadius: 6,
          borderSkipped: false
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'top', labels: { font: { size: 12 }, padding: 16 } }
      },
      scales: {
        x: { grid: { display: false }, ticks: { font: { size: 11 } } },
        y: {
          grid: { color: 'rgba(13,155,163,0.06)' },
          ticks: { callback: v => v + ' tr', font: { size: 11 } }
        }
      }
    }
  });
}

// ============================================================
// MINI SPARKLINE
// ============================================================
function initSparkline(canvasId, data, color = '#6C63FF') {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;
  const ctx = canvas.getContext('2d');
  const gradient = createGradient(ctx, color + '60', color + '05');

  return new Chart(ctx, {
    type: 'line',
    data: {
      labels: Array(data.length).fill(''),
      datasets: [{
        data,
        fill: true,
        backgroundColor: gradient,
        borderColor: color,
        borderWidth: 2,
        pointRadius: 0,
        tension: 0.4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: { enabled: false } },
      scales: {
        x: { display: false },
        y: { display: false }
      },
      animation: { duration: 800 }
    }
  });
}

// ============================================================
// INIT ALL CHARTS ON PAGE
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  if (typeof Chart === 'undefined') return;

  // Revenue chart
  initRevenueChart('revenueChart');
  // Orders donut
  initOrdersDonut('ordersDonut');
  // Services bar
  initServicesBarChart('servicesChart');
  // Rating distribution
  initRatingChart('ratingChart');
  // Complaint status
  initComplaintChart('complaintChart');
  // CTV radar
  initCtvRadarChart('ctvRadarChart');
  // Comparison
  initComparisonChart('comparisonChart');

  // Sparklines (mini charts in stat cards)
  const sparklines = [
    { id: 'spark1', data: [10,15,12,18,22,20,28,25,32,30,38,35], color: '#0D9BA3' },
    { id: 'spark2', data: [8,12,10,15,18,14,20,18,24,22,28,25], color: '#38D48A' },
    { id: 'spark3', data: [5,8,7,10,9,12,11,14,13,16,15,18], color: '#1585C0' },
    { id: 'spark4', data: [3,5,4,6,5,8,7,9,8,11,10,12], color: '#F59E0B' },
    { id: 'spark5', data: [2,4,3,5,4,6,5,7,6,8,7,9], color: '#EF4444' },
  ];
  sparklines.forEach(s => initSparkline(s.id, s.data, s.color));
});
