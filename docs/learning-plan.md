# Lộ trình học lại codebase

Mục tiêu: hiểu toàn bộ code hiện có (do Codex viết), lấy lại quyền kiểm soát, rồi tự viết các phần còn lại. Tài liệu này không chứa code. Chỉ có việc cần làm, từ khoá cần tra và câu hỏi cần trả lời được.

Người học: chưa từng làm Spring Boot, muốn lấy Java làm tech stack chính, chưa dùng debugger.

## 1. Hiện trạng (2026-11-02)

| Tính năng | Trạng thái |
| --- | --- |
| `GET /api/auth/csrf` | Có code và endpoint |
| `POST /api/auth/register` | Có code và endpoint |
| `POST /api/auth/login` | Có code và endpoint |
| `GET /api/auth/me` | Có code và endpoint |
| Refresh token | Mới có `RefreshService`. Class này chưa được đăng ký làm bean, chưa có endpoint, chưa ai gọi |
| Logout | Chưa có |
| Tenancy (Organization, Membership) | Có domain, entity và bảng DB. Chưa có service và API |
| Test | Mới có test cho domain model và mapping |

## 2. Lõi và lớp gia cố

Khoảng một nửa code là lớp gia cố bảo mật, không thuộc luồng chính. Học phần lõi trước. Lớp gia cố để đến khi luồng chính đã rõ.

**Lõi (bắt buộc hiểu):**

- `identity/web/`: `AuthController`, các DTO
- `identity/application/`: `RegisterService`, `RegistrationPolicy`, `LoginService`, `RefreshService`, thư mục `port/`
- `identity/domain/`: `User`, `AuthSession`, `RefreshToken`
- `identity/infrastructure/persistence/`: các adapter, các entity, `SpringDataUserRepository`
- `src/main/resources/db/migration/`: V1, V2, V3
- `identity/infrastructure/configuration/`: `SecurityConfiguration`, `JwtConfiguration`, `IdentityConfiguration`, `AuthProperties`
- `identity/infrastructure/security/`: `SessionJwtAuthenticationConverter`, `JwtAccessTokenIssuer`, `SecureRefreshTokenGenerator`, `SpringPasswordHasher`
- `common/Exception/GlobalExceptionHandler`

**Lớp gia cố (để sau):**

- `common/Exception/ExceptionHandlingFilter`, `ApiErrorWriter`
- `WebConfiguration`: ép request JSON phải đúng chuẩn
- `RegistrationRateLimitFilter`
- Phần claim validator tự viết trong `JwtConfiguration`
- Việc kiểm tra UUIDv7 rải rác ở nhiều nơi
- `security/common-passwords.txt`

## 3. Cách học mỗi giai đoạn

1. **Đoán trước.** Trước khi mở code, viết ra bạn nghĩ luồng chạy thế nào.
2. **Debug từng bước.** Đặt breakpoint rồi đi qua từng dòng. Đừng chỉ đọc.
3. **Tự ghi chú.** Viết lại luồng bằng lời của bạn vào `docs/notes/` (do bạn tự viết).
4. **Phá để hiểu.** Bỏ một annotation hoặc đổi một config, đoán xem cái gì sẽ hỏng, rồi chạy để kiểm chứng. Sau đó `git restore` để trả lại.
5. **Kiểm tra.** Trả lời các câu hỏi của giai đoạn. Claude nhận xét. Qua được thì mới sang giai đoạn sau.
6. **Xoá và viết lại** (khi đã tự tin). Trên một branch nháp, xoá một class đã học, viết lại theo ghi chú, rồi so với bản gốc.

## 4. Các giai đoạn

### Giai đoạn 0: Nền tảng Spring Boot, học ngay trên project này

Lý do: chưa có nền Spring mà đọc project này thì sẽ không biết đâu là "phép màu" của framework, đâu là code của project. Giai đoạn này chỉ học phần nền, chưa đi sâu vào luồng auth.

Chuẩn bị:

- [ ] Commit hoặc stash hết thay đổi đang dở, để working tree sạch. Như vậy sau mỗi thí nghiệm phá code chỉ cần một lệnh `git restore` là về lại như cũ.
- [ ] Nên tạo một branch riêng cho thí nghiệm, ví dụ `learn/phase-0`. Không bao giờ commit code thí nghiệm vào branch feature.

Chạy và tập debugger:

