@echo off
REM Script de Build AAB para Release
REM Executa o script PowerShell correspondente

powershell.exe -ExecutionPolicy Bypass -NoProfile -File "%~dp0build-aab-release.ps1"
pause
