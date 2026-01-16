import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import {ImageAnnotatorClient} from '@google-cloud/vision';
import * as https from 'https';
import * as http from 'http';

const visionClient = new ImageAnnotatorClient();

/**
 * Cloud Function para processar verificação facial e OCR em tempo real
 * Triggered by Realtime Database writes to identity_verifications/{userId}
 */
export const processIdentityVerification = functions.database
  .ref('/identity_verifications/{userId}')
  .onCreate(async (snapshot, context) => {
    const userId = context.params.userId;
    const verificationData = snapshot.val();

    console.log(`Processando verificação de identidade para usuário: ${userId}`);

    try {
      // Atualizar status no Realtime Database
      await snapshot.ref.child('status').set('processing');
      await snapshot.ref.child('processedAt').set(admin.database.ServerValue.TIMESTAMP);

      const results: {
        faceMatch: {success: boolean; confidence: number; message: string} | null;
        ocrResult: {success: boolean; text: string; fields: Record<string, string>} | null;
        documentValidation: {valid: boolean; issues: string[]} | null;
      } = {
        faceMatch: null,
        ocrResult: null,
        documentValidation: null,
      };

      // 1. Verificação Facial - Comparar selfie com documento
      if (verificationData.selfieUrl && verificationData.documentFrontUrl) {
        try {
          const faceMatchResult = await compareFaces(
            verificationData.selfieUrl,
            verificationData.documentFrontUrl
          );
          results.faceMatch = faceMatchResult;

          // Atualizar no Realtime Database
          await snapshot.ref.child('faceMatch').set(faceMatchResult);
        } catch (error) {
          console.error('Erro na verificação facial:', error);
          results.faceMatch = {
            success: false,
            confidence: 0,
            message: `Erro na verificação facial: ${error instanceof Error ? error.message : 'Erro desconhecido'}`,
          };
        }
      }

      // 2. OCR - Extrair texto do documento
      if (verificationData.documentFrontUrl) {
        try {
          const ocrResult = await extractTextFromImage(verificationData.documentFrontUrl);
          results.ocrResult = ocrResult;

          // Atualizar no Realtime Database
          await snapshot.ref.child('ocrResult').set(ocrResult);

          // Validar campos do documento
          const validation = validateDocumentFields(ocrResult.text, ocrResult.fields);
          results.documentValidation = validation;
          await snapshot.ref.child('documentValidation').set(validation);
        } catch (error) {
          console.error('Erro no OCR:', error);
          results.ocrResult = {
            success: false,
            text: '',
            fields: {},
          };
        }
      }

      // 3. Determinar resultado final
      const isApproved =
        results.faceMatch?.success === true &&
        results.documentValidation?.valid === true;

      const finalStatus = isApproved ? 'approved' : 'rejected';
      const finalMessage = isApproved
        ? 'Verificação aprovada automaticamente'
        : 'Verificação rejeitada. Verifique os dados e tente novamente.';

      // Atualizar status final no Realtime Database
      await snapshot.ref.child('status').set(finalStatus);
      await snapshot.ref.child('finalResult').set({
        approved: isApproved,
        message: finalMessage,
        processedAt: admin.database.ServerValue.TIMESTAMP,
      });

      // Atualizar Firestore
      const userRef = admin.firestore().collection('users').doc(userId);
      await userRef.update({
        identityVerified: isApproved,
        identityVerificationStatus: finalStatus,
        identityVerificationProcessedAt: admin.firestore.FieldValue.serverTimestamp(),
        identityVerificationResults: results,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log(`Verificação processada para ${userId}: ${finalStatus}`);

      return {success: true, results, finalStatus};
    } catch (error) {
      console.error(`Erro ao processar verificação para ${userId}:`, error);
      await snapshot.ref.child('status').set('error');
      await snapshot.ref.child('error').set(
        error instanceof Error ? error.message : 'Erro desconhecido'
      );

      throw error;
    }
  });

/**
 * Compara duas imagens para verificar se são da mesma pessoa
 */
async function compareFaces(
  selfieUrl: string,
  documentUrl: string
): Promise<{success: boolean; confidence: number; message: string}> {
  try {
    // Baixar imagens do Storage
    const [selfieBuffer, documentBuffer] = await Promise.all([
      downloadImageFromUrl(selfieUrl),
      downloadImageFromUrl(documentUrl),
    ]);

    // Detectar faces nas duas imagens
    const [selfieResult, documentResult] = await Promise.all([
      visionClient.faceDetection({image: {content: selfieBuffer}}),
      visionClient.faceDetection({image: {content: documentBuffer}}),
    ]);

    const selfieFaces = selfieResult[0].faceAnnotations || [];
    const documentFaces = documentResult[0].faceAnnotations || [];

    if (selfieFaces.length === 0) {
      return {
        success: false,
        confidence: 0,
        message: 'Nenhuma face detectada na selfie',
      };
    }

    if (documentFaces.length === 0) {
      return {
        success: false,
        confidence: 0,
        message: 'Nenhuma face detectada no documento',
      };
    }

    // Comparar as faces usando landmarks e características
    const selfieFace = selfieFaces[0];
    const documentFace = documentFaces[0];

    // Calcular similaridade baseada em landmarks
    const similarity = calculateFaceSimilarity(selfieFace, documentFace);

    // Threshold de 0.7 (70%) para aprovação
    const threshold = 0.7;
    const success = similarity >= threshold;

    return {
      success,
      confidence: similarity,
      message: success
        ? `Faces correspondem (${(similarity * 100).toFixed(1)}% de similaridade)`
        : `Faces não correspondem suficientemente (${(similarity * 100).toFixed(1)}% de similaridade)`,
    };
  } catch (error) {
    console.error('Erro ao comparar faces:', error);
    throw error;
  }
}

/**
 * Extrai texto de uma imagem usando OCR
 */
async function extractTextFromImage(
  imageUrl: string
): Promise<{success: boolean; text: string; fields: Record<string, string>}> {
  try {
    const imageBuffer = await downloadImageFromUrl(imageUrl);

    // Executar OCR
    const [result] = await visionClient.textDetection({image: {content: imageBuffer}});
    const detections = result.textAnnotations || [];

    if (detections.length === 0) {
      return {
        success: false,
        text: '',
        fields: {},
      };
    }

    // Primeiro elemento contém todo o texto
    const fullText = detections[0].description || '';

    // Extrair campos específicos do documento (CPF, RG, nome, etc.)
    const fields = extractDocumentFields(fullText);

    return {
      success: true,
      text: fullText,
      fields,
    };
  } catch (error) {
    console.error('Erro ao extrair texto:', error);
    throw error;
  }
}

/**
 * Calcula similaridade entre duas faces usando landmarks
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function calculateFaceSimilarity(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  face1: any,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  face2: any
): number {
  if (!face1.landmarks || !face2.landmarks || face1.landmarks.length === 0 || face2.landmarks.length === 0) {
    return 0.5; // Se não houver landmarks, retorna score médio
  }

  // Normalizar landmarks pela posição do bounding box
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const normalizeLandmark = (landmark: any, bbox: any) => {
    const vertices = bbox.vertices || bbox;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const minX = Math.min(...vertices.map((v: any) => v.x || 0));
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const maxX = Math.max(...vertices.map((v: any) => v.x || 0));
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const minY = Math.min(...vertices.map((v: any) => v.y || 0));
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const maxY = Math.max(...vertices.map((v: any) => v.y || 0));
    
    const width = maxX - minX || 1;
    const height = maxY - minY || 1;
    
    return {
      x: ((landmark.position?.x || 0) - minX) / width,
      y: ((landmark.position?.y || 0) - minY) / height,
      z: landmark.position?.z || 0,
    };
  };

  // Comparar landmarks principais (olhos, nariz, boca)
  const keyLandmarkTypes = [
    'LEFT_EYE',
    'RIGHT_EYE',
    'NOSE_TIP',
    'MOUTH_LEFT',
    'MOUTH_RIGHT',
    'LEFT_OF_LEFT_EYEBROW',
    'RIGHT_OF_RIGHT_EYEBROW',
  ];

  let totalSimilarity = 0;
  let count = 0;

  for (const type of keyLandmarkTypes) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const lm1 = face1.landmarks.find((l: any) => l.type === type);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const lm2 = face2.landmarks.find((l: any) => l.type === type);

    if (lm1 && lm2) {
      const norm1 = normalizeLandmark(lm1, face1.boundingPoly || face1.fdBoundingPoly);
      const norm2 = normalizeLandmark(lm2, face2.boundingPoly || face2.fdBoundingPoly);

      // Calcular distância euclidiana normalizada
      const distance = Math.sqrt(
        Math.pow(norm1.x - norm2.x, 2) +
        Math.pow(norm1.y - norm2.y, 2) +
        Math.pow(norm1.z - norm2.z, 2)
      );

      // Converter distância em similaridade (0-1)
      const similarity = Math.max(0, 1 - distance);
      totalSimilarity += similarity;
      count++;
    }
  }

  return count > 0 ? totalSimilarity / count : 0;
}

/**
 * Extrai campos específicos do texto do documento
 */
function extractDocumentFields(text: string): Record<string, string> {
  const fields: Record<string, string> = {};

  // CPF (formato: XXX.XXX.XXX-XX)
  const cpfMatch = text.match(/\d{3}\.\d{3}\.\d{3}-\d{2}/);
  if (cpfMatch) {
    fields.cpf = cpfMatch[0];
  }

  // RG (formato: XX.XXX.XXX-X)
  const rgMatch = text.match(/\d{2}\.\d{3}\.\d{3}-\d{1}/);
  if (rgMatch) {
    fields.rg = rgMatch[0];
  }

  // Nome (geralmente na primeira linha ou após "NOME")
  const nomeMatch = text.match(/(?:NOME|NOME COMPLETO)[:\s]+([A-ZÁÉÍÓÚÇ\s]+)/i);
  if (nomeMatch) {
    fields.nome = nomeMatch[1].trim();
  } else {
    // Tentar pegar primeira linha que parece um nome
    const lines = text.split('\n').filter((line) => line.trim().length > 5);
    if (lines.length > 0 && /^[A-ZÁÉÍÓÚÇ\s]+$/.test(lines[0].trim())) {
      fields.nome = lines[0].trim();
    }
  }

  // Data de nascimento (formato: DD/MM/AAAA)
  const dataMatch = text.match(/\d{2}\/\d{2}\/\d{4}/);
  if (dataMatch) {
    fields.dataNascimento = dataMatch[0];
  }

  return fields;
}

/**
 * Valida os campos extraídos do documento
 */
function validateDocumentFields(
  fullText: string,
  fields: Record<string, string>
): {valid: boolean; issues: string[]} {
  const issues: string[] = [];

  // Verificar se CPF foi encontrado
  if (!fields.cpf) {
    issues.push('CPF não encontrado no documento');
  } else {
    // Validar formato do CPF
    const cpfDigits = fields.cpf.replace(/\D/g, '');
    if (cpfDigits.length !== 11) {
      issues.push('CPF inválido (formato incorreto)');
    }
  }

  // Verificar se nome foi encontrado
  if (!fields.nome || fields.nome.length < 3) {
    issues.push('Nome não encontrado ou inválido no documento');
  }

  // Verificar se data de nascimento foi encontrada
  if (!fields.dataNascimento) {
    issues.push('Data de nascimento não encontrada no documento');
  }

  return {
    valid: issues.length === 0,
    issues,
  };
}

/**
 * Baixa uma imagem de uma URL (Storage ou HTTP)
 */
async function downloadImageFromUrl(url: string): Promise<Buffer> {
  try {
    // Se for uma URL do Firebase Storage (gs://)
    if (url.startsWith('gs://')) {
      const bucket = admin.storage().bucket();
      const filePath = url.replace('gs://', '').split('/').slice(1).join('/');
      const file = bucket.file(filePath);
      const [buffer] = await file.download();
      return buffer;
    }

    // Se for uma URL do Firebase Storage (https://firebasestorage.googleapis.com)
    if (url.includes('firebasestorage.googleapis.com')) {
      try {
        // Tentar extrair o caminho do arquivo da URL
        const urlObj = new URL(url);
        const pathMatch = urlObj.pathname.match(/\/v0\/b\/([^/]+)\/o\/(.+)/);
        
        if (pathMatch) {
          const bucketName = pathMatch[1];
          const filePath = decodeURIComponent(pathMatch[2]);
          const bucket = admin.storage().bucket(bucketName);
          const file = bucket.file(filePath);
          const [buffer] = await file.download();
          return buffer;
        }
      } catch (storageError) {
        console.warn('Erro ao baixar do Storage, tentando HTTP:', storageError);
        // Se falhar, tentar baixar via HTTP
      }
    }

    // Se for uma URL HTTP/HTTPS
    if (url.startsWith('http://') || url.startsWith('https://')) {
      const client = url.startsWith('https://') ? https : http;

      return new Promise<Buffer>((resolve, reject) => {
        client.get(url, (response: http.IncomingMessage) => {
          if (response.statusCode !== 200) {
            reject(new Error(`Falha ao baixar imagem: ${response.statusCode}`));
            return;
          }

          const chunks: Buffer[] = [];
          response.on('data', (chunk: Buffer) => chunks.push(chunk));
          response.on('end', () => resolve(Buffer.concat(chunks)));
          response.on('error', reject);
        }).on('error', reject);
      });
    }

    throw new Error(`Formato de URL não suportado: ${url}`);
  } catch (error) {
    console.error('Erro ao baixar imagem:', error);
    throw error;
  }
}

/**
 * Cloud Function HTTP para iniciar verificação de identidade
 * Chamada pelo app quando o usuário envia documentos
 */
export const startIdentityVerification = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check
    if (context.app === undefined && 
        process.env.FUNCTIONS_EMULATOR !== 'true' && 
        process.env.NODE_ENV !== 'development') {
      functions.logger.warn('App Check token missing for startIdentityVerification', {
        uid: context.auth?.uid,
        timestamp: new Date().toISOString(),
      });
      throw new functions.https.HttpsError(
        'failed-precondition',
        'App Check validation failed'
      );
    }
    
    // Verificar autenticação
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'Usuário não autenticado'
      );
    }

    const userId = context.auth.uid;
    const {documentFrontUrl, documentBackUrl, selfieUrl, addressProofUrl} = data;

    // Validar que todos os documentos obrigatórios foram enviados
    if (!documentFrontUrl || !selfieUrl) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Documento frontal e selfie são obrigatórios'
      );
    }

    // Criar entrada no Realtime Database para trigger automático
    const db = admin.database();
    const verificationRef = db.ref(`identity_verifications/${userId}`);

    await verificationRef.set({
      userId,
      documentFrontUrl,
      documentBackUrl: documentBackUrl || null,
      selfieUrl,
      addressProofUrl: addressProofUrl || null,
      status: 'pending',
      createdAt: admin.database.ServerValue.TIMESTAMP,
    });

    // Atualizar Firestore
    const userRef = admin.firestore().collection('users').doc(userId);
    await userRef.update({
      documentFront: documentFrontUrl,
      documentBack: documentBackUrl || null,
      selfie: selfieUrl,
      addressProof: addressProofUrl || null,
      identityVerificationStatus: 'pending',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Identity verification started for user ${userId}`, {
      userId,
      timestamp: new Date().toISOString(),
    });

    return {
      success: true,
      message: 'Verificação iniciada. Processando em tempo real...',
      userId,
    };
  } catch (error) {
    functions.logger.error('Error starting identity verification:', error);
    const message = error instanceof Error ? error.message : 'Erro desconhecido';
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError(
      'internal',
      'Erro ao processar verificação de identidade',
      message
    );
  }
});

