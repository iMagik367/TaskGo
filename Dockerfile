# Dockerfile para Railway - Backend Node.js
FROM node:18-alpine

WORKDIR /app

# Copiar package.json primeiro
COPY backend/package.json ./

# Instalar dependências (usa npm install se não houver package-lock.json)
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

# Copiar código fonte
COPY backend/ ./

# Compilar TypeScript
RUN npm run build

# Expor porta (Railway usa variável PORT automaticamente)
EXPOSE 3000

# Comando de start
CMD ["npm", "start"]
