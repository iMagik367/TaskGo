import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import OpenAI from 'openai';
import {GoogleGenerativeAI} from '@google/generative-ai';
import {RateLimiterMemory} from 'rate-limiter-flexible';
import Filter from 'bad-words';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';
import {getFirestore} from './utils/firestore';
// Removido: getUserLocation e normalizeLocationId não são mais necessários para conversations (dados privados)

// Helper functions para inicializar os provedores de IA
// As secrets são carregadas dinamicamente em cada chamada da função
function getOpenAI() {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) return null;
  try {
    return new OpenAI({apiKey});
  } catch (e) {
    functions.logger.error('Error initializing OpenAI:', e);
    return null;
  }
}

function getGeminiModel() {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) return null;
  try {
    const genAI = new GoogleGenerativeAI(apiKey);
    // Usar gemini-1.5-flash que é compatível com a API v1beta
    // Modelos disponíveis: gemini-1.5-flash (rápido), gemini-1.5-pro (mais preciso)
    // Configurações otimizadas para respostas mais rápidas
    return genAI.getGenerativeModel({
      model: 'gemini-1.5-flash',
      generationConfig: {
        maxOutputTokens: 500, // Reduzir tokens para respostas mais rápidas
        temperature: 0.7,
      },
    });
  } catch (e) {
    functions.logger.error('Error initializing Gemini:', e);
    return null;
  }
}

// Initialize content filter
const filter = new Filter();

// Rate limiter: 10 requests per minute per user
const rateLimiter = new RateLimiterMemory({
  points: 10, // Number of requests
  duration: 60, // Per 60 seconds
});

const SYSTEM_PROMPT = `You are a helpful assistant for TaskGo, a service marketplace platform. 
You help users with questions about services, orders, payments, and general platform usage. 
Be concise, friendly, and professional. Respond in Portuguese (Brazil) unless the user asks in another language.`;

/**
 * Get conversation history from Firestore
 * CRÍTICO: Dados privados - busca em users/{userId}/conversations/{conversationId}/messages (não em locations)
 */
async function getConversationHistoryFromFirestore(
  db: admin.firestore.Firestore,
  conversationId: string,
  userId: string
): Promise<Array<{role: string; content: string}>> {
  try {
    // Dados privados: usar users/{userId}/conversations (não locations)
    const messagesSnapshot = await db.collection('users')
      .doc(userId)
      .collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .orderBy('timestamp', 'asc')
      .get();

    functions.logger.info(
      `📍 Buscando histórico de conversa: users/${userId}/conversations/${conversationId}/messages ` +
      `(${messagesSnapshot.docs.length} mensagens)`
    );

    return messagesSnapshot.docs.map(doc => {
      const data = doc.data();
      return {
        role: data.role || 'user',
        content: data.content || '',
      };
    });
  } catch (error) {
    functions.logger.warn('Error loading conversation history:', error);
    return [];
  }
}

/**
 * Call OpenAI API with retry and exponential backoff
 * Baseado no padrão de engine.py e adaptive_fuzzer.py
 */
async function callOpenAI(
  messages: Array<{role: string; content: string}>,
  systemPrompt: string,
  openaiInstance: OpenAI,
  maxRetries = 3
): Promise<string> {
  if (!openaiInstance) {
    throw new Error('OpenAI not configured');
  }

  const openaiMessages: Array<{role: 'system' | 'user' | 'assistant'; content: string}> = [
    {
      role: 'system',
      content: systemPrompt,
    },
    ...messages.map(msg => ({
      role: (msg.role === 'assistant' ? 'assistant' : 'user') as 'user' | 'assistant',
      content: msg.content,
    })),
  ];

  let lastError: Error | null = null;
  
  // Retry com backoff exponencial: 1s, 2s, 4s
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      // Configurações otimizadas para respostas mais completas
      const completion = await openaiInstance.chat.completions.create({
        model: 'gpt-3.5-turbo',
        messages: openaiMessages,
        max_tokens: 2048, // Aumentado para respostas mais completas (baseado em chat_manager.py)
        temperature: 0.7,
        stream: false,
      });

      return completion.choices[0]?.message?.content || '';
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error));
      functions.logger.warn(`OpenAI attempt ${attempt + 1} failed: ${lastError.message}`);
      
      // Backoff exponencial antes da próxima tentativa
      if (attempt < maxRetries - 1) {
        const delayMs = (1 << attempt) * 1000; // 1s, 2s, 4s
        await new Promise(resolve => setTimeout(resolve, delayMs));
      }
    }
  }
  
  throw lastError || new Error('OpenAI API failed after all retries');
}

