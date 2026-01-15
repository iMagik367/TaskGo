# 🚀 Comandos Rápidos CLI - Correção Extensão Email

## ⚡ Solução Rápida (Copiar e Colar)

### 1. Verificar região do Firestore:
```powershell
gcloud firestore databases list --project=task-go-ee85f
```

### 2. Desinstalar extensão (se existir):
```powershell
# Listar extensões
firebase ext:list --project=task-go-ee85f

# Desinstalar (substitua INSTANCE_ID pelo ID real)
firebase ext:uninstall INSTANCE_ID --project=task-go-ee85f --force
```

### 3. Habilitar APIs necessárias:
```powershell
gcloud services enable cloudfunctions.googleapis.com --project=task-go-ee85f
gcloud services enable firestore.googleapis.com --project=task-go-ee85f
gcloud services enable cloudbuild.googleapis.com --project=task-go-ee85f
gcloud services enable secretmanager.googleapis.com --project=task-go-ee85f
gcloud services enable run.googleapis.com --project=task-go-ee85f
```

### 4. Instalar extensão (interativo):
```powershell
firebase ext:install firebase/firestore-send-email --project=task-go-ee85f
```

**Durante a instalação, quando perguntado sobre Location, digite:** `nam5` (região multi-região do seu Firestore)

### 5. Verificar instalação:
```powershell
firebase ext:list --project=task-go-ee85f
gcloud functions list --project=task-go-ee85f --filter="name:ext-firestore-send-email"
```

---

## 🎯 Usando o Script Automatizado (Recomendado)

### PowerShell:
```powershell
.\install-email-extension.ps1
```

### Bash (WSL/Git Bash):
```bash
chmod +x install-email-extension.sh
./install-email-extension.sh
```

---

## 📋 Sequência Completa de Comandos

```powershell
# 1. Verificar região
$region = (gcloud firestore databases list --project=task-go-ee85f --format="value(locationId)" | Select-Object -First 1)
Write-Host "Região do Firestore: $region"

# 2. Desinstalar (se necessário)
firebase ext:list --project=task-go-ee85f --json | ConvertFrom-Json | Select-Object -ExpandProperty result | Where-Object { $_.ref -like "*firestore-send-email*" } | ForEach-Object { firebase ext:uninstall $_.instanceId --project=task-go-ee85f --force }

# 3. Habilitar APIs
@("cloudfunctions.googleapis.com", "firestore.googleapis.com", "cloudbuild.googleapis.com", "secretmanager.googleapis.com", "run.googleapis.com") | ForEach-Object { gcloud services enable $_ --project=task-go-ee85f }

# 4. Instalar
firebase ext:install firebase/firestore-send-email --project=task-go-ee85f

# 5. Verificar
firebase ext:list --project=task-go-ee85f
```

---

## 🔍 Comandos de Diagnóstico

### Verificar status da extensão:
```powershell
firebase ext:list --project=task-go-ee85f
```

### Ver logs das Cloud Functions:
```powershell
gcloud functions logs read ext-firestore-send-email-processqueue --region=us-central1 --project=task-go-ee85f --limit=50
```

### Verificar Cloud Functions criadas:
```powershell
gcloud functions list --project=task-go-ee85f --filter="name:ext-firestore-send-email"
```

### Verificar APIs habilitadas:
```powershell
gcloud services list --enabled --project=task-go-ee85f --filter="name:cloudfunctions OR name:firestore"
```

---

## ⚠️ Troubleshooting Rápido

### Erro: "Permission denied"
```powershell
# Verificar permissões
gcloud projects get-iam-policy task-go-ee85f
```

### Erro: "API not enabled"
```powershell
# Habilitar todas as APIs de uma vez
gcloud services enable cloudfunctions.googleapis.com firestore.googleapis.com cloudbuild.googleapis.com secretmanager.googleapis.com run.googleapis.com --project=task-go-ee85f
```

### Erro: "Billing not enabled"
```powershell
# Verificar billing
gcloud billing projects describe task-go-ee85f
```

### Limpar recursos parciais:
```powershell
# Listar funções
gcloud functions list --project=task-go-ee85f

# Deletar função específica (ajuste o nome)
gcloud functions delete ext-firestore-send-email-processqueue --region=southamerica-east1 --project=task-go-ee85f --gen2
```

---

## 📝 Notas Importantes

1. **Região**: Use sempre a mesma região do Firestore (geralmente `us-central1`)
2. **SMTP**: Você precisará configurar credenciais SMTP durante a instalação
3. **Tempo**: A instalação pode levar 5-10 minutos
4. **Verificação**: Sempre verifique os logs após a instalação

---

## 🔗 Documentação Completa

Para mais detalhes, consulte:
- `GUIA_CORRECAO_EXTENSAO_EMAIL_CLI.md` - Guia completo CLI
- `GUIA_CORRECAO_EXTENSAO_EMAIL.md` - Guia geral


