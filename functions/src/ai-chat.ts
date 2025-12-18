import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import OpenAI from 'openai';
import {RateLimiterMemory} from 'rate-limiter-flexible';
import Filter from 'bad-words';
import {assertAuthenticated, handleError} from './utils/errors';

// Initialize OpenAI
const openai = process.env.OPENAI_API_KEY ? new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
}) : null;

// Initialize content filter
const filter = new Filter();

// Rate limiter: 10 requests per minute per user
const rateLimiter = new RateLimiterMemory({
  points: 10, // Number of requests
  duration: 60, // Per 60 seconds
});

/**
 * AI chat proxy function with moderation and rate limiting
 */
export const aiChatProxy = functions.https.onCall(async (data, context) => {
  try {
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

    // Add to conversation history if conversationId provided
    if (conversationId) {
      await db.collection('conversations').doc(conversationId).collection('messages').add({
        role: 'user',
        content: message,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    // Check if OpenAI is configured
    if (!openai) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'OpenAI API key is not configured'
      );
    }

    // Call OpenAI API
    const completion = await openai.chat.completions.create({
      model: 'gpt-3.5-turbo',
      messages: [
        {
          role: 'system',
          content: `You are a helpful assistant for TaskGo, a service marketplace platform. 
          You help users with questions about services, orders, payments, and general platform usage. 
          Be concise, friendly, and professional.`,
        },
        {
          role: 'user',
          content: message,
        },
      ],
      max_tokens: 500,
      temperature: 0.7,
    });

    const responseMessage = completion.choices[0]?.message?.content || '';

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
    }

    // Track usage for analytics
    await db.collection('ai_usage').add({
      userId: context.auth!.uid,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      tokensUsed: completion.usage?.total_tokens || 0,
    });

    functions.logger.info(`AI chat request processed for user ${context.auth!.uid}`);

    return {
      response: responseMessage,
      conversationId: conversationId || null,
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
