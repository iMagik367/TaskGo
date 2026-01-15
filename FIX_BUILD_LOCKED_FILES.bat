@echo off
echo ========================================
echo Fixing Build Locked Files Issue
echo Version: 1.0.32 (Code: 33)
echo ========================================
echo.

cd /d "%~dp0"

echo [1/4] Stopping Gradle daemons...
call .\gradlew.bat --stop
timeout /t 3 /nobreak >nul

echo [2/4] Stopping Java/Gradle processes...
taskkill /F /IM java.exe /T >nul 2>&1
taskkill /F /IM gradle.exe /T >nul 2>&1
timeout /t 2 /nobreak >nul

echo [3/4] Removing locked build directories...
if exist "app\build\tmp" (
    rmdir /s /q "app\build\tmp" >nul 2>&1
    echo    - Removed app\build\tmp
)
if exist "app\build\intermediates" (
    rmdir /s /q "app\build\intermediates" >nul 2>&1
    echo    - Removed app\build\intermediates
)
if exist "app\build\kotlinToolingMetadata" (
    rmdir /s /q "app\build\kotlinToolingMetadata" >nul 2>&1
    echo    - Removed app\build\kotlinToolingMetadata
)
timeout /t 2 /nobreak >nul

echo [4/4] Starting build without clean (direct build)...
echo.
call .\gradlew.bat bundleRelease --no-daemon

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo BUILD COMPLETED SUCCESSFULLY!
    echo ========================================
    echo.
    echo AAB Location:
    echo app\build\outputs\bundle\release\app-release.aab
) else (
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    echo.
    echo IMPORTANT: Make sure Android Studio is CLOSED!
    echo Try closing Android Studio completely and run this script again.
)

pause


