# DIAGRAMAS DE FLUXO - POSTGRESQL COM GPS DINÂMICO

## FLUXO 1: Parceiro Cria Produto

```
┌─────────────┐       ┌─────────────┐       ┌──────────┐       ┌──────────────┐
│     App     │       │   Backend   │       │   GPS    │       │  PostgreSQL  │
└──────┬──────┘       └──────┬──────┘       └────┬─────┘       └──────┬───────┘
       │                     │                   │                     │
       │  Obter localização  │                   │                     │
       │────────────────────────────────────────>│                     │
       │                     │                   │                     │
       │    lat: -24.7       │                   │                     │
       │    lng: -53.7       │                   │                     │
       │<────────────────────────────────────────│                     │
       │                     │                   │                     │
       │                     │                   │                     │
       │  POST /api/products/create              │                     │
       │  {title, price, lat, lng}               │                     │
       │────────────────────>│                   │                     │
       │                     │                   │                     │
       │                ┌────┴─────────────────────────────┐           │
       │                │ Geocode reverso                  │           │
       │                │ lat/lng → Cascavel, PR           │           │
       │                └────┬─────────────────────────────┘           │
       │                     │                                         │
       │                     │  INSERT INTO products                   │
       │                     │  (seller_id, created_in_city_id=123)    │
       │                     │────────────────────────────────────────>│
       │                     │                                         │
       │                     │                  ┌──────────────────────┴──────┐
       │                     │                  │ Produto vinculado à cidade  │
       │                     │                  │ Cascavel (id=123)           │
       │                     │                  └──────────────────────┬──────┘
       │                     │                                         │
       │                     │          Product created                │
       │                     │<────────────────────────────────────────│
       │                     │                                         │
       │      Success        │                                         │
       │<────────────────────│                                         │
       │                     │                                         │
```

---

## FLUXO 2: Cliente Vê Produtos (GPS Dinâmico)

```
┌─────────────┐       ┌─────────────┐       ┌──────────┐       ┌──────────────┐
│     App     │       │   Backend   │       │   GPS    │       │  PostgreSQL  │
│   Cliente   │       │             │       │          │       │              │
└──────┬──────┘       └──────┬──────┘       └────┬─────┘       └──────┬───────┘
       │                     │                   │                     │
       │  Obter localização  │                   │                     │
       │────────────────────────────────────────>│                     │
       │                     │                   │                     │
       │    lat: -24.7, lng: -53.7               │                     │
       │<────────────────────────────────────────│                     │
       │                     │                   │                     │
       │                     │                   │                     │
       │  GET /api/products?lat=-24.7&lng=-53.7  │                     │
       │────────────────────>│                   │                     │
       │                     │                   │                     │
       │                ┌────┴──────────────┐    │                     │
       │                │ Geocode: Cascavel │    │                     │
       │                │ city_id = 123     │    │                     │
       │                └────┬──────────────┘    │                     │
       │                     │                   │                     │
       │                     │  UPDATE user_locations                  │
       │                     │  SET city_id=123, is_current=true       │
       │                     │────────────────────────────────────────>│
       │                     │                                         │
       │                     │  SELECT * FROM products                 │
       │                     │  WHERE created_in_city_id=123           │
       │                     │────────────────────────────────────────>│
       │                     │                                         │
       │                     │          [10 produtos]                  │
       │                     │<────────────────────────────────────────│
       │                     │                                         │
       │  Produtos de Cascavel                                         │
       │<────────────────────│                                         │
       │                     │                                         │
       │                     │                                         │
       │ [30 segundos depois - usuário viajou para Foz do Iguaçu]      │
       │                     │                                         │
       │  GET /api/products?lat=-25.5&lng=-54.5  │                     │
       │────────────────────>│                   │                     │
       │                ┌────┴────────────────┐  │                     │
       │                │ Geocode: Foz Iguaçu │  │                     │
       │                │ city_id = 456       │  │                     │
       │                └────┬────────────────┘  │                     │
       │                     │                   │                     │
       │                     │  SELECT * FROM products                 │
       │                     │  WHERE created_in_city_id=456           │
       │                     │────────────────────────────────────────>│
       │                     │                                         │
       │                     │          [8 produtos de Foz]            │
       │                     │<────────────────────────────────────────│
       │                     │                                         │
       │  Produtos de Foz    │                                         │
       │<────────────────────│                                         │
```

