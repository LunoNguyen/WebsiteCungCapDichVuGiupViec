/* ==========================================================
   REALTIME-WORKER.JS – SharedWorker giữ MỘT kết nối SSE dùng chung cho mọi tab quản trị.

   Trình duyệt chỉ cho khoảng 6 kết nối đồng thời tới cùng một máy chủ (HTTP/1.1). Nếu mỗi tab tự mở
   một kết nối SSE thì mở nhiều tab sẽ làm các tab sau bị treo. Worker này mở một kết nối duy nhất
   rồi chuyển sự kiện cho tất cả các tab.
   ========================================================== */
var cacTab = [];
var nguon = null;

function guiTatCa(loai) {
  cacTab.forEach(function (port) {
    try { port.postMessage(loai); } catch (e) { /* tab đã đóng */ }
  });
}

function moKetNoi() {
  if (nguon) return;
  nguon = new EventSource('/realtime/stream');
  nguon.addEventListener('du-lieu-thay-doi', function () { guiTatCa('du-lieu-thay-doi'); });
  nguon.addEventListener('san-sang', function () { guiTatCa('san-sang'); });
  nguon.onerror = function () {
    // Bị từ chối (hết phiên đăng nhập): EventSource tự đóng, bỏ để tab mới có thể mở lại
    if (nguon && nguon.readyState === 2) { nguon = null; }
  };
}

onconnect = function (e) {
  var port = e.ports[0];
  cacTab.push(port);
  port.onmessage = function (msg) {
    if (msg.data === 'dong') {
      cacTab = cacTab.filter(function (p) { return p !== port; });
      if (cacTab.length === 0 && nguon) { nguon.close(); nguon = null; }
    }
  };
  port.start();
  moKetNoi();
};
