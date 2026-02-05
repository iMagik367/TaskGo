export const COLLECTIONS = {
  USERS: 'users',
  ORDERS: 'orders',
  PAYMENTS: 'payments',
  NOTIFICATIONS: 'notifications',
  SERVICES: 'services',
  PRODUCTS: 'products',
  REVIEWS: 'reviews',
  REPORTS: 'reports',
} as const;

export const ORDER_STATUS = {
  PENDING: 'pending',
  PROPOSED: 'proposed',
  ACCEPTED: 'accepted',
  PAYMENT_PENDING: 'payment_pending',
  PAID: 'paid',
  IN_PROGRESS: 'in_progress',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled',
  DISPUTED: 'disputed',
} as const;

export const PAYMENT_STATUS = {
  PENDING: 'pending',
  PROCESSING: 'processing',
  SUCCEEDED: 'succeeded',
  FAILED: 'failed',
  REFUNDED: 'refunded',
} as const;

export const USER_ROLES = {
  CLIENT: 'client',
  PARTNER: 'partner',
  ADMIN: 'admin',
} as const;

export const NOTIFICATION_TYPES = {
  ORDER_CREATED: 'order_created',
  ORDER_ACCEPTED: 'order_accepted',
  ORDER_COMPLETED: 'order_completed',
  PAYMENT_RECEIVED: 'payment_received',
  REVIEW_RECEIVED: 'review_received',
  SYSTEM_ALERT: 'system_alert',
} as const;
