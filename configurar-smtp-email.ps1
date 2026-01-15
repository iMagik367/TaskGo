# Script para configurar credenciais SMTP para envio de email

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Configuracao SMTP para Envio de Email" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "A funcao sendEmail foi deployada com sucesso!" -ForegroundColor Green
Write-Host "Agora precisamos configurar as credenciais SMTP." -ForegroundColor Yellow
Write-Host ""

# Solicitar informações SMTP
Write-Host "Digite as credenciais SMTP:" -ForegroundColor Cyan
Write-Host ""

$smtpHost = Read-Host "SMTP Host (ex: smtp.gmail.com, smtp.sendgrid.net)"
if ([string]::IsNullOrWhiteSpace($smtpHost)) {
    $smtpHost = "smtp.gmail.com"
    Write-Host "Usando padrao: $smtpHost" -ForegroundColor Yellow
}

$smtpPort = Read-Host "SMTP Port (ex: 465 para Gmail, 587 para SendGrid)"
if ([string]::IsNullOrWhiteSpace($smtpPort)) {
    $smtpPort = "465"
    Write-Host "Usando padrao: $smtpPort" -ForegroundColor Yellow
}

$smtpUser = Read-Host "SMTP User (email/usuario)"
if ([string]::IsNullOrWhiteSpace($smtpUser)) {
    Write-Host "Usuario e obrigatorio!" -ForegroundColor Red
    exit 1
}

$smtpPassword = Read-Host "SMTP Password (senha-app para Gmail)" -AsSecureString
if ($smtpPassword.Length -eq 0) {
    Write-Host "Senha e obrigatoria!" -ForegroundColor Red
    exit 1
}

$defaultFrom = Read-Host "Email remetente padrao (ex: noreply@taskgo.com)"
if ([string]::IsNullOrWhiteSpace($defaultFrom)) {
    $defaultFrom = $smtpUser
    Write-Host "Usando: $defaultFrom" -ForegroundColor Yellow
}

$defaultReplyTo = Read-Host "Email para resposta padrao (ex: suporte@taskgo.com)"
if ([string]::IsNullOrWhiteSpace($defaultReplyTo)) {
    $defaultReplyTo = $defaultFrom
    Write-Host "Usando: $defaultReplyTo" -ForegroundColor Yellow
}

# Converter SecureString para String
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($smtpPassword)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)

Write-Host ""
Write-Host "Configurando credenciais SMTP..." -ForegroundColor Cyan

# Configurar SMTP
try {
    firebase functions:config:set smtp.host="$smtpHost"
    firebase functions:config:set smtp.port="$smtpPort"
    firebase functions:config:set smtp.user="$smtpUser"
    firebase functions:config:set smtp.password="$plainPassword"
    
    # Configurar emails padrão
    firebase functions:config:set email.default_from="$defaultFrom"
    firebase functions:config:set email.default_reply_to="$defaultReplyTo"
    
    Write-Host ""
    Write-Host "Configuracao concluida com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Fazendo redeploy da funcao para aplicar as configuracoes..." -ForegroundColor Cyan
    Write-Host ""
    
    firebase deploy --only functions:sendEmail
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Configuracao Completa!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "A funcao sendEmail esta pronta para uso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Para enviar um email, crie um documento na colecao 'mail' do Firestore:" -ForegroundColor Yellow
    Write-Host '{' -ForegroundColor White
    Write-Host '  "to": "destinatario@exemplo.com",' -ForegroundColor White
    Write-Host '  "message": {' -ForegroundColor White
    Write-Host '    "subject": "Assunto do Email",' -ForegroundColor White
    Write-Host '    "html": "<p>Conteudo do email</p>"' -ForegroundColor White
    Write-Host '  }' -ForegroundColor White
    Write-Host '}' -ForegroundColor White
    Write-Host ""
    Write-Host "NOTA IMPORTANTE:" -ForegroundColor Yellow
    Write-Host "- Para Gmail, voce precisa usar uma 'Senha de app' (nao a senha normal)" -ForegroundColor Yellow
    Write-Host "- Gerar em: https://myaccount.google.com/apppasswords" -ForegroundColor Cyan
    Write-Host ""
    
} catch {
    Write-Host ""
    Write-Host "Erro ao configurar: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Tente configurar manualmente:" -ForegroundColor Yellow
    Write-Host "firebase functions:config:set smtp.host=`"$smtpHost`"" -ForegroundColor White
    Write-Host "firebase functions:config:set smtp.port=`"$smtpPort`"" -ForegroundColor White
    Write-Host "firebase functions:config:set smtp.user=`"$smtpUser`"" -ForegroundColor White
    Write-Host "firebase functions:config:set smtp.password=`"[SENHA]`"" -ForegroundColor White
    exit 1
}

















