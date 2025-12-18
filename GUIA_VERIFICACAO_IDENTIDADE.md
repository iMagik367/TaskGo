# Guia de Verificação de Identidade - TaskGo App

## 📋 Visão Geral

Sistema automatizado de verificação de identidade usando:
- **Google Cloud Vision API** para reconhecimento facial e OCR
- **Firebase Realtime Database** para processamento em tempo real
- **Cloud Functions** para processamento automático

## 🏗️ Arquitetura

### Fluxo de Verificação

1. **App envia documentos** → Chama `startIdentityVerification` (HTTP callable)
2. **Function cria entrada no Realtime Database** → Trigger automático
3. **Trigger processa verificação** → `processIdentityVerification` (Database trigger)
4. **Resultados atualizados em tempo real** → Realtime Database + Firestore

### Componentes

#### Cloud Functions

1. **`startIdentityVerification`** (HTTP Callable)
   - Recebe URLs dos documentos do app
   - Cria entrada no Realtime Database
   - Retorna status inicial

2. **`processIdentityVerification`** (Database Trigger)
   - Triggered quando nova entrada é criada em `/identity_verifications/{userId}`
   - Processa verificação facial
   - Executa OCR no documento
   - Valida campos extraídos
   - Atualiza resultados em tempo real

## 🚀 Deploy

### Pré-requisitos

1. **Node.js 20+** instalado
2. **Firebase CLI** instalado: `npm install -g firebase-tools`
3. **Autenticado no Firebase**: `firebase login`
4. **Google Cloud Vision API** ativada

### Ativar Google Cloud Vision API

1. Acesse: https://console.cloud.google.com/apis/library/vision.googleapis.com
2. Selecione o projeto Firebase
3. Clique em **"Ativar"**

### Executar Deploy

**Opção 1: Script PowerShell (Windows)**
```powershell
.\deploy-identity-verification.ps1
```

**Opção 2: Manual**
```bash
# 1. Instalar dependências
cd functions
npm install
cd ..

# 2. Deploy índices
firebase deploy --only firestore:indexes

# 3. Deploy regras Realtime Database
firebase deploy --only database

# 4. Deploy functions
firebase deploy --only functions:processIdentityVerification,functions:startIdentityVerification
```

## 📊 Estrutura de Dados

### Realtime Database: `/identity_verifications/{userId}`

```json
{
  "userId": "user123",
  "documentFrontUrl": "gs://bucket/document.jpg",
  "documentBackUrl": "gs://bucket/document_back.jpg",
  "selfieUrl": "gs://bucket/selfie.jpg",
  "addressProofUrl": "gs://bucket/address.jpg",
  "status": "pending|processing|approved|rejected|error",
  "createdAt": 1234567890,
  "processedAt": 1234567890,
  "faceMatch": {
    "success": true,
    "confidence": 0.85,
    "message": "Faces correspondem (85.0% de similaridade)"
  },
  "ocrResult": {
    "success": true,
    "text": "Texto completo extraído...",
    "fields": {
      "cpf": "123.456.789-00",
      "nome": "JOÃO DA SILVA",
      "dataNascimento": "01/01/1990"
    }
  },
  "documentValidation": {
    "valid": true,
    "issues": []
  },
  "finalResult": {
    "approved": true,
    "message": "Verificação aprovada automaticamente",
    "processedAt": 1234567890
  }
}
```

### Firestore: `users/{userId}`

Campos adicionados:
- `identityVerified: boolean`
- `identityVerificationStatus: string`
- `identityVerificationProcessedAt: timestamp`
- `identityVerificationResults: object`

## 🔍 Funcionalidades

### 1. Verificação Facial

- Compara selfie com foto do documento
- Usa Google Cloud Vision API Face Detection
- Calcula similaridade baseada em landmarks faciais
- Threshold: 70% de similaridade para aprovação

### 2. OCR (Leitura de Texto)

- Extrai texto completo do documento
- Identifica campos específicos:
  - CPF (formato: XXX.XXX.XXX-XX)
  - RG (formato: XX.XXX.XXX-X)
  - Nome completo
  - Data de nascimento (DD/MM/AAAA)

### 3. Validação de Documento

- Valida formato do CPF
- Verifica presença de campos obrigatórios
- Identifica problemas no documento

## 📱 Uso no App Android

### Chamar a Function

```kotlin
val functions = FirebaseFunctions.getInstance()
val data = hashMapOf(
    "documentFrontUrl" to documentFrontUrl,
    "documentBackUrl" to documentBackUrl,
    "selfieUrl" to selfieUrl,
    "addressProofUrl" to addressProofUrl
)

functions.getHttpsCallable("startIdentityVerification")
    .call(data)
    .addOnSuccessListener { result ->
        // Verificação iniciada
    }
    .addOnFailureListener { e ->
        // Erro
    }
```

### Observar Resultados em Tempo Real

```kotlin
val db = FirebaseDatabase.getInstance()
val ref = db.getReference("identity_verifications/${userId}")

ref.addValueEventListener(object : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) {
        val status = snapshot.child("status").getValue(String::class.java)
        val faceMatch = snapshot.child("faceMatch").getValue(Map::class.java)
        val ocrResult = snapshot.child("ocrResult").getValue(Map::class.java)
        val finalResult = snapshot.child("finalResult").getValue(Map::class.java)
        
        // Atualizar UI com resultados
    }
    
    override fun onCancelled(error: DatabaseError) {
        // Tratar erro
    }
})
```

## 🔐 Regras de Segurança

### Realtime Database

- Usuário só pode ler/escrever sua própria verificação
- Admins podem ler todas as verificações

### Firestore

- Usuário pode atualizar apenas seus próprios dados
- Verificação de identidade é atualizada apenas pelas Cloud Functions

## 📈 Monitoramento

### Ver Logs das Functions

```bash
firebase functions:log --only processIdentityVerification
firebase functions:log --only startIdentityVerification
```

### Métricas no Console

- Acesse: https://console.firebase.google.com/project/task-go-ee85f/functions
- Veja execuções, erros e latência

## ⚠️ Troubleshooting

### Erro: "Vision API not enabled"
- Ative a API no Google Cloud Console

### Erro: "Permission denied"
- Verifique regras do Realtime Database
- Verifique permissões do usuário

### Erro: "Image download failed"
- Verifique URLs das imagens
- Certifique-se de que as imagens estão no Storage

### Verificação sempre rejeitada
- Verifique qualidade das imagens
- Certifique-se de que há uma face visível em ambas as imagens
- Verifique se o documento está legível

## 🔄 Próximas Melhorias

- [ ] Integração com Serpro/Serasa para validação de CPF
- [ ] Detecção de documentos falsos
- [ ] Análise de vivacidade (liveness detection)
- [ ] Suporte a múltiplos tipos de documento (CNH, Passaporte, etc.)
- [ ] Cache de resultados para evitar reprocessamento

