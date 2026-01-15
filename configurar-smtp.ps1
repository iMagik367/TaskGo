# Script para configurar credenciais SMTP para a função de email

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Configuracao SMTP para Envio de Email" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Solicitar informações SMTP
Write-Host "Digite as credenciais SMTP:" -ForegroundColor Yellow
Write-Host ""

$smtpHost = Read-Host "SMTP Host (ex: smtp.gmail.com)"
$smtpPort = Read-Host "SMTP Port (ex: 465 para Gmail)"
$smtpUser = Read-Host "SMTP User (email/usuario)"
$smtpPassword = Read-Host "SMTP Password (senha-app para Gmail)" -AsSecureString
$defaultFrom = Read-Host "Email remetente padrao (ex: noreply@taskgo.com)"
$defaultReplyTo = Read-Host "Email para resposta padrao (ex: suporte@taskgo.com)"

# Converter SecureString para String
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($smtpPassword)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)

Write-Host ""
Write-Host "Configurando..." -ForegroundColor Cyan

# Configurar SMTP
firebase functions:config:set smtp.host="$smtpHost"
firebase functions:config:set smtp.port="$smtpPort"
firebase functions:config:set smtp.user="$smtpUser"
firebase functions:config:set smtp.password="$plainPassword"

# Configurar emails padrão
firebase functions:config:set email.default_from="$defaultFrom"
firebase functions:config:set email.default_reply_to="$defaultReplyTo"

Write-Host ""
Write-Host "Configuracao concluida!" -ForegroundColor Green
Write-Host ""
Write-Host "NOTA: Para Gmail, use uma 'Senha de app' em:" -ForegroundColor Yellow
Write-Host "https://myaccount.google.com/apppasswords" -ForegroundColor Cyan
Write-Host ""
Write-Host "Agora faca o deploy da funcao:" -ForegroundColor Yellow
Write-Host "firebase deploy --only functions:sendEmail" -ForegroundColor White

















