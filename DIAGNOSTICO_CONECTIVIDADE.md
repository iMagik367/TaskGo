# 🔍 Diagnóstico de Problemas de Conectividade

## 📋 Problema

O app não consegue se conectar à internet, mesmo com todas as APIs do Google Cloud habilitadas e App Check configurado.

---

## 🔧 SOLUÇÕES IMPLEMENTADAS NO CÓDIGO

### 1. ✅ Utilitário de Diagnóstico de Rede

Criado `NetworkDiagnostic.kt` que verifica:
- ✅ Conexão com a internet
- ✅ Acessibilidade do Firebase
- ✅ Acessibilidade do Google
- ✅ Acessibilidade do reCAPTCHA

**Os logs agora mostram diagnóstico detalhado ao iniciar o app!**

### 2. ✅ Network Security Config Melhorado

Atualizado `network_security_config.xml` para:
- ✅ Permitir certificados do usuário (para desenvolvimento)
- ✅ Configuração específica para domínios do Firebase/Google
- ✅ Suporte para reCAPTCHA

### 3. ✅ Diagnóstico Antes do Login

O app agora verifica conectividade **antes** de tentar fazer login, fornecendo mensagens de erro mais claras.

---

## 🔍 COMO DIAGNOSTICAR

### Passo 1: Verificar os Logs

Ao iniciar o app, procure por estas linhas nos logs:

```
TaskGoApp: === DIAGNÓSTICO DE REDE ===
NetworkDiagnostic: Firebase reachable: true/false
NetworkDiagnostic: Google reachable: true/false
NetworkDiagnostic: reCAPTCHA reachable: true/false
```

### Passo 2: Interpretar os Resultados

#### ✅ Tudo OK:
```
NetworkDiagnostic: Firebase reachable: true
NetworkDiagnostic: Google reachable: true
NetworkDiagnostic: reCAPTCHA reachable: true
```
**Ação:** O problema não é de conectividade. Verifique outras configurações.

#### ❌ Sem Internet:
```
NetworkDiagnostic: Internet: false
```
**Ação:** Verifique conexão Wi-Fi/dados móveis do dispositivo.

#### ❌ Firebase Não Acessível:
```
NetworkDiagnostic: Firebase reachable: false
```
**Ação:** Veja seção "Firebase Não Acessível" abaixo.

#### ❌ Google Não Acessível:
```
NetworkDiagnostic: Google reachable: false
```
**Ação:** Veja seção "Google Não Acessível" abaixo.

#### ❌ reCAPTCHA Não Acessível:
```
NetworkDiagnostic: reCAPTCHA reachable: false
```
**Ação:** Veja seção "reCAPTCHA Não Acessível" abaixo.

---

## 🔧 SOLUÇÕES POR TIPO DE PROBLEMA

### 1. ❌ Sem Conexão com a Internet

**Sintomas:**
- Logs mostram: `Internet: false`
- App não consegue acessar nenhum serviço

**Soluções:**

1. **Verificar Conexão Wi-Fi/Dados Móveis:**
   - Abra um navegador no dispositivo
   - Tente acessar um site (ex: google.com)
   - Se não funcionar, o problema é de conexão do dispositivo

2. **Verificar Configurações de Rede:**
   - Desative e reative Wi-Fi/dados móveis
   - Verifique se há proxy configurado
   - Verifique se há VPN ativa

3. **Reiniciar o Dispositivo:**
   - Reinicie o dispositivo
   - Teste novamente

---

### 2. ❌ Firebase Não Acessível

**Sintomas:**
- Logs mostram: `Firebase reachable: false`
- Erros de conexão ao Firebase

**Soluções:**

1. **Verificar Firewall/Proxy:**
   - Verifique se há firewall bloqueando `firebase.googleapis.com`
   - Verifique se há proxy configurado no dispositivo
   - Se estiver em rede corporativa, verifique com o administrador

2. **Verificar DNS:**
   - Tente alterar o DNS do dispositivo para 8.8.8.8 (Google DNS)
   - Teste novamente

3. **Verificar Certificados SSL:**
   - Verifique se a data/hora do dispositivo está correta
   - Certificados SSL expiram se a data estiver incorreta

4. **Testar em Outra Rede:**
   - Conecte o dispositivo a outra rede Wi-Fi
   - Teste novamente
   - Se funcionar, o problema é da rede original

---

### 3. ❌ Google Não Acessível

**Sintomas:**
- Logs mostram: `Google reachable: false`
- Erros de conexão ao Google

