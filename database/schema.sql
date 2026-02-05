-- ============================================
-- TASKGO POSTGRESQL SCHEMA
-- Migração do Firestore para PostgreSQL
-- ============================================

-- Extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- Para busca de texto
CREATE EXTENSION IF NOT EXISTS "postgis"; -- Para cálculos geográficos (opcional, mas recomendado)

-- ============================================
-- TABELAS BASE DE LOCALIZAÇÃO
-- ============================================

-- Estados brasileiros
CREATE TABLE states (
    id SERIAL PRIMARY KEY,
    code VARCHAR(2) UNIQUE NOT NULL, -- Sigla do estado (SP, RJ, etc.)
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Cidades com coordenadas
CREATE TABLE cities (
    id SERIAL PRIMARY KEY,
    state_id INTEGER NOT NULL REFERENCES states(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL, -- Nome normalizado para busca
    latitude DECIMAL(10, 8) NOT NULL, -- Coordenada do centro da cidade
    longitude DECIMAL(11, 8) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(state_id, normalized_name)
);

CREATE INDEX idx_cities_state ON cities(state_id);
CREATE INDEX idx_cities_normalized_name ON cities USING gin(normalized_name gin_trgm_ops);
CREATE INDEX idx_cities_location ON cities USING gist(point(longitude, latitude));

-- ============================================
-- TABELA DE USUÁRIOS
-- ============================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    firebase_uid VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('client', 'partner', 'admin')),
    display_name VARCHAR(255),
    phone VARCHAR(20),
    photo_url TEXT,
    
    -- Localização atual (dinâmica via GPS)
    current_latitude DECIMAL(10, 8),
    current_longitude DECIMAL(11, 8),
    current_city_id INTEGER REFERENCES cities(id),
    last_location_update TIMESTAMP,
    
    -- Verificação de identidade
    cpf VARCHAR(11),
    cnpj VARCHAR(14),
    rg VARCHAR(20),
    birth_date DATE,
    document_front_url TEXT,
    document_back_url TEXT,
    selfie_url TEXT,
    address_proof_url TEXT,
    
    -- Status de verificação
    profile_complete BOOLEAN DEFAULT false,
    verified BOOLEAN DEFAULT false,
    verified_at TIMESTAMP,
    verified_by UUID REFERENCES users(id),
    
    -- Stripe (legacy - será migrado para stripe_accounts)
    stripe_account_id VARCHAR(255),
    stripe_charges_enabled BOOLEAN DEFAULT false,
    stripe_payouts_enabled BOOLEAN DEFAULT false,
    
    -- Rating
    rating DECIMAL(3, 2) DEFAULT 0.00,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_current_city ON users(current_city_id);
CREATE INDEX idx_users_verified ON users(verified);

-- Histórico de localizações do usuário (GPS)
CREATE TABLE user_locations (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    city_id INTEGER NOT NULL REFERENCES cities(id),
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    is_current BOOLEAN DEFAULT false,
    accuracy DECIMAL(10, 2), -- Precisão do GPS em metros
    source VARCHAR(20) DEFAULT 'gps', -- gps, manual, etc.
    entered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    exited_at TIMESTAMP,
    
    -- Constraint: apenas 1 registro is_current = true por usuário
    CONSTRAINT unique_current_location UNIQUE NULLS NOT DISTINCT (user_id, is_current) WHERE is_current = true
);

CREATE INDEX idx_user_locations_user ON user_locations(user_id);
CREATE INDEX idx_user_locations_city ON user_locations(city_id);
CREATE INDEX idx_user_locations_current ON user_locations(user_id, is_current) WHERE is_current = true;

-- ============================================
-- CATEGORIAS
-- ============================================

CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('service', 'product')),
    icon_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Categorias preferidas do parceiro
CREATE TABLE user_preferred_categories (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, category_id)
);

CREATE INDEX idx_user_preferred_categories_user ON user_preferred_categories(user_id);
CREATE INDEX idx_user_preferred_categories_category ON user_preferred_categories(category_id);

-- ============================================
-- PRODUTOS
-- ============================================

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_in_city_id INTEGER NOT NULL REFERENCES cities(id), -- FIXO - cidade onde foi criado
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100),
    active BOOLEAN DEFAULT true,
    featured BOOLEAN DEFAULT false,
    discount_percentage DECIMAL(5, 2),
    rating DECIMAL(3, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_seller ON products(seller_id);
CREATE INDEX idx_products_city ON products(created_in_city_id);
CREATE INDEX idx_products_active ON products(active) WHERE active = true;
CREATE INDEX idx_products_category ON products(category);

-- Imagens dos produtos
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    position INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_images_product ON product_images(product_id);

-- ============================================
-- POSTS E STORIES
-- ============================================

CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_in_city_id INTEGER NOT NULL REFERENCES cities(id), -- FIXO
    content TEXT NOT NULL,
    media_urls TEXT[], -- Array de URLs
    media_types TEXT[], -- Array de tipos (image/video)
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_posts_user ON posts(user_id);
CREATE INDEX idx_posts_city ON posts(created_in_city_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);

-- Likes em posts
CREATE TABLE post_likes (
    id BIGSERIAL PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)
);

