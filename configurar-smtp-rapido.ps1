# Script para configurar SMTP rapidamente com parâmetros fornecidos
# Uso: .\configurar-smtp-rapido.ps1 -Host "smtp.gmail.com" -Port "465" -User "email@gmail.com" -Password "senha" -From "noreply@taskgo.com" -ReplyTo "suporte@taskgo.com"

param(
    [Parameter(Mandatory=$true)]
    [string]$Host = "smtp.gmail.com",
    
    [Parameter(Mandatory=$true)]
    [string]$Port = "465",
    
    [Parameter(Mandatory=$true)]
    [string]$User,
    
    [Parameter(Mandatory=$true)]
    [string]$Password,
    
    [string]$From,
    
    [string]$ReplyTo
)

if ([string]::IsNullOrWhiteSpace($From)) {
    $From = $User
}

if ([string]::IsNullOrWhiteSpace($ReplyTo)) {
    $ReplyTo = $From
}

Write-Host "Configurando SMTP..." -ForegroundColor Cyan

firebase functions:config:set smtp.host="$Host"
firebase functions:config:set smtp.port="$Port"
firebase functions:config:set smtp.user="$User"
firebase functions:config:set smtp.password="$Password"
firebase functions:config:set email.default_from="$From"
firebase functions:config:set email.default_reply_to="$ReplyTo"

Write-Host "Configuracao concluida! Fazendo redeploy..." -ForegroundColor Green
firebase deploy --only functions:sendEmail

Write-Host ""
Write-Host "SMTP configurado com sucesso!" -ForegroundColor Green

















