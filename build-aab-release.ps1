# Script de Build AAB para Release
# Gera AAB assinado com nova versao (1.0.97)
# Atualizado para correção definitiva do fluxo de localização

param(
    [switch]$SkipVersionCheck = $false,
    [switch]$SkipDeploy = $false
)

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "BUILD AAB RELEASE - VERSÃO 1.0.97" -ForegroundColor Cyan
Write-Host "Correção Definitiva do Fluxo de Localização" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Deploy do Firebase antes do build (se não pular)
if (-not $SkipDeploy) {
    Write-Host "Fazendo deploy do Firebase antes do build..." -ForegroundColor Yellow
    Write-Host "   (Execute .\deploy-firebase-completo-localizacao.ps1 se preferir fazer manualmente)" -ForegroundColor Gray
    Write-Host ""
    
    $deployChoice = Read-Host "Deseja fazer deploy do Firebase agora? (S/N)"
    if ($deployChoice -eq "S" -or $deployChoice -eq "s") {
        Write-Host "Executando deploy do Firebase..." -ForegroundColor Yellow
        & ".\deploy-firebase-completo-localizacao.ps1"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "AVISO: Deploy do Firebase falhou, mas continuando com o build..." -ForegroundColor Yellow
        }
        Write-Host ""
    }
}

Write-Host "Iniciando build do AAB para release..." -ForegroundColor Cyan

# Verificar se esta no diretorio correto
if (-not (Test-Path "app/build.gradle.kts")) {
    Write-Host "ERRO: Execute este script na raiz do projeto Android" -ForegroundColor Red
    exit 1
}

# Verificar versao atual
Write-Host "`nVerificando versao atual..." -ForegroundColor Yellow
$gradleFile = Get-Content "app/build.gradle.kts" -Raw
$versionMatch = [regex]::Match($gradleFile, 'versionCode\s*=\s*(\d+)\s+versionName\s*=\s*"([^"]+)"')
if ($versionMatch.Success) {
    $currentVersionCode = $versionMatch.Groups[1].Value
    $currentVersionName = $versionMatch.Groups[2].Value
    Write-Host "   Versao atual: Code=$currentVersionCode, Name=$currentVersionName" -ForegroundColor White
} else {
    Write-Host "AVISO: Nao foi possivel detectar versao no build.gradle.kts" -ForegroundColor Yellow
}

# Verificar keystore
$keystoreProps = "keystore.properties"
if (-not (Test-Path $keystoreProps)) {
    Write-Host "ERRO: keystore.properties nao encontrado" -ForegroundColor Red
    Write-Host "   Crie o arquivo keystore.properties na raiz do projeto" -ForegroundColor Yellow
    exit 1
}
Write-Host "keystore.properties encontrado" -ForegroundColor Green

# Limpar build anterior
Write-Host "`nLimpando builds anteriores..." -ForegroundColor Yellow
./gradlew clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "AVISO: Erro ao limpar (pode ser normal)" -ForegroundColor Yellow
}

# Build AAB Release
Write-Host "`nCompilando AAB Release (isso pode levar alguns minutos)..." -ForegroundColor Yellow
Write-Host "   Versao: Code=$currentVersionCode, Name=$currentVersionName" -ForegroundColor Gray

./gradlew bundleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Erro ao compilar AAB" -ForegroundColor Red
    exit 1
}

# Localizar AAB gerado
$outputPath = "app/build/outputs/bundle/release/app-release.aab"
if (Test-Path $outputPath) {
    $aabFile = Get-Item $outputPath
    $fileSize = [math]::Round($aabFile.Length / 1MB, 2)
    
    Write-Host "`n" -NoNewline
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "AAB GERADO COM SUCESSO!" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "`nArquivo: $($aabFile.FullName)" -ForegroundColor White
    Write-Host "   Tamanho: $fileSize MB" -ForegroundColor White
    Write-Host "   Versao: $currentVersionName ($currentVersionCode)" -ForegroundColor White
    Write-Host "`nPronto para upload no Google Play Console!" -ForegroundColor Cyan
    
    # Abrir pasta de outputs
    Write-Host "`nAbrindo pasta de outputs..." -ForegroundColor Yellow
    Start-Process "explorer.exe" -ArgumentList "/select,`"$($aabFile.FullName)`""
} else {
    Write-Host "ERRO: AAB nao encontrado em $outputPath" -ForegroundColor Red
    exit 1
}
