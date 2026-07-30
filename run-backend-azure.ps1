$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

if (-not $env:TMDT_DB_URL) {
  $env:TMDT_DB_URL = 'jdbc:mysql://pthttmdt.mysql.database.azure.com:3306/pthttmdt?useSSL=true&requireSSL=true&sslMode=REQUIRED&serverTimezone=UTC'
}

if (-not $env:TMDT_DB_USERNAME) {
  $env:TMDT_DB_USERNAME = 'pthttmdt'
}

if (-not $env:TMDT_DB_PASSWORD) {
  $securePassword = Read-Host 'Nhap mat khau Azure MySQL (TMDT_DB_PASSWORD)' -AsSecureString
  $passwordPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
  try {
    $env:TMDT_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPtr)
  } finally {
    if ($passwordPtr -ne [IntPtr]::Zero) {
      [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPtr)
    }
  }
}

if (-not $env:TMDT_JWT_SECRET) {
  $jwtSecretBytes = New-Object byte[] 32
  $randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
  try {
    $randomGenerator.GetBytes($jwtSecretBytes)
    $env:TMDT_JWT_SECRET = [Convert]::ToBase64String($jwtSecretBytes)
  } finally {
    $randomGenerator.Dispose()
  }
  Write-Host 'Generated a temporary JWT secret for this backend process.'
}

if (-not $env:TMDT_MAIL_USERNAME) {
  $env:TMDT_MAIL_USERNAME = 'local@example.com'
}

if (-not $env:TMDT_MAIL_PASSWORD) {
  $env:TMDT_MAIL_PASSWORD = 'local-password'
}

if (-not $env:GEMINI_API_KEY) {
  $env:GEMINI_API_KEY = 'dummy-local-key'
}

Write-Host 'Starting backend with Azure MySQL configuration...'
Write-Host "Database URL: $env:TMDT_DB_URL"
Write-Host "Database user: $env:TMDT_DB_USERNAME"

& .\mvnw.cmd spring-boot:run
