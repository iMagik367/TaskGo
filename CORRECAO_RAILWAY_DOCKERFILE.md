# Correção Railway - Usando Dockerfile

## 🔧 Problema Identificado

O Railway não estava detectando Node.js porque:
- O `package.json` está em `backend/`
- O Railway estava tentando buildar na raiz
- O `nixpacks.toml` não estava sendo usado corretamente

## ✅ Solução Implementada

### 1. Dockerfile Customizado

Criado `Dockerfile` que:
- Usa Node.js 18 Alpine (imagem oficial)
- Copia apenas o diretório `backend/`
- Instala dependências
- Compila TypeScript
- Inicia o servidor

### 2. railway.json Atualizado

Configurado para usar Dockerfile ao invés de Nixpacks:
```json
{
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile"
  }
}
```

### 3. .dockerignore

Criado para ignorar arquivos desnecessários e focar apenas no backend.

## 🚀 Como Funciona Agora

1. Railway detecta o `Dockerfile`
2. Usa Node.js 18 oficial
3. Copia `backend/` para `/app`
4. Instala dependências (`npm ci`)
5. Compila TypeScript (`npm run build`)
6. Inicia servidor (`npm start`)

## ✅ Vantagens

- ✅ Usa imagem oficial do Node.js (mais confiável)
- ✅ Build mais rápido (cache de dependências)
- ✅ Controle total sobre o processo
- ✅ Não depende de detecção automática

## 📝 Próximos Passos

1. **Commit e push** - Railway fará deploy automático
2. **Verificar logs** - Deve mostrar Node.js instalado
3. **Testar API** - `curl https://taskgo-production.up.railway.app/health`