---

## FLUXO 3: Cliente Cria Ordem → Notifica Parceiros

```
┌──────────┐  ┌─────────┐  ┌──────────────┐  ┌───────────┐  ┌──────────┐
│   App    │  │   GPS   │  │   Backend    │  │PostgreSQL │  │App Parceiro│
│ Cliente  │  │         │  │              │  │           │  │            │
└────┬─────┘  └────┬────┘  └──────┬───────┘  └─────┬─────┘  └─────┬──────┘
     │             │              │                │              │
     │ Obter GPS   │              │                │              │
     │────────────>│              │                │              │
     │             │              │                │              │
     │ lat/lng     │              │                │              │
     │<────────────│              │                │              │
     │             │              │                │              │
     │ POST /api/service-orders   │                │              │
     │ {category: pintura, lat, lng}              │              │
     │───────────────────────────>│                │              │
     │             │              │                │              │
     │             │         ┌────┴────────────┐   │              │
     │             │         │ city_id = 123   │   │              │
     │             │         │ (Cascavel)      │   │              │
     │             │         └────┬────────────┘   │              │
     │             │              │                │              │
     │             │              │ INSERT service_orders         │
     │             │              │ (city_id=123, category=pintura)│
     │             │              │───────────────>│              │
     │             │              │                │              │
     │             │         ┌────┴─────────────────────────────┐│
     │             │         │ SELECT parceiros WHERE           ││
     │             │         │ city_id=123 AND is_current=true  ││
     │             │         │ AND category=pintura             ││
     │             │         └────┬─────────────────────────────┘│
     │             │              │                │              │
     │             │              │  [3 partners]  │              │
     │             │              │<───────────────│              │
     │             │              │                │              │
     │             │              │ INSERT notifications (3x)     │
     │             │              │───────────────>│              │
     │             │              │                │              │
     │             │              │ WebSocket emit(order_created) │
     │             │              │──────────────────────────────────>│
     │             │              │                │              │
     │             │              │ FCM Push (3x)  │              │
     │             │              │──────────────────────────────────>│
     │             │              │                │              │
     │   Success   │              │                │   Push: Nova ordem!
     │<────────────────────────────│                │   Categoria: pintura
     │             │              │                │<──────────────│
```

---

## FLUXO 4: Compra de Produto - Escrow Completo

```
┌──────────┐  ┌─────────┐  ┌────────┐  ┌──────────────┐  ┌──────────┐
│   App    │  │ Backend │  │ Stripe │  │  PostgreSQL  │  │   App    │
│ Cliente  │  │         │  │  API   │  │              │  │ Vendedor │
└────┬─────┘  └────┬────┘  └───┬────┘  └──────┬───────┘  └────┬─────┘
     │             │            │              │              │
     │ POST /api/purchases      │              │              │
     │ {products, payment}      │              │              │
     │────────────>│            │              │              │
     │             │            │              │              │
     │        ┌────┴───────────────────┐       │              │
     │        │ Calcular:              │       │              │
     │        │ total = 100.00         │       │              │
     │        │ platformFee = 5.00(5%) │       │              │
     │        │ escrow = 95.00         │       │              │
     │        └────┬───────────────────┘       │              │
     │             │                           │              │
     │             │ Create Payment Intent     │              │
     │             │ (captureMethod=manual)    │              │
     │             │──────────>│              │              │
     │             │            │              │              │
     │             │  payment_intent_id        │              │
     │             │<───────────│              │              │
     │             │            │              │              │
     │             │  INSERT purchase_orders   │              │
     │             │  (escrow_amount=95, status=pending)       │
     │             │────────────────────────────>│              │
     │             │            │              │              │
     │  {clientSecret}          │              │              │
     │<────────────│            │              │              │
     │             │            │              │              │
     │ confirmPayment(clientSecret)            │              │
     │─────────────────────────>│              │              │
     │             │            │              │              │
     │             │ Webhook: payment_succeeded│              │
     │             │<───────────│              │              │
     │             │            │              │              │
     │             │  UPDATE status=paid       │              │
     │             │────────────────────────────>│              │
     │             │            │              │              │
     │             │  WebSocket + Push: Novo pedido!          │
     │             │─────────────────────────────────────────>│
     │             │            │              │              │
     │             │            │              │  POST /api/purchases/:id/ship
     │             │            │              │  {tracking_code}
     │             │<─────────────────────────────────────────│
     │             │            │              │              │
     │             │  UPDATE shipped_at, tracking_code        │
     │             │────────────────────────────>│              │
     │             │            │              │              │
     │  Push: Pedido enviado!  │              │              │
     │  Código: BR123456        │              │              │
     │<────────────│            │              │              │
     │             │            │              │              │
     │ POST /api/purchases/:id/confirm-delivery │              │
     │────────────>│            │              │              │
     │             │            │              │              │
     │             │  UPDATE delivered_confirmed_by_client=true│
     │             │────────────────────────────>│              │
     │             │            │              │              │
     │        ┌────┴────────────────────────┐   │              │
     │        │ Ambos confirmaram?          │   │              │
     │        │ SIM → Liberar escrow!       │   │              │
     │        └────┬────────────────────────┘   │              │
     │             │            │              │              │
     │             │ Capture Payment (95.00)   │              │
     │             │──────────>│              │              │
     │             │            │              │              │
     │             │  Transfer to seller       │              │
     │             │<───────────│              │              │
     │             │            │              │              │
     │             │  UPDATE escrow_released_at│              │
     │             │────────────────────────────>│              │
     │             │            │              │              │
     │             │  Push: Pagamento liberado R$ 95,00        │
     │             │─────────────────────────────────────────>│
```

