# Script para configurar o Keystore de Release
# Este script ajuda a criar e configurar o keystore para assinar o APK/AAB de release

Write-Host "=== CONFIGURAÇÃO DE KEYSTORE PARA RELEASE ===" -ForegroundColor Cyan
Write-Host ""

# Verificar se o keystore já existe
$keystorePath = "taskgo-release.jks"
if (Test-Path $keystorePath) {
    Write-Host "⚠️  ATENÇÃO: Já existe um keystore em $keystorePath" -ForegroundColor Yellow
    $sobrescrever = Read-Host "Deseja sobrescrever? (s/N)"
    if ($sobrescrever -ne "s" -and $sobrescrever -ne "S") {
        Write-Host "Operação cancelada." -ForegroundColor Yellow
        exit
    }
}

Write-Host "Vamos criar o keystore de release." -ForegroundColor Green
Write-Host ""
Write-Host "Você precisará fornecer:" -ForegroundColor Yellow
Write-Host "  1. Nome e sobrenome (ou nome da organização)" -ForegroundColor White
Write-Host "  2. Nome da unidade organizacional" -ForegroundColor White
Write-Host "  3. Nome da organização" -ForegroundColor White
Write-Host "  4. Nome da cidade" -ForegroundColor White
Write-Host "  5. Nome do estado (2 letras)" -ForegroundColor White
Write-Host "  6. Código do país (2 letras, ex: BR)" -ForegroundColor White
Write-Host "  7. Senha do keystore (GUARDE COM SEGURANÇA!)" -ForegroundColor White
Write-Host "  8. Senha da chave (pode ser a mesma da keystore)" -ForegroundColor White
Write-Host ""
Write-Host "⚠️  IMPORTANTE: Guarde as senhas em local seguro!" -ForegroundColor Red
Write-Host "   Se você perder o keystore ou as senhas, não poderá atualizar o app na Play Store!" -ForegroundColor Red
Write-Host ""

$continuar = Read-Host "Deseja continuar? (s/N)"
if ($continuar -ne "s" -and $continuar -ne "S") {
    Write-Host "Operação cancelada." -ForegroundColor Yellow
    exit
}

Write-Host ""
Write-Host "Agora vamos coletar as informações:" -ForegroundColor Green
Write-Host ""

# Coletar informações
$nome = Read-Host "Nome completo (ou nome da organização)"
$ou = Read-Host "Unidade organizacional (ou pressione Enter para pular)"
$org = Read-Host "Nome da organização (ex: TaskGo)"
$cidade = Read-Host "Cidade"
$estado = Read-Host "Estado (2 letras, ex: SP)"
$pais = Read-Host "Código do país (2 letras, ex: BR)"

Write-Host ""
Write-Host "Agora vamos definir as senhas:" -ForegroundColor Yellow
$storePassword = Read-Host "Senha do keystore" -AsSecureString
$storePasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePassword))

$keyPassword = Read-Host "Senha da chave (pode ser a mesma, pressione Enter para usar a mesma senha)" -AsSecureString
$keyPasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPassword))

if ([string]::IsNullOrWhiteSpace($keyPasswordPlain)) {
    $keyPasswordPlain = $storePasswordPlain
}

Write-Host ""
Write-Host "Criando keystore..." -ForegroundColor Green

# Construir comando keytool
$keytoolArgs = @(
    "-genkeypair",
    "-v",
    "-storetype", "PKCS12",
    "-keystore", $keystorePath,
    "-alias", "taskgo",
    "-keyalg", "RSA",
    "-keysize", "2048",
    "-validity", "10000",
    "-storepass", $storePasswordPlain,
    "-keypass", $keyPasswordPlain,
    "-dname", "CN=$nome, OU=$ou, O=$org, L=$cidade, ST=$estado, C=$pais"
)

try {
    & keytool $keytoolArgs
    Write-Host ""
    Write-Host "✅ Keystore criado com sucesso!" -ForegroundColor Green
    Write-Host ""
    
    # Criar arquivo keystore.properties
    Write-Host "Criando arquivo keystore.properties..." -ForegroundColor Green
    $keystoreProps = @"
TASKGO_RELEASE_STORE_FILE=$keystorePath
TASKGO_RELEASE_STORE_PASSWORD=$storePasswordPlain
TASKGO_RELEASE_KEY_ALIAS=taskgo
TASKGO_RELEASE_KEY_PASSWORD=$keyPasswordPlain
"@
    
    $keystoreProps | Out-File -FilePath "keystore.properties" -Encoding UTF8 -NoNewline
    Write-Host "✅ Arquivo keystore.properties criado!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar se .gitignore já tem as entradas
    $gitignoreContent = Get-Content ".gitignore" -ErrorAction SilentlyContinue
    $needsUpdate = $true
    
    if ($gitignoreContent) {
        if ($gitignoreContent -match "\.jks" -and $gitignoreContent -match "keystore\.properties") {
            $needsUpdate = $false
        }
    }
    
    if ($needsUpdate) {
        Write-Host "Adicionando entradas ao .gitignore..." -ForegroundColor Green
        Add-Content -Path ".gitignore" -Value "`n# Keystore files`n*.jks`n*.keystore`nkeystore.properties"
        Write-Host "✅ .gitignore atualizado!" -ForegroundColor Green
    } else {
        Write-Host "✅ .gitignore já está configurado corretamente!" -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "=== PRÓXIMOS PASSOS ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. ✅ Keystore criado: $keystorePath" -ForegroundColor Green
    Write-Host "2. ✅ Arquivo keystore.properties criado" -ForegroundColor Green
    Write-Host "3. ⚠️  IMPORTANTE: Faça backup do keystore em local seguro!" -ForegroundColor Yellow
    Write-Host "4. ⚠️  IMPORTANTE: Guarde as senhas em gerenciador de senhas!" -ForegroundColor Yellow
    Write-Host "5. 📝 Agora você precisa atualizar o app/build.gradle.kts" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "   O build.gradle.kts já está preparado. Descomente as linhas:" -ForegroundColor White
    Write-Host "   - Carregamento do keystore.properties" -ForegroundColor White
    Write-Host "   - Configuração do signingConfigs" -ForegroundColor White
    Write-Host "   - signingConfig no buildType release" -ForegroundColor White
    Write-Host ""
    Write-Host "6. Testar o build de release:" -ForegroundColor Cyan
    Write-Host "   ./gradlew.bat bundleRelease" -ForegroundColor White
    Write-Host ""
    
} catch {
    Write-Host ""
    Write-Host "❌ Erro ao criar keystore: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Verifique se o keytool está no PATH ou use o caminho completo:" -ForegroundColor Yellow
    Write-Host "   C:\Program Files\Java\jdk-17\bin\keytool.exe" -ForegroundColor White
    exit 1
}

