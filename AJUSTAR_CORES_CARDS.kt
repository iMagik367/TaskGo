// Script para adicionar BorderStroke cinza em todos os cards brancos
// Buscar por: Card(.*containerColor = TaskGoBackgroundWhite
// Adicionar: border = BorderStroke(1.dp, TaskGoBorder)

// Padrão a buscar:
// Card(
//     ...,
//     colors = CardDefaults.cardColors(
//         containerColor = TaskGoBackgroundWhite
//     ),
//     elevation = ...,
//     shape = ...
// )

// Substituir por:
// Card(
//     ...,
//     colors = CardDefaults.cardColors(
//         containerColor = TaskGoBackgroundWhite
//     ),
//     elevation = ...,
//     shape = ...,
//     border = BorderStroke(1.dp, TaskGoBorder)
// )
