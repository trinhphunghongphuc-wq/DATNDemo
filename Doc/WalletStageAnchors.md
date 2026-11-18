# Anchor theo ví được phân công trên từng lô

ChainTrack dùng một registry V2 có ba địa chỉ cố định cho mỗi lô. Admin đăng ký ánh xạ từ các tài khoản Producer, Distributor và Retailer đã được phân công. Registry kiểm tra chữ ký EIP-712 của đúng ví phụ trách trước khi ghi root. Admin vẫn gửi giao dịch và trả gas qua ví backend. Ví tham gia chỉ ký dữ liệu, không gửi giao dịch.

## Cài đặt với Hardhat và Spring Boot

1. Áp bản vá lên nhánh đã có tính năng hoàn trả V8. Flyway cập nhật thêm V9 khi chạy backend.
2. Giữ node Ethereum đang chứa dữ liệu cũ. Deploy thêm registry V2 trên cùng mạng. Nếu thử trên mạng rỗng, dùng một cơ sở dữ liệu mới.
3. Từ thư mục gốc repo, chạy PowerShell:

```powershell
cd DemoDA\SmartContract
npm ci
npx hardhat run scripts/deploy-wallet-registry.js
```

Script kết nối `localhost` tại `http://127.0.0.1:8545` và in địa chỉ mới cùng địa chỉ owner. Với Hardhat mặc định, script deploy từ tài khoản đầu tiên. Ví cấu hình tại `blockchain.private.key` của backend cần là owner của V2 để gán ví. Dùng các ví khác cho ba bên.

Thêm biến môi trường `BLOCKCHAIN_WALLET_CONTRACT_ADDRESS` bằng địa chỉ V2 được in ra vào cấu hình chạy Spring Boot trong IntelliJ. Giữ `blockchain.contract.address` trỏ tới địa chỉ V1 thực tế. Khởi động lại backend và frontend. Địa chỉ V2 để trống thì lô chưa gán ví dùng luồng cũ.

## Thử một lô chưa anchor

1. Trên từng tài khoản Producer, Distributor và Retailer của lô, mở **Ví và ký anchor**, chọn **Kết nối và lưu ví**. Dùng ba địa chỉ Ethereum khác nhau trên ví trình duyệt. Hệ thống kiểm tra quyền giữ khóa khi ký root. Có thể đổi địa chỉ đã lưu cho các lô chưa gán.
2. Admin mở **Lô hàng & Anchor**, chọn lô và chọn **Gán ví ba bên lên Ethereum**. Contract lưu một lần cho lô. Ví trên lô đã gán không đổi theo ví mới trong hồ sơ tài khoản.
3. Khi từng khâu có root cần chốt, bên phụ trách mở **Ví và ký anchor**, nhập ID lô, kiểm tra stage và root rồi ký. Retailer phải đóng ghi nhận trước khi ký. Chữ ký ràng buộc với ID lô, stage, root, thời hạn, chain ID và địa chỉ registry. Chữ ký có hạn một giờ. Nếu bổ sung bản ghi làm đổi root hoặc để quá hạn thì ký lại.
4. Admin chọn **Cập nhật chữ ký** rồi gửi giao dịch theo thứ tự Producer → Distributor → Retailer. Màn hình hiển thị ví đã ký bên cạnh giao dịch. Lịch sử xác minh cùng API đọc root sẽ chọn registry của lô.
5. Lô đã có anchor ở V1 tiếp tục anchor các khâu còn lại và xác minh tại V1. Có thể thử cả hai lô cạnh nhau mà không cần ghi lại root cũ.

Registry tin vào owner khi đăng ký ánh xạ ban đầu. Sau khi gán, owner không thể sửa ánh xạ hoặc anchor bằng hàm cũ. Giao dịch ghi root cần chữ ký của đúng ví đã gán. Trường hợp mất khóa của một ví trên lô đã gán chưa có luồng phục hồi. Các nghiệp vụ giao nhận, hoàn trả, lưu IPFS và phân quyền JWT vẫn sử dụng cơ chế hiện tại.

Thuật ngữ khi báo cáo: **chữ ký phê duyệt root thuộc về bên phụ trách, giao dịch Ethereum do backend chuyển tiếp**. Trường `approvingWallet` và sự kiện `StageApprovalUsed` thể hiện người ký, còn `anchoredBy` trong registry thể hiện người gửi giao dịch.
