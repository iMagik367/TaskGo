<# 
  Diagnóstico profundo (Android app + Firebase Functions)
  - Gera log em build_logs/diagnostico.log
  - Verifica ambiente (Java/Node/Firebase CLI)
  - Roda lint/test/build do app
  - Roda lint/build das functions
  - Checa presença de artefatos críticos (assetlinks.json, firebase.json, rules)
#>

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $repoRoot

$logDir = Join-Path $repoRoot "build_logs"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Force -Path $logDir | Out-Null }
$logFile = Join-Path $logDir "diagnostico.log"

function Write-Log($msg) {
    $timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    "$timestamp $msg" | Tee-Object -FilePath $logFile -Append
}

function Run-Step($name, $cmd) {
    Write-Log "===> $name"
    try {
        & $cmd | Tee-Object -FilePath $logFile -Append
        Write-Log "<=== $name OK"
    } catch {
        Write-Log "<=== $name FALHOU: $($_.Exception.Message)"
        throw
    }
}

Write-Log "Iniciando diagnóstico profundo"

# Ambiente
Write-Log "Versões"
try { java -version 2>&1 | Tee-Object -FilePath $logFile -Append } catch {}
try { node --version | Tee-Object -FilePath $logFile -Append } catch {}
try { firebase --version | Tee-Object -FilePath $logFile -Append } catch {}

# Checagens rápidas de artefatos críticos
Write-Log "Checando artefatos críticos"
@(
    "firebase.json",
    "firestore.rules",
    "storage.rules",
    "public\.well-known\assetlinks.json"
) | ForEach-Object {
    $path = Join-Path $repoRoot $_
    if (Test-Path $path) { Write-Log "OK: $_" } else { Write-Log "FALTA: $_" }
}

# App Android: lint + unit tests + assemble debug (rápido)
Run-Step "Gradle lintDebug" { .\gradlew.bat --no-daemon :app:lintDebug }
Run-Step "Gradle testDebugUnitTest" { .\gradlew.bat --no-daemon :app:testDebugUnitTest }
Run-Step "Gradle assembleDebug" { .\gradlew.bat --no-daemon :app:assembleDebug }

# Functions: npm ci + lint + build
Push-Location (Join-Path $repoRoot "functions")
Run-Step "Functions npm ci" { npm ci }
Run-Step "Functions lint" { npm run lint }
Run-Step "Functions build" { npm run build }
Pop-Location

Write-Log "Diagnóstico concluído"
Write-Output "Diagnóstico concluído. Verifique o log em $logFile"
