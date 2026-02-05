# Script PowerShell para build do AAB V1.3.4
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BUILD AAB V1.3.4 (Code: 137)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$projectRoot = "C:\Users\user\AndroidStudioProjects\TaskGoApp"
Set-Location $projectRoot

# Verificar versão
Write-Host "[1/5] Verificando versão..." -ForegroundColor Yellow
$gradleFile = Get-Content "app\build.gradle.kts" -Raw
if ($gradleFile -notmatch "versionCode = 137") {
    Write-Host "ERRO: versionCode não está definido como 137" -ForegroundColor Red
    exit 1
}
if ($gradleFile -notmatch 'versionName = "1.3.4"') {
    Write-Host "ERRO: versionName não está definido como 1.3.4" -ForegroundColor Red
    exit 1
}
Write-Host "Versão confirmada: 1.3.4 (Code: 137)" -ForegroundColor Green
Write-Host ""

# Parar daemons
Write-Host "[2/5] Parando daemons Gradle..." -ForegroundColor Yellow
& cmd.exe /c "gradlew.bat --stop" | Out-Null
Write-Host ""

# Limpar
Write-Host "[3/5] Limpando build anterior..." -ForegroundColor Yellow
$cleanResult = & cmd.exe /c "gradlew.bat clean --no-daemon" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "AVISO: Erro ao limpar (continuando mesmo assim)" -ForegroundColor Yellow
}
Write-Host ""

# Build
Write-Host "[4/5] Compilando AAB (isso pode levar vários minutos)..." -ForegroundColor Yellow
Write-Host "Por favor, aguarde..." -ForegroundColor Gray
Write-Host ""

$buildOutput = & cmd.exe /c "gradlew.bat bundleRelease --no-daemon" 2>&1
$buildSuccess = $LASTEXITCODE -eq 0

if (-not $buildSuccess) {
    Write-Host ""
    Write-Host "ERRO: Falha no build do AAB" -ForegroundColor Red
    Write-Host ""
    Write-Host "Últimas linhas do output:" -ForegroundColor Yellow
    $buildOutput | Select-Object -Last 20
    exit 1
}

Write-Host ""

# Verificar AAB
Write-Host "[5/5] Verificando AAB gerado..." -ForegroundColor Yellow
$aabPath = "app\build\outputs\bundle\release\app-release.aab"
if (Test-Path $aabPath) {
    $aabFile = Get-Item $aabPath
    $aabSizeMB = [math]::Round($aabFile.Length / 1MB, 2)
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  BUILD CONCLUIDO COM SUCESSO!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Arquivo: $aabPath" -ForegroundColor Cyan
    Write-Host "Tamanho: $aabSizeMB MB" -ForegroundColor Cyan
    Write-Host "Versão: 1.3.4 (Code: 137)" -ForegroundColor Cyan
    Write-Host ""
} else {
    Write-Host "ERRO: AAB não foi gerado em $aabPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Verificando diretório de saída..." -ForegroundColor Yellow
    if (Test-Path "app\build\outputs\bundle\release\") {
        Get-ChildItem "app\build\outputs\bundle\release\"
    } else {
        Write-Host "Diretório não existe" -ForegroundColor Red
    }
    exit 1
}

Write-Host "Build concluído com sucesso!" -ForegroundColor Green
