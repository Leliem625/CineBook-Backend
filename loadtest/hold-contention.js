// Bài thử tải 1: nhiều người cùng tranh một nhóm ít ghế. Mục tiêu: KHÔNG BAO GIỜ giữ trùng một ghế (oversell).
//
//   SHOWTIME=<id suất chiếu RIÊNG> SEATS=<id ghế, cách nhau dấu phẩy> k6 run loadtest/hold-contention.js
//
// Chạy backend ở cổng 8089. Mỗi người dùng ảo (VU) có cookie riêng nên là một phiên khách riêng.
//
// Hai chế độ:
//   mặc định          : VUS người, mỗi người bấm 1 lần cùng lúc (kiểm tra oversell, chạy < 1 giây)
//   DURATION=30s      : VUS người liên tục bấm trong 30 giây (để THEO DÕI: độ trễ, kết nối DB...)
//
// Theo dõi trực tiếp: K6_WEB_DASHBOARD=true k6 run ...  rồi mở http://localhost:5665
// Lưu báo cáo HTML : K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT=report.html k6 run ...
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const SHOWTIME = Number(__ENV.SHOWTIME);
const SEATS = (__ENV.SEATS || '').split(',').filter(Boolean).map(Number);
const VUS = Number(__ENV.VUS || 200);
const BASE = __ENV.BASE || 'http://localhost:8089/api/v1';

const won = new Counter('seat_won');   // giữ ghế thành công (201)
const lost = new Counter('seat_lost'); // thua vì ghế đã có người (409)

const DURATION = __ENV.DURATION;
export const options = {
  scenarios: {
    stampede: DURATION
      ? { executor: 'constant-vus', vus: VUS, duration: DURATION }
      : { executor: 'per-vu-iterations', vus: VUS, iterations: 1, maxDuration: '60s' },
  },
  thresholds: {
    http_req_failed: ['rate<0.001'],            // gần như không có lỗi mạng
    checks: ['rate==1'],                        // mọi phản hồi phải là 201 hoặc 409 (không có 500)
  },
};

export default function () {
  const seat = SEATS[Math.floor(Math.random() * SEATS.length)];
  const res = http.post(`${BASE}/bookings/hold`,
    JSON.stringify({ showtimeId: SHOWTIME, seatIds: [seat],
      customerName: 'Load Test', email: 'load@test.local', phone: '0912345678' }),
    { headers: { 'Content-Type': 'application/json' }, responseCallback: http.expectedStatuses(201, 409) });
  if (res.status === 201) won.add(1);
  else if (res.status === 409) lost.add(1);
  check(res, { 'chỉ 201 hoặc 409': (r) => r.status === 201 || r.status === 409 });
}
