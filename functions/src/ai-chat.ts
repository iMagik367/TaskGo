import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import OpenAI from 'openai';
import {GoogleGenerativeAI} from '@google/generative-ai';
import {RateLimiterMemory} from 'rate-limiter-flexible';
import Filter from 'bad-words';
import {assertAuthenticated, handleError} from './utils/errors';

// Initialize OpenAI
const openai = process.env.OPENAI_API_KEY ? new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
}) : null;

// Initialize Gemini
const geminiApiKey = process.env.GEMINI_API_KEY;
const genAI = geminiApiKey ? new GoogleGenerativeAI(geminiApiKey) : null;
// Usar gemini-1.5-flash que é o modelo mais rápido e estável disponível
// Modelos disponíveis: gemini-1.5-flash (rápido), gemini-1.5-pro (mais preciso), gemini-pro (padrão)
// Configurações otimizadas para respostas mais rápidas
const geminiModel = genAI ? genAI.getGenerativeModel({
  model: 'gemini-1.5-flash',
  generationConfig: {
    maxOutputTokens: 500, // Reduzir tokens para respostas mais rápidas
    temperature: 0.7,
  },
}) : null;

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
 */
async function getConversationHistoryFromFirestore(
  db: admin.firestore.Firestore,
  conversationId: string
): Promise<Array<{role: string; content: string}>> {
  try {
    const messagesSnapshot = await db.collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .orderBy('timestamp', 'asc')
      .get();

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
  maxRetries = 3
): Promise<string> {
  if (!openai) {
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
      const completion = await openai.chat.completions.create({
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
  maxRetries = 3
): Promise<string> {
  if (!geminiModel) {
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

        const chat = geminiModel.startChat({
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
        
        const result = await geminiModel.generateContent(prompt);
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
 */
export const aiChatProxy = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check
    if (context.app === undefined && 
        process.env.FUNCTIONS_EMULATOR !== 'true' && 
        process.env.NODE_ENV !== 'development') {
      functions.logger.warn('App Check token missing for aiChatProxy', {
        uid: context.auth?.uid,
        timestamp: new Date().toISOString(),
      });
      throw new functions.https.HttpsError(
        'failed-precondition',
        'App Check validation failed'
      );
    }
    
    // Verificar autenticação
    assertAuthenticated(context);
    
    const db = admin.firestore();
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

    // Load conversation history if conversationId provided
    let conversationHistory: Array<{role: string; content: string}> = [];
    if (conversationId) {
      conversationHistory = await getConversationHistoryFromFirestore(db, conversationId);
      
      // Add user message to history for API call
      conversationHistory.push({
        role: 'user',
        content: message,
      });

      // Save user message to Firestore
      await db.collection('conversations').doc(conversationId).collection('messages').add({
        role: 'user',
        content: message,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Update conversation updatedAt
      await db.collection('conversations').doc(conversationId).update({
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
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

    try {
      if (openai) {
        responseMessage = await callOpenAI(conversationHistory, SYSTEM_PROMPT, 3);
        provider = 'openai';
      } else if (geminiModel) {
        responseMessage = await callGemini(conversationHistory, SYSTEM_PROMPT, 3);
        provider = 'gemini';
      } else {
        throw new functions.https.HttpsError(
          'failed-precondition',
          'No AI provider configured. Please configure OPENAI_API_KEY or GEMINI_API_KEY.'
        );
      }
    } catch (primaryError) {
      functions.logger.warn('Primary AI provider failed after retries, trying fallback:', primaryError);
      
      // Try fallback com retry também
      if (openai && geminiModel) {
        try {
          responseMessage = await callGemini(conversationHistory, SYSTEM_PROMPT, 3);
          provider = 'gemini-fallback';
        } catch (fallbackError) {
          functions.logger.error('Fallback AI provider also failed after retries:', fallbackError);
          throw new functions.https.HttpsError(
            'internal',
            'AI service unavailable after multiple attempts. Please try again later.'
          );
        }
      } else {
        throw new functions.https.HttpsError(
          'internal',
          'AI service unavailable. Please try again later.'
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
    if (conversationId) {
      await db.collection('conversations').doc(conversationId).collection('messages').add({
        role: 'assistant',
        content: responseMessage,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Update conversation updatedAt
      await db.collection('conversations').doc(conversationId).update({
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
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
export const getConversationHistory = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {conversationId} = data;

    if (!conversationId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Conversation ID is required'
      );
    }

    // Verify user owns this conversation
    const conversationDoc = await db.collection('conversations').doc(conversationId).get();
    
    if (!conversationDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Conversation not found');
    }

    if (conversationDoc.data()?.userId !== context.auth!.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Access denied to this conversation'
      );
    }

    // Get messages
    const messagesSnapshot = await db.collection('conversations')
      .doc(conversationId)
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
export const createConversation = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();

    const conversationRef = await db.collection('conversations').add({
      userId: context.auth!.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Conversation created: ${conversationRef.id}`);
    
    return {conversationId: conversationRef.id};
  } catch (error) {
    functions.logger.error('Error creating conversation:', error);
    throw handleError(error);
  }
});

/**
 * List user conversations
 */
export const listConversations = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();

    const conversationsSnapshot = await db.collection('conversations')
      .where('userId', '==', context.auth!.uid)
      .orderBy('updatedAt', 'desc')
      .limit(50)
      .get();

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
