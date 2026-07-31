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

---

## 🛠️ Yêu cầu môi trường
*   **JDK:** 25 trở lên (`java -version`).
*   **MySQL:** 8 cho cách chạy local.
*   **An toàn thông tin:** Tuyệt đối không lưu mật khẩu hoặc API key trong mã nguồn.

---

## 🚀 Chạy backend bằng VS Code

### Cách 1: Chạy với MySQL local
Mở terminal trong VS Code tại thư mục backend:
```powershell
cd D:\TMDT\6-7-TMDT-Backend-latest
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
Repo có sẵn file script: `run-backend-azure.ps1`.
Mở terminal trong VS Code tại thư mục backend rồi chạy:
```powershell
cd D:\TMDT\6-7-TMDT-Backend-latest
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\run-backend-azure.ps1
```
Script sẽ tự động cấu hình các biến môi trường:
*   `TMDT_DB_URL`
*   `TMDT_DB_USERNAME`
*   `TMDT_JWT_SECRET`
*   `TMDT_MAIL_USERNAME`
*   `TMDT_MAIL_PASSWORD`
*   `GEMINI_API_KEY`


Riêng `TMDT_DB_PASSWORD` không được lưu trong GitHub. Khi chạy, script sẽ hỏi mật khẩu Azure MySQL. Nếu không muốn nhập lại mỗi lần, có thể set trước trong terminal:

```powershell
$env:TMDT_DB_PASSWORD="your-azure-mysql-password"
.\run-backend-azure.ps1
```

Backend chạy đúng khi terminal hiện:

```text
Tomcat started on port 8080
```
