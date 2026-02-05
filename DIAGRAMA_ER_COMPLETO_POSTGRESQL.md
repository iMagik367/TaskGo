# DIAGRAMA ER COMPLETO - POSTGRESQL TASKGO

## Modelo Entidade-Relacionamento Visual

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                       USERS                                             │
│                           (Usuários Globais - GPS Dinâmico)                             │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│  id                          │ UUID          │ PRIMARY KEY                              │
│  firebase_uid                │ VARCHAR(255)  │ UNIQUE NOT NULL                          │
│  email                       │ VARCHAR(255)  │ UNIQUE NOT NULL                          │
│  role                        │ VARCHAR(20)   │ partner / client / admin                 │
│  display_name                │ VARCHAR(255)  │                                          │
│  phone                       │ VARCHAR(20)   │                                          │
│  photo_url                   │ TEXT          │ Firebase Storage URL                     │
│  current_latitude            │ DECIMAL(10,8) │ GPS em tempo real                        │
│  current_longitude           │ DECIMAL(11,8) │ GPS em tempo real                        │
│  current_city_id             │ INTEGER       │ FK → cities.id (onde está AGORA)         │
│  last_location_update        │ TIMESTAMP     │                                          │
│  cpf                         │ VARCHAR(11)   │                                          │
│  cnpj                        │ VARCHAR(14)   │                                          │
│  rg                          │ VARCHAR(20)   │                                          │
│  birth_date                  │ DATE          │                                          │
│  document_front_url          │ TEXT          │ Firebase Storage - doc frente            │
│  document_back_url           │ TEXT          │ Firebase Storage - doc verso             │
│  selfie_url                  │ TEXT          │ Firebase Storage - selfie                │
│  address_proof_url           │ TEXT          │ Firebase Storage - comprovante           │
│  profile_complete            │ BOOLEAN       │ DEFAULT false                            │
│  verified                    │ BOOLEAN       │ DEFAULT false                            │
│  verified_at                 │ TIMESTAMP     │                                          │
│  verified_by                 │ UUID          │ FK → users.id (admin que aprovou)        │
│  stripe_account_id           │ VARCHAR(255)  │                                          │
│  stripe_charges_enabled      │ BOOLEAN       │ DEFAULT false                            │
│  stripe_payouts_enabled      │ BOOLEAN       │ DEFAULT false                            │
│  rating                      │ DECIMAL(3,2)  │ Média de avaliações                      │
│  created_at                  │ TIMESTAMP     │ DEFAULT CURRENT_TIMESTAMP                │
│  updated_at                  │ TIMESTAMP     │ DEFAULT CURRENT_TIMESTAMP                │
└─────────────────────────────────────────────────────────────────────────────────────────┘
        │
        ├──────────────────────────────────────────────────────────────────┐
        │                                                                  │
        ▼                                                                  ▼
┌─────────────────────────────┐                              ┌────────────────────────────────────┐
│        STATES               │                              │      USER_LOCATIONS                │
│   (Estados Brasileiros)     │                              │  (Histórico de Localizações)       │
├─────────────────────────────┤                              ├────────────────────────────────────┤
│  id      │ INTEGER    │ PK │                              │  id          │ BIGSERIAL     │ PK  │
│  code    │ VARCHAR(2) │ UK │                              │  user_id     │ UUID          │ FK  │
│  name    │ VARCHAR    │    │                              │  city_id     │ INTEGER       │ FK  │
└─────────────────────────────┘                              │  latitude    │ DECIMAL(10,8) │     │
        │                                                    │  longitude   │ DECIMAL(11,8) │     │
        │                                                    │  is_current  │ BOOLEAN       │ *   │
        ▼                                                    │  accuracy    │ DECIMAL(10,2) │     │
┌─────────────────────────────────────────────────┐          │  source      │ VARCHAR(20)   │ gps │
│                  CITIES                         │          │  entered_at  │ TIMESTAMP     │     │
│        (Cidades com coordenadas)                │          │  exited_at   │ TIMESTAMP     │     │
├─────────────────────────────────────────────────┤          └────────────────────────────────────┘
│  id                │ INTEGER       │ PK        │              * UNIQUE INDEX: apenas 1 is_current
│  state_id          │ INTEGER       │ FK        │                per user_id
│  name              │ VARCHAR(100)  │           │
│  normalized_name   │ VARCHAR(100)  │ UK        │
│  latitude          │ DECIMAL(10,8) │ centro    │
│  longitude         │ DECIMAL(11,8) │ centro    │
└─────────────────────────────────────────────────┘
        │
        ├───────────────┬──────────────┬────────────────┐
        │               │              │                │
        ▼               ▼              ▼                ▼