---

## FLUXO 5: Chat do Pedido (WebSocket Tempo Real)

```
┌──────────┐       ┌─────────────┐       ┌──────────────┐       ┌──────────┐
│   App    │       │   Backend   │       │  PostgreSQL  │       │   App    │
│ Cliente  │       │  WebSocket  │       │              │       │ Vendedor │
└────┬─────┘       └──────┬──────┘       └──────┬───────┘       └────┬─────┘
     │                    │                     │                    │
     │ GET /api/purchases/:id/chat              │                    │
     │───────────────────>│                     │                    │
     │                    │                     │                    │
     │                    │ SELECT/CREATE conversation               │
     │                    │ (type=order_chat)   │                    │
     │                    │────────────────────>│                    │
     │                    │                     │                    │
     │                    │  conversation_id    │                    │
     │                    │<────────────────────│                    │
     │                    │                     │                    │
     │ {conversationId}   │                     │                    │
     │<───────────────────│                     │                    │
     │                    │                     │                    │
     │ WebSocket.connect(conversationId)        │                    │
     │───────────────────>│                     │                    │
     │                    │                     │                    │
     │                    │                     │  WebSocket.connect(conversationId)
     │                    │<────────────────────────────────────────────│
     │                    │                     │                    │
     │           ┌────────┴──────────┐          │                    │
     │           │ Ambos conectados  │          │                    │
     │           │ na sala            │          │                    │
     │           └────────┬──────────┘          │                    │
     │                    │                     │                    │
     │ send_message       │                     │                    │
     │ "Quando chega?"    │                     │                    │
     │───────────────────>│                     │                    │
     │                    │                     │                    │
     │                    │ INSERT INTO messages│                    │
     │                    │ (sender=client)     │                    │
     │                    │────────────────────>│                    │
     │                    │                     │                    │
     │                    │ emit: new_message   │                    │
     │                    │────────────────────────────────────────>│
     │                    │                     │                    │
     │                    │                     │  "Quando chega?"   │
     │                    │                     │  Mensagem recebida │
     │                    │                     │                    │
     │                    │         send_message: "Chega em 3 dias"  │
     │                    │<────────────────────────────────────────────│
     │                    │                     │                    │
     │                    │ INSERT messages     │                    │
     │                    │────────────────────>│                    │
     │                    │                     │                    │
     │ emit: new_message  │                     │                    │
     │<───────────────────│                     │                    │
     │                    │                     │                    │
     │ "Chega em 3 dias"  │                     │                    │
     │ Mensagem exibida   │                     │                    │
```

---

