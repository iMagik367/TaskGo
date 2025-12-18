# 📋 GUIA COMPLETO - TAREFAS EXTERNAS PARA PUBLICAR NA PLAY STORE

**Data:** 11/11/2025  
**Status:** Lista completa de tarefas externas ao código

---

## 🎯 ÍNDICE

1. [Google Play Console](#1-google-play-console)
2. [Firebase Console](#2-firebase-console)
3. [Conteúdo Legal](#3-conteúdo-legal)
4. [Assets e Materiais](#4-assets-e-materiais)
5. [Configurações de Servidor/API](#5-configurações-de-servidorapi)
6. [Testes e Validação](#6-testes-e-validação)
7. [Configurações de Pagamento](#7-configurações-de-pagamento)
8. [Marketing e Descrições](#8-marketing-e-descrições)

---

## 1. GOOGLE PLAY CONSOLE

### 1.1. Criar Conta de Desenvolvedor
- [ ] **Criar conta Google Play Developer**
  - Acessar: https://play.google.com/console
  - Pagar taxa única de $25 USD (válida para sempre)
  - Preencher informações pessoais/empresariais
  - Aceitar termos e condições

### 1.2. Criar Novo App
- [ ] **Criar aplicativo na Play Console**
  - Nome do app: "TaskGo" (ou nome escolhido)
  - Idioma padrão: Português (Brasil)
  - Tipo: App
  - Grátis ou pago: Definir modelo de negócio

### 1.3. Configurações Básicas do App
- [ ] **Preencher informações básicas:**
  - Nome do app (até 50 caracteres)
  - Descrição curta (até 80 caracteres)
  - Descrição completa (até 4000 caracteres)
  - Categoria: Serviços / Produtividade / Outros
  - Classificação de conteúdo (preencher questionário)

### 1.4. Upload do AAB
- [ ] **Fazer upload do arquivo:**
  - Arquivo: `app\build\outputs\bundle\release\app-release.aab`
  - Criar primeira versão de produção ou teste fechado
  - Preencher notas de versão (o que mudou nesta versão)

### 1.5. Configurar Assinatura de App
- [ ] **Configurar App Signing:**
  - Google Play pode gerenciar a assinatura automaticamente
  - OU fazer upload da chave de upload (se preferir gerenciar)
  - Adicionar SHA-1 e SHA-256 do keystore na Play Console

### 1.6. Configurar Certificados SHA
- [ ] **Adicionar certificados SHA no Firebase:**
  - Obter SHA-1 e SHA-256 do keystore de release
  - Adicionar no Firebase Console > Configurações do Projeto > Seus apps Android
  - Isso permite autenticação com Firebase

---

## 2. FIREBASE CONSOLE

### 2.1. Verificar Configurações do Projeto
- [ ] **Verificar Firebase Project:**
  - Projeto: `task-go-ee85f`
  - Package name: `com.taskgoapp.taskgo` ✅ (já configurado)
  - Verificar se SHA-1 está adicionado

### 2.2. Configurar Firebase App Check
- [ ] **Ativar App Check para produção:**
  - Firebase Console > App Check
  - Configurar para Android
  - Escolher método de atestação (Play Integrity API recomendado)

### 2.3. Configurar Regras de Segurança do Firestore
- [ ] **Revisar e ajustar regras do Firestore:**
  ```javascript
  // Exemplo de regras básicas (ajustar conforme necessário)
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /{document=**} {
        allow read, write: if request.auth != null;
      }
    }
  }
  ```

### 2.4. Configurar Regras de Storage
- [ ] **Revisar regras do Firebase Storage:**
  ```javascript
  // Ajustar conforme necessário
  rules_version = '2';
  service firebase.storage {
    match /b/{bucket}/o {
      match /{allPaths=**} {
        allow read, write: if request.auth != null;
      }
    }
  }
  ```

### 2.5. Configurar Firebase Functions (se aplicável)
- [ ] **Deploy das Cloud Functions:**
  - Verificar se todas as functions estão deployadas
  - Configurar variáveis de ambiente:
    - `openai.api_key` (se usar OpenAI)
    - `stripe.secret_key` (se usar Stripe)
    - Outras variáveis necessárias

### 2.6. Configurar Crashlytics
- [ ] **Verificar Crashlytics:**
  - Firebase Console > Crashlytics
  - Verificar se está ativado
  - Configurar alertas de crash (opcional)

---

## 3. CONTEÚDO LEGAL

### 3.1. Política de Privacidade
- [ ] **Criar Política de Privacidade completa:**
  - Deve incluir:
    - Quais dados são coletados
    - Como os dados são usados
    - Como os dados são armazenados
    - Direitos do usuário (LGPD/GDPR)
    - Como entrar em contato
  - Hospedar em URL pública (ex: seu site)
  - OU atualizar conteúdo nas telas `PrivacyPolicyScreen` e `TermsOfServiceScreen`

### 3.2. Termos de Uso
- [ ] **Criar Termos de Uso completos:**
  - Deve incluir:
    - Regras de uso do app
    - Responsabilidades do usuário
    - Limitações de responsabilidade
    - Política de cancelamento/reembolso
    - Lei aplicável
  - Hospedar em URL pública
  - OU atualizar conteúdo nas telas internas

### 3.3. Adicionar URLs na Play Console
- [ ] **Adicionar links legais:**
  - Play Console > Política e programas > Política de privacidade
  - Adicionar URL da política de privacidade
  - Adicionar URL dos termos de uso (se aplicável)

### 3.4. Conformidade com LGPD (Brasil)
- [ ] **Garantir conformidade LGPD:**
  - Política de privacidade em português
  - Mecanismo para usuário solicitar exclusão de dados
  - Informações sobre tratamento de dados pessoais

---

## 4. ASSETS E MATERIAIS

### 4.1. Ícone do App
- [ ] **Criar ícone de alta qualidade:**
  - Tamanho: 512x512 pixels (PNG, sem transparência)
  - Deve representar o app claramente
  - Seguir diretrizes de design do Material Design

### 4.2. Screenshots
- [ ] **Criar screenshots do app:**
  - Mínimo: 2 screenshots
  - Recomendado: 4-8 screenshots
  - Tamanhos necessários:
    - Phone: 320px - 3840px (largura)
    - Tablet (7"): 320px - 3840px
    - Tablet (10"): 320px - 3840px
  - Mostrar funcionalidades principais
  - Adicionar textos explicativos (opcional)

### 4.3. Imagem de Destaque (Feature Graphic)
- [ ] **Criar imagem de destaque:**
  - Tamanho: 1024x500 pixels
  - Usado na Play Store
  - Deve ser atraente e representar o app

### 4.4. Vídeo Promocional (Opcional mas Recomendado)
- [ ] **Criar vídeo do YouTube:**
  - Duração: 30 segundos a 2 minutos
  - Mostrar funcionalidades principais
  - Adicionar link do YouTube na Play Console

### 4.5. Imagens de Marketing
- [ ] **Preparar materiais adicionais:**
  - Banner promocional (se usar)
  - Imagens para redes sociais
  - Logo em diferentes tamanhos

---

## 5. CONFIGURAÇÕES DE SERVIDOR/API

### 5.1. Configurar URL de API de Produção
- [ ] **Configurar servidor de produção:**
  - Verificar se API está rodando em produção
  - URL deve ser HTTPS (obrigatório)
  - Configurar domínio e certificado SSL
  - Atualizar `local.properties` ou variáveis de ambiente

### 5.2. Configurar CORS (se aplicável)
- [ ] **Configurar CORS no servidor:**
  - Permitir requisições do app Android
  - Configurar headers apropriados

### 5.3. Configurar Rate Limiting
- [ ] **Implementar rate limiting:**
  - Proteger API contra abuso
  - Configurar limites por usuário/IP

### 5.4. Configurar Backup e Monitoramento
- [ ] **Configurar monitoramento:**
  - Logs de erro
  - Monitoramento de performance
  - Alertas para problemas críticos

---

## 6. TESTES E VALIDAÇÃO

### 6.1. Testes Internos
- [ ] **Criar lista de teste interno:**
  - Play Console > Teste > Teste interno
  - Adicionar emails de testadores
  - Fazer upload do AAB
  - Testar todas as funcionalidades

### 6.2. Testes em Dispositivos Reais
- [ ] **Testar em diferentes dispositivos:**
  - Diferentes marcas (Samsung, Xiaomi, Motorola, etc.)
  - Diferentes versões do Android (7.0 até 14)
  - Diferentes tamanhos de tela
  - Verificar:
    - Biometria funciona
    - Upload de documentos funciona
    - Pagamentos funcionam
    - Notificações funcionam
    - Performance está boa

### 6.3. Testes de Segurança
- [ ] **Validar segurança:**
  - Testar autenticação
  - Testar verificação de documentos
  - Verificar se dados sensíveis estão protegidos
  - Testar bloqueio de funcionalidades

### 6.4. Testes de Usabilidade
- [ ] **Validar UX:**
  - Fluxo de cadastro completo
  - Fluxo de verificação de identidade
  - Fluxo de criação de produto/serviço
  - Fluxo de pagamento
  - Navegação geral

---

## 7. CONFIGURAÇÕES DE PAGAMENTO

### 7.1. Google Pay
- [ ] **Configurar Google Pay:**
  - Verificar se conta Google Pay está configurada
  - Testar pagamentos em ambiente de teste
  - Configurar ambiente de produção

### 7.2. Gateway de Pagamento (Stripe/PagSeguro/etc.)
- [ ] **Configurar gateway:**
  - Criar conta no gateway escolhido
  - Obter chaves de API (produção)
  - Configurar webhooks
  - Testar integração completa

### 7.3. Configurar Reembolsos
- [ ] **Implementar política de reembolso:**
  - Definir política clara
  - Implementar fluxo de reembolso
  - Testar processo completo

---

## 8. MARKETING E DESCRIÇÕES

### 8.1. Descrição do App
- [ ] **Escrever descrição atrativa:**
  - Descrição curta (80 caracteres): chamativa
  - Descrição completa (4000 caracteres):
    - O que o app faz
    - Principais funcionalidades
    - Benefícios para o usuário
    - Palavras-chave relevantes (SEO)
    - Formatação com emojis (opcional)

### 8.2. Palavras-chave (SEO)
- [ ] **Otimizar para busca:**
  - Pesquisar palavras-chave relevantes
  - Incluir no nome e descrição
  - Usar termos que usuários buscam

### 8.3. Categoria e Tags
- [ ] **Escolher categoria correta:**
  - Categoria principal
  - Categorias secundárias (se aplicável)
  - Tags relevantes

### 8.4. Informações de Contato
- [ ] **Adicionar informações de suporte:**
  - Email de suporte
  - Site (se tiver)
  - Telefone (opcional)
  - Endereço físico (se necessário)

---

## 9. CONFIGURAÇÕES ADICIONAIS

### 9.1. Preços e Distribuição
- [ ] **Configurar distribuição:**
  - Países onde o app estará disponível
  - Preço (se for pago)
  - Programas (Google Play Pass, etc.)

### 9.2. Classificação de Conteúdo
- [ ] **Preencher questionário:**
  - Responder todas as perguntas
  - Classificar conteúdo do app
  - Informar sobre conteúdo sensível

### 9.3. Permissões Sensíveis
- [ ] **Justificar permissões:**
  - Se usar permissões sensíveis, justificar:
    - Câmera: para upload de documentos
    - Localização: para serviços baseados em localização
    - Biometria: para autenticação segura
  - Adicionar declaração de privacidade para cada permissão

### 9.4. Configurar Data Safety
- [ ] **Preencher Data Safety:**
  - Play Console > Política e programas > Segurança de dados
  - Informar quais dados são coletados
  - Como os dados são usados
  - Se dados são compartilhados
  - Se dados são criptografados

---

## 10. CHECKLIST FINAL ANTES DE PUBLICAR

### 10.1. Verificações Técnicas
- [ ] AAB foi gerado e assinado corretamente
- [ ] Version code incrementado
- [ ] Firebase configurado corretamente
- [ ] API de produção funcionando
- [ ] Todos os testes passaram

### 10.2. Verificações de Conteúdo
- [ ] Política de privacidade completa e acessível
- [ ] Termos de uso completos
- [ ] Descrições preenchidas
- [ ] Screenshots adicionados
- [ ] Ícone adicionado

### 10.3. Verificações Legais
- [ ] Conformidade com LGPD
- [ ] Data Safety preenchido
- [ ] Permissões justificadas
- [ ] Classificação de conteúdo correta

### 10.4. Verificações de Marketing
- [ ] Descrição atrativa
- [ ] Screenshots de qualidade
- [ ] Vídeo promocional (se tiver)
- [ ] Informações de contato

---

## 11. PROCESSO DE PUBLICAÇÃO

### 11.1. Publicação Gradual (Recomendado)
- [ ] **Publicar em etapas:**
  1. Teste interno (poucos testadores)
  2. Teste fechado (grupo maior)
  3. Teste aberto (qualquer pessoa pode testar)
  4. Produção (lançamento gradual por % de usuários)
  5. Produção completa (100% dos usuários)

### 11.2. Monitoramento Pós-Lançamento
- [ ] **Monitorar após publicação:**
  - Crashlytics (verificar crashes)
  - Reviews e avaliações
  - Métricas de uso
  - Feedback dos usuários
  - Performance do app

### 11.3. Atualizações Futuras
- [ ] **Preparar para atualizações:**
  - Incrementar version code a cada atualização
  - Atualizar version name
  - Adicionar notas de versão
  - Testar antes de publicar

---

## 📝 RESUMO DAS PRIORIDADES

### 🔴 CRÍTICO (Fazer antes de publicar):
1. ✅ Criar conta Google Play Developer
2. ✅ Criar política de privacidade completa
3. ✅ Criar termos de uso completos
4. ✅ Configurar Firebase App Check para produção
5. ✅ Testar app em dispositivos reais
6. ✅ Configurar URL de API de produção

### 🟡 IMPORTANTE (Fazer antes ou logo após):
1. ✅ Adicionar screenshots
2. ✅ Adicionar ícone do app
3. ✅ Preencher Data Safety
4. ✅ Justificar permissões sensíveis
5. ✅ Configurar gateway de pagamento

### 🟢 OPCIONAL (Pode fazer depois):
1. ✅ Criar vídeo promocional
2. ✅ Criar lista de teste aberto
3. ✅ Otimizar SEO
4. ✅ Criar materiais de marketing

---

## 🚀 PRÓXIMOS PASSOS IMEDIATOS

1. **Criar conta Google Play Developer** (se ainda não tiver)
2. **Criar política de privacidade e termos de uso** (ou URLs)
3. **Preparar screenshots e ícone**
4. **Configurar Firebase para produção**
5. **Fazer upload do AAB na Play Console**

---

**Boa sorte com a publicação! 🎉**

Se precisar de ajuda com alguma etapa específica, me avise!

