#!/usr/bin/env node
/**
 * Script para executar migração de Custom Claims localmente
 * 
 * Como usar:
 * cd functions
 * npm run build
 * node lib/scripts/run-migration.js [--dry-run]
 */

import {migrateLocal} from '../scripts/migrateExistingUsers';

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run') || args.includes('-d');

console.log('🚀 Iniciando migração de Custom Claims...');
console.log(`Modo: ${dryRun ? 'DRY-RUN (simulação)' : 'PRODUÇÃO (real)'}`);
console.log('');

migrateLocal()
  .then(() => {
    console.log('');
    console.log('✅ Migração concluída com sucesso!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('');
    console.error('❌ Erro na migração:', error);
    process.exit(1);
  });
