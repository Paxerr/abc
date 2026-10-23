@echo off
chcp 65001 >nul
title Dừng hệ thống DANASEA

echo ===================================================
echo             DỪNG HỆ THỐNG DANASEA
echo ===================================================
echo.

echo Đang tắt các dịch vụ Docker (PostgreSQL, Redis, RabbitMQ, Backend)...
docker compose down

echo.
echo Đã tắt an toàn toàn bộ hệ thống!
pause
