/**
 * Utilitários para organização de dados por localização
 * Dados públicos são salvos em coleções organizadas por cidade/estado
 * Estrutura: locations/{city}_{state}/{collection}/{documentId}
 */

import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

/**
 * Normaliza cidade e estado para criar ID válido para coleção
 * Remove espaços, caracteres especiais e converte para lowercase
 * Exemplo: "Osasco" + "SP" -> "osasco_sp"
 */
export function normalizeLocationId(city: string, state: string): string {
  const normalize = (str: string): string => {
    return str
      .toLowerCase()
      .trim()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '') // Remove acentos
      .replace(/[^a-z0-9]/g, '_') // Substitui caracteres especiais por underscore
      .replace(/_+/g, '_') // Remove underscores duplicados
      .replace(/^_|_$/g, ''); // Remove underscores no início e fim
  };

  const normalizedCity = normalize(city || '');
  const normalizedState = normalize(state || '');

  // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de normalização
  functions.logger.info('📍 LOCATION TRACE', {
    function: 'normalizeLocationId',
    rawCity: city || '',
    rawState: state || '',
    normalizedCity,
    normalizedState,
    locationId: !normalizedCity && !normalizedState ? 'unknown' : 
                !normalizedCity ? normalizedState :
                !normalizedState ? normalizedCity :
                `${normalizedCity}_${normalizedState}`,
    timestamp: new Date().toISOString(),
  });

  if (!normalizedCity && !normalizedState) {
    return 'unknown';
  }

  if (!normalizedCity) {
    return normalizedState;
  }

  if (!normalizedState) {
    return normalizedCity;
  }

  return `${normalizedCity}_${normalizedState}`;
}

/**
 * Extrai cidade e estado de uma string de localização
 * Formatos suportados:
 * - "Cidade, Estado"
 * - "Endereço, Cidade, Estado"
 * - "Cidade"
 */
export function parseLocation(location: string): {city: string; state: string} {
  if (!location || typeof location !== 'string') {
    return {city: '', state: ''};
  }

  const parts = location.split(',').map((s) => s.trim()).filter((s) => s.length > 0);

  if (parts.length === 0) {
    return {city: '', state: ''};
  }

  if (parts.length === 1) {
    // Apenas cidade fornecida
    return {city: parts[0], state: ''};
  }

  // Assumir que os últimos dois elementos são cidade e estado
  const state = parts[parts.length - 1];
  const city = parts[parts.length - 2];

  return {city, state};
}

/**
 * Obtém referência da coleção por localização
 * @param db Instância do Firestore
 * @param collection Nome da coleção (orders, products, stories, posts)
 * @param city Cidade
 * @param state Estado
 */
export function getLocationCollection(
  db: admin.firestore.Firestore,
  collection: string,
  city: string,
  state: string,
): admin.firestore.CollectionReference {
  const locationId = normalizeLocationId(city, state);
  return db.collection('locations').doc(locationId).collection(collection);
}

/**
 * Obtém cidade e estado do usuário a partir do documento do usuário
 */
export async function getUserLocation(
  db: admin.firestore.Firestore,
  userId: string,
): Promise<{city: string; state: string}> {
  try {
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) {
      functions.logger.warn('📍 getUserLocation: User document not found', {userId});
      return {city: '', state: ''};
    }

    const userData = userDoc.data();
    
    // CRÍTICO: Buscar state diretamente do perfil do usuário (campo adicionado na versão 88)
    // Primeiro tentar campos diretos do usuário (prioridade)
    const city = userData?.city || '';
    const state = userData?.state || '';
    
    // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de localização do usuário
    functions.logger.info('📍 LOCATION TRACE', {
      function: 'getUserLocation',
      userId,
      rawCity: city,
      rawState: state,
      hasAddress: !!userData?.address,
      addressCity: userData?.address?.city || userData?.address?.cityName || '',
      addressState: userData?.address?.state || userData?.address?.stateName || '',
      timestamp: new Date().toISOString(),
    });
    
    if (city && state) {
      const locationId = normalizeLocationId(city, state);
      functions.logger.info('📍 getUserLocation: Using direct fields', {
        userId,
        city,
        state,
        locationId,
      });
      return {city, state};
    }
    
    // Fallback: tentar obter de address se campos diretos não estiverem disponíveis
    const address = userData?.address;
    if (address) {
      const fallbackCity = address.city || address.cityName || city || '';
      const fallbackState = address.state || address.stateName || state || '';
      const locationId = normalizeLocationId(fallbackCity, fallbackState);
      functions.logger.info('📍 getUserLocation: Using address fallback', {
        userId,
        city: fallbackCity,
        state: fallbackState,
        locationId,
      });
      return {
        city: fallbackCity,
        state: fallbackState,
      };
    }

    // Retornar o que tiver (mesmo que vazio)
    functions.logger.warn('📍 getUserLocation: No location data found', {
      userId,
      city: city || '',
      state: state || '',
    });
    return {
      city: city || '',
      state: state || '',
    };
  } catch (error) {
    functions.logger.error('📍 getUserLocation: Error', {userId, error});
    return {city: '', state: ''};
  }
}
