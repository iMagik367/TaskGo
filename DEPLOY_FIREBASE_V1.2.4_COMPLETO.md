# Deploy Firebase - Versão 1.2.4 (Completo)

## Comandos para Deploy Manual

Se o script `DEPLOY_FIREBASE_V1.2.4_COMPLETO.bat` não funcionar devido a problemas com Firebase CLI, execute os comandos manualmente:

### 1. Compilar Cloud Functions

```bash
cd functions
npm run build
cd ..
```

### 2. Deploy das Firestore Rules

```bash
firebase deploy --only firestore:rules
```

### 3. Deploy das Cloud Functions

```bash
firebase deploy --only functions
```

### 4. Deploy Completo (Rules + Functions)

```bash
firebase deploy --only functions,firestore:rules
```

## Mudanças na Versão 1.2.4

### Frontend
- **Refatoração da Camada de Localização**:
  - `OperationalLocation` como fonte única de verdade
  - `LocationResolver` com lógica de decisão (cache → GPS → perfil)
  - `OperationalLocationStore` para persistência local
  - Remoção de todos os bloqueios com "unknown"
  - Repositories não acessam GPS diretamente

- **AccountTypeSelectionDialog Expandido**:
  - Categorias de serviços quando seleciona Parceiro
  - Campos de documentos obrigatórios (CPF/CNPJ, RG)
  - Validação completa de campos

- **LoginViewModel Atualizado**:
  - Obtém city/state via GPS no cadastro
  - Salva documentos e categorias no Firestore
  - Conecta com verificação de identidade para parceiros

### Backend
- Nenhuma mudança nas Cloud Functions (já estavam corretas)
- Firestore Rules mantidas (já permitem city/state no perfil)

## Nota sobre Secrets

Se houver secrets configurados no Firebase, eles precisam ser atualizados manualmente via Firebase Console ou CLI:

```bash
firebase functions:secrets:set SECRET_NAME
```

## Troubleshooting

### Erro com Firebase CLI

Se encontrar erro com Firebase CLI (SyntaxError no chardet):

1. **Reinstalar Firebase Tools**:
   ```bash
   npm uninstall -g firebase-tools
   npm install -g firebase-tools@latest
   ```

2. **Verificar Node.js**:
   ```bash
   node --version
   ```
   Deve ser Node.js 18+ (recomendado: Node.js 20)

3. **Fazer login**:
   ```bash
   firebase login
   ```

4. **Verificar projeto**:
   ```bash
   firebase projects:list
   firebase use <project-id>
   ```

### Erro de Build das Functions

Se o build das functions falhar:

1. Limpar node_modules:
   ```bash
   cd functions
   rm -rf node_modules
   npm install
   npm run build
   ```

2. Verificar erros de lint:
   ```bash
   cd functions
   npm run lint
   ```

## Verificação Pós-Deploy

Após o deploy, verifique:

1. **Firestore Rules**:
   - Acesse Firebase Console → Firestore Database → Rules
   - Verifique se as rules foram atualizadas

2. **Cloud Functions**:
   - Acesse Firebase Console → Functions
   - Verifique se todas as functions estão ativas

3. **Logs**:
   ```bash
   firebase functions:log
   ```