## FLUXO 6: Parceiro Muda de Cidade (Mobilidade)

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│     App      │      │   Backend    │      │  PostgreSQL  │
│   Parceiro   │      │              │      │              │
└──────┬───────┘      └──────┬───────┘      └──────┬───────┘
       │                     │                     │
       │                     │                     │
       │ ┌───────────────────────────────────────┐ │
       │ │ Parceiro estava em Cascavel (id=123)  │ │
       │ │ Viaja para Foz do Iguaçu              │ │
       │ └───────────────────────────────────────┘ │
       │                     │                     │
       │  GPS Update         │                     │
       │  lat: -25.5 (nova)  │                     │
       │  lng: -54.5 (nova)  │                     │
       │────────────────────>│                     │
       │                     │                     │
       │                ┌────┴──────────────┐     │
       │                │ Geocode: Foz      │     │
       │                │ city_id = 456     │     │
       │                │ (MUDOU!)          │     │
       │                └────┬──────────────┘     │
       │                     │                     │
       │                     │ UPDATE user_locations            │
       │                     │ SET is_current=false, exited_at=NOW()
       │                     │ WHERE user_id=X AND city_id=123  │
       │                     │────────────────────>│
       │                     │                     │
       │                     │ INSERT user_locations            │
       │                     │ (user_id, city_id=456, is_current=true)
       │                     │────────────────────>│
       │                     │                     │
       │                     │ UPDATE users        │
       │                     │ SET current_city_id=456            │
       │                     │────────────────────>│
       │                     │                     │
       │                ┌────┴────────────────────────────────┐│
       │                │ IMPORTANTE:                         ││
       │                │ • Produtos antigos em Cascavel      ││
       │                │   continuam em Cascavel (id=123)    ││
       │                │ • Novos produtos serão criados      ││
       │                │   em Foz do Iguaçu (id=456)         ││
       │                └────┬────────────────────────────────┘│
       │                     │                     │
       │                     │ SELECT pending orders           │
       │                     │ WHERE city_id=456 AND           │
       │                     │ category IN [pintura, eletrica] │
       │                     │────────────────────>│
       │                     │                     │
       │                     │ [2 new orders in Foz]            │
       │                     │<────────────────────│
       │                     │                     │
       │     Push: 2 novas ordens em Foz do Iguaçu!            │
       │<────────────────────│                     │
```

---

## FLUXO 7: Rastreamento de Pedido

```
┌──────────┐  ┌─────────┐  ┌──────────────┐  ┌──────────┐  ┌──────────┐
│   App    │  │ Backend │  │ Correios API │  │PostgreSQL│  │   App    │
│ Cliente  │  │ Tracking│  │              │  │          │  │ Vendedor │
└────┬─────┘  └────┬────┘  └──────┬───────┘  └────┬─────┘  └────┬─────┘
     │             │              │              │              │
     │ GET /api/purchases/:id/tracking            │              │
     │────────────>│              │              │              │
     │             │              │              │              │
     │             │ SELECT order + tracking_events            │
     │             │────────────────────────────>│              │
     │             │              │              │              │
     │             │          Events[]           │              │
     │             │<────────────────────────────│              │
     │             │              │              │              │
     │ Timeline    │              │              │              │
     │ exibida     │              │              │              │
     │<────────────│              │              │              │
     │             │              │              │              │
     │ ┌───────────────────────────────────────┐ │              │
     │ │ Background Job - A cada 1 hora        │ │              │
     │ └───────────────────────────────────────┘ │              │
     │             │              │              │              │
     │             │ GET /rastreamento/BR123456 │              │
     │             │─────────────>│              │              │
     │             │              │              │              │
     │             │   Events[]   │              │              │
     │             │<─────────────│              │              │
     │             │              │              │              │
     │             │ INSERT order_tracking_events (novos)      │
     │             │────────────────────────────>│              │
     │             │              │              │              │
     │             │ WebSocket: emit(tracking_update)          │
     │             │────────────────────────────────────────────│
     │             │──────────>   │              │              │
     │             │              │              │              │
     │ Novo evento!│              │              │   Novo evento!
     │ "Em trânsito"              │              │   "Em trânsito"
     │<────────────│              │              │<─────────────│