/**
 * Call Gemini API (fallback) with retry and exponential backoff
 * Baseado no padrão de engine.py e adaptive_fuzzer.py
 */
async function callGemini(
  messages: Array<{role: string; content: string}>,
  systemPrompt: string,
  geminiModelInstance: ReturnType<typeof getGeminiModel>,
  maxRetries = 3
): Promise<string> {
  if (!geminiModelInstance) {
    throw new Error('Gemini not configured');
  }

  let lastError: Error | null = null;
  
  // Retry com backoff exponencial: 1s, 2s, 4s
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      // Se houver histórico, usar chat com histórico
      if (messages.length > 1) {
        // Converter histórico (todas exceto a última mensagem)
        const history = messages.slice(0, -1).map(msg => ({
          role: msg.role === 'assistant' ? 'model' : 'user',
          parts: [{text: msg.content}],
        }));

        const chat = geminiModelInstance.startChat({
          systemInstruction: systemPrompt,
          history: history as Array<{role: string; parts: Array<{text: string}>}>,
        });

        const lastMessage = messages[messages.length - 1];
        const result = await chat.sendMessage(lastMessage.content);
        const response = await result.response;
        return response.text();
      } else {
        // Mensagem única - usar generateContent com system prompt incorporado
        // IMPORTANTE: Para mensagens únicas, incorporar o system prompt no início da mensagem
        // pois generateContent não suporta systemInstruction diretamente
        const prompt = systemPrompt 
          ? `[SISTEMA] ${systemPrompt}\n\n[USUÁRIO] ${messages[0].content}\n\n[ASSISTENTE]`
          : messages[0].content;
        
        const result = await geminiModelInstance.generateContent(prompt);
        const response = await result.response;
        return response.text();
      }
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error));
      functions.logger.warn(`Gemini attempt ${attempt + 1} failed: ${lastError.message}`);
      
      // Backoff exponencial antes da próxima tentativa
      if (attempt < maxRetries - 1) {
        const delayMs = (1 << attempt) * 1000; // 1s, 2s, 4s
        await new Promise(resolve => setTimeout(resolve, delayMs));
      }
    }
  }
  
  throw lastError || new Error('Gemini API failed after all retries');
}

/**
 * AI chat proxy function with moderation and rate limiting
 * Uses OpenAI as primary, Gemini as fallback
 * 
 * REQUIRED SECRETS:
 * - GEMINI_API_KEY (recommended) OR OPENAI_API_KEY
 * Configure with: firebase functions:secrets:set GEMINI_API_KEY
 */
