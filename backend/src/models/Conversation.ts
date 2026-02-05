import { UUID } from 'crypto';

export interface Conversation {
  id: UUID;
  type: 'user_user' | 'order_chat' | 'service_chat' | 'support_ai';
  purchase_order_id?: UUID;
  service_order_id?: UUID;
  created_at: Date;
  updated_at: Date;
}

export interface ConversationParticipant {
  conversation_id: UUID;
  user_id: UUID;
  joined_at: Date;
  last_read?: Date;
}

export interface Message {
  id: UUID;
  conversation_id: UUID;
  sender_id: UUID;
  content: string;
  media_url?: string;
  media_type?: string;
  read_by?: UUID[]; // Array de IDs que leram
  created_at: Date;
}

export interface AIConversation {
  id: UUID;
  user_id: UUID;
  title?: string;
  model?: string; // gpt-4, gemini, etc.
  created_at: Date;
  updated_at: Date;
}

export interface AIMessage {
  id: UUID;
  conversation_id: UUID;
  role: 'user' | 'assistant';
  content: string;
  created_at: Date;
}
