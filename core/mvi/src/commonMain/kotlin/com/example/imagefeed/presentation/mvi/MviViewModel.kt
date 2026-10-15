package com.example.imagefeed.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel implementing the Model-View-Intent (MVI) / Unidirectional Data Flow (UDF) pattern.
 *
 * @param S The immutable screen state type implementing [UiState].
 * @param I The user action/intent type implementing [UiIntent].
 * @param E The one-off side effect type implementing [UiEffect].
 */
@Suppress("unused")
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()
    val iosState: CommonFlow<S> = CommonFlow(state)

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()
    val iosEffects: CommonFlow<E> = CommonFlow(effects)

    /**
     * Dispatches a user intent to be processed by the ViewModel.
     */
    abstract fun dispatch(intent: I)

    /**
     * Atomically updates the current UI state.
     */
    protected fun updateState(transform: (S) -> S) {
        _state.update(transform)
    }

    /**
     * Emits a one-off side effect into the buffered effect channel.
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    /**
     * Deterministic teardown hook callable from Darwin runtimes (`deinit`) or test harnesses.
     */
    open fun teardown() {
        // Can be overridden by subclasses for additional cleanup
    }

    fun clear() {
        viewModelScope.cancel()
        _effects.close()
        teardown()
    }

    override fun onCleared() {
        super.onCleared()
        _effects.close()
        teardown()
    }
}
