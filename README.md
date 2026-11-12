# CineBook Backend

Backend cho CineBook, website lịch chiếu và đặt vé xem phim. Dự án cá nhân luyện system design: transaction, Kafka, AI, Kubernetes.

- Đặc tả: [docs/CineBook_SRS_v1.1.pdf](docs/CineBook_SRS_v1.1.pdf)
- Frontend: repo riêng `cinebook-frontend` (Next.js)

## Công nghệ
Spring Boot 4.1 (Java 21), PostgreSQL (pgvector), Redis, Apache Kafka, Flyway. Kiến trúc modular monolith.

## Cấu trúc repo
```
cinebook-backend/
├── src/main/java/com/cinebook/   code theo module (xem bên dưới)
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/             Flyway: V{n}__*.sql
├── docs/                         SRS
├── docker-compose.yml            Postgres, Redis, Kafka, Kafka UI, Mailpit
└── .env.example
```

## Package theo module nghiệp vụ
Mỗi module là một thư mục dưới `com.cinebook`, bên trong chia tầng:

| Package | Vai trò |
|---|---|
| `controller` | REST API public; `controller/admin` cho trang quản trị |
| `service` | **Interface** nghiệp vụ: "hợp đồng" công khai của module (method + Javadoc) |
| `service/impl` | Cài đặt `XxxServiceImpl` (`@Service`, `@Transactional`, xử lý code) |
| `repository` | Spring Data JPA |
| `entity` | JPA entity + enum trạng thái |
| `dto` | Request/response (Java record) |
| `mapper` | Entity ↔ DTO (MapStruct) |
| `event` | Kafka consumer của module |
| `job` | Job định kỳ (`@Scheduled` + ShedLock) |

| Module | Nội dung | SRS |
|---|---|---|
| `common` | config, exception (RFC 7807), outbox, idempotency, rate limit, phiên khách | 8.2 |
| `catalog` | Phim, thể loại, tỉnh/thành, đồng bộ TMDB | M01 |
| `cinema` | Hệ thống rạp, cụm rạp, phòng, ghế | M02 |
| `showtime` | Suất chiếu, sơ đồ ghế theo suất, realtime WebSocket | M02, M03 |
| `pricing` | Bảng giá | BR-11..13 |
| `booking` | Giữ ghế, booking, máy trạng thái, Saga, hết hạn | M03, BR-01..10 |
| `payment` | VNPay, IPN, hoàn tiền, đối soát | M05, BR-20..25 |
| `promotion` | Voucher, combo | BR-14, BR-18 |
| `ticket` | Vé QR, tra cứu, hủy vé bằng OTP, soát vé | M04, M08 |
| `notification` | Email | M06 |
| `analytics` | Bảng xếp hạng, dashboard | FR-901 |
| `waitingroom` | Phòng chờ ảo | BR-55, BR-56 |
| `assistant` | Trợ lý AI (tool calling, RAG) | M07 |
| `search` | Tìm kiếm không dấu | |
| `iam` | Tài khoản nội bộ (admin, manager, staff) | |
| `customer` | Tài khoản khách hàng: **làm cuối cùng** | M10 |
| `seed` | Sinh dữ liệu demo (profile `dev`) | 7.3 |

Quy ước:
- Module khác chỉ gọi nhau qua **interface** trong `service`, không dùng `service/impl` hay `repository` của module khác (sẽ kiểm tra bằng ArchUnit).
- Inject theo interface: `private final SeatHoldService seatHoldService;` (không inject `SeatHoldServiceImpl`).
- Thay đổi dữ liệu cần báo cho module khác thì ghi event qua `OutboxService` trong cùng transaction.
- Schema DB do Flyway quản lý, mỗi giai đoạn một file migration.

## Chạy local
```bash
cp .env.example .env              # điền TMDB_TOKEN, VNPay sandbox
docker compose up -d              # hạ tầng
./mvnw spring-boot:run
```
- API: http://localhost:8080, Swagger: http://localhost:8080/swagger-ui.html
- Adminer (quản lý DB): http://localhost:8081 (System: PostgreSQL, Server: `postgres`, user/pass/db: `cinebook`)
- Kafka UI: http://localhost:8090, Mailpit: http://localhost:8025
- Postgres: `localhost:5432`, Redis: `localhost:6380` (6380 để không trùng Redis Homebrew)
- CORS cho phép frontend tại http://localhost:3000

Cần JDK 21 (`brew install openjdk@21`). Maven không cần cài, dùng `./mvnw`.
