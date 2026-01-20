@echo off
REM Script Completo: Deploy Firebase + Build AAB
REM Executa o script PowerShell correspondente

powershell.exe -ExecutionPolicy Bypass -NoProfile -File "%~dp0deploy-e-build-completo.ps1" %*
pause
