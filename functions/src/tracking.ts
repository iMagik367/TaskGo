import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import * as https from 'https';
import {handleError} from './utils/errors';

const db = admin.firestore();

/**
 * Track order using Correios API
 */
export const trackCorreiosOrder = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    const {trackingCode} = data;
    if (!trackingCode) {
      throw new functions.https.HttpsError('invalid-argument', 'Tracking code is required');
    }

    // Buscar informações de rastreamento dos Correios
    const trackingInfo = await fetchCorreiosTracking(trackingCode);

    return {
      success: true,
      trackingInfo: trackingInfo,
    };
  } catch (error) {
    functions.logger.error('Error tracking Correios order:', error);
    throw handleError(error);
  }
});

/**
 * Update shipment tracking automatically
 * This function should be called periodically (via scheduled function or worker)
 */
export const updateShipmentTracking = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    const {shipmentId} = data;
    if (!shipmentId) {
      throw new functions.https.HttpsError('invalid-argument', 'Shipment ID is required');
    }

    const shipmentDoc = await db.collection('shipments').doc(shipmentId).get();
    if (!shipmentDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Shipment not found');
    }

    const shipment = shipmentDoc.data();
    const trackingCode = shipment?.trackingCode;
    const shippingMethod = shipment?.shippingMethod;

    if (!trackingCode) {
      throw new functions.https.HttpsError('failed-precondition', 'Shipment has no tracking code');
    }

    let trackingInfo;
    if (shippingMethod === 'CORREIOS') {
      trackingInfo = await fetchCorreiosTracking(trackingCode);
    } else if (shipment?.trackingUrl) {
      // Para outras transportadoras, usar a URL fornecida
      trackingInfo = await fetchGenericTracking(shipment.trackingUrl);
    } else {
      throw new functions.https.HttpsError('failed-precondition', 'No tracking method available');
    }

    // Atualizar eventos de rastreamento
    if (trackingInfo.events && trackingInfo.events.length > 0) {
      for (const event of trackingInfo.events) {
        await db.collection('tracking_events').add({
          orderId: shipment.orderId,
          shipmentId: shipmentId,
          eventType: event.type,
          description: event.description,
          location: event.location,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          done: event.done || false,
          source: shippingMethod === 'CORREIOS' ? 'CORREIOS_API' : 'OTHER_API',
        });
      }

      // Atualizar status do envio baseado no último evento
      const lastEvent = trackingInfo.events[trackingInfo.events.length - 1];
      await shipmentDoc.ref.update({
        status: lastEvent.type,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Se entregue, atualizar data de entrega
      if (lastEvent.type === 'DELIVERED') {
        await shipmentDoc.ref.update({
          deliveredAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
    }

    return {
      success: true,
      trackingInfo: trackingInfo,
    };
  } catch (error) {
    functions.logger.error('Error updating shipment tracking:', error);
    throw handleError(error);
  }
});

/**
 * Scheduled function to update tracking for all active shipments
 * Runs every hour
 */
export const scheduledTrackingUpdate = functions.pubsub
  .schedule('every 1 hours')
  .onRun(async (_context) => {
    try {
      const activeShipments = await db.collection('shipments')
        .where('status', 'in', ['SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY'])
        .get();

      functions.logger.info(`Updating tracking for ${activeShipments.size} active shipments`);

      for (const shipmentDoc of activeShipments.docs) {
        const shipment = shipmentDoc.data();
        const trackingCode = shipment.trackingCode;
        const shippingMethod = shipment.shippingMethod;

        if (!trackingCode) continue;

        try {
          let trackingInfo;
          if (shippingMethod === 'CORREIOS') {
            trackingInfo = await fetchCorreiosTracking(trackingCode);
          } else if (shipment.trackingUrl) {
            trackingInfo = await fetchGenericTracking(shipment.trackingUrl);
          } else {
            continue;
          }

          // Atualizar eventos
          if (trackingInfo.events && trackingInfo.events.length > 0) {
            const lastEvent = trackingInfo.events[trackingInfo.events.length - 1];
            await shipmentDoc.ref.update({
              status: lastEvent.type,
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
          }
        } catch (error) {
          functions.logger.error(`Error updating tracking for shipment ${shipmentDoc.id}:`, error);
        }
      }

      return null;
    } catch (error) {
      functions.logger.error('Error in scheduled tracking update:', error);
      return null;
    }
  });

interface TrackingInfo {
  trackingCode?: string;
  url?: string;
  events: Array<{
    type: string;
    description: string;
    location?: string;
    timestamp?: Date;
    done: boolean;
  }>;
  status: string;
}

/**
 * Fetch tracking information from Correios API
 */
async function fetchCorreiosTracking(trackingCode: string): Promise<TrackingInfo> {
  return new Promise((resolve, reject) => {
    // Formato da URL dos Correios para rastreamento
    const url = `https://www2.correios.com.br/sistemas/rastreamento/resultado.cfm?objetos=${trackingCode}`;

    https.get(url, (res) => {
      let data = '';

      res.on('data', (chunk) => {
        data += chunk;
      });

      res.on('end', () => {
        try {
          // Parse HTML response (simplified - in production, use proper HTML parser)
          const events = parseCorreiosHTML(data);
          resolve({
            trackingCode: trackingCode,
            events: events,
            status: events.length > 0 ? events[events.length - 1].type : 'UNKNOWN',
          });
        } catch (error) {
          reject(error);
        }
      });
    }).on('error', (error) => {
      reject(error);
    });
  });
}

interface TrackingEvent {
  type: string;
  description: string;
  location?: string;
  timestamp?: Date;
  done: boolean;
}

/**
 * Parse Correios HTML response (simplified)
 * In production, use a proper HTML parser like cheerio
 */
function parseCorreiosHTML(html: string): Array<TrackingEvent> {
  // This is a simplified parser - in production, use cheerio or similar
  const events: Array<TrackingEvent> = [];

  // Basic parsing logic (simplified)
  // In production, implement proper HTML parsing
  if (html.includes('Objeto entregue')) {
    events.push({
      type: 'DELIVERED',
      description: 'Objeto entregue ao destinatário',
      done: true,
    });
  } else if (html.includes('Objeto saiu para entrega')) {
    events.push({
      type: 'OUT_FOR_DELIVERY',
      description: 'Objeto saiu para entrega',
      done: false,
    });
  } else if (html.includes('Objeto em trânsito')) {
    events.push({
      type: 'IN_TRANSIT',
      description: 'Objeto em trânsito',
      done: false,
    });
  }

  return events;
}

/**
 * Fetch tracking information from generic URL
 */
async function fetchGenericTracking(url: string): Promise<TrackingInfo> {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      res.on('data', () => {
        // Consume data but don't store it
      });

      res.on('end', () => {
        try {
          // Parse generic tracking page (simplified)
          resolve({
            url: url,
            events: [],
            status: 'UNKNOWN',
          });
        } catch (error) {
          reject(error);
        }
      });
    }).on('error', (error) => {
      reject(error);
    });
  });
}

