@echo off
REM Script de Deploy Completo - Firebase Functions, Rules e Storage Rules
REM Executa o script PowerShell correspondente

powershell.exe -ExecutionPolicy Bypass -NoProfile -File "%~dp0deploy-firebase-completo.ps1"
pause
