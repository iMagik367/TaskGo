# Script para executar build AAB no Cursor sem timeout
Write-Host "========================================"
Write-Host "Building AAB Release Bundle"
Write-Host "Versao: 1.0.86 (Code: 86)"
Write-Host "========================================"
Write-Host ""

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

# Executar build
& .\gradlew.bat bundleRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================"
    Write-Host "BUILD COMPLETED SUCCESSFULLY!"
    Write-Host "========================================"
    Write-Host ""
    Write-Host "AAB Location:"
    Write-Host "app\build\outputs\bundle\release\app-release.aab"
    Write-Host ""
    
    $aabPath = "app\build\outputs\bundle\release\app-release.aab"
    if (Test-Path $aabPath) {
        $fileInfo = Get-Item $aabPath
        Write-Host "Tamanho: $($fileInfo.Length) bytes"
        Write-Host "Data: $($fileInfo.LastWriteTime)"
    }
} else {
    Write-Host ""
    Write-Host "========================================"
    Write-Host "BUILD FAILED!"
    Write-Host "========================================"
    Write-Host ""
    Write-Host "Exit Code: $LASTEXITCODE"
}

Write-Host ""
Write-Host "Pressione qualquer tecla para continuar..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")


