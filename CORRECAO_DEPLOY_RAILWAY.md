# Correção do Deploy Railway

## 🔧 Problema Identificado

O Railway estava detectando o projeto como **Android/Gradle** (por causa dos arquivos na raiz) e não instalava o **Node.js**, causando o erro:

```
/bin/bash: line 1: npm: command not found
```

## ✅ Correções Aplicadas

### 1. Criado `backend/nixpacks.toml`
Força o Railway a usar Node.js 18 e npm 9 durante o build.

### 2. Criado `.railwayignore`
Ignora arquivos Android durante o build, focando apenas no backend.

### 3. Atualizado `railway.json`
Adicionado `watchPatterns` para monitorar apenas o diretório `backend`.

### 4. Atualizado `backend/package.json`
Adicionado `engines` para especificar versões mínimas de Node.js e npm.

## 🚀 Próximos Passos

1. **Faça commit das alterações:**
```bash
git add .
git commit -m "Fix Railway build configuration"
git push
```

2. **O Railway fará deploy automático** após o push.

3. **Verifique os logs** no Railway Dashboard para confirmar que o build está funcionando.

## ✅ Verificação

Após o deploy, verifique:

- ✅ Build concluído com sucesso
- ✅ Node.js instalado corretamente
- ✅ npm install executado
- ✅ npm run build executado
- ✅ Servidor iniciado na porta 3000

## 🐛 Se Ainda Houver Problemas

1. No Railway Dashboard, vá em **Settings** do serviço backend
2. Verifique se o **Root Directory** está vazio ou configurado como `/`
3. Verifique se as variáveis de ambiente estão corretas
4. Veja os logs completos do build
