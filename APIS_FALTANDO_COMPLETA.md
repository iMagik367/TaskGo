# 🔍 Análise Completa - APIs que AINDA FALTAM ser Ativadas

## 📊 Resumo Executivo

**Total de APIs ativadas:** ~85 APIs  
**Total de APIs necessárias:** ~95 APIs  
**APIs faltando:** ~10 APIs críticas

---

## ❌ APIs CRÍTICAS que FALTAM (OBRIGATÓRIAS)

### 1. ⚠️ **Cloud Functions API** 
**Nome exato:** `Cloud Functions API`  
**API ID:** `cloudfunctions.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🔴 **CRÍTICA**

**Onde é usada:**
- ✅ Todas as Cloud Functions do Firebase (28 funções)
- ✅ Deploy de funções via Firebase CLI
- ✅ Execução de funções serverless
- ✅ Funções agendadas (Pub/Sub triggers)

**Impacto sem esta API:**
- ❌ Nenhuma Cloud Function funcionará
- ❌ Criação de pedidos falhará
- ❌ Pagamentos falharão
- ❌ Notificações push não funcionarão
- ❌ Chat com IA não funcionará
- ❌ Verificação 2FA não funcionará
- ❌ Exclusão de conta falhará

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/cloudfunctions.googleapis.com?project=task-go-ee85f
```

---

### 2. ⚠️ **Cloud Scheduler API**
**Nome exato:** `Cloud Scheduler API`  
**API ID:** `cloudscheduler.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🔴 **CRÍTICA**

**Onde é usada:**
- ✅ `sendGradualNotifications` - Agendada a cada 6 horas
- ✅ `checkAndRefundUnshippedOrders` - Agendada a cada 5 minutos
- ✅ `scheduledTrackingUpdate` - Agendada a cada 1 hora
- ✅ `cleanupExpiredTwoFactorCodes` - Agendada a cada 1 hora

**Impacto sem esta API:**
- ❌ Notificações graduais não serão enviadas
- ❌ Reembolsos automáticos não funcionarão
- ❌ Atualizações de tracking não funcionarão
- ❌ Limpeza de códigos 2FA expirados não funcionará

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/cloudscheduler.googleapis.com?project=task-go-ee85f
```

---

### 3. ⚠️ **Secret Manager API**
**Nome exato:** `Secret Manager API`  
**API ID:** `secretmanager.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🟡 **IMPORTANTE**

**Onde é usada:**
- ✅ Gerenciar secrets das Cloud Functions
- ✅ Armazenar API Keys de forma segura
- ✅ STRIPE_SECRET_KEY
- ✅ STRIPE_WEBHOOK_SECRET
- ✅ OPENAI_API_KEY (se usado)

**Impacto sem esta API:**
- ⚠️ Secrets devem ser configurados via Firebase Console (ainda funciona, mas menos seguro)
- ⚠️ Não há gerenciamento centralizado de secrets
- ✅ Funcionalidade ainda funciona, mas não é a melhor prática

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/secretmanager.googleapis.com?project=task-go-ee85f
```

---

### 4. ⚠️ **Cloud Build API**
**Nome exato:** `Cloud Build API`  
**API ID:** `cloudbuild.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🟡 **IMPORTANTE**

**Onde é usada:**
- ✅ Deploy de Cloud Functions via Firebase CLI
- ✅ Build automático de funções
- ✅ CI/CD pipelines

**Impacto sem esta API:**
- ❌ Deploy de Cloud Functions falhará
- ❌ Builds automáticos não funcionarão
- ⚠️ Pode funcionar se já estiver deployado, mas novos deploys falharão

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/cloudbuild.googleapis.com?project=task-go-ee85f
```

---

