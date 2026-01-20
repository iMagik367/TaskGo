# Script Completo: Deploy Firebase + Build AAB
# Executa deploy completo e depois build do AAB

param(
    [switch]$SkipDeploy = $false,
    [switch]$SkipBuild = $false
)

Write-Host "SCRIPT COMPLETO: Deploy Firebase + Build AAB" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Deploy Firebase
if (-not $SkipDeploy) {
    Write-Host "`nETAPA 1: Deploy Firebase" -ForegroundColor Yellow
    Write-Host "------------------------------------------------------------" -ForegroundColor Gray
    & "$PSScriptRoot\deploy-firebase-completo.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "`nDeploy falhou. Deseja continuar com o build? (S/N)" -ForegroundColor Red
        $continue = Read-Host
        if ($continue -ne "S" -and $continue -ne "s") {
            exit 1
        }
    }
} else {
    Write-Host "`nPulando deploy do Firebase (--SkipDeploy)" -ForegroundColor Yellow
}

# 2. Build AAB
if (-not $SkipBuild) {
    Write-Host "`nETAPA 2: Build AAB" -ForegroundColor Yellow
    Write-Host "------------------------------------------------------------" -ForegroundColor Gray
    & "$PSScriptRoot\build-aab-release.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "`nBuild falhou" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "`nPulando build do AAB (--SkipBuild)" -ForegroundColor Yellow
}

Write-Host "`n" -NoNewline
Write-Host "============================================================" -ForegroundColor Green
Write-Host "PROCESSO COMPLETO FINALIZADO!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "`nResumo:" -ForegroundColor White
if (-not $SkipDeploy) {
    Write-Host "   - Firebase deployed (Functions, Rules, Indexes)" -ForegroundColor Green
}
if (-not $SkipBuild) {
    Write-Host "   - AAB gerado e pronto para upload" -ForegroundColor Green
}
