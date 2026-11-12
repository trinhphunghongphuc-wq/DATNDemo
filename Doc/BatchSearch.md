# Tìm kiếm và phân trang lô hàng

## Áp dụng

Bản vá bổ sung trên code đã sửa khóa batch/proof và thông báo. Không có migration mới: không thêm cột, bảng hoặc sửa dữ liệu PostgreSQL. Dependency H2 mới chỉ phục vụ test, không dùng khi chạy backend thật.

Áp dụng cả backend và frontend rồi restart backend. Không áp dụng lại bản vá thông báo để cài tìm kiếm.

## Hành vi

- Ba trang Producer, Distributor và Retailer đều có ô tìm mã/tên lô, lọc trạng thái, tổng số kết quả và phân trang 10 lô/trang.
- Mặc định `createdAt DESC, id DESC`: lô mới nhất trước, ID giảm dần khi trùng thời gian.
- Tìm trên toàn bộ lô được phép xem, không chỉ các lô của trang đang mở. Không phân biệt hoa/thường; có phân biệt dấu tiếng Việt.
- Từ khóa được debounce 300 ms. Request cũ bị hủy và không được ghi đè kết quả mới.
- Đổi từ khóa/trạng thái hoặc xóa bộ lọc quay về trang đầu. Tạo lô mới cũng tải lại trang đầu, giữ bộ lọc đang chọn.
- Khi nhận lô làm trang cuối của bộ lọc hết dữ liệu, tự chuyển về trang hợp lệ.
- Giữ nguyên thao tác tạo lô, thêm record, cập nhật hạn dùng, nhận hàng và mở chi tiết.

## API

Tất cả request dùng Bearer token. Không nhận userId từ client; tài khoản do backend xác định từ JWT.

| Role | Endpoint | Phạm vi |
|---|---|---|
| PRODUCER | `GET /api/producer/batches` | Lô do chính Producer tạo |
| DISTRIBUTOR | `GET /api/distributor/batches` | Lô được gán cho Distributor |
| RETAILER | `GET /api/retailer/batches` | Lô được gán cho Retailer |

Ví dụ: `GET http://localhost:8080/api/distributor/batches?keyword=xo%C3%A0i&status=ASSIGNED_TO_DISTRIBUTOR&page=0&size=10`

Tham số: `keyword` tối đa 200 ký tự, `status` là mã BatchStatus hoặc để trống, `page` bắt đầu từ 0, `size` từ 1 đến 50 (mặc định 10). Frontend hiển thị trang bắt đầu từ 1.

Response ba endpoint chuyển từ mảng sang object:

```json
{
  "content": [],
  "number": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

Mỗi phần tử `content` có id, name, batchCode, status, createdAt, expiryDate, anchorStatus, merkleRoot, chainTxHash, recordCount. Script/Postman cũ phải đọc `response.content` thay vì coi toàn bộ response là mảng.

Backend lọc quyền/từ khóa/trạng thái rồi sắp xếp và phân trang bằng SQL. Số record được lấy bằng một truy vấn tổng hợp cho các ID trên trang; không tải toàn bộ collection record.

## Kiểm thử

1. Tạo hơn 10 lô cho một tài khoản; xác nhận có trang 2 và lô mới ở đầu trang 1.
2. Tìm mã hoặc tên của một lô vốn ở trang 2: phải tìm được ngay, giao diện quay về trang 1.
3. Tìm cùng từ khóa bằng token tài khoản khác: chỉ trả lô tài khoản đó được phép xem; totalElements cũng chỉ đếm phạm vi này.
4. Chọn trạng thái rồi gõ từ khóa: hai điều kiện phải cùng có hiệu lực. Xóa bộ lọc trở về danh sách mới nhất.
5. Gõ nhanh nhiều từ khóa: kết quả cuối cùng phải khớp từ khóa cuối.
6. Distributor lọc ASSIGNED_TO_DISTRIBUTOR rồi nhận lô: nút vẫn hoạt động, lô được loại khỏi bộ lọc và số kết quả cập nhật.
7. Gọi API với page=-1, size=1000 hoặc status không tồn tại: phải trả 400.

Test backend: `mvn test`; `BatchSearchServiceTest` dùng database H2 riêng, không sửa database thật và không chạy Flyway.
