#!/bin/bash
# Script de backup do banco PostgreSQL para TaskGoApp
# Uso: ./backup_db.sh <NOME_DO_BANCO> <USUARIO> <DIRETORIO_DESTINO>

DB_NAME="$1"
DB_USER="$2"
DEST_DIR="$3"
DATE=$(date +%Y-%m-%d_%H-%M-%S)

if [ -z "$DB_NAME" ] || [ -z "$DB_USER" ] || [ -z "$DEST_DIR" ]; then
  echo "Uso: $0 <NOME_DO_BANCO> <USUARIO> <DIRETORIO_DESTINO>"
  exit 1
fi

mkdir -p "$DEST_DIR"
pg_dump -U "$DB_USER" -F c -b -v -f "$DEST_DIR/${DB_NAME}_backup_$DATE.dump" "$DB_NAME"
echo "Backup salvo em $DEST_DIR/${DB_NAME}_backup_$DATE.dump"
