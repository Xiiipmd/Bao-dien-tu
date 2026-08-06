# Danh mục API của dự án Báo điện tử

## 1. Phạm vi và quy ước

- Bản cũ dùng để đối chiếu: `D:\TMDT\backend`.
- Bản hiện tại: `D:\TMDT\6-7-TMDT-Backend-latest`.
- **API cũ**: endpoint đã tồn tại trong bản cũ và vẫn được giữ trong bản hiện tại.
- **API mới**: endpoint chỉ xuất hiện trong bản hiện tại.
- Tổng cộng bản hiện tại có **50 API**, gồm **11 API cũ** và **39 API mới**.
- Tiền tố chung: `/api`.
- API có ghi **Đăng nhập** yêu cầu header `Authorization: Bearer <jwtToken>`.
- Các kiểu vai trò trong hệ thống: `ADMIN`, `AUTHOR`, `CENSOR`, `VIP` và người dùng thông thường.
- Lỗi nghiệp vụ thường trả JSON có dạng:

```json
{
  "status": 400,
  "message": "Nội dung lỗi",
  "timestamp": "2026-07-31T10:00:00"
}
```

## 2. API cũ được giữ lại

### 2.1. Xác thực

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `POST` | `/api/auth/register` | Công khai | Body: `email`, `name`, `password`, `confirmation`, `role` | `200`, không có body | Đăng ký tài khoản mới; kiểm tra email, xác nhận mật khẩu và dữ liệu trùng lặp. |
| `POST` | `/api/auth/login` | Công khai | Body: `email`, `password` | `jwtToken`, `role`, `name`, `vipExpiryDate`, `freeArticlesLeft` | Xác thực người dùng và cấp JWT để gọi các API được bảo vệ. |

### 2.2. Gói VIP công khai

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/vip-packages` | Công khai | Không | Danh sách gói gồm `id`, `name`, `durationDays`, `price`, `description` | Lấy toàn bộ gói VIP đang cung cấp để hiển thị cho khách hàng. |
| `GET` | `/api/vip-packages/{id}` | Công khai | Path: `id` | Chi tiết một gói VIP | Lấy thông tin gói VIP theo mã gói. |

### 2.3. Quản trị gói VIP

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/admin/vip-packages` | `ADMIN` | Không | Danh sách gói, có thêm `discountPercent` | Lấy toàn bộ gói VIP phục vụ màn hình quản trị. |
| `GET` | `/api/admin/vip-packages/{id}` | `ADMIN` | Path: `id` | Chi tiết gói VIP | Lấy một gói VIP để xem hoặc đưa vào biểu mẫu chỉnh sửa. |
| `PUT` | `/api/admin/vip-packages/{id}` | `ADMIN` | Body: `name`, `durationDays`, `price`, `discountPercent`, `description` | Gói VIP sau khi cập nhật | Cập nhật tên, thời hạn, giá, phần trăm giảm giá và mô tả của gói. `durationDays` và `price` phải dương; giảm giá từ 0 đến 100. |

### 2.4. Thanh toán VNPay

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `POST` | `/api/transactions/create` | Đăng nhập | Body: `packageId` | `{ "paymentUrl": "..." }` | Tạo giao dịch mua gói VIP và trả URL để frontend chuyển người dùng sang cổng VNPay. |
| `GET` | `/api/transactions/vnpay-return` | Công khai | Các query parameter do VNPay gửi | Chuỗi thông báo thành công/thất bại | Điểm chuyển hướng người dùng về sau khi thanh toán. API này chỉ hiển thị kết quả, không phải nguồn xác nhận cập nhật giao dịch. |
| `GET` | `/api/transactions/vnpay-ipn` | Công khai/VNPay | Các query parameter và chữ ký VNPay | `{ "RspCode": "...", "Message": "..." }` | Nhận IPN từ VNPay, kiểm tra chữ ký và số tiền, cập nhật trạng thái giao dịch, kích hoạt thời hạn VIP. Đây là điểm xác nhận thanh toán chính thức. |
| `GET` | `/api/transactions/my` | Đăng nhập | Không | Danh sách `id`, `packageName`, `amount`, `status`, `paymentCode`, `createdAt` | Lấy lịch sử giao dịch của người dùng hiện tại. |

## 3. API mới

