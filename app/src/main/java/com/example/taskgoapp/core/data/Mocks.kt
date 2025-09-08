package com.example.taskgoapp.core.data

import com.example.taskgoapp.R

// Mock Users
val mockUsers = listOf(
    User(
        id = 1,
        name = "Carlos Amaral",
        email = "carlos@email.com",
        phone = "(11) 99999-9999",
        city = "São Paulo",
        accountType = AccountType.CLIENT,
        timeOnAppYears = 2,
        rating = 4.8f
    ),
    User(
        id = 2,
        name = "Maria Santos",
        email = "maria@email.com",
        phone = "(11) 88888-8888",
        city = "São Paulo",
        accountType = AccountType.CLIENT,
        timeOnAppYears = 1,
        rating = 4.5f
    ),
    User(
        id = 3,
        name = "João Silva",
        email = "joao@email.com",
        phone = "(11) 77777-7777",
        city = "São Paulo",
        accountType = AccountType.PROVIDER,
        timeOnAppYears = 3,
        rating = 4.9f
    )
)

// Mock Service Categories
val mockServiceCategories = listOf(
    ServiceCategory(id = 1, name = "Montagem"),
    ServiceCategory(id = 2, name = "Reforma"),
    ServiceCategory(id = 3, name = "Jardinagem"),
    ServiceCategory(id = 4, name = "Elétrica"),
    ServiceCategory(id = 5, name = "Limpeza")
)

// Mock Providers
val mockProviders = listOf(
    Provider(
        id = 1,
        name = "Rodrigo Silva",
        role = "Montador de Móveis",
        rating = 4.8f,
        reviewsCount = 127,
        services = listOf("Montagem de guarda-roupas", "Montagem de camas", "Montagem de estantes"),
        city = "São Paulo"
    ),
    Provider(
        id = 2,
        name = "Ana Costa",
        role = "Jardinheira",
        rating = 4.7f,
        reviewsCount = 89,
        services = listOf("Jardinagem", "Poda de árvores", "Manutenção de jardins"),
        city = "São Paulo"
    ),
    Provider(
        id = 3,
        name = "Pedro Santos",
        role = "Eletricista",
        rating = 4.9f,
        reviewsCount = 156,
        services = listOf("Instalação elétrica", "Reparo de tomadas", "Instalação de ventiladores"),
        city = "São Paulo"
    )
)

// Mock Products
val mockProducts = listOf(
    Product(
        id = 1,
        title = "Guarda Roupa",
        price = 750.00,
        description = "Guarda roupa 6 portas com espelho, madeira maciça",
        seller = mockProviders[0],
        category = "Móveis",
        inStock = true
    ),
    Product(
        id = 2,
        title = "Furadeira sem fio",
        price = 250.00,
        description = "Furadeira de impacto 20V com bateria de lítio",
        seller = mockProviders[1],
        category = "Ferramentas",
        inStock = true
    ),
    Product(
        id = 3,
        title = "Forno de Embutir",
        price = 1200.00,
        description = "Forno de embutir 60cm, 6 queimadores, aço inox",
        seller = mockProviders[2],
        category = "Eletrodomésticos",
        inStock = true
    ),
    Product(
        id = 4,
        title = "Martelo",
        price = 35.00,
        description = "Martelo profissional 500g, cabo ergonômico",
        seller = mockProviders[0],
        category = "Ferramentas",
        inStock = true
    )
)

// Mock Orders
val mockOrders = listOf(
    Order(
        id = 1,
        items = listOf(
            CartItem(mockProducts[0], 1)
        ),
        total = 750.00,
        status = OrderStatus.IN_TRANSIT,
        tracking = listOf(
            TrackingEvent("Postado", System.currentTimeMillis() - 86400000, "Pedido postado"),
            TrackingEvent("Em trânsito", System.currentTimeMillis() - 43200000, "Em trânsito para entrega"),
            TrackingEvent("Saiu para entrega", System.currentTimeMillis() - 3600000, "Saiu para entrega")
        )
    ),
    Order(
        id = 2,
        items = listOf(
            CartItem(mockProducts[1], 1)
        ),
        total = 250.00,
        status = OrderStatus.DELIVERED,
        tracking = listOf(
            TrackingEvent("Postado", System.currentTimeMillis() - 172800000, "Pedido postado"),
            TrackingEvent("Em trânsito", System.currentTimeMillis() - 129600000, "Em trânsito para entrega"),
            TrackingEvent("Saiu para entrega", System.currentTimeMillis() - 86400000, "Saiu para entrega"),
            TrackingEvent("Entregue", System.currentTimeMillis() - 43200000, "Pedido entregue")
        )
    ),
    Order(
        id = 3,
        items = listOf(
            CartItem(mockProducts[2], 1)
        ),
        total = 1200.00,
        status = OrderStatus.CANCELLED
    )
)

// Mock Proposals
val mockProposals = listOf(
    Proposal(
        id = 1,
        requesterName = "Maria Santos",
        title = "Montagem de guarda roupa duas portas",
        date = System.currentTimeMillis() - 86400000,
        location = "Rua São José, 123, São Paulo - SP",
        budget = 180.00,
        provider = mockProviders[0],
        rating = 4.6f,
        description = "Preciso montar um guarda roupa de duas portas. É de madeira e já está desmontado. Preciso que seja feito com cuidado para não danificar o acabamento.",
        status = ProposalStatus.PENDING
    ),
    Proposal(
        id = 2,
        requesterName = "João Silva",
        title = "Instalação Elétrica",
        date = System.currentTimeMillis() - 172800000,
        location = "Av. Paulista, 1000 - São Paulo",
        budget = 300.00,
        provider = mockProviders[2],
        rating = 4.7f,
        description = "Preciso instalar algumas tomadas e lâmpadas em um apartamento.",
        status = ProposalStatus.ACCEPTED
    )
)

