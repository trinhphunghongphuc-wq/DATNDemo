# Hoàn trả lô bị từ chối khi bàn giao

Bản vá dành cho nhánh `feature/phase3-evaluation`, tại commit `68bf820` hoặc phiên bản tương thích đã có thông báo realtime và migration V7. Khi chạy backend, Flyway tự áp dụng V8. PostgreSQL và IPFS cần chạy như ở bản trước. Luồng hoàn trả này không yêu cầu deploy lại smart contract.

## Chạy thử bằng giao diện

1. Tạo lô **100 kg** bằng tài khoản Producer, chọn Distributor và Retailer đã liên kết. Dùng luồng thông thường để Distributor xác nhận nhận và giao lô. Xe có thể là xe thử nghiệm riêng của cùng Distributor.
2. Vào tài khoản Retailer, mở lô đang chờ nhận. Chọn **Từ chối nhận và trả toàn bộ lô**, nhập lý do và tải ảnh bằng chứng lên. Producer nhận thông báo dẫn thẳng tới **Hồ sơ hoàn trả**.
3. Producer mở hồ sơ, xem ảnh và bấm **Duyệt chuyến trả toàn bộ lô**. Lúc này Distributor mới nhận thông báo để tạo chuyến về. Bản này giữ quy tắc trả toàn bộ tại lần giao bị từ chối. Việc chia khối lượng được thực hiện sau khi kiểm chứng.
4. Distributor mở thông báo, chọn xe đã đăng ký public key Ed25519, nhập tên và địa chỉ kho cách ly, xác nhận lượng thực giao lên xe. Muốn demo IoT, đăng ký chìa khóa cho xe **trước khi** tạo chuyến như phần bên dưới.
5. Trong lúc chuyến đang chạy, gửi vài điểm GPS/nhiệt độ/độ ẩm. Bấm **Tải điểm mới** để xem. Khi tới kho, Distributor nhập số cân thực nhận, tình trạng, lý do chênh lệch nếu có và ảnh nhập kho.
6. Producer ghi tên người kiểm chứng, biên bản và ảnh, rồi nhập phương án cho đúng tổng khối lượng thực nhận. Ví dụ thực nhận **98 kg** thì có thể chuyển về Producer **50 kg**, tiêu hủy **38 kg**, giao lại **10 kg**.
7. Producer xác nhận đã nhận phần chuyển về. Distributor xác nhận phần tiêu hủy đã được xử lý và phần giao lại đã được tách sang lô con. Mỗi xác nhận cần bằng chứng và đúng khối lượng đã duyệt. Lô con được phân công cho các đối tác của lô gốc và tiếp tục quy trình giao bình thường. Producer chỉ đóng hồ sơ sau khi hoàn tất mọi phương án có khối lượng dương.

Ảnh bằng chứng hiển thị qua IPFS gateway. Mặc định giao diện dùng `http://127.0.0.1:8081/ipfs`. Nếu gateway khác, đặt `VITE_RETURN_IPFS_GATEWAY` trong `DemoDA/FE/frontend/.env.local` và khởi động lại Vite. Ảnh được lưu theo cơ chế upload IPFS hiện có, vì vậy chỉ dùng ảnh thử nghiệm không chứa thông tin nhạy cảm.

## Gửi dữ liệu IoT mô phỏng trên Windows

Dùng Node.js 20+ và tài khoản **Distributor**. Chọn một xe riêng để chạy demo và lấy ID từ danh sách xe. Đặt token của tài khoản đó (đang đăng nhập) trong terminal cục bộ, không đưa vào commit. API demo đăng ký khóa dùng cấu hình hiện có `app.demo-device.enabled=true`.

Tại thư mục gốc repo, chạy PowerShell:

```powershell
$env:CHAINTRACK_TOKEN = "<JWT của Distributor>"
$env:CHAINTRACK_API = "http://localhost:8080/api"
$keyFile = Join-Path $env:TEMP "chaintrack-return-demo.pem"

# Chạy một lần, trước khi tạo chuyến. Ví dụ xe ID 1.
node .\DemoDA\FE\frontend\scripts\return-device.mjs register 1 $keyFile

# Chạy sau khi tạo chuyến. Ví dụ batch ID 25.
node .\DemoDA\FE\frontend\scripts\return-device.mjs send 25 $keyFile 11.9404 108.4583 8.5 78
node .\DemoDA\FE\frontend\scripts\return-device.mjs send 25 $keyFile 11.9510 108.4690 9.3 80
```

Thay ID bằng dữ liệu thực tế. Dùng tên file khác cho lần đăng ký mới. Private key nằm trong file cục bộ. Script gửi public key khi đăng ký và gửi bản tin đã ký khi đo. Sequence dùng chung theo xe, có kiểm tra đúng batch, chuyến và xe. Chuyến demo đi và chuyến về không dùng cùng xe đồng thời. Để demo sự cố cảm biến, nhập một giá trị mô phỏng bị lệch so với số đo độc lập ghi trong biên bản. Giá trị vẫn có chữ ký hợp lệ.

## Phạm vi của bản vá

- Producer dùng tài khoản sở hữu lô để ghi biên bản cho người đi kiểm chứng. Phiên bản này chưa có tài khoản nhân viên riêng hoặc luồng ủy quyền kiểm chứng.
- Distributor chỉ định kho bằng tên và địa chỉ trên từng chuyến. Chưa có danh mục kho dùng chung hay quản lý vị trí trong kho.
- Hồ sơ mới được tạo khi Retailer từ chối một lô có Producer sở hữu và Distributor được phân công. Các lô bị từ chối trước V8 vẫn dùng màn hình nhận hàng trả cũ của Distributor.
- Nhật ký hoàn trả và dữ liệu IoT chuyến về nằm trong bảng riêng tại PostgreSQL. Bản vá không anchor nhật ký hoàn trả lên blockchain và không thêm các sự kiện này vào cây Merkle cũ. Bản ghi từ chối gốc vẫn đi theo cơ chế Merkle hiện có. Lô con có bản ghi liên kết với lô gốc và cây Merkle riêng.