### 3.1. Đọc, tìm kiếm và khám phá bài viết

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/articles/{articleId}/preview` | Công khai | Path: `articleId` | Thông tin xem trước và `paywallRequired` | Xem trước bài viết, đặc biệt cho bài VIP; chỉ trả phần nội dung xem trước khi có paywall. |
| `GET` | `/api/articles/{articleId}/read` | Công khai, hỗ trợ đăng nhập | Path: `articleId`; cookie `bdt_reader_key` được tạo khi cần | Nội dung bài, quyền VIP, trạng thái lượt đọc miễn phí và số lượt còn lại | Đọc toàn bộ bài viết theo quyền truy cập. Với khách chưa đăng nhập, hệ thống dùng cookie để đo giới hạn số bài miễn phí. |
| `GET` | `/api/articles/search` | Công khai | Query tùy chọn: `keyword`, `categoryId`, `authorName` | Danh sách kết quả bài viết | Tìm bài theo từ khóa, chuyên mục và tên tác giả; các điều kiện có thể kết hợp. |
| `GET` | `/api/articles/personalized` | Đăng nhập | Không | Danh sách bài viết | Đề xuất bài viết cá nhân hóa dựa trên chủ đề người dùng đã chọn. |
| `GET` | `/api/articles/home` | Công khai, hỗ trợ đăng nhập | Không | Danh sách bài viết | Lấy nội dung trang chủ; nếu có JWT thì có thể áp dụng sở thích người dùng, nếu không thì trả nội dung chung. |
| `GET` | `/api/articles/trending` | Công khai | Không | Danh sách bài viết | Lấy các bài nổi bật/xu hướng dựa trên dữ liệu lượt xem của hệ thống. |
| `GET` | `/api/articles/summary` | `VIP` hoặc `ADMIN` | Query bắt buộc: `articleId` | `ArticleDTO` chứa kết quả tóm tắt | Tạo/lấy bản tóm tắt bài viết bằng chức năng AI cho người dùng VIP hoặc quản trị viên. |
| `GET` | `/api/articles/{articleId}/download-pdf` | Đăng nhập | Path: `articleId` | File `application/pdf` dạng attachment | Xuất và tải nội dung bài viết dưới dạng PDF; tên file được backend thiết lập qua header `Content-Disposition`. |

### 3.2. Chuyên mục

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/categories` | Công khai | Không | Danh sách `{ id, name }` | Lấy danh sách chuyên mục để lọc bài, tạo bài, theo dõi chủ đề và thiết lập sở thích. |

### 3.3. Bình luận

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/articles/{articleId}/comments` | Công khai | Path: `articleId` | Danh sách bình luận | Lấy các bình luận của bài, gồm người bình luận, nội dung và thời điểm tạo. |
| `POST` | `/api/articles/{articleId}/comments` | Đăng nhập | Path: `articleId`; Body: `content` tối đa 2.000 ký tự | Bình luận vừa tạo | Thêm bình luận mới cho bài viết bằng tài khoản hiện tại. |

### 3.4. Tác giả và quản lý nội dung

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/staff/articles` | `AUTHOR` hoặc `ADMIN` | Query tùy chọn: `q` | Danh sách `ArticleDTO` | Lấy danh sách bài mà nhân sự hiện tại có quyền quản lý; hỗ trợ tìm theo từ khóa. |
| `GET` | `/api/staff/articles/{articleId}` | `AUTHOR` hoặc `ADMIN` | Path: `articleId` | Chi tiết `ArticleDTO` | Lấy dữ liệu đầy đủ của một bài để xem hoặc chỉnh sửa, có kiểm tra quyền sở hữu/quản lý ở service. |
| `POST` | `/api/staff/articles/create` | `AUTHOR` | Body: `authorId`, `coverImage`, `categoryId`, `title`, `sapo`, `content`, `type` | `201`, không có body | Tác giả tạo bài mới. `type` nhận `FREE` hoặc `VIP`; bài mới đi theo luồng trạng thái của hệ thống. |
| `PUT` | `/api/staff/articles/{articleId}` | `AUTHOR` hoặc `ADMIN` | Body: `coverImage`, `categoryId`, `title`, `sapo`, `content`, `type` | Bài viết sau cập nhật | Chỉnh sửa nội dung bài; tác giả chỉ được sửa bài thuộc quyền của mình, quản trị viên có quyền rộng hơn. |

