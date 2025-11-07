# 🚀 Plano de Implementação - Biometria, 2FA e Pagamentos

**Data:** 2024  
**Status:** Em Implementação

---

## 📋 ETAPA 1: AUTENTICAÇÃO BIOMÉTRICA

### 1.1 Adicionar Dependências

```kotlin
// app/build.gradle.kts
dependencies {
    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Google Play Billing
    implementation("com.android.billingclient:billing:6.1.0")
    implementation("com.android.billingclient:billing-ktx:6.1.0")
    
    // Google Pay
    implementation("com.google.android.gms:play-services-wallet:19.2.0")
}
```

### 1.2 Criar BiometricManager

Criar `app/src/main/java/com/taskgoapp/taskgo/core/biometric/BiometricManager.kt`

### 1.3 Adicionar Permissões

Atualizar `AndroidManifest.xml`

---

## 📋 ETAPA 2: ATUALIZAR MODELO DE USUÁRIO

### 2.1 Atualizar UserFirestore

Adicionar campos:
- `cpf`: String?
- `cnpj`: String?
- `birthDate`: Date?
- `documentFront`: String? (URL da foto)
- `documentBack`: String? (URL da foto)
- `selfie`: String? (URL da selfie)
- `address`: Address?
- `addressProof`: String? (URL do comprovante)
- `biometricEnabled`: Boolean
- `twoFactorEnabled`: Boolean
- `twoFactorMethod`: String? ("sms", "email", "authenticator")
- `verifiedAt`: Date?
- `verifiedBy`: String?

### 2.2 Criar Modelo Address

Criar `app/src/main/java/com/taskgoapp/taskgo/core/model/Address.kt`

---

## 📋 ETAPA 3: ATUALIZAR FORMULÁRIO DE CADASTRO

### 3.1 Adicionar Campos ao SignUpScreen

- CPF/CNPJ
- Data de nascimento
- Endereço completo
- Opção de habilitar biometria
- Opção de habilitar 2FA

### 3.2 Criar Tela de Verificação de Identidade

Criar `app/src/main/java/com/taskgoapp/taskgo/feature/auth/presentation/IdentityVerificationScreen.kt`

---

## 📋 ETAPA 4: GOOGLE PLAY BILLING

### 4.1 Configurar Produtos no Google Play Console

- Criar produtos digitais
- Configurar preços
- Configurar assinaturas (se necessário)

### 4.2 Implementar BillingManager

Criar `app/src/main/java/com/taskgoapp/taskgo/core/billing/BillingManager.kt`

### 4.3 Integrar com Checkout

Atualizar fluxo de checkout para usar Google Play Billing

---

## 📋 ETAPA 5: GOOGLE PAY

### 5.1 Registrar no Google Pay Business Console

- Obter Merchant ID
- Configurar ambiente de teste

### 5.2 Implementar GooglePayManager

Criar `app/src/main/java/com/taskgoapp/taskgo/core/payment/GooglePayManager.kt`

### 5.3 Integrar com Checkout

Adicionar botão Google Pay no checkout

---

## 📋 ETAPA 6: BACKEND (CLOUD FUNCTIONS)

### 6.1 Criar Função de Verificação de Identidade

Criar `functions/src/identityVerification.ts`

### 6.2 Criar Função de Webhook para Billing

Criar `functions/src/billingWebhook.ts`

### 6.3 Atualizar Função de Criação de Usuário

Atualizar `functions/src/auth.ts` para incluir novos campos

---

## 📋 ETAPA 7: POLÍTICA DE PRIVACIDADE

### 7.1 Criar Página de Política de Privacidade

Criar `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/PrivacyPolicyScreen.kt`

### 7.2 Adicionar Link no App

Adicionar link na tela de configurações

### 7.3 Publicar Política Online

Criar página web ou usar Firebase Hosting

---

## ✅ ORDEM DE IMPLEMENTAÇÃO

1. ✅ **Etapa 1:** Autenticação Biométrica
2. ✅ **Etapa 2:** Atualizar Modelo de Usuário
3. ✅ **Etapa 3:** Atualizar Formulário de Cadastro
4. ✅ **Etapa 4:** Google Play Billing
5. ✅ **Etapa 5:** Google Pay
6. ✅ **Etapa 6:** Backend
7. ✅ **Etapa 7:** Política de Privacidade

---

**Status Atual:** Iniciando implementação...


