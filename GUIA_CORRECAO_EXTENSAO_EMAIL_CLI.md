# 🔧 Guia CLI: Correção da Extensão "Trigger Email from Firestore"

## 📋 Pré-requisitos

Antes de começar, certifique-se de ter:

1. **Firebase CLI instalado**:
   ```bash
   npm install -g firebase-tools
   ```

2. **gcloud CLI instalado** (para verificar regiões):
   ```bash
   # Windows (via PowerShell)
   (New-Object Net.WebClient).DownloadFile("https://dl.google.com/dl/cloudsdk/channels/rapid/GoogleCloudSDKInstaller.exe", "$env:Temp\GoogleCloudSDKInstaller.exe")
   & $env:Temp\GoogleCloudSDKInstaller.exe
   ```

3. **Autenticado no Firebase**:
   ```bash
   firebase login
   ```

4. **Autenticado no Google Cloud**:
   ```bash
   gcloud auth login
   gcloud config set project task-go-ee85f
   ```

---

## 🔍 Passo 1: Verificar a Região do Firestore

### Via gcloud CLI:

```bash
# Listar todos os bancos de dados Firestore do projeto
gcloud firestore databases list --project=task-go-ee85f
```

**Saída esperada:**
```
NAME      LOCATION          TYPE
(default) us-central1       FIRESTORE_NATIVE
```

Anote a **LOCATION** (ex: `us-central1`). Você precisará dela para instalar a extensão.

### Alternativa via Firebase CLI:

```bash
# Navegar para o diretório do projeto
cd c:\Users\user\AndroidStudioProjects\TaskGoApp

# Verificar configuração do projeto
firebase projects:list

# Verificar configuração atual
firebase use
```

---

## 🗑️ Passo 2: Desinstalar a Extensão (se necessário)

### Verificar extensões instaladas:

```bash
# Listar todas as extensões instaladas
firebase ext:list --project=task-go-ee85f
```

### Desinstalar a extensão:

```bash
# Desinstalar a extensão "Trigger Email from Firestore"
# Substitua EXTENSION_INSTANCE_ID pelo ID da instalação (obtido no comando anterior)
firebase ext:uninstall ext-firestore-send-email --project=task-go-ee85f
```

**OU** se você não souber o ID exato:

```bash
# Listar extensões e identificar o ID
firebase ext:list --project=task-go-ee85f

# Desinstalar usando o ID completo (exemplo)
firebase ext:uninstall firebase/firestore-send-email@0.1.XX --project=task-go-ee85f
```

**Nota:** Se a extensão não estiver instalada ou não aparecer na lista, pule este passo.

---

## 🧹 Passo 3: Limpar Recursos Parciais (se houver)

Se a instalação anterior falhou parcialmente, pode haver recursos criados:

### Verificar Cloud Functions criadas:

```bash
# Listar todas as Cloud Functions
gcloud functions list --project=task-go-ee85f --format="table(name,status,region)"

# OU para Cloud Functions v2
gcloud functions list --gen2 --project=task-go-ee85f --format="table(name,state,location)"
```

### Deletar Cloud Functions relacionadas (se necessário):

```bash
# Deletar função específica (ajuste o nome conforme necessário)
gcloud functions delete ext-firestore-send-email-processqueue --region=southamerica-east1 --project=task-go-ee85f --gen2

# OU para Cloud Functions v1
gcloud functions delete ext-firestore-send-email-processqueue --region=southamerica-east1 --project=task-go-ee85f
```

**⚠️ CUIDADO:** Só delete funções que começam com `ext-firestore-send-email-*` e que foram criadas pela extensão.

---

## ✅ Passo 4: Verificar APIs Habilitadas

A extensão precisa de algumas APIs habilitadas:

```bash
# Verificar APIs habilitadas
gcloud services list --enabled --project=task-go-ee85f

# Habilitar APIs necessárias (se não estiverem habilitadas)
gcloud services enable cloudfunctions.googleapis.com --project=task-go-ee85f
gcloud services enable firestore.googleapis.com --project=task-go-ee85f
gcloud services enable cloudbuild.googleapis.com --project=task-go-ee85f
gcloud services enable secretmanager.googleapis.com --project=task-go-ee85f
```

---

## 📦 Passo 5: Instalar a Extensão com a Região Correta

### Opção A: Instalação Interativa (Recomendado para primeira vez)

```bash
# Iniciar instalação interativa
firebase ext:install firebase/firestore-send-email --project=task-go-ee85f
```

Durante a instalação interativa, você será perguntado:
- **Location**: Digite `us-central1` (ou a região do seu Firestore)
- **Firestore Database**: Digite `(default)`
- **SMTP Connection URI**: Configure seu servidor SMTP
- Outros parâmetros conforme necessário