### 5. ⚠️ **Artifact Registry API**
**Nome exato:** `Artifact Registry API`  
**API ID:** `artifactregistry.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🟡 **IMPORTANTE**

**Onde é usada:**
- ✅ Armazenar imagens Docker para Cloud Functions
- ✅ Build e deploy de funções
- ✅ CI/CD pipelines

**Impacto sem esta API:**
- ⚠️ Pode afetar deploy de Cloud Functions
- ⚠️ Pode funcionar com configuração alternativa, mas não é recomendado

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/artifactregistry.googleapis.com?project=task-go-ee85f
```

---

### 6. ⚠️ **Generative Language API (Gemini)**
**Nome exato:** `Generative Language API`  
**API ID:** `generativelanguage.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🔴 **CRÍTICA** (para chat com IA)

**Onde é usada:**
- ✅ Chat com IA no app (`GoogleCloudAIService.kt`)
- ✅ Modelo: `gemini-2.5-flash-latest`

**Impacto sem esta API:**
- ❌ Chat com IA não funcionará
- ❌ Todas as requisições de chat falharão

**Nota:** Esta API usa uma API Key separada (`AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4`) e pode estar em um projeto diferente.

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com?project=task-go-ee85f
```

---

### 7. ⚠️ **Cloud Translation API**
**Nome exato:** `Cloud Translation API`  
**API ID:** `translate.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🟢 **OPCIONAL** (se usado para tradução)

**Onde é usada:**
- ⚠️ Pode ser usado para tradução de mensagens no chat
- ⚠️ Verificar se está realmente sendo usado no código

**Impacto sem esta API:**
- ⚠️ Tradução automática não funcionará (se usada)
- ✅ Chat ainda funciona sem tradução

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/translate.googleapis.com?project=task-go-ee85f
```

---

### 8. ⚠️ **Cloud Pub/Sub API**
**Nome exato:** `Cloud Pub/Sub API`  
**API ID:** `pubsub.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🔴 **CRÍTICA**

**Onde é usada:**
- ✅ Funções agendadas (`functions.pubsub.schedule()`)
- ✅ `sendGradualNotifications` - Pub/Sub schedule
- ✅ `checkAndRefundUnshippedOrders` - Pub/Sub schedule
- ✅ `scheduledTrackingUpdate` - Pub/Sub schedule
- ✅ `cleanupExpiredTwoFactorCodes` - Pub/Sub schedule

**Impacto sem esta API:**
- ❌ Funções agendadas não funcionarão
- ❌ Todos os schedules falharão

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/pubsub.googleapis.com?project=task-go-ee85f
```

---

### 9. ⚠️ **Cloud Resource Manager API**
**Nome exato:** `Cloud Resource Manager API`  
**API ID:** `cloudresourcemanager.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🟡 **IMPORTANTE**

**Onde é usada:**
- ✅ Gerenciamento de recursos do projeto
- ✅ Operações administrativas
- ✅ Pode ser necessário para algumas operações do Firebase

**Impacto sem esta API:**
- ⚠️ Algumas operações administrativas podem falhar
- ⚠️ Pode afetar configurações de projeto

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/cloudresourcemanager.googleapis.com?project=task-go-ee85f
```

---

### 10. ⚠️ **Service Usage API**
**Nome exato:** `Service Usage API`  
**API ID:** `serviceusage.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🟡 **IMPORTANTE**

**Onde é usada:**
- ✅ Gerenciamento de uso de serviços
- ✅ Verificação de quotas
- ✅ Pode ser necessário para algumas operações

**Impacto sem esta API:**
- ⚠️ Algumas verificações de quota podem falhar
- ⚠️ Geralmente funciona, mas pode causar problemas em casos específicos

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/serviceusage.googleapis.com?project=task-go-ee85f
```

---

### 11. ⚠️ **Cloud Monitoring API**
**Nome exato:** `Cloud Monitoring API`  
**API ID:** `monitoring.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🟢 **OPCIONAL**

**Onde é usada:**
- ✅ Monitoramento de Cloud Functions
- ✅ Métricas e alertas
- ✅ Dashboard de métricas

**Impacto sem esta API:**
- ⚠️ Métricas detalhadas não estarão disponíveis
- ✅ Funcionalidade principal não é afetada

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/monitoring.googleapis.com?project=task-go-ee85f
```