### 3.5. Kiểm duyệt và ẩn/hiện bài

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/moderation/articles/pending` | `CENSOR` hoặc `ADMIN` | Không | Danh sách bài chờ duyệt | Lấy hàng đợi các bài đang chờ kiểm duyệt. |
| `GET` | `/api/moderation/articles/visibility` | `ADMIN` | Query tùy chọn: `q` | Danh sách bài | Lấy danh sách bài phục vụ quản lý trạng thái hiển thị; hỗ trợ tìm kiếm. |
| `GET` | `/api/moderation/articles/{articleId}` | `CENSOR` hoặc `ADMIN` | Path: `articleId` | Chi tiết bài chờ duyệt | Xem đầy đủ nội dung bài trước khi đưa ra quyết định duyệt hoặc từ chối. |
| `GET` | `/api/moderation/articles/{articleId}/visibility` | `ADMIN` | Path: `articleId` | Chi tiết bài | Xem chi tiết bài trong chức năng quản lý ẩn/hiện. |
| `POST` | `/api/moderation/articles/{articleId}/decision` | `CENSOR` hoặc `ADMIN` | Body: `approved`, `rejectionReason` | Bài sau kiểm duyệt | Duyệt hoặc từ chối bài. Khi từ chối, `rejectionReason` dùng để thông báo lý do cho tác giả. |
| `POST` | `/api/moderation/articles/{articleId}/hide` | `ADMIN` | Path: `articleId` | Bài sau cập nhật | Ẩn một bài khỏi khu vực công khai mà không xóa dữ liệu bài. |
| `POST` | `/api/moderation/articles/{articleId}/show` | `ADMIN` | Path: `articleId` | Bài sau cập nhật | Khôi phục hiển thị cho bài đang bị ẩn. |

### 3.6. Theo dõi tác giả/chủ đề

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/subscriptions/my` | Đăng nhập | Không | Danh sách `id`, `targetType`, `targetId`, `targetName` | Lấy danh sách tác giả hoặc chủ đề mà người dùng hiện tại đang theo dõi. |
| `POST` | `/api/subscriptions` | Đăng nhập | Body: `targetType`, `targetId` | `201` và thông tin theo dõi | Theo dõi một đối tượng. `targetType` nhận `AUTHOR` hoặc `CATEGORY`. |
| `DELETE` | `/api/subscriptions/{targetType}/{targetId}` | Đăng nhập | Path: `targetType`, `targetId` | `204`, không có body | Hủy theo dõi tác giả hoặc chủ đề tương ứng. |

### 3.7. Sở thích và thông báo

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/me/preferences` | Đăng nhập | Không | `selectedTopics`, `pushNotificationsEnabled` | Lấy các chủ đề quan tâm và trạng thái bật/tắt thông báo của người dùng. |
| `PUT` | `/api/me/preferences` | Đăng nhập | Body: `categoryIds`, `pushNotificationsEnabled` | Sở thích sau cập nhật | Thay thế danh sách chủ đề quan tâm và cấu hình nhận thông báo; dữ liệu này được dùng cho cá nhân hóa. |
| `GET` | `/api/me/notifications` | Đăng nhập | Không | Danh sách thông báo | Lấy thông báo tin mới, gồm bài liên quan, ảnh, chuyên mục, loại, tiêu đề, nội dung, trạng thái đã đọc và thời gian tạo. |
| `GET` | `/api/me/notifications/unread-count` | Đăng nhập | Không | `{ "count": số_lượng }` | Đếm nhanh số thông báo chưa đọc để hiển thị huy hiệu trên giao diện. |
| `PATCH` | `/api/me/notifications/{notificationId}/read` | Đăng nhập | Path: `notificationId` | `204`, không có body | Đánh dấu một thông báo thuộc người dùng hiện tại là đã đọc. |
| `PATCH` | `/api/me/notifications/read-all` | Đăng nhập | Không | `204`, không có body | Đánh dấu toàn bộ thông báo của người dùng là đã đọc. |

### 3.8. Tài khoản cá nhân

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/me/account` | Đăng nhập | Không | `id`, `fullName`, `email`, `role`, `vipExpiryDate`, `createdAt` | Lấy hồ sơ của tài khoản hiện tại. |
| `PUT` | `/api/me/account` | Đăng nhập | Body: `fullName`, `email` | Hồ sơ sau cập nhật | Cập nhật họ tên và email; email phải đúng định dạng và không được xung đột với tài khoản khác. |
| `PUT` | `/api/me/account/password` | Đăng nhập | Body: `currentPassword`, `newPassword`, `confirmation` | `204`, không có body | Đổi mật khẩu sau khi kiểm tra mật khẩu hiện tại và xác nhận mật khẩu mới; mật khẩu mới dài từ 8 đến 100 ký tự. |

