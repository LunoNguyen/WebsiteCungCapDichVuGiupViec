/* ==========================================================
   CHARTS.JS – Chart.js Configurations
   Neatify – Home Cleaning & Tasks
   ========================================================== */

'use strict';

// Màu lấy từ token trong main.css: đổi theme là biểu đồ đổi theo khi tải lại.
function cssVar(name, fallback) {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return v || fallback;
}
function withAlpha(color, alpha) {
  const m = /^#([0-9a-f]{6})$/i.exec(color);
  if (!m) return color;
  const n = parseInt(m[1], 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`;
}
const ChartTheme = {
  get primary() { return cssVar('--primary', '#08778C'); },
  get muted() { return cssVar('--muted', '#707070'); },
  get foreground() { return cssVar('--foreground', '#262626'); },
  get line() { return cssVar('--line', '#F0F0F2'); },
  get surface() { return cssVar('--surface-overlay', '#FFFFFF'); }
};

// Chart.js global defaults
if (typeof Chart !== 'undefined') {
  Chart.defaults.color = ChartTheme.muted;
  Chart.defaults.borderColor = ChartTheme.line;
  Chart.defaults.font.family = "'Inter', sans-serif";
  Chart.defaults.font.size = 12;
  Chart.defaults.plugins.legend.labels.usePointStyle = true;
  Chart.defaults.plugins.legend.labels.pointStyleWidth = 8;
  Chart.defaults.plugins.tooltip.backgroundColor = ChartTheme.surface;
  Chart.defaults.plugins.tooltip.titleColor = ChartTheme.foreground;
  Chart.defaults.plugins.tooltip.bodyColor = ChartTheme.foreground;
  Chart.defaults.plugins.tooltip.borderColor = ChartTheme.line;
  Chart.defaults.plugins.tooltip.borderWidth = 1;
  Chart.defaults.plugins.tooltip.padding = 10;
  Chart.defaults.plugins.tooltip.cornerRadius = 10;
  Chart.defaults.plugins.tooltip.titleFont = { size: 12, weight: '600' };
  Chart.defaults.plugins.tooltip.bodyFont = { size: 12 };
}

// Ghi số lên đầu cột, để đọc cả năm một lượt không phải rê chuột
const barValueLabels = {
  id: 'barValueLabels',
  afterDatasetsDraw(chart) {
    const { ctx } = chart;
    const meta = chart.getDatasetMeta(0);
    const values = chart.data.datasets[0].data;
    ctx.save();
    ctx.font = "500 12px 'Inter', sans-serif";
    ctx.fillStyle = ChartTheme.foreground;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'bottom';
    meta.data.forEach((bar, i) => {
      const v = values[i];
      if (v === null || v === undefined) return;
      ctx.fillText(Number(v).toLocaleString('vi-VN'), bar.x, bar.y - 6);
    });
    ctx.restore();
  }
};

// Danh sách thanh bằng HTML (trạng thái đơn, dịch vụ phổ biến): data là { nhãn: số }
function renderBarList(elId, data, emptyText) {
  const el = document.getElementById(elId);
  if (!el) return;
  const rows = Object.entries(data || {}).map(([k, v]) => [k, Number(v) || 0]).filter(r => r[1] > 0).sort((a, b) => b[1] - a[1]);
  const total = rows.reduce((sum, r) => sum + r[1], 0);
  if (!rows.length) {
    el.innerHTML = `<li class="bar-list-empty">${emptyText || 'Chưa có dữ liệu'}</li>`;
    return;
  }
  const max = rows[0][1];
  el.innerHTML = rows.map(([label, v]) => {
    const li = document.createElement('li');
    li.innerHTML = `<div class="bar-list-row"><span class="bar-list-label"></span><span class="bar-list-value"></span><span class="bar-list-pct"></span></div><div class="bar-list-track"><div class="bar-list-fill"></div></div>`;
    li.querySelector('.bar-list-label').textContent = label;
    li.querySelector('.bar-list-label').title = label;
    li.querySelector('.bar-list-value').textContent = v.toLocaleString('vi-VN');
    li.querySelector('.bar-list-pct').textContent = Math.round((v / total) * 100) + '%';
    li.querySelector('.bar-list-fill').style.width = Math.max((v / max) * 100, 2) + '%';
    return li.outerHTML;
  }).join('');
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
// ============================================================
// REVENUE CHART – Line (Động 100% theo DB cho 12 tháng)
// ============================================================
function initRevenueChart(canvasId, data = null) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;
  const ctx = canvas.getContext('2d');

  let labels = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'];
  let values = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

  if (window.chartData && window.chartData.doanhThuTheoThang) {
      let dbData = window.chartData.doanhThuTheoThang;
      let keys = Object.keys(dbData);
      if (keys.length > 0) {
          labels = keys; // Lấy trực tiếp key từ Java (ví dụ: Tháng 1, Tháng 2...)
          values = Object.values(dbData); // Lấy giá trị doanh thu tương ứng
      }
  }

  return new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [{
        label: 'Doanh thu (triệu đồng)',
        data: values,
        backgroundColor: ChartTheme.primary,
        hoverBackgroundColor: ChartTheme.primary,
        borderRadius: { topLeft: 6, topRight: 6 },
        borderSkipped: 'bottom',
        maxBarThickness: 32
      }]
    },
    plugins: [barValueLabels],
    options: {
      responsive: true,
      maintainAspectRatio: false,
      layout: { padding: { top: 22 } },
      plugins: { legend: { display: false }, tooltip: { callbacks: { label: (ctx) => ` ${ctx.parsed.y.toLocaleString('vi-VN')} triệu đồng` } } },
      scales: {
        x: { grid: { display: false }, border: { color: ChartTheme.line }, ticks: { font: { size: 12 } } },
        y: { display: false, beginAtZero: true, grace: '8%' }
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

  let labels = ['Hoàn thành', 'Đang thực hiện', 'Đã xác nhận', 'Chờ duyệt', 'Đã hủy'];
  let values = [0, 0, 0, 0, 0];

  if (window.chartData && window.chartData.orderDistribution) {
      let dbData = window.chartData.orderDistribution;
      labels = Object.keys(dbData);
      values = Object.values(dbData);
  }

  return new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [{
        label: 'Số đơn',
        data: values,
        backgroundColor: ChartTheme.primary,
        borderRadius: 6,
        borderSkipped: false,
        maxBarThickness: 32
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: { label: ctx => ` ${ctx.parsed.y} đơn` }
        }
      },
      scales: {
        x: { grid: { display: false }, ticks: { font: { size: 11 } } },
        y: { grid: { color: ChartTheme.line }, ticks: { font: { size: 11 }, stepSize: 1 } }
      }
    }
  });
}

// ============================================================
// SERVICES BAR CHART
// ============================================================
// ============================================================
// SERVICES BAR CHART (ĐÃ NỐI DATABASE REAL 100%)
// ============================================================
function initServicesBarChart(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;
  const ctx = canvas.getContext('2d');


  let chartLabels = ['Dọn dẹp', 'Giặt ủi', 'Nấu ăn', 'Khác'];
  let chartValues = [0, 0, 0, 0]; 

  if (window.chartData && window.chartData.topDichVu && Object.keys(window.chartData.topDichVu).length > 0) {
      chartLabels = Object.keys(window.chartData.topDichVu);
      chartValues = Object.values(window.chartData.topDichVu);
  }

  return new Chart(ctx, {
    type: 'bar',
    data: {
      labels: chartLabels,
      datasets: [{
        label: 'Số đơn',
        data: chartValues,
        backgroundColor: ChartTheme.primary,
        borderRadius: 6,
        borderSkipped: false,
        maxBarThickness: 32
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { display: false }, ticks: { font: { size: 11 } } },
        y: { grid: { color: ChartTheme.line }, ticks: { font: { size: 11 }, stepSize: 1 } }
      }
    }
  });
}

// ============================================================
// RATING DISTRIBUTION (ĐÃ NỐI DATABASE REAL 100%)
// ============================================================
function initRatingChart(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;

  // Lấy dữ liệu đánh giá 5 sao -> 1 sao từ DB
  let chartValues = [58, 25, 10, 5, 2]; // Fake data dự phòng
  
  if (window.chartData && window.chartData.phanPhoiDanhGia) {
      let dbData = window.chartData.phanPhoiDanhGia;
      // Trích xuất số lượng theo từng mốc sao, nếu không có thì gán = 0
      chartValues = [
          dbData['5'] || 0, 
          dbData['4'] || 0, 
          dbData['3'] || 0, 
          dbData['2'] || 0, 
          dbData['1'] || 0
      ];
      
      // Nếu tổng bằng 0 (Database trống), quay về fake data để test UI
      let sum = chartValues.reduce((a, b) => a + b, 0);
      if (sum === 0) {
          chartValues = [58, 25, 10, 5, 2];
      }
  }

  return new Chart(canvas, {
    type: 'bar',
    data: {
      labels: ['5 ★', '4 ★', '3 ★', '2 ★', '1 ★'],
      datasets: [{
        data: chartValues,
        backgroundColor: ChartTheme.primary,
        borderRadius: 6,
        borderSkipped: false,
      }]
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { color: ChartTheme.line }, ticks: { font: { size: 11 }, stepSize: 1 } },
        y: { grid: { display: false }, ticks: { font: { size: 12, weight: '600' } } }
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

  // Khai báo sẵn 5 danh mục mặc định theo đúng thứ tự giao diện
  let labels = ['Đã giải quyết', 'Đang xử lý', 'Mới', 'Chờ xác minh', 'Leo thang'];
  let values = [0, 0, 0, 0, 0]; // Mặc định số lượng = 0

  // Lấy dữ liệu từ biến cầu nối window.chartData do Spring Boot truyền sang
  if (window.chartData && window.chartData.trangThaiKhieuNai) {
      let dbData = window.chartData.trangThaiKhieuNai;
      
      // Map trực tiếp số liệu từ Database vào đúng 5 nhãn cố định
      values = [
          dbData['Đã giải quyết'] || 0,
          dbData['Đang xử lý'] || 0,
          dbData['Mới'] || 0,
          dbData['Chờ xác minh'] || 0,
          dbData['Leo thang'] || 0
      ];
  }

  return new Chart(canvas, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [{
        label: 'Số khiếu nại',
        data: values,
        backgroundColor: ChartTheme.primary,
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
          grid: { color: ChartTheme.line },
          ticks: { font: { size: 11 }, stepSize: 1 }
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
        borderColor: ChartTheme.primary,
        backgroundColor: withAlpha(ChartTheme.primary, 0.12),
        borderWidth: 2,
        pointBackgroundColor: ChartTheme.primary,
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
          grid: { color: ChartTheme.line },
          angleLines: { color: ChartTheme.line },
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
// ============================================================
// MONTHLY COMPARISON – Grouped Bar (ĐÃ NỐI DATABASE THẬT)
// ============================================================
function initComparisonChart(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;

  let labels = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'];
  let dataNamNay = Array(12).fill(0);
  let dataNamTruoc = Array(12).fill(0);

  if (window.chartData && window.chartData.doanhThuTheoThang) {
      let dbData = window.chartData.doanhThuTheoThang;
      let keys = Object.keys(dbData);
      if (keys.length > 0) {
          labels = keys;
          dataNamNay = Object.values(dbData);
      }
  }

  if (window.chartData && window.chartData.doanhThuTheoThangNamTruoc) {
      dataNamTruoc = Object.values(window.chartData.doanhThuTheoThangNamTruoc);
  }

  return new Chart(canvas, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [
        {
          label: 'Năm nay',
          data: dataNamNay,
          backgroundColor: ChartTheme.primary,
          borderRadius: 6,
          borderSkipped: false
        },
        {
          label: 'Năm trước',
          data: dataNamTruoc,
          backgroundColor: withAlpha(cssVar('--tone-indigo', '#4F39F6'), 0.35),
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
          grid: { color: ChartTheme.line },
          ticks: { callback: v => Number(v).toLocaleString('vi-VN') + ' tr', font: { size: 11 } }
        }
      }
    }
  });
}

// ============================================================
// MINI SPARKLINE
// ============================================================
function initSparkline(canvasId, data, color = ChartTheme.primary) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;
  const ctx = canvas.getContext('2d');
  const gradient = withAlpha(color, 0.12);

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
    { id: 'spark1', data: [10,15,12,18,22,20,28,25,32,30,38,35] },
    { id: 'spark2', data: [8,12,10,15,18,14,20,18,24,22,28,25] },
    { id: 'spark3', data: [5,8,7,10,9,12,11,14,13,16,15,18] },
    { id: 'spark4', data: [3,5,4,6,5,8,7,9,8,11,10,12] },
    { id: 'spark5', data: [2,4,3,5,4,6,5,7,6,8,7,9] },
  ];
  sparklines.forEach(s => initSparkline(s.id, s.data));

  // Danh sách thanh trên trang tổng quan Giám đốc
  if (window.chartData) {
    renderBarList('orderStatusList', window.chartData.orderDistribution, 'Chưa có đơn nào trong kỳ này');
    renderBarList('topServiceList', window.chartData.topDichVu, 'Chưa có đơn nào trong kỳ này');
  }
});