---

### 12. ⚠️ **Cloud Trace API**
**Nome exato:** `Cloud Trace API`  
**API ID:** `cloudtrace.googleapis.com`  
**Status:** ❌ **FALTANDO**  
**Prioridade:** 🟢 **OPCIONAL**

**Onde é usada:**
- ✅ Rastreamento de performance de Cloud Functions
- ✅ Análise de latência
- ✅ Debugging de performance

**Impacto sem esta API:**
- ⚠️ Rastreamento de performance não estará disponível
- ✅ Funcionalidade principal não é afetada

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/cloudtrace.googleapis.com?project=task-go-ee85f
```

---

### 13. ⚠️ **Firebase Realtime Database API** (verificar se está ativada)
**Nome exato:** `Firebase Realtime Database API`  
**API ID:** `firebasedatabase.googleapis.com`  
**Status:** ⚠️ **VERIFICAR**  
**Prioridade:** 🟡 **IMPORTANTE**

**Nota:** Você tem "Firebase Realtime Database Management API" ativada, mas pode precisar também da API básica do Realtime Database.

**Onde é usada:**
- ✅ Realtime Database para mensagens
- ✅ Presence (status online/offline)
- ✅ Typing indicators
- ✅ Identity verifications (em alguns casos)

**Impacto sem esta API:**
- ❌ Realtime Database não funcionará
- ❌ Mensagens em tempo real falharão

**URL para ativar:**
```
https://console.cloud.google.com/apis/library/firebasedatabase.googleapis.com?project=task-go-ee85f
```

---

## 📋 APIs que JÁ ESTÃO ATIVADAS (Confirmadas)

### Firebase APIs ✅
- ✅ Firebase Installations API
- ✅ Firebase Cloud Messaging API
- ✅ Identity Toolkit API
- ✅ Cloud Firestore API
- ✅ Cloud Storage API
- ✅ Firebase App Check API
- ✅ Firebase Realtime Database Management API
- ✅ Firebase Rules API
- ✅ Firebase Management API
- ✅ Firebase ML API
- ✅ Firebase Remote Config API
- ✅ ML Kit API
- ✅ Token Service API

### Google Cloud Core APIs ✅
- ✅ Cloud Logging API
- ✅ Cloud Storage
- ✅ Cloud Vision API
- ✅ Cloud Identity API
- ✅ Security Token Service API
- ✅ Service Control API
- ✅ Service Management API

### Maps APIs ✅
- ✅ Maps SDK for Android
- ✅ Maps SDK for iOS
- ✅ Geocoding API
- ✅ Geolocation API
- ✅ Places API
- ✅ Routes API
- ✅ Roads API
- ✅ Maps Elevation API
- ✅ Maps Static API
- ✅ Maps JavaScript API
- ✅ Maps Embed API

### Outras APIs ✅
- ✅ Google Play Integrity API
- ✅ IAM API
- ✅ Cloud Tasks API
- ✅ Storage Transfer API
- ✅ Cloud Location Finder API
- ✅ Cloud Key Management Service (KMS) API

---

## 🎯 LISTA FINAL - APIs que DEVEM ser Ativadas

### 🔴 PRIORIDADE CRÍTICA (Ativar IMEDIATAMENTE):

1. **Cloud Functions API** (`cloudfunctions.googleapis.com`)
   - ⚠️ SEM ESTA API, NENHUMA CLOUD FUNCTION FUNCIONA
   - **CRÍTICO:** Ative esta PRIMEIRO

2. **Cloud Pub/Sub API** (`pubsub.googleapis.com`)
   - ⚠️ SEM ESTA API, FUNÇÕES AGENDADAS NÃO FUNCIONAM
   - **CRÍTICO:** Necessária para schedules

3. **Cloud Scheduler API** (`cloudscheduler.googleapis.com`)
   - ⚠️ NECESSÁRIA PARA FUNÇÕES AGENDADAS
   - **CRÍTICO:** Trabalha junto com Pub/Sub

4. **Generative Language API** (`generativelanguage.googleapis.com`)
   - ⚠️ SEM ESTA API, CHAT COM IA NÃO FUNCIONA
   - **CRÍTICO:** Para funcionalidade de chat

5. **Firebase Realtime Database API** (`firebasedatabase.googleapis.com`)
   - ⚠️ VERIFICAR se já está ativada
   - ⚠️ SEM ESTA API, MENSAGENS EM TEMPO REAL NÃO FUNCIONAM
   - **CRÍTICO:** Para chat em tempo real

### 🟡 PRIORIDADE ALTA (Ativar em breve):

6. **Cloud Build API** (`cloudbuild.googleapis.com`)
   - Necessária para deploy de Cloud Functions

7. **Artifact Registry API** (`artifactregistry.googleapis.com`)
   - Necessária para build e deploy

8. **Secret Manager API** (`secretmanager.googleapis.com`)
   - Recomendada para gerenciar secrets

### 🟢 PRIORIDADE MÉDIA (Opcional):

9. **Cloud Translation API** (`translate.googleapis.com`)
   - Apenas se usar tradução automática

10. **Cloud Resource Manager API** (`cloudresourcemanager.googleapis.com`)
    - Para operações administrativas

11. **Service Usage API** (`serviceusage.googleapis.com`)
    - Para gerenciamento de uso

12. **Cloud Monitoring API** (`monitoring.googleapis.com`)
    - Para métricas e monitoramento

13. **Cloud Trace API** (`cloudtrace.googleapis.com`)
    - Para rastreamento de performance

---

## 📝 Como Ativar

### Método 1: Via Google Cloud Console (Recomendado)

1. Acesse: https://console.cloud.google.com/apis/library?project=task-go-ee85f
2. Para cada API na lista acima, procure pelo nome ou API ID
3. Clique na API
4. Clique em **"ENABLE"** ou **"ATIVAR"**
5. Aguarde alguns segundos para a ativação

### Método 2: Via gcloud CLI (Individual)

```powershell
# APIs CRÍTICAS (Execute estas PRIMEIRO)
gcloud services enable cloudfunctions.googleapis.com --project=task-go-ee85f
gcloud services enable pubsub.googleapis.com --project=task-go-ee85f
gcloud services enable cloudscheduler.googleapis.com --project=task-go-ee85f
gcloud services enable generativelanguage.googleapis.com --project=task-go-ee85f
gcloud services enable firebasedatabase.googleapis.com --project=task-go-ee85f

