# Script para gerar arquivo ZIP com simbolos de depuracao nativos

$symbolsPath = "app\build\intermediates\native_symbol_tables\release\extractReleaseNativeSymbolTables"
$outputDir = "app\build\outputs\native-debug-symbols\release"
$zipPath = "$outputDir\native-debug-symbols.zip"

Write-Host ""
Write-Host "Verificando simbolos nativos..." -ForegroundColor Cyan

if (-not (Test-Path $symbolsPath)) {
    Write-Host "ERRO: Pasta de simbolos nao encontrada: $symbolsPath" -ForegroundColor Red
    Write-Host "Execute primeiro: .\gradlew.bat bundleRelease" -ForegroundColor Yellow
    exit 1
}

Write-Host "OK: Simbolos encontrados!" -ForegroundColor Green

# Criar diretorio de saida
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

# Remover ZIP existente se houver
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
    Write-Host "ZIP anterior removido" -ForegroundColor Yellow
}

# Criar ZIP
Write-Host "Criando arquivo ZIP..." -ForegroundColor Cyan
Add-Type -AssemblyName System.IO.Compression.FileSystem
$symbolsFullPath = (Resolve-Path $symbolsPath).Path
$zipFullPath = (Resolve-Path $outputDir).Path + "\native-debug-symbols.zip"

try {
    [System.IO.Compression.ZipFile]::CreateFromDirectory($symbolsFullPath, $zipFullPath)
    Write-Host "OK: ZIP criado com sucesso!" -ForegroundColor Green
    Write-Host ""
    $zipFile = Get-Item $zipFullPath
    Write-Host "Arquivo:" -ForegroundColor Cyan
    Write-Host "   $($zipFile.FullName)" -ForegroundColor White
    Write-Host "Tamanho: $([math]::Round($zipFile.Length / 1MB, 2)) MB" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Para fazer upload no Google Play Console:" -ForegroundColor Cyan
    Write-Host "   1. Acesse: https://play.google.com/console" -ForegroundColor White
    Write-Host "   2. Selecione seu app -> Versao -> Simbolos de depuracao nativos" -ForegroundColor White
    Write-Host "   3. Faca upload do arquivo acima" -ForegroundColor White
} catch {
    Write-Host "ERRO ao criar ZIP: $_" -ForegroundColor Red
    exit 1
}
