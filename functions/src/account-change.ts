import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

/**
 * Função de limpeza e fortalecimento da consistência das solicitações de mudança de conta.
 *
 * Ela:
 *  - Garante que campos obrigatórios existam
 *  - Normaliza o status para PENDING/PROCESSED/REJECTED
 *  - Remove qualquer campo de processamento enviado pelo cliente
 *
 * Observação: o processamento efetivo da mudança de conta está sendo feito no app
 * via WorkManager/Worker Android. Esta função é um reforço de segurança e consistência.
 */
export const onAccountChangeRequestWrite = functions.firestore
  .document('account_change_requests/{requestId}')
  .onWrite(async (change, context) => {
    const requestId = context.params.requestId as string;

    // Se foi deletado, nada a fazer
    if (!change.after.exists) {
      return;
    }

    const beforeData = change.before.exists ? change.before.data() || {} : {};
    const afterData = change.after.data() || {};

    // Se foi criado, garantir consistência mínima
    if (!change.before.exists) {
      const userId = afterData.userId as string | undefined;
      const currentAccountType = afterData.currentAccountType as string | undefined;
      const requestedAccountType = afterData.requestedAccountType as string | undefined;

      if (!userId || !currentAccountType || !requestedAccountType) {
        functions.logger.warn('Solicitação de mudança de conta incompleta, removendo', {
          requestId,
          userId,
        });
        await change.after.ref.delete();
        return;
      }

      const allowedAccountTypes = ['PRESTADOR', 'VENDEDOR', 'CLIENTE'];
      if (
        !allowedAccountTypes.includes(currentAccountType) ||
        !allowedAccountTypes.includes(requestedAccountType)
      ) {
        functions.logger.warn('Tipo de conta inválido em solicitação, removendo', {
          requestId,
          currentAccountType,
          requestedAccountType,
        });
        await change.after.ref.delete();
        return;
      }

      // Normalizar status e limpar campos que só o backend pode controlar
      const cleanData: Record<string, unknown> = {
        ...afterData,
        status: 'PENDING',
        processedAt: null,
        processedBy: null,
        rejectionReason: null,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        createdAt: afterData.createdAt || admin.firestore.FieldValue.serverTimestamp(),
      };

      await change.after.ref.set(cleanData, { merge: true });
      functions.logger.info('Solicitação de mudança de conta criada/normalizada', {
        requestId,
        userId,
      });
      return;
    }

    // Em updates vindos do cliente, não permitir alteração de status / campos de processamento
    const beforeStatus = beforeData.status as string | undefined;
    const afterStatus = afterData.status as string | undefined;

    // Se status mudou mas não foi o backend (não temos uma forma 100% segura de saber aqui,
    // mas reforçamos mantendo o status anterior em qualquer mudança suspeita)
    if (beforeStatus && afterStatus && beforeStatus !== afterStatus) {
      functions.logger.warn('Tentativa de alteração de status detectada, revertendo', {
        requestId,
        beforeStatus,
        afterStatus,
      });

      await change.after.ref.set(
        {
          status: beforeStatus,
          processedAt: beforeData.processedAt || null,
          processedBy: beforeData.processedBy || null,
          rejectionReason: beforeData.rejectionReason || null,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      return;
    }

    // Apenas atualizar updatedAt para qualquer outra modificação
    await change.after.ref.set(
      {
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
  });


