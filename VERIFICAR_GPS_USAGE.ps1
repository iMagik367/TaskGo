# Script para verificar TODOS os arquivos que usam GPS para city/state
# Executa varredura completa e gera relatório

Write-Host "🔍 Iniciando varredura completa de uso de GPS para city/state..." -ForegroundColor Cyan

$filesToCheck = @()

# Buscar todos os arquivos Kotlin
$kotlinFiles = Get-ChildItem -Path "app/src/main/java" -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName
$filesToCheck += $kotlinFiles

# Buscar todos os arquivos TypeScript (backend)
$tsFiles = Get-ChildItem -Path "functions/src" -Recurse -Filter "*.ts" | Select-Object -ExpandProperty FullName
$filesToCheck += $tsFiles

Write-Host "📁 Total de arquivos para verificar: $($filesToCheck.Count)" -ForegroundColor Yellow

$issues = @()

foreach ($file in $filesToCheck) {
    $content = Get-Content $file -Raw -ErrorAction SilentlyContinue
    if ($null -eq $content) { continue }
    
    $relativePath = $file.Replace((Get-Location).Path + "\", "")
    
    # Padrões problemáticos: GPS usado para city/state
    $patterns = @(
        @{ Pattern = "getAddressFromLocation.*city|getAddressFromLocation.*state"; Description = "getAddressFromLocation usado para city/state" },
        @{ Pattern = "getAddressGuaranteed.*city|getAddressGuaranteed.*state"; Description = "getAddressGuaranteed usado para city/state" },
        @{ Pattern = "address\.locality.*city|address\.adminArea.*state"; Description = "Address.locality/adminArea usado para city/state" },
        @{ Pattern = "LocationUpdateService.*startLocationMonitoring|locationUpdateService.*startLocationMonitoring"; Description = "LocationUpdateService ainda sendo usado" },
        @{ Pattern = "getLocationFromGPSOrParams"; Description = "getLocationFromGPSOrParams ainda sendo chamado" },
        @{ Pattern = "GPS.*city|GPS.*state|gps.*city|gps.*state"; Description = "GPS mencionado junto com city/state" },
        @{ Pattern = "fromGPS|fromLocation.*city|fromLocation.*state"; Description = "Função fromGPS/fromLocation para city/state" }
    )
    
    foreach ($patternInfo in $patterns) {
        if ($content -match $patternInfo.Pattern) {
            $lineNumber = ($content -split "`n" | Select-String -Pattern $patternInfo.Pattern | Select-Object -First 1).LineNumber
            $issues += @{
                File = $relativePath
                Line = $lineNumber
                Issue = $patternInfo.Description
                Pattern = $patternInfo.Pattern
            }
        }
    }
}

Write-Host "`n📊 RESULTADO DA VARREdura:" -ForegroundColor Cyan
Write-Host "Total de problemas encontrados: $($issues.Count)" -ForegroundColor $(if ($issues.Count -eq 0) { "Green" } else { "Red" })

if ($issues.Count -gt 0) {
    Write-Host "`n❌ PROBLEMAS ENCONTRADOS:" -ForegroundColor Red
    $issues | Group-Object File | ForEach-Object {
        Write-Host "`n📄 $($_.Name)" -ForegroundColor Yellow
        $_.Group | ForEach-Object {
            Write-Host "  Linha $($_.Line): $($_.Issue)" -ForegroundColor Red
        }
    }
} else {
    Write-Host "`n✅ Nenhum problema encontrado! Todos os arquivos estão corretos." -ForegroundColor Green
}

# Salvar relatório
$reportPath = "RELATORIO_GPS_VARREDURA_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
$issues | Format-Table -AutoSize | Out-File $reportPath
Write-Host "`n📝 Relatório salvo em: $reportPath" -ForegroundColor Cyan