### Opção B: Instalação com Parâmetros (Avançado)

Crie um arquivo de configuração `ext-config.json`:

```json
{
  "params": {
    "LOCATION": "us-central1",
    "FIRESTORE_COLLECTION_NAME": "mail",
    "SMTP_CONNECTION_URI": "smtps://username:password@smtp.example.com:465",
    "DEFAULT_FROM": "noreply@example.com",
    "DEFAULT_REPLY_TO": "support@example.com",
    "USERS_COLLECTION": "users",
    "SMTP_PASSWORD": "your-smtp-password"
  }
}
```

Depois instale:

```bash
# Instalar com arquivo de configuração
firebase ext:install firebase/firestore-send-email --project=task-go-ee85f --params=ext-config.json
```

### Opção C: Instalação com Parâmetros Inline

```bash
# Instalar especificando parâmetros diretamente
firebase ext:install firebase/firestore-send-email \
  --project=task-go-ee85f \
  --params=LOCATION=us-central1,FIRESTORE_COLLECTION_NAME=mail,SMTP_CONNECTION_URI=smtps://user:pass@smtp.example.com:465
```

**⚠️ IMPORTANTE:** 
- Substitua `us-central1` pela região do seu Firestore (obtida no Passo 1)
- Configure o `SMTP_CONNECTION_URI` com suas credenciais SMTP reais

---

## 🔍 Passo 6: Verificar Instalação

### Verificar extensão instalada:

```bash
# Listar extensões instaladas
firebase ext:list --project=task-go-ee85f
```

### Verificar Cloud Functions criadas:

```bash
# Listar funções criadas pela extensão
gcloud functions list --project=task-go-ee85f --filter="name:ext-firestore-send-email" --format="table(name,status,region)"
```

### Verificar logs da extensão:

```bash
# Ver logs das Cloud Functions
gcloud functions logs read ext-firestore-send-email-processqueue --region=us-central1 --project=task-go-ee85f --limit=50
```

### Verificar status da instalação:

```bash
# Ver informações detalhadas da extensão
firebase ext:info firebase/firestore-send-email --project=task-go-ee85f
```

---

## 🧪 Passo 7: Testar a Extensão

### Criar um documento de teste no Firestore:

```bash
# Usar gcloud para criar um documento de teste
gcloud firestore documents create \
  --collection=mail \
  --data='{"to":"test@example.com","message":{"subject":"Test","text":"This is a test"}}' \
  --project=task-go-ee85f \
  --database="(default)"
```

**OU** via Firebase CLI (se disponível):

```bash
# Usar firebase-tools para criar documento
# (pode requerer configuração adicional)
```

### Verificar se o email foi enviado:

```bash
# Verificar logs da função para ver se processou o email
gcloud functions logs read ext-firestore-send-email-processqueue \
  --region=us-central1 \
  --project=task-go-ee85f \
  --limit=10 \
  --format="table(timestamp,severity,textPayload)"
```

---

## 🔧 Script Completo (PowerShell)

Crie um arquivo `install-email-extension.ps1`:

```powershell
# Script para instalar a extensão Trigger Email from Firestore
# Uso: .\install-email-extension.ps1

$PROJECT_ID = "task-go-ee85f"
$EXTENSION_ID = "firebase/firestore-send-email"
$LOCATION = "us-central1"  # Ajuste conforme sua região do Firestore

Write-Host "🔍 Verificando região do Firestore..." -ForegroundColor Cyan
gcloud firestore databases list --project=$PROJECT_ID

Write-Host "`n🗑️ Verificando extensões instaladas..." -ForegroundColor Cyan
firebase ext:list --project=$PROJECT_ID

Write-Host "`n🧹 Limpando instalações anteriores (se houver)..." -ForegroundColor Yellow
# Desinstalar se existir
$extensions = firebase ext:list --project=$PROJECT_ID --json | ConvertFrom-Json
if ($extensions.result -and $extensions.result.Count -gt 0) {
    foreach ($ext in $extensions.result) {
        if ($ext.ref -like "*firestore-send-email*") {
            Write-Host "Desinstalando: $($ext.ref)" -ForegroundColor Yellow
            firebase ext:uninstall $ext.instanceId --project=$PROJECT_ID --force
        }
    }
}

Write-Host "`n✅ Verificando APIs habilitadas..." -ForegroundColor Cyan
gcloud services enable cloudfunctions.googleapis.com --project=$PROJECT_ID
gcloud services enable firestore.googleapis.com --project=$PROJECT_ID
gcloud services enable cloudbuild.googleapis.com --project=$PROJECT_ID
gcloud services enable secretmanager.googleapis.com --project=$PROJECT_ID

