@echo off
setlocal
cd /d "%~dp0"
echo ==============================================
echo   CampusLoop - Peer Rental (Spring Boot)
echo ==============================================

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java not found. Install JDK 17 or newer and try again.
  pause
  exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven not found. Install Maven 3.8+ or open this folder in IntelliJ IDEA / Eclipse.
  pause
  exit /b 1
)

if "%DB_USERNAME%"=="" set DB_USERNAME=root
if "%DB_PASSWORD%"=="" (
  set /p DB_PASSWORD=Enter MySQL password for user %DB_USERNAME% (press Enter if blank):
)

echo.
echo Starting... open http://localhost:8080 when you see "Started PeerRentalApplication".
echo Health check: http://localhost:8080/api/health
echo.
call mvn spring-boot:run
pause
