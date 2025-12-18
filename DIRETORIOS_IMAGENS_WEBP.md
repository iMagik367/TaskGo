# 📁 Diretórios para Salvar Imagens WebP

## 🎯 Ícone do App (Launcher Icon)

### Para Android 8.0+ (Adaptive Icon)
**Diretório:**
```
app/src/main/res/drawable/ic_launcher_icon.webp
```

**Especificações:**
- **Tamanho**: 512x512 pixels
- **Formato**: WebP
- **Área segura**: Conteúdo importante na área central de 341x341 pixels (66.67% da imagem)
- **Margem**: 85 pixels de padding em cada lado
- **Fundo**: Transparente ou branco

### Para Versões Antigas do Android (Fallback)
Se quiser garantir compatibilidade com versões antigas, também coloque PNGs nas pastas mipmap:
- `app/src/main/res/mipmap-mdpi/ic_launcher.webp` (48x48)
- `app/src/main/res/mipmap-hdpi/ic_launcher.webp` (72x72)
- `app/src/main/res/mipmap-xhdpi/ic_launcher.webp` (96x96)
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp` (144x144)
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` (192x192)

---

## 🚀 Logo do Splash Screen

**Diretório:**
```
app/src/main/res/drawable/ic_taskgo_logo_vertical.webp
```

**Especificações:**
- **Tamanho**: Recomendado 512x512 pixels ou proporção vertical (ex: 400x600)
- **Formato**: WebP
- **Fundo**: Transparente (o splash já tem fundo verde)
- **Uso**: Logo vertical do TaskGo que aparece centralizado no splash

---

## ✅ Resumo dos Arquivos

### 1. Ícone do App
```
📂 app/src/main/res/drawable/
   └── ic_launcher_icon.webp (512x512px)
```

### 2. Logo do Splash
```
📂 app/src/main/res/drawable/
   └── ic_taskgo_logo_vertical.webp (recomendado: 512x512px ou proporção vertical)
```

---

## 📝 Notas Importantes

1. **WebP é suportado nativamente pelo Android** desde a API 15+, então não precisa de conversão
2. **O sistema já está configurado** para usar esses arquivos automaticamente
3. **Após colocar os arquivos**, faça um build limpo (`./gradlew clean`) e depois build completo
4. **Se você já tem os arquivos em PNG**, pode convertê-los para WebP usando ferramentas online ou o Android Studio

---

## 🔄 Como Converter PNG para WebP

### Opção 1: Android Studio
1. Clique com botão direito no arquivo PNG
2. Selecione "Convert to WebP"
3. Escolha as opções de qualidade

### Opção 2: Online
- Use ferramentas como: https://convertio.co/png-webp/ ou https://cloudconvert.com/png-to-webp

### Opção 3: Command Line (se tiver cwebp instalado)
```bash
cwebp -q 80 input.png -o output.webp
```

