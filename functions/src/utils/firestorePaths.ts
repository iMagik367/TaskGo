/**
 * Utilitário centralizado para paths do Firestore
 * TODAS as escritas de dados públicos devem usar exclusivamente:
 * locations/{locationId}/{collection}
 * 
 * Este arquivo padroniza onde e como os dados são gravados no Firestore.
 */

import * as admin from 'firebase-admin';
import {normalizeLocationId, getUserLocation} from './location';

/**
 * Retorna a referência da coleção de produtos para uma localização
 * Path: locations/{locationId}/products
 */
export function productsPath(
  db: admin.firestore.Firestore,
  locationId: string,
): admin.firestore.CollectionReference {
  return db.collection('locations').doc(locationId).collection('products');
}

/**
 * Retorna a referência da coleção de serviços para uma localização
 * Path: locations/{locationId}/services
 */
export function servicesPath(
  db: admin.firestore.Firestore,
  locationId: string,
): admin.firestore.CollectionReference {
  return db.collection('locations').doc(locationId).collection('services');
}

/**
 * Retorna a referência da coleção de stories para uma localização
 * Path: locations/{locationId}/stories
 */
export function storiesPath(
  db: admin.firestore.Firestore,
  locationId: string,
): admin.firestore.CollectionReference {
  return db.collection('locations').doc(locationId).collection('stories');
}

/**
 * Retorna a referência da coleção de posts (feed) para uma localização
 * Path: locations/{locationId}/posts
 */
export function feedPath(
  db: admin.firestore.Firestore,
  locationId: string,
): admin.firestore.CollectionReference {
  return db.collection('locations').doc(locationId).collection('posts');
}

/**
 * Retorna a referência da coleção de pedidos para uma localização
 * Path: locations/{locationId}/orders
 * NOTA: Usado tanto para pedidos de serviços quanto para pedidos de produtos (purchase_orders)
 */
export function ordersPath(
  db: admin.firestore.Firestore,
  locationId: string,
): admin.firestore.CollectionReference {
  return db.collection('locations').doc(locationId).collection('orders');
}

/**
 * Retorna a referência da coleção de pedidos de produtos (purchase_orders) para uma localização
 * Path: locations/{locationId}/orders
 * NOTA: Por enquanto, usa a mesma coleção que orders de serviços, seguindo o modelo canônico
 * TODO: Avaliar se purchase_orders deve ter coleção separada ou usar orders mesmo
 */
export function purchaseOrdersPath(
  db: admin.firestore.Firestore,
  locationId: string,
): admin.firestore.CollectionReference {
  // Por enquanto, usa a mesma coleção que orders (modelo canônico menciona apenas 'orders')
  return db.collection('locations').doc(locationId).collection('orders');
}

/**
 * Helper para obter locationId a partir de city e state
 * CRÍTICO: Lança exceção se city ou state forem inválidos - NUNCA retorna 'unknown'
 */
export function getLocationId(city: string, state: string): string {
  if (!city || !state || city.trim() === '' || state.trim() === '') {
    throw new Error(
      `Localização inválida: city='${city}', state='${state}'. ` +
      'Não é possível salvar dados sem localização válida.'
    );
  }
  return normalizeLocationId(city, state);
}

/**
 * Helper para obter locationId do usuário
 */
export async function getUserLocationId(
  db: admin.firestore.Firestore,
  userId: string,
): Promise<string> {
  const location = await getUserLocation(db, userId);
  return getLocationId(location.city, location.state);
}

/**
 * Helper para criar payload padrão com timestamps e active
 * Garante que TODA escrita inclua createdAt, updatedAt e active quando aplicável
 */
export function createStandardPayload(data: Record<string, unknown>, isActive = true): Record<string, unknown> {
  return {
    ...data,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    ...(isActive !== undefined && {active: isActive}),
  };
}

/**
 * Helper para atualizar payload padrão com updatedAt
 */
export function createUpdatePayload(data: Record<string, unknown>): Record<string, unknown> {
  return {
    ...data,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
}
