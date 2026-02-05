# Script para corrigir indentação de TaskGoCard
$files = @(
    "app\src\main\java\com\taskgoapp\taskgo\feature\settings\presentation\AboutScreen.kt",
    "app\src\main\java\com\taskgoapp\taskgo\feature\settings\presentation\BankAccountScreen.kt",
    "app\src\main\java\com\taskgoapp\taskgo\feature\settings\presentation\PreferencesScreen.kt",
    "app\src\main\java\com\taskgoapp\taskgo\feature\settings\presentation\PrivacyScreen.kt",
    "app\src\main\java\com\taskgoapp\taskgo\feature\settings\presentation\SupportScreen.kt"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        $original = $content
        
        # Corrigir padrão: TaskGoCard( seguido de linha sem indentação adequada
        $content = $content -replace '(?m)(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\()\s*\r?\n\s*modifier\s*=\s*Modifier\.fillMaxWidth\(\)\s*\r?\n\s*\)\s*\{', "`$1`n                modifier = Modifier.fillMaxWidth()`n            ) {"
        
        if ($content -ne $original) {
            Set-Content -Path $file -Value $content -NoNewline -Encoding UTF8
            Write-Host "Corrigido: $file"
        }
    }
}

Write-Host "Correção de indentação concluída!"