CREATE INDEX idx_post_likes_post ON post_likes(post_id);
CREATE INDEX idx_post_likes_user ON post_likes(user_id);

-- Comentários em posts
CREATE TABLE post_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_post_comments_post ON post_comments(post_id);
CREATE INDEX idx_post_comments_user ON post_comments(user_id);

-- Stories (expiram em 24h)
CREATE TABLE stories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_in_city_id INTEGER NOT NULL REFERENCES cities(id), -- FIXO
    media_url TEXT NOT NULL,
    media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('image', 'video')),
    expires_at TIMESTAMP NOT NULL, -- created_at + 24h
    views_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stories_user ON stories(user_id);
CREATE INDEX idx_stories_city ON stories(created_in_city_id);
CREATE INDEX idx_stories_expires ON stories(expires_at) WHERE expires_at > CURRENT_TIMESTAMP;

-- ============================================
-- ORDENS DE SERVIÇO
-- ============================================

CREATE TABLE service_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    partner_id UUID REFERENCES users(id), -- NULL até ser aceita
    created_in_city_id INTEGER NOT NULL REFERENCES cities(id), -- FIXO
    category VARCHAR(100) NOT NULL,
    details TEXT NOT NULL,
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    budget_min DECIMAL(10, 2),
    budget_max DECIMAL(10, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'completed', 'cancelled')),
    accepted_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_orders_client ON service_orders(client_id);
CREATE INDEX idx_service_orders_partner ON service_orders(partner_id);
CREATE INDEX idx_service_orders_city ON service_orders(created_in_city_id);
CREATE INDEX idx_service_orders_status ON service_orders(status);
CREATE INDEX idx_service_orders_category ON service_orders(category);
CREATE INDEX idx_service_orders_pending ON service_orders(created_in_city_id, category, status) WHERE status = 'pending' AND partner_id IS NULL;

-- Propostas/Orçamentos
CREATE TABLE proposals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL,
    message TEXT,
    estimated_time VARCHAR(100),
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(order_id, partner_id)
);

CREATE INDEX idx_proposals_order ON proposals(order_id);
CREATE INDEX idx_proposals_partner ON proposals(partner_id);

-- ============================================
-- PEDIDOS DE PRODUTOS
-- ============================================

CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_number VARCHAR(50) UNIQUE NOT NULL,
    client_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2),
    delivery_fee DECIMAL(10, 2),
    platform_fee DECIMAL(10, 2), -- 5% do total
    escrow_amount DECIMAL(10, 2), -- Valor retido até confirmação
    escrow_released_at TIMESTAMP,
    escrow_released_to UUID REFERENCES users(id), -- seller
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'paid', 'shipped', 'delivered', 'cancelled')),
    payment_method VARCHAR(50), -- pix, credit, debit
    payment_status VARCHAR(20),
    stripe_payment_intent_id VARCHAR(255),
    delivery_address TEXT,
    delivery_city_id INTEGER REFERENCES cities(id),
    tracking_code VARCHAR(100),
    courier VARCHAR(50), -- Correios, Sedex, etc.
    shipped_at TIMESTAMP,
    shipped_confirmed_by_seller BOOLEAN DEFAULT false,
    delivered_at TIMESTAMP,
    delivered_confirmed_by_client BOOLEAN DEFAULT false,
    delivered_confirmed_by_seller BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_purchase_orders_client ON purchase_orders(client_id);
CREATE INDEX idx_purchase_orders_seller ON purchase_orders(seller_id);
CREATE INDEX idx_purchase_orders_status ON purchase_orders(status);
CREATE INDEX idx_purchase_orders_order_number ON purchase_orders(order_number);

