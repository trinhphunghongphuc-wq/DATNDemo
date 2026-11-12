# Thông báo lô hàng trực tiếp

## Hành vi

- Khi Producer tạo lô và phân công đối tác, tạo thông báo riêng cho đúng Distributor và Retailer được chỉ định. Thông báo Retailer lúc này là đang chờ giao, chưa phải hàng đã đến.
- Khi Distributor xác nhận giao hàng, tạo một thông báo cho đúng Retailer.
- Chuông ở sidebar, ngay trên khung tài khoản; badge đếm thông báo chưa đọc, tối đa hiển thị `99+`.
- Mở `/notifications` chỉ xem danh sách, chưa đánh dấu đọc. Bấm một thông báo sẽ đánh dấu riêng mục đó rồi mở đúng trang lô hàng.
- Trạng thái đọc nằm trong PostgreSQL, giữ qua reload và đăng nhập lại; hai tab cùng tài khoản được cập nhật qua SSE.
- Không tạo lại thông báo cho lô có từ trước migration; chỉ thông báo sự kiện mới.

## Chạy

1. Áp dụng trên nhánh `feature/phase3-evaluation`, nền commit `78e6711`.
2. Migration mới là `V7__user_notifications.sql`. Nếu dự án đã có V7 cho việc khác, đổi sang phiên bản trống tiếp theo trước lần chạy đầu.
3. Giữ các biến JWT/AES và dịch vụ hiện có. Restart backend để Flyway tạo bảng `notifications`.
4. Frontend dùng dependency hiện có, không cần cài thêm package. Chạy `npm run dev`.

## API

Tất cả API yêu cầu `Authorization: Bearer <token>` và role nghiệp vụ. Backend lấy người nhận từ tài khoản đang đăng nhập, không nhận `userId` do client cung cấp.

| Method | URL | Mục đích |
|---|---|---|
| GET | `/api/notifications?page=0&size=20` | Danh sách riêng của tài khoản; không đánh dấu đọc |
| GET | `/api/notifications/unread-count` | Số chưa đọc |
| PATCH | `/api/notifications/{id}/read` | Đánh dấu đúng một mục; lặp lại không trừ số hai lần |
| GET | `/api/notifications/stream` | SSE qua fetch với Bearer header, không đưa token lên URL |

Thông báo được lưu cùng transaction nghiệp vụ. Tín hiệu SSE chỉ gửi sau commit; transaction rollback không tạo thông báo giả. Event chỉ yêu cầu client tải lại dữ liệu, số chưa đọc luôn tính từ database. Reconnect, focus cửa sổ và đối soát định kỳ 30 giây giúp khôi phục khi mất kết nối.

Stream có heartbeat 20 giây, kiểm tra tài khoản còn bật, role và hạn token. Stream không dùng Servlet timeout 60 giây nữa; heartbeat chủ động đóng sạch và mở lại khoảng mỗi 15 phút qua JWT filter. Lỗi socket bị ngắt không được chuyển thành JSON ErrorResponse trong luồng SSE. Nếu dùng reverse proxy, tắt response buffering cho đường dẫn stream và cho phép timeout dài hơn heartbeat.

Đây là triển khai một backend instance. Nếu chạy nhiều instance, cần pub/sub chung để phân phối event ngay lập tức giữa các instance; tác vụ đối soát vẫn khôi phục số từ database.

## Kiểm thử thủ công

Dùng hai browser profile hoặc một cửa sổ thường và một ẩn danh để đăng nhập khác tài khoản; hai tab cùng profile dùng chung localStorage.

1. Distributor D đăng nhập. Producer P tạo lô mới và gán D. Badge D tăng, không cần reload; Distributor khác không nhận.
   Retailer R được chỉ định cũng nhận thông báo phân công ngay lúc tạo lô; Retailer khác không nhận. Chỉ hiện hành động xác nhận nhận hàng khi Distributor đã giao.
2. D mở trang thông báo: badge chưa giảm. D bấm xem mục mới: mở chi tiết lô, badge giảm một. Reload và đăng nhập lại vẫn giữ đã đọc.
3. Tạo thêm hai lô cho D. Badge tăng hai. Bấm một mục chỉ giảm một; bấm lại mục đã đọc không giảm nữa.
4. Hai tab D cùng mở thông báo, đánh dấu ở một tab: tab kia cập nhật. Dùng token người khác gọi PATCH vào ID thông báo D phải bị 403.
5. Distributor xác nhận giao hàng cho Retailer R: chỉ R nhận thông báo; bấm vào mở trang record lô để xác nhận hoặc từ chối nhận hàng.
6. Ngắt mạng, tạo sự kiện, nối lại: trang tự kết nối lại và tải số/danh sách từ database. Không nhân đôi thông báo.
7. Cho transaction tạo/giao lô thất bại: không có thông báo được lưu hoặc đẩy.

## Kiểm tra tự động

- Backend: `mvn test` (có `NotificationServiceTest`). Test IPFS cũ vẫn cần biến `RUN_IPFS_TEST=true` và IPFS local.
- Frontend: `npm run build`; lint các file thông báo và file tích hợp. Repo còn các lỗi lint cũ ở những trang khác.
- Parser SSE: `node --test tests/notificationStream.test.mjs` trong thư mục frontend.
