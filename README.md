# Thực tập tốt nghiệp

## Quy tắc làm việc nhóm
1. Nhánh main dùng để chứa mã nguồn hoàn thiện.
2. Nhánh dev chứa code được merge từ feature vào, test code.
3. Khi đẩy 1 tính năng mới lên Github: Phải đặt tên nhánh theo format `feature/<Tên tính năng bằng tiếng Anh>`.
4. Mỗi khi bắt đầu code: Chạy `git pull` ở trên Terminal để kéo code mới về.
5. Khi muốn đẩy code mới lên Github: Không được push trực tiếp vào nhánh main mà phải tạo pull request mới, bắt @Copilot review code trước khi merge vào nhánh.
6. Commit message phải có ý nghĩa, làm cho người khác hiểu.
7. Xong 1 feature thì phải commit rồi push lên Github ngay.
8. Workflow phải pass thì mới được merge, pull request.

## Yêu cầu môi trường

- JDK 25 trở lên (`java -version`)
- MySQL 8 cho cách chạy local
- Không lưu mật khẩu hoặc API key trong mã nguồn

## Chạy backend bằng VS Code

### Cách 1: Chạy với MySQL local
Mở terminal trong VS Code tại thư mục `Bao-dien-tu`:

```powershell
cd D:\app\Bao-dien-tu
$env:TMDT_JWT_SECRET="dG1kdC1sb2NhbC1zZWNyZXQta2V5LTMyLWJ5dGVzISE="
$env:TMDT_MAIL_USERNAME="local@example.com"
$env:TMDT_MAIL_PASSWORD="local-password"
$env:GEMINI_API_KEY="dummy-local-key"
.\mvnw.cmd spring-boot:run
```

Theo mặc định, backend sẽ dùng MySQL local:

```text
jdbc:mysql://localhost:3306/pthttmdt
```

### Cách 2: Chạy với Azure MySQL
Repo có sẵn file:

```text
run-backend-azure.ps1
```

Mở terminal trong VS Code tại thư mục `Bao-dien-tu` rồi chạy:

```powershell
cd D:\app\Bao-dien-tu
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\run-backend-azure.ps1
```

Script sẽ tự cấu hình:

```text
TMDT_DB_URL
TMDT_DB_USERNAME
TMDT_JWT_SECRET
TMDT_MAIL_USERNAME
TMDT_MAIL_PASSWORD
GEMINI_API_KEY
```

Nếu chưa có `TMDT_JWT_SECRET`, script tạo một khóa JWT ngẫu nhiên chỉ cho tiến trình hiện tại. Muốn token đăng nhập còn hiệu lực sau khi khởi động lại backend, hãy đặt một khóa riêng trong biến môi trường và tuyệt đối không commit khóa đó.

Riêng `TMDT_DB_PASSWORD` không được lưu trong GitHub. Khi chạy, script sẽ hỏi mật khẩu Azure MySQL. Nếu không muốn nhập lại mỗi lần, có thể set trước trong terminal:

```powershell
$env:TMDT_DB_PASSWORD="your-azure-mysql-password"
.\run-backend-azure.ps1
```

Backend chạy đúng khi terminal hiện:

```text
Tomcat started on port 8082
```

Backend mặc định dùng cổng `8082` cho cả web và app. Có thể đổi cổng tạm thời bằng biến môi trường `PORT`.

Web chạy qua Vite proxy tại `http://localhost:8082`. Nếu backend dùng địa chỉ khác:

```powershell
$env:VITE_API_PROXY_TARGET="http://localhost:8082"
npm run dev
```

App Expo đọc địa chỉ backend từ `EXPO_PUBLIC_API_URL`. Khi chạy app trên điện thoại thật, dùng IP LAN của máy tính thay cho `localhost`:

```powershell
$env:EXPO_PUBLIC_API_URL="http://<IP-LAN-CUA-MAY>:8082"
npm start
```

Không commit mật khẩu database, JWT secret, Gemini API key hoặc VNPay hash secret. Các biến VNPay được hỗ trợ gồm:

```text
VNPAY_TMN_CODE
VNPAY_HASH_SECRET
VNPAY_URL
VNPAY_RETURN_URL
VNPAY_IPN_URL
```
