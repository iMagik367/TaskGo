# Script para corrigir a extensão Trigger Email
# Execute este script no PowerShell após verificar a região correta do Firestore

Write-Host "=== Correção da Extensão Trigger Email ===" -ForegroundColor Cyan
Write-Host ""

# Passo 1: Desinstalar extensão com erro
Write-Host "1. Desinstalando extensão com erro..." -ForegroundColor Yellow
firebase ext:uninstall firestore-send-email --force

Write-Host ""
Write-Host "2. Aguarde alguns segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Passo 2: Verificar região do Firestore
Write-Host ""
Write-Host "3. Verificando região do Firestore..." -ForegroundColor Yellow
firebase firestore:databases:list

Write-Host ""
Write-Host "IMPORTANTE: Verifique a região do Firestore acima." -ForegroundColor Red
Write-Host "Regiões comuns: nam5 (US multi-region), us-central1, etc." -ForegroundColor Yellow
Write-Host ""
Write-Host "Após verificar a região, você precisará:" -ForegroundColor Cyan
Write-Host "1. Reinstalar a extensão via Firebase Console" -ForegroundColor White
Write-Host "   - Vá para: Extensions > Browse Catalog" -ForegroundColor White
Write-Host "   - Procure: 'Trigger Email'" -ForegroundColor White
Write-Host "   - Durante a instalação, configure:" -ForegroundColor White
Write-Host "     * Location: [Use a mesma região do Firestore]" -ForegroundColor White
Write-Host "     * Collection path: mail" -ForegroundColor White
Write-Host "     * SMTP connection URI: [Configure seu SMTP]" -ForegroundColor White
Write-Host ""
Write-Host "OU" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Reinstalar via CLI (ajuste a região antes de executar):" -ForegroundColor White
Write-Host "   firebase ext:install firebase/firestore-send-email `--params=location=nam5" -ForegroundColor Green

Write-Host ""
Write-Host "=== Script concluído ===" -ForegroundColor Cyan