┌────────────────────────────────────────────────────────────────────┐
│                          PRODUCTS                                  │
│              (Produtos vinculados à cidade de criação)             │
├────────────────────────────────────────────────────────────────────┤
│  id                     │ UUID          │ PK                       │
│  seller_id              │ UUID          │ FK → users.id            │
│  created_in_city_id     │ INTEGER       │ FK → cities.id (fixo)    │
│  title                  │ VARCHAR(255)  │ NOT NULL                 │
│  description            │ TEXT          │                          │
│  price                  │ DECIMAL(10,2) │ NOT NULL                 │
│  category               │ VARCHAR(100)  │                          │
│  active                 │ BOOLEAN       │ DEFAULT true             │
│  featured               │ BOOLEAN       │ DEFAULT false            │
│  discount_percentage    │ DECIMAL(5,2)  │                          │
│  rating                 │ DECIMAL(3,2)  │                          │
│  created_at             │ TIMESTAMP     │                          │
│  updated_at             │ TIMESTAMP     │                          │
└────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────┐
│       PRODUCT_IMAGES                   │
├────────────────────────────────────────┤
│  id          │ BIGSERIAL   │ PK       │
│  product_id  │ UUID        │ FK       │
│  image_url   │ TEXT        │ Firebase │
│  position    │ INTEGER     │          │
└────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│                      POSTS                             │
│           (Posts vinculados à cidade)                  │
├────────────────────────────────────────────────────────┤
│  id                   │ UUID          │ PK             │
│  user_id              │ UUID          │ FK → users.id  │
│  created_in_city_id   │ INTEGER       │ FK → cities.id │
│  content              │ TEXT          │ NOT NULL       │
│  media_urls           │ TEXT[]        │ Array          │
│  media_types          │ TEXT[]        │ Array          │
│  likes_count          │ INTEGER       │ DEFAULT 0      │
│  comments_count       │ INTEGER       │ DEFAULT 0      │
│  created_at           │ TIMESTAMP     │                │
│  updated_at           │ TIMESTAMP     │                │
└────────────────────────────────────────────────────────┘
        │
        ├────────────────┬─────────────────┐
        ▼                ▼                 │
┌──────────────────┐  ┌───────────────────┐
│   POST_LIKES     │  │  POST_COMMENTS    │
├──────────────────┤  ├───────────────────┤
│ • id      BIGINT │  │ • id      BIGINT  │
│ • post_id UUID   │  │ • post_id UUID    │
│ • user_id UUID   │  │ • user_id UUID    │
│ • created TSTP   │  │ • content TEXT    │
└──────────────────┘  │ • created TSTP    │
                      └───────────────────┘

