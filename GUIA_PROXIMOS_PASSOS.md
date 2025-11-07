# 🚀 Guia dos Próximos Passos - Integração Completa

**Data:** 2024  
**Status:** Base Implementada - Pronto para Integração

---

## ✅ O QUE JÁ FOI FEITO

1. ✅ **Dependências Adicionadas:**
   - Biometric Authentication
   - Google Play Billing
   - Google Pay

2. ✅ **Classes Criadas:**
   - `BiometricManager.kt`
   - `BillingManager.kt`
   - `GooglePayManager.kt`
   - `Address.kt`

3. ✅ **Modelo Atualizado:**
   - `UserFirestore.kt` com todos os campos necessários

4. ✅ **Módulos Hilt:**
   - `BiometricModule.kt`
   - `BillingModule.kt`
   - `PaymentModule.kt`

5. ✅ **Permissões:**
   - Adicionadas no AndroidManifest

---

## 📋 PRÓXIMOS PASSOS (ORDEM DE IMPLEMENTAÇÃO)

### 1. **Integrar Biometria no Login** ⚠️ REQUER AJUSTE

**Problema:** O `BiometricManager` precisa de `FragmentActivity`, mas o app usa `ComponentActivity`.

**Solução:**
- Opção A: Converter `MainActivity` para `FragmentActivity` (recomendado)
- Opção B: Criar um Fragment intermediário para biometria

**Arquivos a modificar:**
- `MainActivity.kt` - Converter para FragmentActivity
- `LoginPersonScreen.kt` - Adicionar botão de biometria
- `LoginViewModel.kt` - Adicionar método de login com biometria

---

### 2. **Atualizar Formulário de Cadastro**

**Campos a adicionar:**
- CPF/CNPJ
- Data de nascimento
- Endereço completo (usar modelo Address)
- Checkbox para habilitar biometria
- Checkbox para habilitar 2FA

**Arquivos a modificar:**
- `SignUpScreen.kt` - Adicionar novos campos
- `SignupViewModel.kt` - Atualizar lógica de cadastro
- `UserFirestore.kt` - Já atualizado ✅

---

### 3. **Criar Tela de Verificação de Identidade**

**Funcionalidades:**
- Upload de documento (frente)
- Upload de documento (verso)
- Captura de selfie
- Upload de comprovante de endereço
- Validação de documentos

**Arquivos a criar:**
- `IdentityVerificationScreen.kt`
- `IdentityVerificationViewModel.kt`

**Recursos:**
- Usar `ImagePicker` já existente
- Usar `FirebaseStorage` para upload
- Usar `Camera` para selfie

---

### 4. **Integrar Google Pay no Checkout**

**Funcionalidades:**
- Verificar disponibilidade do Google Pay
- Adicionar botão Google Pay
- Processar pagamento
- Enviar token para backend

**Arquivos a modificar:**
- `CheckoutScreen.kt` - Adicionar botão Google Pay
- `CheckoutViewModel.kt` - Integrar GooglePayManager
- `PaymentMethodScreen.kt` - Adicionar opção Google Pay

**Configuração necessária:**
- Registrar no Google Pay Business Console
- Obter Merchant ID
- Configurar gateway de pagamento

---

### 5. **Integrar Google Play Billing**

**Funcionalidades:**
- Query de produtos
- Iniciar compra
- Verificar compras
- Restaurar compras

**Arquivos a modificar:**
- `CheckoutScreen.kt` - Para produtos digitais
- `CheckoutViewModel.kt` - Integrar BillingManager

**Configuração necessária:**
- Configurar produtos no Google Play Console
- Criar produtos de teste
- Configurar preços

---

### 6. **Backend (Cloud Functions)**

**Funções a criar:**
- `verifyIdentity.ts` - Verificar documentos
- `processPayment.ts` - Processar pagamentos
- `billingWebhook.ts` - Webhook para billing

**Arquivos a criar/modificar:**
- `functions/src/identityVerification.ts`
- `functions/src/payment.ts`
- `functions/src/billingWebhook.ts`
- `functions/src/auth.ts` - Atualizar para incluir novos campos

---

### 7. **Configurações - Biometria e 2FA**

**Funcionalidades:**
- Habilitar/desabilitar biometria
- Habilitar/desabilitar 2FA
- Escolher método de 2FA
- Configurar autenticador

**Arquivos a modificar:**
- `SettingsScreen.kt` - Adicionar seção de segurança
- `SettingsViewModel.kt` - Adicionar lógica
- Criar `SecuritySettingsScreen.kt`

---

### 8. **Política de Privacidade**

**Arquivos a criar:**
- `PrivacyPolicyScreen.kt`
- Página web ou Firebase Hosting

**Arquivos a modificar:**
- `AboutScreen.kt` - Adicionar link
- `SettingsScreen.kt` - Adicionar link

---

## 🔧 CONFIGURAÇÕES NECESSÁRIAS

### Google Pay Business Console
1. Acessar: https://pay.google.com/business/console/
2. Registrar empresa
3. Obter Merchant ID
4. Configurar gateway de pagamento

### Google Play Console
1. Acessar: https://play.google.com/console
2. Ir em "Monetização" > "Produtos"
3. Criar produtos
4. Configurar preços
5. Configurar assinaturas (se necessário)

### Firebase
1. Configurar Cloud Functions
2. Configurar Storage para documentos
3. Configurar regras de segurança
4. Configurar webhooks

---

## ⚠️ IMPORTANTE

### Biometria
- **Problema:** ComponentActivity vs FragmentActivity
- **Solução:** Converter MainActivity ou usar Fragment intermediário
- **Teste:** Dispositivos com e sem biometria

### Google Pay
- **Ambiente:** Mudar para PRODUCTION em produção
- **Merchant ID:** Configurar corretamente
- **Gateway:** Integrar com backend

### Google Play Billing
- **Produtos:** Configurar antes de testar
- **Testes:** Usar contas de teste
- **Verificação:** Implementar server-side

---

## 📝 CHECKLIST FINAL

### Implementação
- [ ] Converter MainActivity para FragmentActivity
- [ ] Integrar biometria no login
- [ ] Atualizar formulário de cadastro
- [ ] Criar tela de verificação
- [ ] Integrar Google Pay
- [ ] Integrar Google Play Billing
- [ ] Criar Cloud Functions
- [ ] Adicionar configurações de segurança

### Configuração
- [ ] Registrar no Google Pay Business Console
- [ ] Configurar produtos no Google Play Console
- [ ] Configurar Firebase
- [ ] Criar política de privacidade
- [ ] Publicar política online

### Testes
- [ ] Testar biometria
- [ ] Testar cadastro completo
- [ ] Testar verificação de identidade
- [ ] Testar Google Pay (sandbox)
- [ ] Testar Google Play Billing (teste)
- [ ] Testar em diferentes dispositivos

---

**Status:** Base pronta - Aguardando integração no app


