# 📱 Instruções para o Ícone do App

## Tamanho e Especificações da Imagem PNG

Para que sua imagem apareça corretamente no ícone do app, ela precisa seguir estas especificações:

### Tamanho Recomendado
- **Tamanho da imagem**: **512x512 pixels** (ou múltiplos de 512)
- **Formato**: PNG com fundo transparente ou branco

### Área Segura (Safe Zone)
O Android Adaptive Icon usa um sistema de viewport onde:
- **Viewport total**: 108x108 dp
- **Área segura (onde o conteúdo importante deve estar)**: **72x72 dp no centro**
- **Padding automático**: 18 dp em cada lado

### Como Preparar sua Imagem

1. **Crie uma imagem de 512x512 pixels**
2. **Coloque o conteúdo importante (checkmark verde) na área central de 256x256 pixels**
   - Isso garante que o conteúdo fique visível mesmo com o padding do sistema
3. **Use fundo transparente ou branco**
4. **Salve como PNG**

### Proporção Visual
- A área central de **72x72 dp** corresponde a aproximadamente **66.67%** da imagem total
- Em uma imagem de 512x512 pixels, o conteúdo importante deve estar dentro de aproximadamente **341x341 pixels** no centro
- Deixe uma margem de segurança de pelo menos **85 pixels** em cada lado

### Exemplo de Estrutura
```
┌─────────────────────────┐
│   Padding (85px)        │
│  ┌───────────────────┐  │
│  │                   │  │
│  │  Conteúdo        │  │ ← Área segura (341x341px)
│  │  Importante      │  │
│  │                   │  │
│  └───────────────────┘  │
│   Padding (85px)        │
└─────────────────────────┘
     Total: 512x512px
```

### Onde Colocar a Imagem
Coloque sua imagem PNG em:
```
app/src/main/res/drawable/ic_launcher_icon.png
```

O sistema já está configurado para usar essa imagem automaticamente.

