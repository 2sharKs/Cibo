package com.example.cibo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// REMEMBER: keep casual comments in code to show human intent and trade-offs

// --- Models ---
data class DeliveryTelemetry(
    val orderId: String,
    val currentStatus: DeliveryStatus,
    val latitude: Double,
    val longitude: Double,
    val dynamicETA: Int // seconds remaining estimate
)

enum class DeliveryStatus {
    PLACED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED
}

data class DeliveryTelemetryState(
    val telemetry: DeliveryTelemetry? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- Repository ---
class OrderTrackingRepository {
    // Emits 5 sequential, chronological delivery states with 3s gaps to simulate live updates
    fun observeOrderTrackingStream(orderId: String): Flow<DeliveryTelemetry> = flow {
        // Mock timeline: placed -> preparing -> out -> out (moving) -> delivered
        emit(
            DeliveryTelemetry(
                orderId = orderId,
                currentStatus = DeliveryStatus.PLACED,
                latitude = 12.9716,
                longitude = 77.5946,
                dynamicETA = 1800
            )
        )
        delay(3000)

        emit(
            DeliveryTelemetry(
                orderId = orderId,
                currentStatus = DeliveryStatus.PREPARING,
                latitude = 12.9719,
                longitude = 77.5948,
                dynamicETA = 1500
            )
        )
        delay(3000)

        emit(
            DeliveryTelemetry(
                orderId = orderId,
                currentStatus = DeliveryStatus.OUT_FOR_DELIVERY,
                latitude = 12.9725,
                longitude = 77.5955,
                dynamicETA = 900
            )
        )
        // REMEMBER: this next emission shows movement - still OUT_FOR_DELIVERY
        delay(3000)

        emit(
            DeliveryTelemetry(
                orderId = orderId,
                currentStatus = DeliveryStatus.OUT_FOR_DELIVERY,
                latitude = 12.9750,
                longitude = 77.5970,
                dynamicETA = 300
            )
        )
        delay(3000)

        emit(
            DeliveryTelemetry(
                orderId = orderId,
                currentStatus = DeliveryStatus.DELIVERED,
                latitude = 12.9755,
                longitude = 77.5975,
                dynamicETA = 0
            )
        )
        // TODO: Swap this mock loop with the real Python backend stream once API is live
    }.flowOn(Dispatchers.IO) // REMEMBER: keep this on Dispatchers.IO so we don't freeze the map UI
}

// --- ViewModel ---
class OrderTrackingViewModel(
    private val repository: OrderTrackingRepository = OrderTrackingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryTelemetryState(isLoading = false))
    val uiState: StateFlow<DeliveryTelemetryState> = _uiState

    private var collecting = false

    fun startTracking(orderId: String) {
        if (collecting) return
        collecting = true
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                repository.observeOrderTrackingStream(orderId).collect { telemetry ->
                    // Update state for UI; keep it simple and explicit
                    _uiState.update { DeliveryTelemetryState(telemetry = telemetry, isLoading = false) }
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = t.message ?: "Unknown error", isLoading = false) }
            }
        }
    }
}

// --- Compose UI ---
@Composable
fun OrderTrackerScreen(
    orderId: String,
    viewModel: OrderTrackingViewModel = viewModel(factory = androidx.lifecycle.viewmodel.initializer { OrderTrackingViewModel() })
) {
    // Kick off tracking when screen appears
    LaunchedEffect(key1 = orderId) {
        viewModel.startTracking(orderId)
    }

    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tracking: ${state.telemetry?.currentStatus ?: "Loading..."}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Raw coordinates display
        Text(text = "Coords: ${state.telemetry?.latitude ?: "-"}, ${state.telemetry?.longitude ?: "-"}")

        // Elegant progress bar mapping statuses -> progress
        val progress = when (state.telemetry?.currentStatus) {
            DeliveryStatus.PLACED -> 0.0f
            DeliveryStatus.PREPARING -> 0.33f
            DeliveryStatus.OUT_FOR_DELIVERY -> 0.66f
            DeliveryStatus.DELIVERED -> 1.0f
            else -> 0.0f
        }

        LinearProgressIndicator(progress = progress, modifier = Modifier
            .fillMaxWidth()
            .height(8.dp))

        // Small legend / ETA and actions
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "ETA: ${state.telemetry?.dynamicETA ?: "--"}s")
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { viewModel.startTracking(orderId) }) {
                Text(text = "Refresh")
            }
        }

        // Error or loading hints
        if (state.isLoading) {
            Text(text = "Connecting to tracking stream...")
        }
        state.error?.let { err ->
            Text(text = "Error: $err")
        }

        // TODO: Replace this placeholder with a Map composable showing the courier marker
        Text(text = "[Map placeholder — render a Map with marker at the coords]")
    }
}