```

---

## FLUXO 8: Suporte IA (ChatGPT)

```
┌──────────┐       ┌─────────────┐       ┌──────────┐       ┌──────────────┐
│   App    │       │   Backend   │       │ OpenAI   │       │  PostgreSQL  │
└────┬─────┘       └──────┬──────┘       │   API    │       └──────┬───────┘
     │                    │              └────┬─────┘              │
     │ Iniciar conversa   │                   │                    │
     │───────────────────>│                   │                    │
     │                    │                   │                    │
     │                    │ INSERT ai_conversations                │
     │                    │───────────────────────────────────────>│
     │                    │                   │                    │
     │ conversationId     │                   │                    │
     │<───────────────────│                   │                    │
     │                    │                   │                    │
     │ send_message       │                   │                    │
     │ "Como rastrear?"   │                   │                    │
     │───────────────────>│                   │                    │
     │                    │                   │                    │
     │                    │ INSERT ai_messages (user, content)      │
     │                    │───────────────────────────────────────>│
     │                    │                   │                    │
     │                    │ SELECT conversation history             │
     │                    │───────────────────────────────────────>│
     │                    │                   │                    │
     │                    │    messages[]     │                    │
     │                    │<───────────────────────────────────────│
     │                    │                   │                    │
     │                    │ POST /v1/chat/completions              │
     │                    │ {messages[]}      │                    │
     │                    │──────────────────>│                    │
     │                    │                   │                    │
     │                    │                   │                    │
     │                    │         ┌─────────┴───────────┐        │
     │                    │         │ GPT-4 processa      │        │
     │                    │         │ Contexto do TaskGo  │        │
     │                    │         └─────────┬───────────┘        │
     │                    │                   │                    │
     │                    │  AI Response      │                    │
     │                    │<──────────────────│                    │
     │                    │                   │                    │
     │                    │ INSERT ai_messages (assistant, response)│
     │                    │───────────────────────────────────────>│
     │                    │                   │                    │
     │ AI Response        │                   │                    │
     │ "Acesse Meus       │                   │                    │
     │  Pedidos..."       │                   │                    │
     │<───────────────────│                   │                    │
```

---

## FLUXO 9: Verificação Facial com Firebase ML Kit

```
┌──────────┐  ┌─────────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐
│   App    │  │   Backend   │  │ Firebase │  │ Firebase │  │  PostgreSQL  │
│ Parceiro │  │             │  │  Storage │  │  ML Kit  │  │              │
└────┬─────┘  └──────┬──────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘
     │               │              │            │              │
     │ Capturar doc  │              │            │              │
     │ frente/verso  │              │            │              │
     │ + selfie      │              │            │              │
     │               │              │            │              │
     │ Upload images (3x)           │            │              │
     │─────────────────────────────>│            │              │
     │               │              │            │              │
     │  URLs[]       │              │            │              │
     │<─────────────────────────────│            │              │
     │               │              │            │              │
     │ POST /api/verification/submit              │              │
     │ {documentUrls[]}             │            │              │
     │──────────────>│              │            │              │
     │               │              │            │              │
     │          ┌────┴─────────────────────────┐ │              │
     │          │ Firebase ML Kit              │ │              │
     │          │ Face Detection API           │ │              │
     │          └────┬─────────────────────────┘ │              │
     │               │              │            │              │
     │               │ Detect faces in selfie    │              │
     │               │─────────────────────────────>│              │
     │               │              │            │              │
     │               │      Face detected: true  │              │
     │               │<─────────────────────────────│              │
     │               │              │            │              │
     │               │ Text Recognition (OCR)    │              │
     │               │ Extract: CPF, nome, data  │              │
     │               │─────────────────────────────>│              │
     │               │              │            │              │
     │               │      Extracted data       │              │
     │               │<─────────────────────────────│              │
     │               │              │            │              │
     │               │ UPDATE users SET           │              │
     │               │ document_urls, verified=false (pending)   │
     │               │──────────────────────────────────────────>│
     │               │              │            │              │
     │ Status: Aguardando aprovação do admin     │              │
     │<──────────────│              │            │              │