# APIs IMPORTANTES
gcloud services enable cloudbuild.googleapis.com --project=task-go-ee85f
gcloud services enable artifactregistry.googleapis.com --project=task-go-ee85f
gcloud services enable secretmanager.googleapis.com --project=task-go-ee85f

# APIs OPCIONAIS
gcloud services enable translate.googleapis.com --project=task-go-ee85f
gcloud services enable cloudresourcemanager.googleapis.com --project=task-go-ee85f
gcloud services enable serviceusage.googleapis.com --project=task-go-ee85f
gcloud services enable monitoring.googleapis.com --project=task-go-ee85f
gcloud services enable cloudtrace.googleapis.com --project=task-go-ee85f
```

### Método 3: Via Script PowerShell (Recomendado)

Execute o script `COMANDO_ATIVAR_TODAS_APIS.ps1` que foi criado na raiz do projeto:

```powershell
.\COMANDO_ATIVAR_TODAS_APIS.ps1
```

O script:
- ✅ Ativa todas as APIs críticas automaticamente
- ✅ Ativa APIs importantes
- ✅ Pergunta se deseja ativar APIs opcionais
- ✅ Mostra resumo final
- ✅ Indica quais APIs foram ativadas com sucesso

---

## ✅ Checklist de Ativação

### APIs Críticas:
- [ ] Cloud Functions API
- [ ] Cloud Pub/Sub API
- [ ] Cloud Scheduler API
- [ ] Generative Language API (Gemini)

### APIs Importantes:
- [ ] Cloud Build API
- [ ] Artifact Registry API
- [ ] Secret Manager API

### APIs Opcionais:
- [ ] Cloud Translation API (se usar tradução)
- [ ] Cloud Resource Manager API
- [ ] Service Usage API
- [ ] Cloud Monitoring API
- [ ] Cloud Trace API

### APIs para Verificar:
- [ ] Firebase Realtime Database API (verificar se já está ativada)
  - Você tem "Firebase Realtime Database Management API" ativada
  - Pode precisar também da API básica: `firebasedatabase.googleapis.com`

---

## 🔍 Verificação Pós-Ativação

Após ativar todas as APIs, verificar:

1. **Cloud Functions:**
   ```powershell
   firebase functions:list
   ```
   Deve listar todas as funções sem erros

2. **Funções Agendadas:**
   ```powershell
   gcloud scheduler jobs list --project=task-go-ee85f
   ```
   Deve listar os jobs agendados

3. **Chat com IA:**
   - Testar enviando mensagem no chat
   - Verificar logs se houver erro

4. **Deploy de Functions:**
   ```powershell
   firebase deploy --only functions:health
   ```
   Deve funcionar sem erros

---

## 📊 Comparação: APIs Ativadas vs Necessárias

**APIs já ativadas:** ~85 APIs  
**APIs necessárias:** ~98 APIs  
**APIs faltando:** ~13 APIs

**Percentual de cobertura:** ~87%  
**APIs críticas faltando:** 5  
**APIs importantes faltando:** 3  
**APIs opcionais faltando:** 5

### Detalhamento das APIs Críticas Faltando:
1. 🔴 Cloud Functions API - **SEM ESTA, NENHUMA FUNCTION FUNCIONA**
2. 🔴 Cloud Pub/Sub API - **SEM ESTA, FUNÇÕES AGENDADAS NÃO FUNCIONAM**
3. 🔴 Cloud Scheduler API - **NECESSÁRIA PARA SCHEDULES**
4. 🔴 Generative Language API - **CHAT COM IA NÃO FUNCIONA**
5. 🔴 Firebase Realtime Database API - **VERIFICAR** (mensagens em tempo real)

### Detalhamento das APIs Importantes Faltando:
6. 🟡 Cloud Build API - Deploy de functions
7. 🟡 Artifact Registry API - Build e deploy
8. 🟡 Secret Manager API - Gerenciamento de secrets

---

## 🚨 Notas Importantes

1. **Cloud Functions API é a MAIS CRÍTICA:**
   - Sem ela, nenhuma Cloud Function funcionará
   - Ative esta PRIMEIRO

2. **Cloud Pub/Sub + Cloud Scheduler:**
   - Ambas são necessárias para funções agendadas
   - Ative ambas juntas

3. **Generative Language API:**
   - Pode estar em outro projeto (verificar)
   - API Key separada já está configurada

4. **Secret Manager API:**
   - Recomendada, mas não obrigatória
   - Secrets podem ser configurados via Firebase Console

---

## 🔗 Links Úteis

- Google Cloud APIs Library: https://console.cloud.google.com/apis/library?project=task-go-ee85f
- Firebase Console: https://console.firebase.google.com/project/task-go-ee85f
- Cloud Functions: https://console.cloud.google.com/functions?project=task-go-ee85f
- Cloud Scheduler: https://console.cloud.google.com/cloudscheduler?project=task-go-ee85f