export const aiChatProxy = functions.runWith({
  secrets: ['GEMINI_API_KEY'], // OPENAI_API_KEY é opcional
}).https.onCall(async (data, context) => {
  try {
    // Validar App Check
    validateAppCheck(context);
    
    // Verificar autenticação
    assertAuthenticated(context);
    
    const db = getFirestore();
    const {message, conversationId} = data;

    if (!message || typeof message !== 'string') {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Message is required and must be a string'
      );
    }

    // Rate limiting
    try {
      await rateLimiter.consume(context.auth!.uid);
    } catch (rateLimiterRes) {
      throw new functions.https.HttpsError(
        'resource-exhausted',
        'Too many requests. Please try again later.'
      );
    }

    // Content moderation - check for profanity
    if (filter.isProfane(message)) {
      // Log moderated message
      await db.collection('moderation_logs').add({
        userId: context.auth!.uid,
        message: message,
        reason: 'profanity',
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      throw new functions.https.HttpsError(
        'invalid-argument',
        'Message contains inappropriate content'
      );
    }

    // CRÍTICO: Dados privados - salvar em users/{userId}/conversations (não em locations)
    const userId = context.auth!.uid;
    
    // Load conversation history if conversationId provided
    let conversationHistory: Array<{role: string; content: string}> = [];
    if (conversationId) {
      conversationHistory = await getConversationHistoryFromFirestore(db, conversationId, userId);
      
      // Add user message to history for API call
      conversationHistory.push({
        role: 'user',
        content: message,
      });

      // CRÍTICO: Dados privados - salvar conversa em users/{userId}/conversations/{conversationId}
      const conversationRef = db.collection('users')
        .doc(userId)
        .collection('conversations')
        .doc(conversationId);
      const conversationDoc = await conversationRef.get();
      
      if (!conversationDoc.exists) {
        // Criar documento da conversa se não existir
        await conversationRef.set({
          userId: userId,
          type: 'ai',
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          lastMessage: message,
        });
        functions.logger.info('📍 Conversa criada em users/' + userId + '/conversations/' + conversationId);
      } else {
        // Atualizar apenas updatedAt se a conversa já existe
        await conversationRef.update({
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          lastMessage: message,
        });
      }

      // Save user message to Firestore
      await conversationRef.collection('messages').add({
        role: 'user',
        content: message,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });
    } else {
      // No conversationId - single message
      conversationHistory = [{
        role: 'user',
        content: message,
      }];
    }

    // Try OpenAI first, then Gemini as fallback (com retry automático)
    // Baseado no padrão de chat_manager.py que tenta múltiplos provedores
    let responseMessage: string;
    const tokensUsed = 0; // TODO: Track actual tokens from API response
    let provider = 'openai';

    // Inicializar provedores dinamicamente (secrets são carregadas em runtime)
    const openai = getOpenAI();
    const geminiModel = getGeminiModel();
    
    // Log de diagnóstico (sem expor as chaves)
    functions.logger.info('AI Providers Status:', {
      openaiConfigured: !!process.env.OPENAI_API_KEY,
      geminiConfigured: !!process.env.GEMINI_API_KEY,
      openaiKeyLength: process.env.OPENAI_API_KEY?.length || 0,
      geminiKeyLength: process.env.GEMINI_API_KEY?.length || 0,
    });

    // Verificar se pelo menos um provedor está configurado
    if (!openai && !geminiModel) {
      functions.logger.error(
        'No AI provider configured. OPENAI_API_KEY and GEMINI_API_KEY are both missing.'
      );
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Serviço de IA não configurado. Configure OPENAI_API_KEY ou GEMINI_API_KEY ' +
        'nas secrets do Firebase. Veja CONFIGURAR_CHAT_IA.md para instruções.'
      );
    }

    try {
      // Priorizar OpenAI se disponível, senão usar Gemini
      if (openai) {
        functions.logger.info('Using OpenAI as primary provider');
        responseMessage = await callOpenAI(conversationHistory, SYSTEM_PROMPT, openai, 3);
        provider = 'openai';
      } else if (geminiModel) {
        functions.logger.info('Using Gemini as primary provider (OpenAI not available)');
        responseMessage = await callGemini(conversationHistory, SYSTEM_PROMPT, geminiModel, 3);
        provider = 'gemini';
      } else {
        // Este caso não deveria acontecer devido à verificação acima, mas mantido por segurança
        throw new Error('No AI provider available');
      }
    } catch (primaryError) {
      functions.logger.warn('Primary AI provider failed after retries, trying fallback:', {
        error: primaryError instanceof Error ? primaryError.message : String(primaryError),
        openaiAvailable: !!openai,
        geminiAvailable: !!geminiModel,
      });
      
      // Try fallback com retry também
      if (openai && geminiModel) {
        try {
          functions.logger.info('Trying Gemini as fallback provider');
          responseMessage = await callGemini(conversationHistory, SYSTEM_PROMPT, geminiModel, 3);
          provider = 'gemini-fallback';
        } catch (fallbackError) {
          functions.logger.error('Fallback AI provider also failed after retries:', {
            error: fallbackError instanceof Error ? fallbackError.message : String(fallbackError),
          });
          throw new functions.https.HttpsError(
            'internal',
            'Serviço de IA indisponível após múltiplas tentativas. Tente novamente mais tarde.'
          );
        }
      } else {
        // Se não há fallback disponível, retornar erro mais específico
        const errorMessage = primaryError instanceof Error ? primaryError.message : String(primaryError);
        functions.logger.error('AI provider failed and no fallback available:', {
          error: errorMessage,
          openaiAvailable: !!openai,
          geminiAvailable: !!geminiModel,
        });
        throw new functions.https.HttpsError(
          'internal',
          `Serviço de IA indisponível: ${errorMessage}. Tente novamente mais tarde.`
        );
      }
    }

    // Verify AI response doesn't contain profanity
    if (filter.isProfane(responseMessage)) {
      functions.logger.warn('AI generated inappropriate response');
      throw new functions.https.HttpsError(
        'internal',
        'Unable to generate appropriate response'
      );
    }

    // Save AI response to conversation history
    // CRÍTICO: Dados privados - salvar em users/{userId}/conversations/{conversationId} (não em locations)
    if (conversationId) {
      const conversationRef = db.collection('users')
        .doc(userId)
        .collection('conversations')
        .doc(conversationId);
      
      // Garantir que o documento existe antes de atualizar
      const conversationDoc = await conversationRef.get();
      if (!conversationDoc.exists) {
        // Criar conversa se não existir
        await conversationRef.set({
          userId: userId,
          type: 'ai',
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          lastMessage: responseMessage,
        });
        functions.logger.info('📍 Conversa criada em users/' + userId + '/conversations/' + conversationId);
      } else {
        // Atualizar apenas se existir
        await conversationRef.update({
          lastMessage: responseMessage,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
      
      // Salvar mensagem da IA
      await conversationRef.collection('messages').add({
        role: 'assistant',
        content: responseMessage,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    // Track usage for analytics
    await db.collection('ai_usage').add({
      userId: context.auth!.uid,
      conversationId: conversationId || null,
      provider: provider,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      tokensUsed: tokensUsed,
    });

    functions.logger.info(`AI chat request processed for user ${context.auth!.uid} using ${provider}`);

    return {
      response: responseMessage,
      conversationId: conversationId || null,
      provider: provider,
    };
  } catch (error) {
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    functions.logger.error('Error in AI chat proxy:', error);
    throw handleError(error);
  }
});

/**
 * Get conversation history
 */
export const getConversationHistory = functions.runWith({
  secrets: ['GEMINI_API_KEY'], // OPENAI_API_KEY é opcional
}).https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    const {conversationId} = data;

    if (!conversationId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Conversation ID is required'
      );
    }

    // CRÍTICO: Dados privados - buscar em users/{userId}/conversations (não em locations)
    const userId = context.auth!.uid;
    
    // Verify user owns this conversation
    const conversationDoc = await db.collection('users')
      .doc(userId)
      .collection('conversations')
      .doc(conversationId)
      .get();
    
    if (!conversationDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Conversation not found');
    }

    if (conversationDoc.data()?.userId !== userId) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Access denied to this conversation'
      );
    }

    // Get messages
    const messagesSnapshot = await conversationDoc.ref
      .collection('messages')
      .orderBy('timestamp', 'asc')
      .get();

    const messages = messagesSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
    }));

    return {messages};
  } catch (error) {
    functions.logger.error('Error fetching conversation history:', error);
    throw handleError(error);
  }
});

