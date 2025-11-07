# 📋 Requisitos Google Play Store - Autenticação Biométrica, 2FA e Pagamentos

**Data:** 2024  
**Status:** Em Implementação

---

## 🔐 1. AUTENTICAÇÃO BIOMÉTRICA E 2FA

### Requisitos do Google Play Store:

#### ✅ **Biometric Authentication (BiometricPrompt)**
- **Requisito:** Apps que usam autenticação biométrica devem usar a API `BiometricPrompt` do Android
- **Biblioteca:** `androidx.biometric:biometric:1.1.0` ou superior
- **Suporta:** Impressão digital, Face ID, IRIS
- **Permissões:** Não requer permissões especiais no AndroidManifest

#### ✅ **Two-Factor Authentication (2FA)**
- **Requisito:** Apps que oferecem 2FA devem seguir as melhores práticas de segurança
- **Recomendado:** Usar SMS, Email, ou Authenticator Apps (Google Authenticator)
- **Alternativa:** Usar Firebase Phone Auth + Firebase Auth para 2FA

#### ✅ **Identity Verification**
- **Requisito:** Apps que coletam dados pessoais devem implementar verificação de identidade
- **Dados necessários:**
  - Nome completo
  - CPF/CNPJ (Brasil) ou documento equivalente
  - Data de nascimento
  - Foto do documento de identidade
  - Selfie para verificação facial (opcional)
  - Endereço completo
  - Telefone verificado

---

## 📱 2. DADOS DE USUÁRIO NECESSÁRIOS

### Para Verificação de Identidade:

#### **Dados Básicos (Obrigatórios):**
- ✅ Nome completo
- ✅ Email (verificado)
- ✅ Telefone (verificado)
- ✅ Data de nascimento
- ✅ CPF/CNPJ ou documento de identidade

#### **Dados para Verificação (Obrigatórios para alguns serviços):**
- ✅ Foto do documento de identidade (frente)
- ✅ Foto do documento de identidade (verso)
- ✅ Selfie para verificação facial
- ✅ Endereço completo
- ✅ Comprovante de endereço

#### **Dados para Pagamentos:**
- ✅ Informações bancárias (para recebimentos)
- ✅ Dados do cartão (tokenizado, nunca armazenar completo)
- ✅ Endereço de cobrança

---

## 💳 3. GOOGLE PAY E GOOGLE PLAY BILLING

### Google Play Billing (Para Produtos Digitais):

#### **Requisitos:**
- ✅ Usar apenas para produtos digitais (in-app purchases)
- ✅ Não pode ser usado para produtos físicos ou serviços reais
- ✅ Taxa: 15-30% dependendo do valor
- ✅ Biblioteca: `com.android.billingclient:billing:6.0.0` ou superior

#### **Tipos de Produtos:**
- ✅ **Produtos únicos:** Compras de uma vez
- ✅ **Assinaturas:** Pagamentos recorrentes
- ✅ **Produtos consumíveis:** Podem ser comprados múltiplas vezes

### Google Pay API (Para Produtos Físicos/Serviços):

#### **Requisitos:**
- ✅ Usar para produtos físicos, serviços reais, reservas
- ✅ Não pode ser usado para produtos digitais dentro do app
- ✅ Biblioteca: `com.google.android.gms:play-services-wallet:19.2.0` ou superior
- ✅ Requer registro no Google Pay Business Console

#### **Métodos de Pagamento Suportados:**
- ✅ Cartões de crédito/débito
- ✅ PayPal
- ✅ Pix (Brasil)
- ✅ Contas bancárias vinculadas

---

## 🔒 4. PERMISSÕES E CONFIGURAÇÕES

### Permissões AndroidManifest.xml:

```xml
<!-- Biometric Authentication -->
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />

<!-- Camera para selfie/documento -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Storage para salvar documentos -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
```

### Features:

```xml
<uses-feature
    android:name="android.hardware.biometric"
    android:required="false" />
<uses-feature
    android:name="android.hardware.camera"
    android:required="false" />
```

---

## 📋 5. POLÍTICA DE PRIVACIDADE

### Requisitos Obrigatórios:

1. **Política de Privacidade:**
   - ✅ Deve estar acessível no app
   - ✅ Deve explicar como os dados são coletados
   - ✅ Deve explicar como os dados são usados
   - ✅ Deve explicar como os dados são armazenados
   - ✅ Link deve ser fornecido no Google Play Console

2. **Declaração de Dados Sensíveis:**
   - ✅ Declarar se coleta dados biométricos
   - ✅ Declarar se coleta dados financeiros
   - ✅ Declarar se coleta documentos de identidade
   - ✅ Declarar finalidade da coleta

---

## ✅ 6. CHECKLIST DE IMPLEMENTAÇÃO

### Autenticação Biométrica:
- [ ] Adicionar biblioteca `androidx.biometric`
- [ ] Implementar `BiometricPrompt`
- [ ] Adicionar fallback para senha/PIN
- [ ] Testar em dispositivos com/sem biometria

### Verificação de Identidade:
- [ ] Atualizar modelo de usuário com campos necessários
- [ ] Criar tela de upload de documentos
- [ ] Implementar captura de selfie (opcional)
- [ ] Implementar validação de documentos
- [ ] Criar backend para verificação

### Google Play Billing:
- [ ] Adicionar biblioteca `com.android.billingclient:billing`
- [ ] Configurar produtos no Google Play Console
- [ ] Implementar fluxo de compra
- [ ] Implementar verificação de assinaturas
- [ ] Implementar restauração de compras

### Google Pay:
- [ ] Registrar no Google Pay Business Console
- [ ] Adicionar biblioteca `com.google.android.gms:play-services-wallet`
- [ ] Implementar botão Google Pay
- [ ] Implementar fluxo de pagamento
- [ ] Testar em ambiente de sandbox

### Política de Privacidade:
- [ ] Criar página de política de privacidade
- [ ] Adicionar link no app
- [ ] Adicionar link no Google Play Console
- [ ] Revisar termos de uso

---

## 📚 REFERÊNCIAS

- [Google Play Billing Documentation](https://developer.android.com/google/play/billing)
- [Google Pay API Documentation](https://developers.google.com/pay/api/android/overview)
- [BiometricPrompt Documentation](https://developer.android.com/training/sign-in/biometric-auth)
- [Google Play Store Policies](https://support.google.com/googleplay/android-developer/answer/9888170)
- [Data Safety Section](https://support.google.com/googleplay/android-developer/answer/10787469)

---

**Próximos Passos:** Implementar todas as funcionalidades listadas acima.


