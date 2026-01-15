@echo off
echo ========================================
echo Building AAB Release Bundle
echo ========================================
echo.
echo Version: 1.0.72 (Code: 73)
echo.

cd /d "%~dp0"

echo Starting Gradle build...
echo.

.\gradlew.bat bundleRelease

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo BUILD COMPLETED SUCCESSFULLY!
    echo ========================================
    echo.
    echo AAB Location:
    echo app\build\outputs\bundle\release\app-release.aab
    echo.
) else (
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    echo.
)

pause