```

---

## FLUXO 10: Parceiro Adiciona Conta Bancária

```
┌──────────┐            ┌─────────────┐            ┌──────────────┐
│   App    │            │   Backend   │            │  PostgreSQL  │
│ Parceiro │            │             │            │   (PRIVADO)  │
└────┬─────┘            └──────┬──────┘            └──────┬───────┘
     │                         │                          │
     │ POST /api/bank-accounts │                          │
     │ {accountHolder, bank,   │                          │
     │  agency, account, pix}  │                          │
     │────────────────────────>│                          │
     │                         │                          │
     │                    ┌────┴─────────────────────┐    │
     │                    │ Validar:                 │    │
     │                    │ • user.role = partner    │    │
     │                    │ • CPF/CNPJ válido        │    │
     │                    │ • Dados bancários OK     │    │
     │                    └────┬─────────────────────┘    │
     │                         │                          │
     │                         │ INSERT INTO bank_accounts│
     │                         │ (user_id, account_data,  │
     │                         │  is_primary=false)       │
     │                         │─────────────────────────>│
     │                         │                          │
     │                         │    Account created       │
     │                         │<─────────────────────────│
     │                         │                          │
     │      Success            │                          │
     │<────────────────────────│                          │
     │                         │                          │
     │                         │                          │
     │ PUT /api/bank-accounts/:id/set-primary             │
     │────────────────────────>│                          │
     │                         │                          │
     │                         │ UPDATE bank_accounts     │
     │                         │ SET is_primary=false     │
     │                         │ WHERE user_id=X          │
     │                         │─────────────────────────>│
     │                         │                          │
     │                         │ UPDATE bank_accounts     │
     │                         │ SET is_primary=true      │
     │                         │ WHERE id=accountId       │
     │                         │─────────────────────────>│
     │                         │                          │
     │ Conta principal definida│                          │
     │<────────────────────────│                          │
```

---

## FLUXO 11: Atualização GPS Automática (Background)

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│     App      │      │   Backend    │      │  PostgreSQL  │
│ (Background) │      │              │      │              │
└──────┬───────┘      └──────┬───────┘      └──────┬───────┘
       │                     │                      │
       │ ┌─────────────────────────────────────┐   │
       │ │ Loop: A cada 5 minutos              │   │
       │ └─────────────────────────────────────┘   │
       │                     │                      │
       │  getCurrentLocation()                      │
       │  lat: -24.7055     │                      │
       │  lng: -53.7520     │                      │
       │                     │                      │
       │ POST /api/locations/update                 │
       │ {lat, lng, accuracy}│                      │
       │────────────────────>│                      │
       │                     │                      │
       │                ┌────┴─────────────────┐    │
       │                │ getCityFromGPS()     │    │
       │                │ Haversine formula    │    │
       │                └────┬─────────────────┘    │
       │                     │                      │
       │                     │ SELECT nearest city  │
       │                     │ WHERE distance < 50km│
       │                     │─────────────────────>│
       │                     │                      │
       │                     │ city_id=123 (Cascavel)│
       │                     │<─────────────────────│
       │                     │                      │
       │                     │ INSERT user_locations│
       │                     │ UPDATE users         │
       │                     │─────────────────────>│
       │                     │                      │
       │      Location updated                     │
       │<────────────────────│                      │
       │                     │                      │
       │ ┌─────────────────────────────────────┐   │
       │ │ Aguardar 5 minutos                  │   │
       │ │ Repetir                             │   │
       │ └─────────────────────────────────────┘   │
```

---

## ARQUITETURA DE MÓDULOS (NestJS Backend)

```
┌──────────────────────────────────────────────────────────────────┐
│                        NESTJS BACKEND                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────┐  ┌────────────┐  ┌─────────────┐              │
│  │   Auth     │  │ Locations  │  │  Products   │              │
│  │  Module    │  │   Module   │  │   Module    │              │
│  └─────┬──────┘  └─────┬──────┘  └──────┬──────┘              │
│        │               │                 │                      │
│        │               │                 │                      │
│  ┌─────▼──────┐  ┌────▼──────┐  ┌───────▼─────┐               │
│  │ Firebase   │  │ Geocoding │  │   Prisma    │               │
│  │   Auth     │  │  Service  │  │    ORM      │               │
│  │  Verify    │  │  (Havers) │  │             │               │
│  └────────────┘  └───────────┘  └──────┬──────┘               │
│                                         │                       │
│                                         ▼                       │
│                                  ┌─────────────┐                │
│                                  │ PostgreSQL  │                │
│                                  │  Database   │                │
│                                  └─────────────┘                │
│                                                                  │
│  ┌────────────┐  ┌────────────┐  ┌─────────────┐              │
│  │  Orders    │  │ Purchases  │  │  Messages   │              │
│  │  Module    │  │   Module   │  │   Module    │              │
│  └─────┬──────┘  └─────┬──────┘  └──────┬──────┘              │
│        │               │                 │                      │
│        ▼               ▼                 ▼                      │
│  ┌─────────────────────────────────────────────┐               │
│  │         WebSocket Gateway                   │               │
│  │  (Notificações + Chat em Tempo Real)        │               │
│  └─────────────────────────────────────────────┘               │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```
