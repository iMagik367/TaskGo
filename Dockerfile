# Dockerfile para Railway - Backend Node.js
FROM node:18-alpine

WORKDIR /app

# Copiar package.json e package-lock.json primeiro (para cache de dependências)
COPY backend/package*.json ./

# Instalar dependências (incluindo devDependencies para build)
RUN npm ci

# Copiar código fonte
COPY backend/ ./

# Compilar TypeScript
RUN npm run build

# Expor porta
EXPOSE 3000

# Variável de ambiente para porta
ENV PORT=3000

# Comando de start
CMD ["npm", "start"]