┌────────────────────────────────────────────────────────┐
│                     STORIES                            │
│    (Stories temporárias - expiram em 24h)              │
├────────────────────────────────────────────────────────┤
│  id                   │ UUID          │ PK             │
│  user_id              │ UUID          │ FK → users.id  │
│  created_in_city_id   │ INTEGER       │ FK → cities.id │
│  media_url            │ TEXT          │ Firebase       │
│  media_type           │ VARCHAR(20)   │ image/video    │
│  expires_at           │ TIMESTAMP     │ created + 24h  │
│  views_count          │ INTEGER       │ DEFAULT 0      │
│  created_at           │ TIMESTAMP     │                │
└────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                      SERVICE_ORDERS                              │
│        (Ordens de Serviço - fixas na cidade de criação)          │
├──────────────────────────────────────────────────────────────────┤
│  id                   │ UUID          │ PK                       │
│  client_id            │ UUID          │ FK → users.id            │
│  partner_id           │ UUID          │ FK (NULL até aceitar)    │
│  created_in_city_id   │ INTEGER       │ FK → cities.id           │
│  category             │ VARCHAR(100)  │ NOT NULL                 │
│  details              │ TEXT          │ NOT NULL                 │
│  address              │ TEXT          │ Endereço do serviço      │
│  latitude             │ DECIMAL(10,8) │                          │
│  longitude            │ DECIMAL(11,8) │                          │
│  budget_min           │ DECIMAL(10,2) │                          │
│  budget_max           │ DECIMAL(10,2) │                          │
│  status               │ VARCHAR(20)   │ pending/accepted/done    │
│  accepted_at          │ TIMESTAMP     │                          │
│  completed_at         │ TIMESTAMP     │                          │
│  created_at           │ TIMESTAMP     │                          │
│  updated_at           │ TIMESTAMP     │                          │
└──────────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────┐
│                    PROPOSALS                         │
│            (Propostas/Orçamentos)                    │
├──────────────────────────────────────────────────────┤
│  id               │ UUID          │ PK               │
│  order_id         │ UUID          │ FK service_order │
│  partner_id       │ UUID          │ FK → users.id    │
│  amount           │ DECIMAL(10,2) │ NOT NULL         │
│  message          │ TEXT          │                  │
│  estimated_time   │ VARCHAR(100)  │ "2-3 dias"       │
│  status           │ VARCHAR(20)   │ pending/accepted │
│  created_at       │ TIMESTAMP     │                  │
└──────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│                        PURCHASE_ORDERS                                     │
│            (Pedidos de Produtos - Sistema de Escrow)                       │
├────────────────────────────────────────────────────────────────────────────┤
│  id                            │ UUID          │ PK                        │
│  order_number                  │ VARCHAR(50)   │ UNIQUE                    │
│  client_id                     │ UUID          │ FK → users.id             │
│  seller_id                     │ UUID          │ FK → users.id             │
│  total                         │ DECIMAL(10,2) │ NOT NULL                  │
│  subtotal                      │ DECIMAL(10,2) │                           │
│  delivery_fee                  │ DECIMAL(10,2) │                           │
│  platform_fee                  │ DECIMAL(10,2) │ 5% do total               │
│  escrow_amount                 │ DECIMAL(10,2) │ $ retido até confirmação  │
│  escrow_released_at            │ TIMESTAMP     │ Quando liberado           │
│  escrow_released_to            │ UUID          │ FK → users.id (seller)    │
│  status                        │ VARCHAR(20)   │ pending/paid/shipped/done │
│  payment_method                │ VARCHAR(50)   │ pix/credit/debit          │
│  payment_status                │ VARCHAR(20)   │                           │
│  stripe_payment_intent_id      │ VARCHAR(255)  │ Stripe ID                 │
│  delivery_address              │ TEXT          │                           │
│  delivery_city_id              │ INTEGER       │ FK → cities.id            │
│  tracking_code                 │ VARCHAR(100)  │ Correios                  │
│  courier                       │ VARCHAR(50)   │ Correios/Sedex            │
│  shipped_at                    │ TIMESTAMP     │                           │
│  shipped_confirmed_by_seller   │ BOOLEAN       │ DEFAULT false             │
│  delivered_at                  │ TIMESTAMP     │                           │
│  delivered_confirmed_by_client │ BOOLEAN       │ DEFAULT false             │
│  delivered_confirmed_by_seller │ BOOLEAN       │ DEFAULT false             │
│  created_at                    │ TIMESTAMP     │                           │
│  updated_at                    │ TIMESTAMP     │                           │
└────────────────────────────────────────────────────────────────────────────┘
        │
        ├──────────────────┬────────────────────────────────┐
        ▼                  ▼                                ▼
┌──────────────────────┐  ┌─────────────────────────┐  ┌──────────────────────────┐
│   ORDER_ITEMS        │  │ ORDER_TRACKING_EVENTS   │  │   CONVERSATIONS          │
├──────────────────────┤  ├─────────────────────────┤  │    (Chat do Pedido)      │
│ • id       BIGSERIAL │  │ • id       BIGSERIAL    │  ├──────────────────────────┤
│ • order_id UUID  FK  │  │ • order_id UUID     FK  │  │ • id          UUID   PK  │
│ • product_id UUID FK │  │ • status   VARCHAR(50)  │  │ • type        VARCHAR    │
│ • product_name       │  │ • description TEXT      │  │ • order_id    UUID   FK  │
│ • product_image_url  │  │ • location  TEXT        │  └──────────────────────────┘
│ • quantity  INTEGER  │  │ • event_date TIMESTAMP  │
│ • unit_price DECIMAL │  │ • created_at TIMESTAMP  │
│ • total_price DECIMAL│  └─────────────────────────┘
└──────────────────────┘

