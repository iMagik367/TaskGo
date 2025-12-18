package com.taskgoapp.taskgo.core.data

/**
 * Lista de estados brasileiros e suas cidades
 * Fonte: IBGE
 */
object BrazilianCities {
    val statesWithCities = mapOf(
        "AC" to listOf("Rio Branco", "Cruzeiro do Sul", "Sena Madureira", "Tarauacá", "Feijó"),
        "AL" to listOf("Maceió", "Arapiraca", "Palmeira dos Índios", "Rio Largo", "Penedo"),
        "AP" to listOf("Macapá", "Santana", "Laranjal do Jari", "Oiapoque", "Mazagão"),
        "AM" to listOf("Manaus", "Parintins", "Itacoatiara", "Manacapuru", "Coari"),
        "BA" to listOf("Salvador", "Feira de Santana", "Vitória da Conquista", "Camaçari", "Juazeiro"),
        "CE" to listOf("Fortaleza", "Caucaia", "Juazeiro do Norte", "Maracanaú", "Sobral"),
        "DF" to listOf("Brasília"),
        "ES" to listOf("Vitória", "Vila Velha", "Cariacica", "Serra", "Cachoeiro de Itapemirim"),
        "GO" to listOf("Goiânia", "Aparecida de Goiânia", "Anápolis", "Rio Verde", "Luziânia"),
        "MA" to listOf("São Luís", "Imperatriz", "Caxias", "Timon", "Codó"),
        "MT" to listOf("Cuiabá", "Várzea Grande", "Rondonópolis", "Sinop", "Tangará da Serra"),
        "MS" to listOf("Campo Grande", "Dourados", "Três Lagoas", "Corumbá", "Ponta Porã"),
        "MG" to listOf("Belo Horizonte", "Uberlândia", "Contagem", "Juiz de Fora", "Betim"),
        "PA" to listOf("Belém", "Ananindeua", "Marituba", "Paragominas", "Castanhal"),
        "PB" to listOf("João Pessoa", "Campina Grande", "Santa Rita", "Patos", "Bayeux"),
        "PR" to listOf("Curitiba", "Londrina", "Maringá", "Ponta Grossa", "Cascavel"),
        "PE" to listOf("Recife", "Jaboatão dos Guararapes", "Olinda", "Caruaru", "Petrolina"),
        "PI" to listOf("Teresina", "Parnaíba", "Picos", "Piripiri", "Campo Maior"),
        "RJ" to listOf("Rio de Janeiro", "São Gonçalo", "Duque de Caxias", "Nova Iguaçu", "Niterói"),
        "RN" to listOf("Natal", "Mossoró", "Parnamirim", "São Gonçalo do Amarante", "Macaíba"),
        "RS" to listOf("Porto Alegre", "Caxias do Sul", "Pelotas", "Canoas", "Santa Maria"),
        "RO" to listOf("Porto Velho", "Ji-Paraná", "Ariquemes", "Vilhena", "Cacoal"),
        "RR" to listOf("Boa Vista", "Rorainópolis", "Caracaraí", "Alto Alegre", "Mucajaí"),
        "SC" to listOf("Florianópolis", "Joinville", "Blumenau", "São José", "Criciúma"),
        "SP" to listOf("São Paulo", "Guarulhos", "Campinas", "São Bernardo do Campo", "Santo André"),
        "SE" to listOf("Aracaju", "Nossa Senhora do Socorro", "Lagarto", "Itabaiana", "São Cristóvão"),
        "TO" to listOf("Palmas", "Araguaína", "Gurupi", "Porto Nacional", "Paraíso do Tocantins")
    )
    
    val allStates = statesWithCities.keys.sorted()
    
    fun getCitiesForState(state: String): List<String> {
        return statesWithCities[state]?.sorted() ?: emptyList()
    }
}