### 3.9. Thống kê

| Method | Endpoint | Quyền | Dữ liệu vào | Kết quả | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/api/stats/article` | `AUTHOR` hoặc `ADMIN` | Query: `articleId`, `startDate`, `endDate`, `granularity` | `views`, `estimatedEarning`, `viewsByLevelOfGranularity` | Thống kê lượt xem và doanh thu ước tính của một bài trong khoảng thời gian, chia theo độ chi tiết yêu cầu. `startDate`, `endDate` dùng định dạng `Instant` ISO-8601. |
| `GET` | `/api/stats/author` | `AUTHOR` | Query: `authorId`, `startDate`, `endDate`, `groupBy` (mặc định `day`) | Tổng bài, lượt xem, doanh thu, người theo dõi, biểu đồ, top bài và thống kê chủ đề | Cung cấp dashboard hiệu quả nội dung cho tác giả. |
| `GET` | `/api/stats/admin/overview` | `ADMIN` | Query bắt buộc: `startDate`, `endDate`; tùy chọn: `authorId`, `categoryId`, `groupBy=day` | Tổng quan và dữ liệu biểu đồ/chi tiết | Thống kê tổng quan toàn hệ thống, có thể lọc theo tác giả hoặc chuyên mục. |
| `GET` | `/api/stats/admin/top` | `ADMIN` | Query: `startDate`, `endDate`; tùy chọn `targetType=author`, `sortBy=revenue`, `sortDirection=desc`, `limit=10` | Danh sách xếp hạng | Xếp hạng đối tượng như tác giả/chuyên mục theo doanh thu, lượt xem hoặc tiêu chí được hỗ trợ. |
| `GET` | `/api/stats/admin/authors` | `ADMIN` | Không | Danh sách `{ id, name }` | Lấy danh sách tác giả dạng tùy chọn để dùng trong bộ lọc thống kê quản trị. |

## 4. Tóm tắt theo nhóm chức năng

| Nhóm chức năng | API cũ | API mới | Tổng |
|---|---:|---:|---:|
| Xác thực | 2 | 0 | 2 |
| Gói VIP và quản trị gói | 5 | 0 | 5 |
| Thanh toán | 4 | 0 | 4 |
| Đọc, tìm kiếm, khám phá và tải bài | 0 | 8 | 8 |
| Chuyên mục | 0 | 1 | 1 |
| Bình luận | 0 | 2 | 2 |
| Tác giả/quản lý bài | 0 | 4 | 4 |
| Kiểm duyệt và ẩn/hiện | 0 | 7 | 7 |
| Theo dõi | 0 | 3 | 3 |
| Sở thích và thông báo | 0 | 6 | 6 |
| Tài khoản cá nhân | 0 | 3 | 3 |
| Thống kê | 0 | 5 | 5 |
| **Tổng cộng** | **11** | **39** | **50** |

## 5. Lưu ý kỹ thuật

1. Hai callback VNPay là API công khai vì được VNPay gọi trực tiếp; bảo mật dựa trên kiểm tra chữ ký VNPay.
2. `GET /api/articles/home` chấp nhận cả khách và người đã đăng nhập; JWT là tùy chọn.
3. `GET /api/articles/{id}/read` dùng cookie `bdt_reader_key` có thời hạn một năm để đo lượt đọc miễn phí của khách.
4. Tất cả API không được khai báo công khai trong `SecurityConfig` mặc định đều yêu cầu JWT.
5. Quyền chi tiết theo vai trò được kiểm tra bằng `@PreAuthorize`; một số kiểm tra sở hữu tài nguyên tiếp tục được thực hiện trong lớp service.
6. Repository gốc chưa có commit Git, vì vậy nhãn “cũ/mới” trong tài liệu được suy ra bằng cách so sánh trực tiếp controller của hai bản mã nguồn.