**Soluções:**

1. **Verificar Firewall/Proxy:**
   - Verifique se há firewall bloqueando `google.com`
   - Verifique se há proxy configurado

2. **Verificar DNS:**
   - Tente alterar o DNS do dispositivo para 8.8.8.8 (Google DNS)

3. **Verificar Restrições de Rede:**
   - Se estiver em rede corporativa, verifique com o administrador
   - Algumas redes bloqueiam acesso ao Google

---

### 4. ❌ reCAPTCHA Não Acessível

**Sintomas:**
- Logs mostram: `reCAPTCHA reachable: false`
- Login falha com erro de reCAPTCHA

**Soluções:**

1. **Verificar Firewall/Proxy:**
   - Verifique se há firewall bloqueando `recaptcha.net` e `google.com/recaptcha`
   - Verifique se há proxy configurado

2. **Verificar DNS:**
   - Tente alterar o DNS do dispositivo para 8.8.8.8 (Google DNS)

3. **Verificar Configuração do Firebase:**
   - Verifique se o reCAPTCHA está configurado no Firebase Console
   - Acesse: https://console.firebase.google.com/project/task-go-ee85f/authentication/settings

---

## 🔒 PROBLEMAS COMUNS EM REDES CORPORATIVAS/VPN

### Problema: Firewall Bloqueando

**Solução:**
- Configure o firewall para permitir:
  - `*.googleapis.com`
  - `*.google.com`
  - `*.gstatic.com`
  - `*.recaptcha.net`
  - `*.firebaseapp.com`

### Problema: Proxy Requerido

**Solução:**
- Configure proxy no dispositivo Android:
  1. Vá em **Configurações** > **Wi-Fi**
  2. Toque longo na rede Wi-Fi
  3. Selecione **Modificar rede**
  4. Configure o proxy

---

## 📱 TESTES MANUAIS

### Teste 1: Navegador

1. Abra um navegador no dispositivo
2. Acesse: https://firebase.googleapis.com
3. Se não carregar, há problema de conectividade

### Teste 2: Aplicativo de Teste

1. Baixe um app de teste de conectividade
2. Teste conexão com:
   - `firebase.googleapis.com`
   - `google.com`
   - `recaptcha.net`

### Teste 3: Terminal/ADB

```bash
adb shell ping -c 3 firebase.googleapis.com
adb shell ping -c 3 google.com
adb shell ping -c 3 recaptcha.net
```

---

## 🆘 SE NADA FUNCIONAR

### 1. Verificar Logs Completos

Envie os logs completos do app, especialmente:
- Linhas com `NetworkDiagnostic:`
- Linhas com `FirebaseAuthRepository:`
- Linhas com `TaskGoApp:`

### 2. Verificar Configurações do Dispositivo

- Data/hora corretas
- DNS configurado corretamente
- Sem proxy/VPN ativos
- Permissões de internet concedidas ao app

### 3. Testar em Outro Dispositivo

- Instale o app em outro dispositivo
- Teste na mesma rede
- Se funcionar, o problema é específico do dispositivo

### 4. Testar em Outra Rede

- Conecte a outra rede Wi-Fi
- Teste o app
- Se funcionar, o problema é da rede original

---

## 📋 CHECKLIST DE DIAGNÓSTICO

- [ ] App tem permissão de INTERNET (AndroidManifest.xml)
- [ ] Dispositivo tem conexão com internet
- [ ] Navegador no dispositivo consegue acessar google.com
- [ ] Navegador no dispositivo consegue acessar firebase.googleapis.com
- [ ] Sem firewall bloqueando
- [ ] Sem proxy configurado (ou configurado corretamente)
- [ ] Sem VPN ativa
- [ ] Data/hora do dispositivo corretas
- [ ] DNS configurado corretamente (ou usando 8.8.8.8)
- [ ] Logs mostram diagnóstico de rede
- [ ] Testado em outra rede
- [ ] Testado em outro dispositivo

---

## 🔗 LINKS ÚTEIS

- **Teste de Conectividade Firebase:** https://firebase.googleapis.com
- **Teste de Conectividade Google:** https://www.google.com
- **Teste de Conectividade reCAPTCHA:** https://www.google.com/recaptcha/api.js
- **Google DNS:** 8.8.8.8 e 8.8.4.4
- **Cloudflare DNS:** 1.1.1.1 e 1.0.0.1

---

**Última atualização:** 2025-11-07