- [ ] Chạy app bằng `F5` với cấu hình "Spring Boot - Local". Postgres trong Docker và cặp khoá JWT trên máy đã sẵn sàng.
- [ ] Mở *Spring Boot Dashboard* ở thanh bên trái VS Code khi app đang chạy. Xem hai mục *Beans* và *Endpoint Mappings*.
- [ ] Đặt breakpoint trong `AuthController.csrf` (endpoint đơn giản nhất). Mở `http://localhost:8080/api/auth/csrf` bằng trình duyệt.
- [ ] Khi dừng ở breakpoint, tìm hiểu ba khu vực *Variables*, *Call Stack*, và các nút *Step Over / Step Into / Continue*. Đọc tên các class ở cuối Call Stack.

Tra từ khoá: *Spring IoC container*, *Dependency Injection qua constructor*, *`@Component` và `@Bean`*, *Spring Boot auto-configuration*, *Externalized Configuration*, *Profiles*. Nguồn: Spring Boot Reference, các mục *Spring Beans and Dependency Injection*, *Externalized Configuration*, *Profiles*.

Phá để hiểu. Mỗi lần chỉ thử một việc, đoán kết quả trước khi chạy, xong thì `git restore`:

- [ ] Bỏ `@Service` khỏi `RegisterService`, rồi khởi động app.
- [ ] Bỏ method `@Bean loginService` trong `IdentityConfiguration`, rồi khởi động app.
- [ ] Bỏ `@RestController` khỏi `AuthController`, rồi gọi `/api/auth/csrf`.
- [ ] Chạy app không bật profile `local`.
- [ ] Đổi `app.auth.access-token-lifetime` thành một giá trị lớn hơn `absolute-lifetime`, rồi khởi động app.

Phải trả lời được:

- Ai tạo ra object của các class có `@Service` hay `@RestController`? Tạo lúc nào?
- Constructor của `AuthController` có 6 tham số. Ai truyền vào? Mỗi tham số đến từ đâu?
- Có mấy cách để một class trở thành bean trong project này? Cho ví dụ mỗi cách.
- Profile `local` khác profile mặc định ở điểm nào? Vì sao chạy thiếu profile này thì app lỗi?
- Giá trị trong `application.properties` đi vào code Java bằng những đường nào?

### Giai đoạn 1: Chạy app trước khi đọc code

- [ ] Bật Postgres bằng `docker-compose.yaml`, chạy app bằng cấu hình debug trong VS Code.
- [ ] Gọi lần lượt csrf, register, login, me theo `docs/registration-api.md`. Login và me thì tự suy ra cách gọi.
- [ ] Mở DB xem các bảng `users`, `auth_sessions`, `refresh_tokens` sau mỗi bước.

Phải trả lời được:

- Sau khi login, client nhận về những gì và nhận ở đâu (body, header, cookie)?
- Login 2 lần với cùng một user thì DB thay đổi thế nào?
- Gọi `/me` không kèm token thì nhận được gì? Kèm token sai thì sao?

### Giai đoạn 2: Bản đồ kiến trúc

- [ ] Vẽ tay sơ đồ các package và chiều phụ thuộc giữa chúng.
- [ ] Tra từ khoá: *Hexagonal architecture*, *Ports and Adapters*, *Dependency Inversion Principle*.

Phải trả lời được:

- Tại sao `UserRepository` là interface nằm trong `application/port`, còn class implement lại nằm trong `infrastructure`?
- Mỗi interface trong `port/` do class nào implement?
- `RegisterService` dùng `@Service`, còn `LoginService` được tạo bằng `@Bean` trong `IdentityConfiguration`. Vì sao có hai cách?
- `domain/` có phụ thuộc vào Spring hay JPA không? Kiểm chứng bằng cách xem phần import.

### Giai đoạn 3: Dữ liệu

- [ ] Đọc theo thứ tự: migration V1 → V3, rồi domain `User`, `AuthSession`, `RefreshToken`, rồi các entity tương ứng.
- [ ] Tra từ khoá: *Flyway versioned migration*, *JPA Entity và EntityManager*, *persistence context*, *PostgreSQL CHECK constraint*, *UUIDv7*.

Phải trả lời được:

- Vì sao tồn tại cả `User` lẫn `UserEntity`? Hai class chuyển đổi qua lại ở đâu?
- Ràng buộc nào được kiểm tra ở cả domain lẫn DB? Kiểm tra hai lần có lợi gì, hại gì?
- Vì sao bảng `refresh_tokens` lưu `token_hash` mà không lưu token gốc?
- Các domain class đều immutable. Vậy khi "thay đổi" một session thì chuyện gì xảy ra?

### Giai đoạn 4: Luồng Register

