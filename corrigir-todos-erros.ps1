# Script para corrigir todos os erros de sintaxe restantes

$files = @(
    @{
        File = "app\src\main\java\com\taskgoapp\taskgo\feature\feed\presentation\components\InlinePostCreator.kt"
        Pattern = "com\.taskgoapp\.taskgo\.core\.design\.TaskGocom\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\("
        Replacement = "com.taskgoapp.taskgo.core.design.TaskGoCard("
    }
)

foreach ($item in $files) {
    if (Test-Path $item.File) {
        $content = Get-Content $item.File -Raw -Encoding UTF8
        $original = $content
        
        $content = $content -replace $item.Pattern, $item.Replacement
        
        if ($content -ne $original) {
            Set-Content -Path $item.File -Value $content -NoNewline -Encoding UTF8
            Write-Host "Corrigido: $($item.File)"
        }
    }
}

# Corrigir AdsScreen.kt - remover linhas vazias extras
$adsFile = "app\src\main\java\com\taskgoapp\taskgo\feature\ads\presentation\AdsScreen.kt"
if (Test-Path $adsFile) {
    $content = Get-Content $adsFile -Raw -Encoding UTF8
    $content = $content -replace '(?m)\}\s*\r?\n\s*\r?\n\s*\r?\n', "}`n`n"
    Set-Content -Path $adsFile -Value $content -NoNewline -Encoding UTF8
    Write-Host "Corrigido: $adsFile"
}

# Corrigir AddressBookScreen.kt - remover linhas vazias extras
$addressFile = "app\src\main\java\com\taskgoapp\taskgo\feature\checkout\presentation\AddressBookScreen.kt"
if (Test-Path $addressFile) {
    $content = Get-Content $addressFile -Raw -Encoding UTF8
    $content = $content -replace '(?m)\}\s*\r?\n\s*\r?\n', "}`n`n"
    Set-Content -Path $addressFile -Value $content -NoNewline -Encoding UTF8
    Write-Host "Corrigido: $addressFile"
}

Write-Host "Correções concluídas!"
