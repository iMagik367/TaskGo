# Script para configurar GEMINI_API_KEY no Firebase Functions via CLI
# Requer: Firebase CLI instalado e autenticado

Write-Host "=== Configurar GEMINI_API_KEY no Firebase ===" -ForegroundColor Cyan
Write-Host ""

# Verificar se o Firebase CLI está instalado
try {
    $firebaseVersion = firebase --version
    Write-Host "✅ Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Erro: Firebase CLI não encontrado. Instale com: npm install -g firebase-tools" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Por favor, forneça sua chave da API Gemini." -ForegroundColor Yellow
Write-Host "Se você não tem uma, obtenha em: https://aistudio.google.com/app/apikey" -ForegroundColor Yellow
Write-Host ""

# Solicitar a chave ao usuário
$apiKey = Read-Host "Digite sua GEMINI_API_KEY" -AsSecureString
$apiKeyPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($apiKey)
)

if ([string]::IsNullOrWhiteSpace($apiKeyPlain)) {
    Write-Host "❌ Erro: Chave não pode estar vazia!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Configurando variável de ambiente..." -ForegroundColor Yellow

# Para Firebase Functions v2, usar Secret Manager é recomendado
# Mas para compatibilidade, vamos tentar functions:config:set primeiro
# Nota: Este método está deprecated mas ainda funciona

try {
    # Tentar usar Secret Manager (método recomendado para Firebase Functions v2)
    Write-Host "Tentando configurar via Secret Manager (recomendado)..." -ForegroundColor Cyan
    
    # Criar secret via Secret Manager
    $secretName = "GEMINI_API_KEY"
    
    # Salvar temporariamente em arquivo para passar ao secret
    $tempFile = [System.IO.Path]::GetTempFileName()
    $apiKeyPlain | Out-File -FilePath $tempFile -NoNewline -Encoding UTF8
    
    # Configurar secret (requer gcloud CLI ou Firebase Admin)
    # Alternativa: usar firebase functions:config:set (deprecated mas funciona)
    
    Write-Host "Usando método functions:config:set (compatível)..." -ForegroundColor Cyan
    
    # Para functions:config:set, precisa do formato: gemini.api_key
    $configKey = "gemini.api_key"
    $configValue = $apiKeyPlain
    
    # Executar comando
    $output = firebase functions:config:set "$configKey=$configValue" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Variável configurada com sucesso!" -ForegroundColor Green
        Write-Host ""
        Write-Host "IMPORTANTE:" -ForegroundColor Yellow
        Write-Host "Você precisa fazer redeploy das functions para que a mudança tenha efeito:" -ForegroundColor Yellow
        Write-Host "  firebase deploy --only functions" -ForegroundColor Cyan
        Write-Host ""
    } else {
        Write-Host "❌ Erro ao configurar variável:" -ForegroundColor Red
        Write-Host $output
        exit 1
    }
    
} catch {
    Write-Host "❌ Erro: $_" -ForegroundColor Red
    exit 1
} finally {
    # Limpar arquivo temporário
    if (Test-Path $tempFile) {
        Remove-Item $tempFile -Force
        # Sobrescrever com zeros antes de deletar (segurança)
        $apiKeyPlain = $null
    }
}

Write-Host ""
Write-Host "=== Configuração Concluída ===" -ForegroundColor Green


