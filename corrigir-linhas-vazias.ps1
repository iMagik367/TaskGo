# Script para remover linhas vazias extras no final dos arquivos

$files = @(
    "app\src\main\java\com\taskgoapp\taskgo\feature\ads\presentation\AdsScreen.kt",
    "app\src\main\java\com\taskgoapp\taskgo\feature\auth\presentation\IdentityVerificationScreen.kt",
    "app\src\main\java\com\taskgoapp\taskgo\feature\checkout\presentation\AddressBookScreen.kt"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        $original = $content
        
        # Remover múltiplas linhas vazias no final
        $content = $content -replace '(?m)\}\s*\r?\n\s*\r?\n\s*$', "}`n"
        
        # Remover linhas vazias extras (mais de 2 linhas vazias consecutivas)
        $content = $content -replace '(?m)(\r?\n\s*){3,}$', "`n`n"
        
        if ($content -ne $original) {
            Set-Content -Path $file -Value $content -NoNewline -Encoding UTF8
            Write-Host "Corrigido: $file"
        }
    }
}

Write-Host "Correção concluída!"
