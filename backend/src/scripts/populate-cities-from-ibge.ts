/**
 * Script para popular cidades do IBGE
 * 
 * Este script busca dados do IBGE e popula a tabela cities
 * 
 * Uso:
 * npm run populate:cities
 */

import { query } from '../database/connection';
import { LocationRepository } from '../../repositories/LocationRepository';

const locationRepo = new LocationRepository();

/**
 * Dados principais de cidades brasileiras (exemplo)
 * Em produção, usar API do IBGE ou arquivo completo
 */
const majorCities = [
  // São Paulo
  { name: 'São Paulo', state: 'SP', lat: -23.5505, lng: -46.6333 },
  { name: 'Campinas', state: 'SP', lat: -22.9056, lng: -47.0608 },
  { name: 'Guarulhos', state: 'SP', lat: -23.4538, lng: -46.5331 },
  { name: 'São Bernardo do Campo', state: 'SP', lat: -23.6939, lng: -46.5650 },
  { name: 'Santo André', state: 'SP', lat: -23.6639, lng: -46.5383 },
  { name: 'Osasco', state: 'SP', lat: -23.5329, lng: -46.7915 },
  { name: 'Ribeirão Preto', state: 'SP', lat: -21.1775, lng: -47.8103 },
  { name: 'Sorocaba', state: 'SP', lat: -23.5015, lng: -47.4526 },
  
  // Rio de Janeiro
  { name: 'Rio de Janeiro', state: 'RJ', lat: -22.9068, lng: -43.1729 },
  { name: 'São Gonçalo', state: 'RJ', lat: -22.8269, lng: -43.0539 },
  { name: 'Duque de Caxias', state: 'RJ', lat: -22.7856, lng: -43.3117 },
  { name: 'Nova Iguaçu', state: 'RJ', lat: -22.7556, lng: -43.4603 },
  { name: 'Niterói', state: 'RJ', lat: -22.8833, lng: -43.1036 },
  
  // Minas Gerais
  { name: 'Belo Horizonte', state: 'MG', lat: -19.9167, lng: -43.9345 },
  { name: 'Uberlândia', state: 'MG', lat: -18.9186, lng: -48.2772 },
  { name: 'Contagem', state: 'MG', lat: -19.9311, lng: -44.0536 },
  { name: 'Juiz de Fora', state: 'MG', lat: -21.7595, lng: -43.3398 },
  
  // Paraná
  { name: 'Curitiba', state: 'PR', lat: -25.4284, lng: -49.2733 },
  { name: 'Londrina', state: 'PR', lat: -23.3103, lng: -51.1628 },
  { name: 'Maringá', state: 'PR', lat: -23.4205, lng: -51.9332 },
  
  // Rio Grande do Sul
  { name: 'Porto Alegre', state: 'RS', lat: -30.0346, lng: -51.2177 },
  { name: 'Caxias do Sul', state: 'RS', lat: -29.1680, lng: -51.1794 },
  { name: 'Pelotas', state: 'RS', lat: -31.7619, lng: -52.3378 },
  
  // Distrito Federal
  { name: 'Brasília', state: 'DF', lat: -15.7942, lng: -47.8822 },
  
  // Bahia
  { name: 'Salvador', state: 'BA', lat: -12.9714, lng: -38.5014 },
  { name: 'Feira de Santana', state: 'BA', lat: -12.2667, lng: -38.9667 },
  
  // Ceará
  { name: 'Fortaleza', state: 'CE', lat: -3.7172, lng: -38.5433 },
  
  // Pernambuco
  { name: 'Recife', state: 'PE', lat: -8.0476, lng: -34.8770 },
  
  // Goiás
  { name: 'Goiânia', state: 'GO', lat: -16.6864, lng: -49.2643 },
  
  // Pará
  { name: 'Belém', state: 'PA', lat: -1.4558, lng: -48.5044 },
  
  // Santa Catarina
  { name: 'Florianópolis', state: 'SC', lat: -27.5954, lng: -48.5480 },
  { name: 'Joinville', state: 'SC', lat: -26.3044, lng: -48.8461 },
  
  // Espírito Santo
  { name: 'Vitória', state: 'ES', lat: -20.3155, lng: -40.3128 },
  
  // Alagoas
  { name: 'Maceió', state: 'AL', lat: -9.5713, lng: -36.7820 },
  
  // Amazonas
  { name: 'Manaus', state: 'AM', lat: -3.1190, lng: -60.0217 },
];

/**
 * Normaliza nome da cidade para busca
 */
function normalizeCityName(name: string): string {
  return name
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim();
}

/**
 * Popula cidades
 */
async function populateCities(): Promise<void> {
  console.log('🔄 Populando cidades do IBGE...');
  
  let inserted = 0;
  let skipped = 0;
  let errors = 0;
  
  for (const city of majorCities) {
    try {
      // Buscar estado
      const state = await locationRepo.findStateByCode(city.state);
      if (!state) {
        console.warn(`  ⚠️ Estado ${city.state} não encontrado`);
        skipped++;
        continue;
      }
      
      // Verificar se cidade já existe
      const existing = await locationRepo.findCityByNameAndState(city.name, city.state);
      if (existing) {
        skipped++;
        continue;
      }
      
      // Inserir cidade
      const normalizedName = normalizeCityName(city.name);
      await query(
        `INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
         VALUES ($1, $2, $3, $4, $5)`,
        [state.id, city.name, normalizedName, city.lat, city.lng]
      );
      
      inserted++;
      if (inserted % 10 === 0) {
        console.log(`  ✅ ${inserted} cidades inseridas...`);
      }
    } catch (error: any) {
      console.error(`  ❌ Erro ao inserir ${city.name}, ${city.state}:`, error.message);
      errors++;
    }
  }
  
  console.log(`\n✅ População concluída:`);
  console.log(`   Inseridas: ${inserted}`);
  console.log(`   Ignoradas (já existem): ${skipped}`);
  console.log(`   Erros: ${errors}`);
}

/**
 * Função principal
 */
async function main(): Promise<void> {
  try {
    await populateCities();
  } catch (error) {
    console.error('❌ Erro:', error);
    process.exit(1);
  } finally {
    process.exit(0);
  }
}

if (require.main === module) {
  main();
}

export { populateCities };