┌───────────────────────────────────────────────────────────────────┐
│                       BANK_ACCOUNTS                               │
│               (Contas Bancárias - PRIVADAS)                       │
│                    Apenas para PARCEIROS                          │
├───────────────────────────────────────────────────────────────────┤
│  id                    │ UUID         │ PK                       │
│  user_id               │ UUID         │ FK → users.id            │
│  account_holder_name   │ VARCHAR(255) │ NOT NULL                 │
│  bank_code             │ VARCHAR(10)  │ 001=BB, 341=Itau         │
│  bank_name             │ VARCHAR(100) │ Banco do Brasil          │
│  account_type          │ VARCHAR(20)  │ checking/savings/payment │
│  agency                │ VARCHAR(10)  │ Agência                  │
│  account_number        │ VARCHAR(20)  │ Conta                    │
│  account_digit         │ VARCHAR(5)   │ Dígito                   │
│  cpf_cnpj              │ VARCHAR(14)  │ NOT NULL                 │
│  pix_key               │ VARCHAR(255) │ Chave PIX                │
│  pix_key_type          │ VARCHAR(20)  │ cpf/cnpj/email/phone     │
│  is_primary            │ BOOLEAN      │ DEFAULT false            │
│  verified              │ BOOLEAN      │ DEFAULT false            │
│  created_at            │ TIMESTAMP    │                          │
│  updated_at            │ TIMESTAMP    │                          │
└───────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   CONVERSATIONS                             │
│        (Conversas entre Usuários e Suporte IA)              │
├─────────────────────────────────────────────────────────────┤
│  id                   │ UUID       │ PK                     │
│  type                 │ VARCHAR    │ user_user/order_chat   │
│                       │            │ service_chat/support_ai│
│  purchase_order_id    │ UUID       │ FK (se for chat pedido)│
│  service_order_id     │ UUID       │ FK (se for chat serviço│
│  created_at           │ TIMESTAMP  │                        │
│  updated_at           │ TIMESTAMP  │                        │
└─────────────────────────────────────────────────────────────┘
        │
        ├────────────────────┬─────────────────────────┐
        ▼                    ▼                         │
┌────────────────────────┐  ┌──────────────────────────────────┐
│ CONVERSATION_         │  │        MESSAGES                  │
│   PARTICIPANTS        │  ├──────────────────────────────────┤
├────────────────────────┤  │ • id             UUID       PK   │
│ • conversation_id UUID │  │ • conversation_id UUID      FK   │
│ • user_id        UUID  │  │ • sender_id       UUID      FK   │
│ • joined_at   TIMESTAMP│  │ • content         TEXT           │
│ • last_read   TIMESTAMP│  │ • media_url       TEXT           │
└────────────────────────┘  │ • media_type      VARCHAR(20)    │
                            │ • read_by         UUID[]         │
                            │ • created_at      TIMESTAMP      │
                            └──────────────────────────────────┘

┌───────────────────────────────────────────────────┐
│           AI_CONVERSATIONS                        │
│         (Suporte IA - ChatGPT)                    │
├───────────────────────────────────────────────────┤
│  id          │ UUID        │ PK                  │
│  user_id     │ UUID        │ FK → users.id       │
│  title       │ VARCHAR(255)│                     │
│  model       │ VARCHAR(50) │ gpt-4/gemini        │
│  created_at  │ TIMESTAMP   │                     │
│  updated_at  │ TIMESTAMP   │                     │
└───────────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────┐
│            AI_MESSAGES                            │
├───────────────────────────────────────────────────┤
│  id                │ UUID       │ PK              │
│  conversation_id   │ UUID       │ FK              │
│  role              │ VARCHAR    │ user/assistant  │
│  content           │ TEXT       │ NOT NULL        │
│  created_at        │ TIMESTAMP  │                 │
└───────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────┐
│               CATEGORIES                              │
│        (Categorias de Serviços/Produtos)              │
├───────────────────────────────────────────────────────┤
│  id        │ INTEGER      │ PK                       │
│  name      │ VARCHAR(100) │ UNIQUE (Pintura, etc)    │
│  type      │ VARCHAR(20)  │ service / product        │
│  icon_url  │ TEXT         │ Firebase Storage         │
│  created_at│ TIMESTAMP    │                          │
└───────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────┐
│       USER_PREFERRED_CATEGORIES                      │
│    (Categorias que o PARCEIRO oferece)               │
├──────────────────────────────────────────────────────┤
│  user_id      │ UUID    │ FK → users.id (partner)   │
│  category_id  │ INTEGER │ FK → categories.id        │
│  created_at   │ TIMESTAMP│                           │
│  PRIMARY KEY (user_id, category_id)                  │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│                NOTIFICATIONS                         │
│          (Push e In-App)                             │
├──────────────────────────────────────────────────────┤
│  id          │ UUID        │ PK                     │
│  user_id     │ UUID        │ FK → users.id          │
│  type        │ VARCHAR(50) │ new_order/shipped/etc  │
│  title       │ VARCHAR(255)│ NOT NULL               │
│  message     │ TEXT        │ NOT NULL               │
│  data        │ JSONB       │ Extra data (orderId)   │
│  read        │ BOOLEAN     │ DEFAULT false          │
│  read_at     │ TIMESTAMP   │                        │
│  created_at  │ TIMESTAMP   │                        │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│                   REVIEWS                            │
│        (Avaliações de Produtos/Parceiros)            │
├──────────────────────────────────────────────────────┤
│  id            │ UUID        │ PK                   │
│  reviewer_id   │ UUID        │ FK → users.id        │
│  target_id     │ UUID        │ product/service/user │
│  target_type   │ VARCHAR(20) │ product/service/part │
│  rating        │ INTEGER     │ 1-5 stars            │
│  comment       │ TEXT        │                      │
│  created_at    │ TIMESTAMP   │                      │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                   USER_SETTINGS                              │
│              (Configurações do Usuário)                      │
├──────────────────────────────────────────────────────────────┤
│  user_id              │ UUID    │ PK FK → users.id         │
│  push_enabled         │ BOOLEAN │ DEFAULT true             │
│  promo_enabled        │ BOOLEAN │ DEFAULT true             │
│  sound_enabled        │ BOOLEAN │ DEFAULT true             │
│  email_enabled        │ BOOLEAN │ DEFAULT false            │
│  sms_enabled          │ BOOLEAN │ DEFAULT false            │
│  location_sharing     │ BOOLEAN │ DEFAULT true             │
│  profile_visible      │ BOOLEAN │ DEFAULT true             │
│  contact_info_sharing │ BOOLEAN │ DEFAULT false            │
│  analytics            │ BOOLEAN │ DEFAULT true             │
│  biometric_enabled    │ BOOLEAN │ DEFAULT false            │
│  two_factor_enabled   │ BOOLEAN │ DEFAULT false            │
│  two_factor_method    │ VARCHAR │ sms/email/app            │
│  updated_at           │ TIMESTAMP│                          │
└──────────────────────────────────────────────────────────────┘
```

---

## RELACIONAMENTOS PRINCIPAIS

```
USERS (1) ─────── (N) PRODUCTS      (Parceiro cria produtos)
USERS (1) ─────── (N) POSTS         (Parceiro cria posts)
USERS (1) ─────── (N) STORIES       (Parceiro cria stories)
USERS (1) ─────── (N) SERVICE_ORDERS (Cliente cria, Parceiro aceita)
USERS (1) ─────── (N) USER_LOCATIONS (Histórico GPS)
USERS (1) ─────── (N) BANK_ACCOUNTS (Parceiro tem contas)
USERS (N) ─────── (N) CATEGORIES    (Parceiro escolhe categorias)

CITIES (1) ────── (N) PRODUCTS      (Produto pertence à cidade onde foi criado)
CITIES (1) ────── (N) POSTS         (Post pertence à cidade onde foi criado)
CITIES (1) ────── (N) STORIES       (Story pertence à cidade onde foi criada)
CITIES (1) ────── (N) SERVICE_ORDERS (Ordem pertence à cidade onde foi criada)

PURCHASE_ORDERS (1) ── (N) ORDER_ITEMS          (Itens do pedido)
PURCHASE_ORDERS (1) ── (N) ORDER_TRACKING_EVENTS (Eventos de rastreamento)
PURCHASE_ORDERS (1) ── (1) CONVERSATIONS        (Chat do pedido)

SERVICE_ORDERS (1) ─── (N) PROPOSALS            (Propostas recebidas)
SERVICE_ORDERS (1) ─── (1) CONVERSATIONS        (Chat do serviço)
```

