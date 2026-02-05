/**
 * Utilitários para organização de dados por localização
 * Dados públicos são salvos em coleções organizadas por cidade/estado
 * Estrutura: locations/{city}_{state}/{collection}/{documentId}
 */

import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

/**
 * Valida se city e state são válidos
 * CRÍTICO: Garante que city e state sejam sempre corretos antes de salvar
 */
export function validateCityAndState(
  city: string,
  state: string,
): {valid: boolean; city?: string; state?: string; error?: string} {
  // Estados válidos do Brasil (siglas de 2 caracteres)
  const VALID_BRAZILIAN_STATES = new Set([
    'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA',
    'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN',
    'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
  ]);

  // Valores inválidos/genéricos
  const INVALID_VALUES = new Set([
    'unknown', 'desconhecido', 'null', 'undefined', 'n/a', 'na',
    'cidade', 'city', 'local', 'location', 'endereço', 'address',
  ]);

  // Normalizar city
  const normalizedCity = city?.trim() || '';
  if (!normalizedCity || normalizedCity.length < 2) {
    return {valid: false, error: `City inválido: '${city}'`};
  }

  if (INVALID_VALUES.has(normalizedCity.toLowerCase())) {
    return {valid: false, error: `City é um valor genérico/inválido: '${normalizedCity}'`};
  }

  // Normalizar state (deve ser sigla de 2 caracteres)
  const normalizedState = state?.trim().toUpperCase() || '';
  if (!normalizedState || normalizedState.length !== 2) {
    return {valid: false, error: `State não tem 2 caracteres: '${state}'`};
  }

  if (!VALID_BRAZILIAN_STATES.has(normalizedState)) {
    return {valid: false, error: `State não é uma sigla válida do Brasil: '${normalizedState}'`};
  }

  return {valid: true, city: normalizedCity, state: normalizedState};
}

/**
 * Normaliza cidade e estado para criar ID válido para coleção
 * Remove espaços, caracteres especiais e converte para lowercase
 * Exemplo: "Osasco" + "SP" -> "osasco_sp"
 * CRÍTICO: Valida city e state antes de normalizar
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

  // CRÍTICO: Validar city e state antes de normalizar
  const validation = validateCityAndState(city, state);
  if (!validation.valid) {
    const errorMsg = `Localização inválida: city='${city}', state='${state}'. ` +
      `${validation.error || 'Não é possível salvar dados sem localização válida.'}`;
    functions.logger.error('📍 normalizeLocationId: Validação falhou', {
      city,
      state,
      error: validation.error,
    });
    // CRÍTICO: Lançar exceção em vez de retornar 'unknown' - NUNCA salvar sem localização válida
    throw new Error(errorMsg);
  }

  const validatedCity = validation.city!;
  const validatedState = validation.state!;

  const normalizedCity = normalize(validatedCity);
  const normalizedState = normalize(validatedState);

  // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de normalização
  functions.logger.info('📍 LOCATION TRACE', {
    function: 'normalizeLocationId',
    rawCity: city || '',
    rawState: state || '',
    validatedCity,
    validatedState,
    normalizedCity,
    normalizedState,
    locationId: `${normalizedCity}_${normalizedState}`,
    timestamp: new Date().toISOString(),
  });

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
    // LEI MÁXIMA DO TASKGO: Buscar primeiro em users global (legacy), depois em locations/{locationId}/users
    // Estratégia híbrida para compatibilidade com dados antigos
    
    // 1. Tentar buscar na coleção global "users" (legacy/migração)
    const globalUserDoc = await db.collection('users').doc(userId).get();
    let userData = globalUserDoc.exists ? globalUserDoc.data() : null;
    let city = userData?.city || '';
    let state = userData?.state || '';
    
    // 2. Se encontrou city/state na coleção global, tentar buscar em locations/{locationId}/users também
    if (city && state) {
      try {
        const locationId = normalizeLocationId(city, state);
        const locationUserDoc = await db.collection('locations').doc(locationId)
          .collection('users').doc(userId).get();
        
        if (locationUserDoc.exists) {
          const locationUserData = locationUserDoc.data();
          const locationCity = locationUserData?.city || '';
          const locationState = locationUserData?.state || '';
          
          if (locationCity && locationState) {
            // Usar dados de locations/{locationId}/users (mais atualizado)
            city = locationCity;
            state = locationState;
            userData = locationUserData;
            functions.logger.info('📍 getUserLocation: Usando dados de locations/{locationId}/users', {
              userId,
              locationId,
              city,
              state
            });
          }
        }
      } catch (e) {
        functions.logger.warn(
            '📍 getUserLocation: Erro ao buscar em locations, usando users global',
            {userId, error: e}
        );
      }
    }
    
    if (!globalUserDoc.exists && !city && !state) {
      functions.logger.warn('📍 getUserLocation: User document not found', {userId});
      return {city: '', state: ''};
    }
    
    // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de localização do usuário
    // Lei 1: Localização vem EXCLUSIVAMENTE de users/{userId}.city e users/{userId}.state na raiz
    functions.logger.info('📍 LOCATION TRACE', {
      function: 'getUserLocation',
      userId,
      rawCity: city,
      rawState: state,
      source: 'users/{userId} root fields (city, state)',
      timestamp: new Date().toISOString(),
    });
    
    // CRÍTICO: Lei 1 - A localização é determinada EXCLUSIVAMENTE pelos campos city e state na raiz
    // NÃO existe fallback para address. Se city ou state não existirem na raiz, retornar vazio.
    if (city && state) {
      // CRÍTICO: Validar city e state antes de retornar
      const validation = validateCityAndState(city, state);
      if (validation.valid) {
        const locationId = normalizeLocationId(validation.city!, validation.state!);
        functions.logger.info('📍 getUserLocation: Using direct fields (validated)', {
          userId,
          city: validation.city,
          state: validation.state,
          locationId,
        });
        return {city: validation.city!, state: validation.state!};
      } else {
        functions.logger.error('📍 getUserLocation: City/State inválidos nos campos diretos', {
          userId,
          city,
          state,
          error: validation.error,
        });
        // Lei 1: Se validação falhar, retornar vazio (não fazer fallback)
        return {city: '', state: ''};
      }
    }

    // Lei 1: Se city ou state não existirem na raiz, retornar vazio
    // NÃO fazer fallback para address - isso viola a Lei 1 do modelo canônico
    functions.logger.error('📍 getUserLocation: Localização não encontrada na raiz do documento users/{userId}', {
      userId,
      hasCity: !!city,
      hasState: !!state,
      city: city || '',
      state: state || '',
      message: 'Localização DEVE estar em users/{userId}.city e ' +
        'users/{userId}.state na raiz do documento. ' +
        'Fallback para address é PROIBIDO.',
    });
    return {
      city: '',
      state: '',
    };
  } catch (error) {
    functions.logger.error('📍 getUserLocation: Error', {userId, error});
    return {city: '', state: ''};
  }
}
