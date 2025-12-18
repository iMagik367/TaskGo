# Correções de Crash - App TaskGo

## Problemas Identificados e Soluções

### 1. ❌ Erro: Canvas trying to draw too large bitmap (~203MB)

**Causa:** As imagens PNG dos banners promocionais (`banner_prestadores_locais.png` e `banner_produtos_descontos.png`) estão muito grandes em resolução.

**Solução Implementada:**
- Alterado `PromotionalBanner` para usar `AsyncImage` do Coil com redimensionamento automático
- Adicionado `.size(200, 200)` para limitar o tamanho máximo da imagem carregada na memória

**Ação Necessária:**
1. Redimensionar manualmente as imagens PNG para no máximo 200x200px ou 400x400px
2. Localização: `app/src/main/res/drawable/banner_prestadores_locais.png` e `app/src/main/res/drawable/banner_produtos_descontos.png`
3. Usar ferramenta como GIMP, Photoshop ou online (ex: https://www.iloveimg.com/resize-image)

### 2. ❌ Erro: Firestore Index Missing

**Causa:** Queries no Firestore que filtram por `active` e ordenam por `createdAt` requerem índice composto.

**Solução Implementada:**
- Adicionados comentários no código indicando quais índices são necessários
- Documentação das queries que precisam de índices

**Ação Necessária:**
1. Acessar: https://console.firebase.google.com/project/task-go-ee85f/firestore/indexes
2. Criar os seguintes índices:

   **Índice 1:**
   - Collection: `products`
   - Fields:
     - `active` (Ascending)
     - `createdAt` (Ascending)
   
   **Índice 2:**
   - Collection: `products`
   - Fields:
     - `sellerId` (Ascending)
     - `active` (Ascending)
     - `createdAt` (Ascending)

3. Aguardar a criação dos índices (pode levar alguns minutos)

## Status

✅ **Código corrigido** - Build deve compilar sem erros
⚠️ **Ações externas necessárias:**
   - Redimensionar imagens PNG dos banners
   - Criar índices no Firestore Console

## Teste

Após as correções:
1. Redimensionar as imagens PNG
2. Criar os índices no Firestore
3. Fazer build e testar o app
4. Verificar se o crash não ocorre mais ao navegar para a HomeScreen