/**
 * Create new conversation
 */
export const createConversation = functions.runWith({
  secrets: ['GEMINI_API_KEY'], // OPENAI_API_KEY é opcional
}).https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    
    // CRÍTICO: Dados privados - salvar em users/{userId}/conversations (não em locations)
    const userId = context.auth!.uid;

    const conversationRef = await db.collection('users')
      .doc(userId)
      .collection('conversations')
      .add({
        userId: userId,
        type: 'ai',
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

    functions.logger.info(`📍 Conversa criada em users/${userId}/conversations/${conversationRef.id}`);
    
    return {conversationId: conversationRef.id};
  } catch (error) {
    functions.logger.error('Error creating conversation:', error);
    throw handleError(error);
  }
});

/**
 * List user conversations
 */
export const listConversations = functions.runWith({
  secrets: ['GEMINI_API_KEY'], // OPENAI_API_KEY é opcional
}).https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    
    // CRÍTICO: Dados privados - buscar em users/{userId}/conversations (não em locations)
    const userId = context.auth!.uid;

    const conversationsSnapshot = await db.collection('users')
      .doc(userId)
      .collection('conversations')
      .orderBy('updatedAt', 'desc')
      .limit(50)
      .get();
    
    functions.logger.info(
      `📍 Listando conversas de users/${userId}/conversations ` +
      `(${conversationsSnapshot.docs.length} encontradas)`
    );

    const conversations = conversationsSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
    }));

    return {conversations};
  } catch (error) {
    functions.logger.error('Error listing conversations:', error);
    throw handleError(error);
  }
});