Write-Host "`n📦 Instalando extensão..." -ForegroundColor Green
Write-Host "⚠️ Você precisará fornecer os parâmetros SMTP durante a instalação" -ForegroundColor Yellow
firebase ext:install $EXTENSION_ID --project=$PROJECT_ID

Write-Host "`n🔍 Verificando instalação..." -ForegroundColor Cyan
firebase ext:list --project=$PROJECT_ID
gcloud functions list --project=$PROJECT_ID --filter="name:ext-firestore-send-email"

Write-Host "`n✅ Instalação concluída!" -ForegroundColor Green
```

**Uso:**
```powershell
.\install-email-extension.ps1
```

---

## 🔧 Script Completo (Bash - para WSL/Git Bash)

Crie um arquivo `install-email-extension.sh`:

```bash
#!/bin/bash

PROJECT_ID="task-go-ee85f"
EXTENSION_ID="firebase/firestore-send-email"
LOCATION="us-central1"  # Ajuste conforme sua região do Firestore

echo "🔍 Verificando região do Firestore..."
gcloud firestore databases list --project=$PROJECT_ID

echo ""
echo "🗑️ Verificando extensões instaladas..."
firebase ext:list --project=$PROJECT_ID

echo ""
echo "🧹 Limpando instalações anteriores (se houver)..."
# Desinstalar se existir
firebase ext:list --project=$PROJECT_ID --json | jq -r '.result[]? | select(.ref | contains("firestore-send-email")) | .instanceId' | while read instanceId; do
    echo "Desinstalando: $instanceId"
    firebase ext:uninstall "$instanceId" --project=$PROJECT_ID --force
done

echo ""
echo "✅ Verificando APIs habilitadas..."
gcloud services enable cloudfunctions.googleapis.com --project=$PROJECT_ID
gcloud services enable firestore.googleapis.com --project=$PROJECT_ID
gcloud services enable cloudbuild.googleapis.com --project=$PROJECT_ID
gcloud services enable secretmanager.googleapis.com --project=$PROJECT_ID

echo ""
echo "📦 Instalando extensão..."
echo "⚠️ Você precisará fornecer os parâmetros SMTP durante a instalação"
firebase ext:install $EXTENSION_ID --project=$PROJECT_ID

echo ""
echo "🔍 Verificando instalação..."
firebase ext:list --project=$PROJECT_ID
gcloud functions list --project=$PROJECT_ID --filter="name:ext-firestore-send-email"

echo ""
echo "✅ Instalação concluída!"
```

**Uso:**
```bash
chmod +x install-email-extension.sh
./install-email-extension.sh
```

---

## 📝 Comandos Úteis Adicionais

### Atualizar configuração da extensão:

```bash
# Atualizar parâmetros da extensão
firebase ext:configure firebase/firestore-send-email --project=task-go-ee85f
```

### Ver logs em tempo real:

```bash
# Seguir logs da função
gcloud functions logs tail ext-firestore-send-email-processqueue \
  --region=us-central1 \
  --project=task-go-ee85f
```

### Verificar status da função:

```bash
# Ver detalhes da função
gcloud functions describe ext-firestore-send-email-processqueue \
  --region=us-central1 \
  --project=task-go-ee85f \
  --gen2
```

### Listar todas as extensões disponíveis:

```bash
# Ver extensões disponíveis no marketplace
firebase ext:list --available
```

---

## ⚠️ Troubleshooting

### Erro: "Extension not found"

```bash
# Verificar se a extensão existe
firebase ext:list --available | grep firestore-send-email
```

### Erro: "Permission denied"

```bash
# Verificar permissões
gcloud projects get-iam-policy task-go-ee85f

# Verificar se você tem as permissões necessárias
gcloud projects describe task-go-ee85f
```

### Erro: "API not enabled"

```bash
# Habilitar todas as APIs necessárias
gcloud services enable cloudfunctions.googleapis.com --project=task-go-ee85f
gcloud services enable firestore.googleapis.com --project=task-go-ee85f
gcloud services enable cloudbuild.googleapis.com --project=task-go-ee85f
gcloud services enable secretmanager.googleapis.com --project=task-go-ee85f
gcloud services enable run.googleapis.com --project=task-go-ee85f
```

### Verificar billing:

```bash
# Verificar se o billing está habilitado
gcloud billing projects describe task-go-ee85f
```

---

## 📞 Próximos Passos

1. ✅ Execute o Passo 1 para verificar a região do Firestore
2. ✅ Execute o Passo 2 para desinstalar (se necessário)
3. ✅ Execute o Passo 5 para instalar com a região correta
4. ✅ Execute o Passo 6 para verificar a instalação
5. ✅ Execute o Passo 7 para testar

Se encontrar problemas, verifique a seção **Troubleshooting** acima.

















