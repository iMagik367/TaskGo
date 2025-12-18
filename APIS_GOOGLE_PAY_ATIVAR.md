# 🔑 APIs do Google que Precisam ser Ativadas para o Google Pay

## ✅ APIs Obrigatórias para Google Pay

Para o Google Pay funcionar corretamente no TaskGo App, as seguintes APIs precisam estar **ATIVADAS** no Google Cloud Console:

### 1. **Google Pay API** ⭐ (Principal)
- **Nome**: Google Pay API
- **ID**: `payments.googleapis.com`
- **Status**: ⚠️ **PRECISA SER ATIVADA**
- **Descrição**: API principal para processar pagamentos via Google Pay
- **Link**: https://console.cloud.google.com/apis/library/payments.googleapis.com?project=task-go-ee85f

### 2. **Google Pay for Passes API**
- **Nome**: Google Pay for Passes API
- **ID**: `walletobjects.googleapis.com`
- **Status**: ⚠️ **PRECISA SER ATIVADA** (se usar passes/cartões virtuais)
- **Descrição**: Para criar e gerenciar passes do Google Pay
- **Link**: https://console.cloud.google.com/apis/library/walletobjects.googleapis.com?project=task-go-ee85f

### 3. **Identity Toolkit API**
- **Nome**: Identity Toolkit API
- **ID**: `identitytoolkit.googleapis.com`
- **Status**: ✅ **JÁ ATIVADA** (usada para Firebase Auth)
- **Descrição**: Autenticação de usuários

### 4. **Secure Token Service API**
- **Nome**: Secure Token Service API
- **ID**: `securetoken.googleapis.com`
- **Status**: ⚠️ **PRECISA SER ATIVADA** (pode estar bloqueada)
- **Descrição**: Geração de tokens seguros para autenticação
- **Link**: https://console.cloud.google.com/apis/library/securetoken.googleapis.com?project=task-go-ee85f

### 5. **Cloud Firestore API**
- **Nome**: Cloud Firestore API
- **ID**: `firestore.googleapis.com`
- **Status**: ✅ **JÁ ATIVADA**
- **Descrição**: Banco de dados NoSQL

### 6. **Cloud Functions API**
- **Nome**: Cloud Functions API
- **ID**: `cloudfunctions.googleapis.com`
- **Status**: ✅ **JÁ ATIVADA**
- **Descrição**: Funções serverless

### 7. **Cloud Storage API**
- **Nome**: Cloud Storage API
- **ID**: `storage-component.googleapis.com`
- **Status**: ✅ **JÁ ATIVADA**
- **Descrição**: Armazenamento de arquivos

---

## 📋 Como Ativar as APIs

### Passo 1: Acessar o Google Cloud Console
1. Acesse: https://console.cloud.google.com/apis/library?project=task-go-ee85f
2. Ou use os links diretos acima para cada API

### Passo 2: Ativar cada API
1. Clique no nome da API
2. Clique no botão **"ENABLE"** ou **"ATIVAR"**
3. Aguarde a confirmação

### Passo 3: Verificar Status
- Todas as APIs devem aparecer como **"ENABLED"** ou **"ATIVADA"**

---

## ⚠️ APIs que Podem Estar Bloqueadas

### Secure Token Service API
- **Problema**: Esta API pode estar bloqueada por padrão
- **Solução**: 
  1. Acesse: https://console.cloud.google.com/apis/library/securetoken.googleapis.com?project=task-go-ee85f
  2. Se estiver bloqueada, clique em **"REQUEST ACCESS"** ou **"SOLICITAR ACESSO"**
  3. Preencha o formulário explicando o uso (autenticação Firebase)
  4. Aguarde aprovação (pode levar alguns dias)

---

## 🔍 Verificar APIs Ativadas

### Via Console
1. Acesse: https://console.cloud.google.com/apis/dashboard?project=task-go-ee85f
2. Veja todas as APIs ativadas na lista

### Via Comando
```bash
gcloud services list --enabled --project=task-go-ee85f
```

---

## ✅ Checklist Final

- [ ] Google Pay API (`payments.googleapis.com`) - **ATIVAR**
- [ ] Google Pay for Passes API (`walletobjects.googleapis.com`) - **ATIVAR** (opcional)
- [ ] Secure Token Service API (`securetoken.googleapis.com`) - **VERIFICAR/ATIVAR**
- [x] Identity Toolkit API (`identitytoolkit.googleapis.com`) - **JÁ ATIVADA**
- [x] Cloud Firestore API (`firestore.googleapis.com`) - **JÁ ATIVADA**
- [x] Cloud Functions API (`cloudfunctions.googleapis.com`) - **JÁ ATIVADA**
- [x] Cloud Storage API (`storage-component.googleapis.com`) - **JÁ ATIVADA**

---

## 📝 Notas Importantes

1. **Google Pay API** é a mais importante e **DEVE** estar ativada
2. **Secure Token Service API** pode precisar de aprovação manual
3. Após ativar as APIs, pode levar alguns minutos para propagação
4. Verifique se a API Key tem permissões para essas APIs
5. Para produção, configure restrições de API na API Key

---

## 🔗 Links Úteis

- **Google Cloud Console**: https://console.cloud.google.com/?project=task-go-ee85f
- **APIs & Services**: https://console.cloud.google.com/apis/dashboard?project=task-go-ee85f
- **Credentials**: https://console.cloud.google.com/apis/credentials?project=task-go-ee85f
- **Google Pay Documentation**: https://developers.google.com/pay/api/android/overview

---

**Última atualização**: $(Get-Date -Format "dd/MM/yyyy HH:mm")

