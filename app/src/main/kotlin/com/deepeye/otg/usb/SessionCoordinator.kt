package com.deepeye.otg.usb

import com.deepeye.otg.domain.models.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCoordinator @Inject constructor() {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state = _state.asStateFlow()

    private var currentSessionId: String? = null

    fun transition(newState: ConnectionState, reason: String = "") {
        val oldState = _state.value
        if (oldState == newState) return

        if (newState !is ConnectionState.Idle && currentSessionId == null) {
            currentSessionId = UUID.randomUUID().toString()
        }

        _state.value = newState
        
        Timber.i("[SESSION] transition from=${oldState::class.simpleName} to=${newState::class.simpleName} sessionId=$currentSessionId reason=\"$reason\"")
        
        if (newState is ConnectionState.Idle || newState is ConnectionState.Disconnected) {
            // Keep sessionId for disconnected state to allow correlation during reconnect?
            // Actually, a new session should likely get a new ID if it's a cold fresh start.
            // But if it's "Recovering", we keep it.
        }
    }

    fun getSessionId(): String = currentSessionId ?: "nosession"
}
