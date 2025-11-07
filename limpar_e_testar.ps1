# Script completo de limpeza e preparação para build no Android Studio

Write-Host "=== LIMPEZA COMPLETA E PREPARAÇÃO ===" -ForegroundColor Yellow
Write-Host ""

# 1. Parar daemons
Write-Host "1. Parando daemons do Gradle..." -ForegroundColor Cyan
./gradlew.bat --stop 2>&1 | Out-Null
Start-Sleep -Seconds 2

# 2. Limpar caches do projeto
Write-Host "2. Removendo caches do projeto..." -ForegroundColor Cyan
Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "app\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".idea" -Recurse -Force -ErrorAction SilentlyContinue

# 3. Limpar cache global do Gradle
Write-Host "3. Removendo cache global do Gradle..." -ForegroundColor Cyan
$gradleHome = "$env:USERPROFILE\.gradle"

if (Test-Path "$gradleHome\caches") {
    Remove-Item -Path "$gradleHome\caches" -Recurse -Force -ErrorAction SilentlyContinue
}

if (Test-Path "$gradleHome\daemon") {
    Remove-Item -Path "$gradleHome\daemon" -Recurse -Force -ErrorAction SilentlyContinue
}

# Remover cache do Gradle 8.13 antigo
Get-ChildItem -Path "$gradleHome\wrapper\dists" -Directory -Filter "gradle-8.13-*" -ErrorAction SilentlyContinue | 
    Remove-Item -Recurse -Force -ErrorAction SilentlyContinue

# 4. Verificar local.properties
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
        "sdk.dir=$sdkPath" | Out-File -FilePath $localPropsPath -Encoding UTF8 -NoNewline
    } else {
        Write-Host "   local.properties OK" -ForegroundColor Green
        # Garantir que não tem quebra de linha extra
        $content = $content.Trim()
        if ($content -notmatch "`r?`n$") {
            $content | Out-File -FilePath $localPropsPath -Encoding UTF8 -NoNewline
        }
    }
}

# 5. Verificar SDK
Write-Host "5. Verificando SDK..." -ForegroundColor Cyan
if (Test-Path $sdkPath) {
    Write-Host "   SDK encontrado: $sdkPath" -ForegroundColor Green
} else {
    Write-Host "   ERRO: SDK não encontrado em $sdkPath" -ForegroundColor Red
    exit 1
}

# 6. Verificar versões
Write-Host "6. Verificando versões configuradas..." -ForegroundColor Cyan
$agp = Select-String -Path "gradle/libs.versions.toml" -Pattern 'agp = "([^"]+)"' | ForEach-Object { $_.Matches.Groups[1].Value }
$gradle = Select-String -Path "gradle/wrapper/gradle-wrapper.properties" -Pattern "gradle-([\d.]+)-bin" | ForEach-Object { $_.Matches.Groups[1].Value }

Write-Host "   Android Gradle Plugin: $agp" -ForegroundColor Green
Write-Host "   Gradle: $gradle" -ForegroundColor Green

Write-Host ""
Write-Host "=== LIMPEZA CONCLUÍDA ===" -ForegroundColor Green
Write-Host ""
Write-Host "PRÓXIMOS PASSOS NO ANDROID STUDIO:" -ForegroundColor Yellow
Write-Host "1. Feche o Android Studio COMPLETAMENTE (se estiver aberto)" -ForegroundColor White
Write-Host "2. Abra o Android Studio" -ForegroundColor White
Write-Host "3. File → Open → Selecione esta pasta" -ForegroundColor White
Write-Host "4. File → Invalidate Caches / Restart..." -ForegroundColor White
Write-Host "5. File → Sync Project with Gradle Files" -ForegroundColor White
Write-Host "6. Build → Rebuild Project" -ForegroundColor White
Write-Host ""