-- Itens do pedido
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    product_image_url TEXT,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- Eventos de rastreamento
CREATE TABLE order_tracking_events (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL, -- PREPARING, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED
    description TEXT,
    location TEXT,
    event_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tracking_events_order ON order_tracking_events(order_id);
CREATE INDEX idx_tracking_events_date ON order_tracking_events(event_date DESC);

-- ============================================
-- CONTAS BANCÁRIAS (PRIVADAS - APENAS PARCEIROS)
-- ============================================

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_holder_name VARCHAR(255) NOT NULL,
    bank_code VARCHAR(10) NOT NULL, -- 001=BB, 341=Itau, etc.
    bank_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('checking', 'savings', 'payment')),
    agency VARCHAR(10),
    account_number VARCHAR(20) NOT NULL,
    account_digit VARCHAR(5),
    cpf_cnpj VARCHAR(14) NOT NULL,
    pix_key VARCHAR(255),
    pix_key_type VARCHAR(20) CHECK (pix_key_type IN ('cpf', 'cnpj', 'email', 'phone', 'random')),
    is_primary BOOLEAN DEFAULT false,
    verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_partner_role CHECK (
        EXISTS (
            SELECT 1 FROM users WHERE users.id = bank_accounts.user_id AND users.role = 'partner'
        )
    )
);

CREATE INDEX idx_bank_accounts_user ON bank_accounts(user_id);
CREATE INDEX idx_bank_accounts_primary ON bank_accounts(user_id, is_primary) WHERE is_primary = true;

-- ============================================
-- GATEWAY DE PAGAMENTO STRIPE
-- ============================================

CREATE TABLE stripe_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    stripe_account_id VARCHAR(255) UNIQUE NOT NULL,
    charges_enabled BOOLEAN DEFAULT false,
    payouts_enabled BOOLEAN DEFAULT false,
    details_submitted BOOLEAN DEFAULT false,
    platform_fee_percentage DECIMAL(5, 2) DEFAULT 5.00, -- 5%
    application_fee_percentage DECIMAL(5, 2),
    transfer_data_destination VARCHAR(255),
    on_behalf_of VARCHAR(255),
    country VARCHAR(2) DEFAULT 'BR',
    default_currency VARCHAR(3) DEFAULT 'BRL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stripe_accounts_user ON stripe_accounts(user_id);
CREATE INDEX idx_stripe_accounts_stripe_id ON stripe_accounts(stripe_account_id);

CREATE TABLE stripe_payment_intents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    stripe_payment_intent_id VARCHAR(255) UNIQUE NOT NULL,
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'BRL',
    status VARCHAR(50) NOT NULL, -- succeeded, pending, failed
    client_secret VARCHAR(255),
    application_fee_amount DECIMAL(10, 2),
    transfer_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stripe_payment_intents_order ON stripe_payment_intents(purchase_order_id);
CREATE INDEX idx_stripe_payment_intents_stripe_id ON stripe_payment_intents(stripe_payment_intent_id);
CREATE INDEX idx_stripe_payment_intents_status ON stripe_payment_intents(status);

-- ============================================
-- CONVERSAS E CHAT
-- ============================================

CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type VARCHAR(20) NOT NULL CHECK (type IN ('user_user', 'order_chat', 'service_chat', 'support_ai')),
    purchase_order_id UUID REFERENCES purchase_orders(id) ON DELETE CASCADE,
    service_order_id UUID REFERENCES service_orders(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversations_type ON conversations(type);
CREATE INDEX idx_conversations_purchase_order ON conversations(purchase_order_id);
CREATE INDEX idx_conversations_service_order ON conversations(service_order_id);

-- Participantes da conversa
CREATE TABLE conversation_participants (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_read TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conversation_participants_conversation ON conversation_participants(conversation_id);
CREATE INDEX idx_conversation_participants_user ON conversation_participants(user_id);

-- Mensagens
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    media_url TEXT,
    media_type VARCHAR(20),
    read_by UUID[], -- Array de IDs que leram
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);

-- Conversas com IA (Suporte)
CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255),
    model VARCHAR(50), -- gpt-4, gemini, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conversations_user ON ai_conversations(user_id);

-- Mensagens de IA
CREATE TABLE ai_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_messages_conversation ON ai_messages(conversation_id);

-- ============================================
-- NOTIFICAÇÕES
-- ============================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- new_service_order_available, order_accepted, etc.
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB, -- Dados extras (orderId, category, etc.)
    read BOOLEAN DEFAULT false,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(user_id, read) WHERE read = false;
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- ============================================
-- AVALIAÇÕES
-- ============================================

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_id UUID NOT NULL, -- Pode ser product, service_order, ou user
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('product', 'service', 'partner')),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_reviewer ON reviews(reviewer_id);
CREATE INDEX idx_reviews_target ON reviews(target_id, target_type);
CREATE INDEX idx_reviews_rating ON reviews(rating);

-- ============================================
-- CONFIGURAÇÕES DO USUÁRIO
-- ============================================

CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    -- Notificações
    push_enabled BOOLEAN DEFAULT true,
    promo_enabled BOOLEAN DEFAULT true,
    sound_enabled BOOLEAN DEFAULT true,
    email_enabled BOOLEAN DEFAULT false,
    sms_enabled BOOLEAN DEFAULT false,
    -- Privacidade
    location_sharing BOOLEAN DEFAULT true,
    profile_visible BOOLEAN DEFAULT true,
    contact_info_sharing BOOLEAN DEFAULT false,
    -- Segurança
    biometric_enabled BOOLEAN DEFAULT false,
    two_factor_enabled BOOLEAN DEFAULT false,
    two_factor_method VARCHAR(20) CHECK (two_factor_method IN ('sms', 'email', 'authenticator')),
    -- Analytics
    analytics BOOLEAN DEFAULT true,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TRIGGERS E FUNÇÕES
-- ============================================

-- Função para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Aplicar trigger em tabelas com updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_posts_updated_at BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_service_orders_updated_at BEFORE UPDATE ON service_orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_purchase_orders_updated_at BEFORE UPDATE ON purchase_orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bank_accounts_updated_at BEFORE UPDATE ON bank_accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_stripe_accounts_updated_at BEFORE UPDATE ON stripe_accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_stripe_payment_intents_updated_at BEFORE UPDATE ON stripe_payment_intents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ai_conversations_updated_at BEFORE UPDATE ON ai_conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Função para atualizar contadores de likes e comentários
CREATE OR REPLACE FUNCTION update_post_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF TG_TABLE_NAME = 'post_likes' THEN
            UPDATE posts SET likes_count = likes_count + 1 WHERE id = NEW.post_id;
        ELSIF TG_TABLE_NAME = 'post_comments' THEN
            UPDATE posts SET comments_count = comments_count + 1 WHERE id = NEW.post_id;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        IF TG_TABLE_NAME = 'post_likes' THEN
            UPDATE posts SET likes_count = GREATEST(0, likes_count - 1) WHERE id = OLD.post_id;
        ELSIF TG_TABLE_NAME = 'post_comments' THEN
            UPDATE posts SET comments_count = GREATEST(0, comments_count - 1) WHERE id = OLD.post_id;
        END IF;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_post_likes_count AFTER INSERT OR DELETE ON post_likes
    FOR EACH ROW EXECUTE FUNCTION update_post_counts();

CREATE TRIGGER update_post_comments_count AFTER INSERT OR DELETE ON post_comments
    FOR EACH ROW EXECUTE FUNCTION update_post_counts();

-- Função para notificar quando nova ordem de serviço é criada
CREATE OR REPLACE FUNCTION notify_new_service_order()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify(
        'new_service_order',
        json_build_object(
            'order_id', NEW.id,
            'city_id', NEW.created_in_city_id,
            'category', NEW.category,
            'client_id', NEW.client_id
        )::text
    );
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER service_order_created_notify
    AFTER INSERT ON service_orders
    FOR EACH ROW
    WHEN (NEW.partner_id IS NULL)
    EXECUTE FUNCTION notify_new_service_order();

-- Função para limpar stories expiradas (executar periodicamente)
CREATE OR REPLACE FUNCTION cleanup_expired_stories()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM stories WHERE expires_at < CURRENT_TIMESTAMP;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ language 'plpgsql';

-- ============================================
-- COMENTÁRIOS NAS TABELAS
-- ============================================

COMMENT ON TABLE users IS 'Usuários globais - localização atualizada dinamicamente via GPS';
COMMENT ON TABLE user_locations IS 'Histórico de localizações do usuário via GPS';
COMMENT ON TABLE products IS 'Produtos vinculados à cidade onde foram criados (created_in_city_id)';
COMMENT ON TABLE posts IS 'Posts vinculados à cidade onde foram criados (created_in_city_id)';
COMMENT ON TABLE stories IS 'Stories vinculadas à cidade onde foram criadas (created_in_city_id)';
COMMENT ON TABLE service_orders IS 'Ordens de serviço vinculadas à cidade onde foram criadas';
COMMENT ON TABLE purchase_orders IS 'Pedidos de produtos com sistema de escrow e rastreamento';
COMMENT ON TABLE bank_accounts IS 'Contas bancárias privadas - apenas para parceiros';
COMMENT ON TABLE stripe_accounts IS 'Configurações completas do gateway Stripe';
COMMENT ON TABLE conversations IS 'Conversas entre usuários e suporte IA';
COMMENT ON TABLE notifications IS 'Notificações push e in-app';
