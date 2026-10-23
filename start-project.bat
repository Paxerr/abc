@echo off
chcp 65001 >nul
title Khởi động hệ thống DANASEA

echo ===================================================
echo        DANASEA - HỆ THỐNG DU LỊCH BIỂN ĐÀ NẴNG
echo ===================================================
echo.

:: 1. Kiểm tra và bật Docker Desktop nếu chưa chạy
echo [1/3] Kiểm tra Docker...
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Docker chưa chạy. Đang tự động mở Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    echo Đang chờ Docker Desktop khởi động hoàn tất (khoảng 15-30 giây)...
    :wait_docker
    timeout /t 4 /nobreak >nul
    docker info >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo   ...đang chờ Docker Engine kết nối...
        goto wait_docker
    )
    echo   - Docker đã sẵn sàng!
) else (
    echo   - Docker đang hoạt động tốt.
)

echo.
:: 2. Chỉ khởi động Database & Backend trên Docker (tránh đè bản cũ của Frontend)
echo [2/3] Khởi động Database (Postgres, Redis, RabbitMQ) và Backend API trên Docker...
docker compose up -d postgres redis rabbitmq backend
:: Đảm bảo tắt container frontend cũ trong Docker để nhường cổng 3000 cho code mới nhất
docker compose stop frontend >nul 2>&1

echo.
:: 3. Khởi động giao diện Web Frontend trực tiếp (Code mới nhất 100% từ nhánh làm việc)
echo [3/3] Khởi động Frontend Vite với code mới nhất (http://localhost:3000)...
timeout /t 2 /nobreak >nul
start http://localhost:3000
cd frontend
npm run dev
