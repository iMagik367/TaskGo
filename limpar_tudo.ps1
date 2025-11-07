# Script para limpar completamente o projeto e caches do Gradle

Write-Host "=== LIMPEZA COMPLETA DO PROJETO ===" -ForegroundColor Yellow

# 1. Parar todos os daemons do Gradle
Write-Host "1. Parando daemons do Gradle..." -ForegroundColor Cyan
./gradlew.bat --stop
Start-Sleep -Seconds 2

# 2. Remover caches do projeto
Write-Host "2. Removendo caches do projeto..." -ForegroundColor Cyan
Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "app\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".idea" -Recurse -Force -ErrorAction SilentlyContinue

# 3. Remover cache global do Gradle
Write-Host "3. Removendo cache global do Gradle..." -ForegroundColor Cyan
$gradleHome = "$env:USERPROFILE\.gradle"
if (Test-Path "$gradleHome\daemon") {
    Remove-Item -Path "$gradleHome\daemon" -Recurse -Force -ErrorAction SilentlyContinue
}
if (Test-Path "$gradleHome\caches") {
    Remove-Item -Path "$gradleHome\caches" -Recurse -Force -ErrorAction SilentlyContinue
}

# 4. Verificar/criar local.properties
Write-Host "4. Verificando local.properties..." -ForegroundColor Cyan
$localPropsPath = "local.properties"
$sdkPath = "C:/Users/user/AppData/Local/Android/Sdk"

if (-not (Test-Path $localPropsPath)) {
    Write-Host "   Criando local.properties..." -ForegroundColor Yellow
    "sdk.dir=$sdkPath" | Out-File -FilePath $localPropsPath -Encoding UTF8 -NoNewline
} else {
    $content = Get-Content $localPropsPath -Raw
    if ($content -notmatch "sdk\.dir=") {
        Write-Host "   Atualizando local.properties..." -ForegroundColor Yellow
        "sdk.dir=$sdkPath`r`n" | Out-File -FilePath $localPropsPath -Encoding UTF8
    } else {
        Write-Host "   local.properties OK" -ForegroundColor Green
    }
}

# 5. Verificar se SDK existe
Write-Host "5. Verificando SDK..." -ForegroundColor Cyan
if (Test-Path $sdkPath) {
    Write-Host "   SDK encontrado: $sdkPath" -ForegroundColor Green
} else {
    Write-Host "   ERRO: SDK não encontrado em $sdkPath" -ForegroundColor Red
    Write-Host "   Configure o caminho correto do SDK no local.properties" -ForegroundColor Yellow
}

Write-Host "`n=== LIMPEZA CONCLUÍDA ===" -ForegroundColor Green
Write-Host "Agora:" -ForegroundColor Yellow
Write-Host "1. Feche o Android Studio COMPLETAMENTE" -ForegroundColor White
Write-Host "2. Abra o Android Studio novamente" -ForegroundColor White
Write-Host "3. File -> Invalidate Caches / Restart..." -ForegroundColor White
Write-Host "4. File -> Sync Project with Gradle Files" -ForegroundColor White

