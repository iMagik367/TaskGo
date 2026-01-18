@echo off
echo ========================================
echo Building AAB Release Bundle (Direct Build)
echo Version: 1.0.86 (Code: 86)
echo ========================================
echo.

cd /d "%~dp0"

echo IMPORTANT: Make sure Android Studio is CLOSED!
echo.
echo Starting build...
echo.

call .\gradlew.bat bundleRelease --no-daemon --no-build-cache

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
    echo If R8 error persists, the build cache might be corrupted.
    echo Try: .\gradlew.bat clean --no-daemon
    echo Then run this script again.
)

pause