// Mock Message Threads
val mockMessageThreads = listOf(
    MessageThread(
        id = 1,
        title = "Ordem de serviço - Montagem de Móveis",
        lastPreview = "Perfeito! Vou estar lá às 14h.",
        lastTime = System.currentTimeMillis() - 3600000,
        unreadCount = 1
    ),
    MessageThread(
        id = 2,
        title = "Compra de Furadeira",
        lastPreview = "Agradecemos a compra! Seu pedido foi enviado.",
        lastTime = System.currentTimeMillis() - 7200000,
        unreadCount = 0
    ),
    MessageThread(
        id = 3,
        title = "Suporte Técnico",
        lastPreview = "Como posso ajudar?",
        lastTime = System.currentTimeMillis() - 10800000,
        unreadCount = 0
    ),
    MessageThread(
        id = 4,
        title = "Proposta - Jardinagem",
        lastPreview = "Gostaria de fazer um orçamento para seu jardim.",
        lastTime = System.currentTimeMillis() - 86400000,
        unreadCount = 2
    )
)

// Mock Chat Messages
val mockChatMessages = listOf(
    ChatMessage(
        id = 1,
        threadId = 1,
        author = "Maria Santos",
        text = "Olá! Gostaria de fazer um orçamento para montagem de móveis.",
        time = "14:00",
        content = "Olá! Gostaria de fazer um orçamento para montagem de móveis.",
        timestamp = System.currentTimeMillis() - 3600000
    ),
    ChatMessage(
        id = 2,
        threadId = 1,
        author = "Rodrigo Silva",
        text = "Claro! Posso fazer um orçamento de R$ 150,00 para a montagem.",
        time = "14:30",
        content = "Claro! Posso fazer um orçamento de R$ 150,00 para a montagem.",
        timestamp = System.currentTimeMillis() - 1800000
    ),
    ChatMessage(
        id = 3,
        threadId = 1,
        author = "Maria Santos",
        text = "Perfeito! Vou estar lá às 14h.",
        time = "15:00",
        content = "Perfeito! Vou estar lá às 14h.",
        timestamp = System.currentTimeMillis() - 900000
    )
)

// Mock Notifications
val mockNotifications = listOf(
    NotificationItem(
        id = 1,
        title = "Pedido Confirmado",
        snippet = "Seu pedido #12345 foi confirmado e está sendo preparado.",
        dateLabel = "Agora",
        type = NotificationType.ORDER_UPDATE
    ),
    NotificationItem(
        id = 2,
        title = "Nova Proposta",
        snippet = "Você recebeu uma nova proposta para montagem de móveis.",
        dateLabel = "Há 5 min",
        type = NotificationType.PROPOSAL_UPDATE
    ),
    NotificationItem(
        id = 3,
        title = "Pedido em Trânsito",
        snippet = "Seu pedido #12345 saiu para entrega.",
        dateLabel = "Há 1 hora",
        type = NotificationType.ORDER_UPDATE
    )
)

// Mock Reviews
val mockReviews = listOf(
    Review(
        id = 1,
        rating = 5.0f,
        comment = "Excelente serviço! Muito profissional e pontual. Recomendo muito!",
        author = mockUsers[0],
        target = mockUsers[2]
    ),
    Review(
        id = 2,
        rating = 4.5f,
        comment = "Trabalho bem feito, preço justo. Ficou muito bom!",
        author = mockUsers[1],
        target = mockUsers[2]
    ),
    Review(
        id = 3,
        rating = 5.0f,
        comment = "Superou minhas expectativas. Muito atencioso e cuidadoso com o trabalho.",
        author = mockUsers[0],
        target = mockUsers[2]
    )
)

// Mock Banners
val mockBanners = listOf(
    Banner(
        id = 1,
        title = "Banner Pequeno",
        description = "R$50/dia",
        price = 50.0,
        type = BannerType.SMALL
    ),
    Banner(
        id = 2,
        title = "Banner Grande",
        description = "R$90/dia",
        price = 90.0,
        type = BannerType.LARGE
    )
)

// Mock Plan
val mockPlan = Plan(
    name = "Plano Mensal",
    pricePerMonth = 20.00
)

// Mock Payment Methods
val mockPaymentMethods = listOf(
    PaymentMethod(
        type = PaymentType.CREDIT_CARD,
        masked = "**** **** **** 1234",
        holder = "Lucas Almeida"
    ),
    PaymentMethod(
        type = PaymentType.DEBIT_CARD,
        masked = "**** **** **** 5678",
        holder = "Lucas Almeida"
    )
)

// Mock Addresses
val mockAddresses = listOf(
    Address(
        name = "Casa",
        phone = "(11) 99999-9999",
        cep = "01234-567",
        street = "Rua das Flores, 123",
        district = "Centro",
        city = "São Paulo",
        state = "SP"
    ),
    Address(
        name = "Trabalho",
        phone = "(11) 88888-8888",
        cep = "04567-890",
        street = "Av. Paulista, 1000",
        district = "Bela Vista",
        city = "São Paulo",
        state = "SP"
    )
)