- [ ] Đặt breakpoint ở `AuthController.register`, `RegisterService.register` và `UserRepositoryAdapter.create`, rồi debug từng bước.
- [ ] Đi thử các nhánh lỗi: email trùng, mật khẩu yếu, thiếu CSRF token. Với mỗi lỗi, lần xem exception sinh ra ở đâu và biến thành JSON ở đâu.
- [ ] Tra từ khoá: *Spring Security filter chain*, *DispatcherServlet*, *Bean Validation `@Valid`*, *`@RestControllerAdvice`*, *CSRF double-submit cookie*.

Phải trả lời được:

- Vẽ đường đi đầy đủ của request: từ lúc vào server, qua các filter, tới controller, rồi quay về.
- Vì sao `UserRepositoryAdapter.create` gọi flush ngay?
- Hai request đăng ký cùng một email đến cùng lúc thì chuyện gì xảy ra?

### Giai đoạn 5: Luồng Login

- [ ] Debug `LoginService.login`, cả trường hợp đúng mật khẩu và sai mật khẩu.
- [ ] Tra từ khoá: *user enumeration*, *timing attack*, *`TransactionTemplate` và `@Transactional`*, *JWT*, *RS256 và cặp khoá RSA*, *cookie HttpOnly / Secure / SameSite*.

Phải trả lời được:

- `dummyPasswordHash` dùng để làm gì?
- Vì sao `createLogin` đọc user từ DB lần thứ hai?
- Phần nào nằm trong transaction, phần nào nằm ngoài? Vì sao chia như vậy?
- Access token và refresh token khác nhau thế nào về chỗ lưu, thời hạn và mục đích?
- Endpoint login được permitAll, vậy Spring Security có tham gia xác thực mật khẩu không?

### Giai đoạn 6: Request đã đăng nhập (`/me`)

- [ ] Debug một request `/me` có token, đặt breakpoint trong `SessionJwtAuthenticationConverter`.
- [ ] Tra từ khoá: *Spring Security OAuth2 Resource Server JWT*, *JwtDecoder*, *JwtAuthenticationConverter*, *SecurityContext*, *`@AuthenticationPrincipal`*.

Phải trả lời được:

- Ai đọc header `Authorization`? Ai kiểm tra chữ ký?
- JWT đã hợp lệ về chữ ký, sao vẫn phải query DB? Như vậy hệ thống còn "stateless" không?
- Idle timeout được gia hạn ở đâu?

### Giai đoạn 7: Refresh (khó nhất, đọc kỹ)

- [ ] Đọc `RefreshService` cùng với `AuthSessionRepositoryAdapter` và `RefreshTokenRepositoryAdapter`.
- [ ] Tra từ khoá: *refresh token rotation*, *refresh token reuse detection*, *pessimistic locking / SELECT FOR UPDATE*, *transaction propagation (REQUIRED, MANDATORY)*, *rollback rules*.

Phải trả lời được:

- Một refresh token đã dùng bị gửi lại thì chuyện gì xảy ra? Vì sao lại thiết kế như vậy?
- Hai request refresh cùng một token đến cùng lúc thì sao? Khoá nằm ở đâu?
- Vì sao có những method repository bắt buộc phải được gọi bên trong transaction?

### Giai đoạn 8: Tự viết code

Từ giai đoạn này, mọi dòng code mới là của bạn. Claude chỉ hỏi, hướng dẫn và review.

- [ ] Đăng ký `RefreshService` làm bean và viết endpoint `POST /api/auth/refresh`.
- [ ] Logout: thu hồi session, xoá cookie.
- [ ] Test tích hợp cho login, refresh và logout.
- [ ] Tenancy: tạo organization, membership, phân quyền theo role (xem `docs/authentication-authorization.md`).
- [ ] Quay lại đọc lớp gia cố ở mục 2.

## 5. Tài liệu tham khảo

- spring.io/guides: các guide nhập môn
- Spring Boot Reference Documentation
- Spring Security Reference, các mục *Architecture* và *OAuth2 Resource Server → JWT*
- Spring Framework Reference, mục *Transaction Management*
- Jakarta Persistence (JPA) specification / Hibernate ORM User Guide
- Flyway documentation
- OWASP Cheat Sheet Series: *Password Storage*, *Session Management*, *JSON Web Token*, *CSRF Prevention*
- Các docs có sẵn trong `docs/` (lưu ý: có chứa code mẫu do Codex viết)

## 6. Nhật ký tiến độ

| Ngày | Giai đoạn | Ghi chú |
| --- | --- | --- |
| | | |
