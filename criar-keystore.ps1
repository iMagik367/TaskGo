# Script PowerShell para criar keystore do TaskGo
# Execute este script na raiz do projeto

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Criador de Keystore - TaskGo App" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se Java esta instalado
Write-Host "Verificando Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "OK Java encontrado: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "ERRO: Java nao encontrado! Instale o JDK primeiro." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Voce precisara fornecer as seguintes informacoes:" -ForegroundColor Yellow
Write-Host "  - Senha do keystore (guarde bem!)" -ForegroundColor Yellow
Write-Host "  - Senha do alias (pode ser a mesma)" -ForegroundColor Yellow
Write-Host "  - Informacoes pessoais/empresariais" -ForegroundColor Yellow
Write-Host ""

# Solicitar informacoes
$keystorePassword = Read-Host "Digite a senha do keystore" -AsSecureString
$keystorePasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($keystorePassword))

$aliasPasswordInput = Read-Host "Digite a senha do alias (ou pressione Enter para usar a mesma)" -AsSecureString
if ($aliasPasswordInput.Length -eq 0) {
    $aliasPasswordPlain = $keystorePasswordPlain
} else {
    $aliasPasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($aliasPasswordInput))
}

Write-Host ""
Write-Host "Informacoes da organizacao:" -ForegroundColor Yellow
$cn = Read-Host "Nome e sobrenome (ou nome do app)" 
if ([string]::IsNullOrWhiteSpace($cn)) { $cn = "TaskGo App" }

$ou = Read-Host "Unidade organizacional" 
if ([string]::IsNullOrWhiteSpace($ou)) { $ou = "TaskGo" }

$o = Read-Host "Organizacao" 
if ([string]::IsNullOrWhiteSpace($o)) { $o = "TaskGo" }

$l = Read-Host "Cidade" 
if ([string]::IsNullOrWhiteSpace($l)) { $l = "Sao Paulo" }

$st = Read-Host "Estado (sigla)" 
if ([string]::IsNullOrWhiteSpace($st)) { $st = "SP" }

$c = Read-Host "Pais (codigo de 2 letras)" 
if ([string]::IsNullOrWhiteSpace($c)) { $c = "BR" }

# Criar pasta para keystore (fora do projeto)
$keystoreDir = "$env:USERPROFILE\AndroidKeystores"
if (-not (Test-Path $keystoreDir)) {
    New-Item -ItemType Directory -Path $keystoreDir | Out-Null
    Write-Host "OK Pasta criada: $keystoreDir" -ForegroundColor Green
}

$keystorePath = "$keystoreDir\taskgo-release-key.jks"

Write-Host ""
Write-Host "Criando keystore..." -ForegroundColor Yellow

# Criar arquivo temporario com as respostas
$dname = "CN=$cn, OU=$ou, O=$o, L=$l, ST=$st, C=$c"

# Comando keytool
$keytoolArgs = @(
    "-genkey",
    "-v",
    "-keystore", $keystorePath,
    "-keyalg", "RSA",
    "-keysize", "2048",
    "-validity", "10000",
    "-alias", "taskgo-release",
    "-storepass", $keystorePasswordPlain,
    "-keypass", $aliasPasswordPlain,
    "-dname", $dname
)

try {
    & keytool $keytoolArgs
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "OK Keystore criado com sucesso!" -ForegroundColor Green
        Write-Host "  Local: $keystorePath" -ForegroundColor Cyan
        
        # Criar arquivo keystore.properties
        Write-Host ""
        Write-Host "Criando arquivo keystore.properties..." -ForegroundColor Yellow
        
        $keystorePathForProps = $keystorePath.Replace('\', '/')
        $propertiesContent = "TASKGO_RELEASE_STORE_FILE=$keystorePathForProps`nTASKGO_RELEASE_KEY_ALIAS=taskgo-release`nTASKGO_RELEASE_STORE_PASSWORD=$keystorePasswordPlain`nTASKGO_RELEASE_KEY_PASSWORD=$aliasPasswordPlain"
        
        $propertiesPath = "keystore.properties"
        $propertiesContent | Out-File -FilePath $propertiesPath -Encoding UTF8 -NoNewline
        
        Write-Host "OK Arquivo keystore.properties criado!" -ForegroundColor Green
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "  PROXIMOS PASSOS:" -ForegroundColor Yellow
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "1. Adicione ao .gitignore (ja esta adicionado):" -ForegroundColor Yellow
        Write-Host "   *.jks" -ForegroundColor White
        Write-Host "   *.keystore" -ForegroundColor White
        Write-Host "   keystore.properties" -ForegroundColor White
        Write-Host ""
        Write-Host "2. Descomente as linhas no app/build.gradle.kts:" -ForegroundColor Yellow
        Write-Host "   - Linhas 45-51 (carregamento do keystore.properties)" -ForegroundColor White
        Write-Host "   - Linhas 140-147 (signingConfigs)" -ForegroundColor White
        Write-Host "   - Linha 133 (signingConfig = ...)" -ForegroundColor White
        Write-Host ""
        Write-Host "3. Sincronize o projeto no Android Studio" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "4. Teste o build:" -ForegroundColor Yellow
        Write-Host "   .\gradlew.bat bundleRelease" -ForegroundColor White
        Write-Host ""
        Write-Host "IMPORTANTE: Guarde as senhas em local seguro!" -ForegroundColor Red
        Write-Host "IMPORTANTE: Faca backup do keystore!" -ForegroundColor Red
        Write-Host ""
        
    } else {
        Write-Host "ERRO ao criar keystore" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERRO: $_" -ForegroundColor Red
    exit 1
}
