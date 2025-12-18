package com.taskgoapp.taskgo.core.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import android.location.Location
import com.taskgoapp.taskgo.core.theme.TaskGoGreen

/**
 * Componente de mapa que exibe localização do usuário e lojas em tempo real
 * Utiliza Google Maps SDK com atualização em tempo real de localizações
 */
@Composable
fun ProvidersMapView(
    userLocation: Location?,
    stores: List<StoreLocation>,
    onStoreClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Posição inicial do mapa (centro do Brasil ou localização do usuário)
    val initialPosition = remember {
        userLocation?.let {
            LatLng(it.latitude, it.longitude)
        } ?: LatLng(-14.2350, -51.9253) // Centro do Brasil
    }
    
    var cameraPositionState by remember {
        mutableStateOf(
            CameraPositionState(
                position = CameraPosition.fromLatLngZoom(initialPosition, 12f)
            )
        )
    }
    
    // Atualizar câmera quando localização do usuário mudar
    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            val newPosition = LatLng(location.latitude, location.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(newPosition, 13f)
                )
            )
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = userLocation != null,
                isTrafficEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true,
                compassEnabled = true,
                zoomGesturesEnabled = true,
                scrollGesturesEnabled = true,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = true
            )
        ) {
            // Marcador da localização do usuário (se disponível)
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                    title = "Sua localização",
                    snippet = "Você está aqui"
                )
            }
            
            // Marcadores de lojas
            stores.forEach { store ->
                Marker(
                    state = MarkerState(
                        position = LatLng(store.latitude, store.longitude)
                    ),
                    title = store.name,
                    snippet = store.type,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                    onClick = {
                        onStoreClick(store.id)
                        true
                    }
                )
            }
        }
        
        // Legenda do mapa
        Card(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopEnd)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapLegendItem(
                    color = Color(0xFF2196F3),
                    label = "Lojas"
                )
                if (userLocation != null) {
                    MapLegendItem(
                        color = Color.Red,
                        label = "Você"
                    )
                }
            }
        }
    }
}

@Composable
private fun MapLegendItem(
    color: Color,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
    }
}

// Modelos de dados
data class ProviderLocation(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Float? = null,
    val isOnline: Boolean = true
)

data class StoreLocation(
    val id: String,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Float? = null,
    val isOpen: Boolean = true
)

