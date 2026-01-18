@echo off
echo ========================================
echo Building AAB Release Bundle (R8 Fix)
echo Version: 1.0.86 (Code: 86)
echo ========================================
echo.

cd /d "%~dp0"

echo IMPORTANT: Make sure Android Studio is CLOSED!
echo.
echo Stopping Gradle daemons...
call .\gradlew.bat --stop
timeout /t 3 /nobreak >nul

echo Starting build with increased memory...
echo.
call .\gradlew.bat bundleRelease --no-daemon --no-build-cache --rerun-tasks

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
    echo R8 error might require:
    echo 1. Close Android Studio completely
    echo 2. Restart computer (to clear all locks)
    echo 3. Run this script again
)

pause

