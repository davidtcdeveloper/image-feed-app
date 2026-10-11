package com.example.imagefeed.presentation.mvi

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private data class TestState(
    val count: Int = 0,
) : UiState

private sealed interface TestIntent : UiIntent {
    data object Increment : TestIntent

    data class TriggerEffect(
        val message: String,
    ) : TestIntent
}

private sealed interface TestEffect : UiEffect {
    data class Message(
        val text: String,
    ) : TestEffect
}

private class ConcreteTestViewModel : MviViewModel<TestState, TestIntent, TestEffect>(TestState()) {
    var cleared = false

    override fun dispatch(intent: TestIntent) {
        when (intent) {
            is TestIntent.Increment -> updateState { it.copy(count = it.count + 1) }
            is TestIntent.TriggerEffect -> sendEffect(TestEffect.Message(intent.message))
        }
    }

    override fun teardown() {
        super.teardown()
        cleared = true
    }
}

class MviViewModelTest {
    @Test
    fun initialStateIsConfiguredCorrectly() {
        val viewModel = ConcreteTestViewModel()
        assertEquals(0, viewModel.state.value.count)
    }

    @Test
    fun dispatchUpdatesStateAtomically() {
        val viewModel = ConcreteTestViewModel()
        viewModel.dispatch(TestIntent.Increment)
        assertEquals(1, viewModel.state.value.count)
        viewModel.dispatch(TestIntent.Increment)
        assertEquals(2, viewModel.state.value.count)
    }

    @Test
    fun sendEffectEmitsThroughChannel() =
        runTest {
            val viewModel = ConcreteTestViewModel()
            viewModel.dispatch(TestIntent.TriggerEffect("Hello MVI"))
            val effect = viewModel.effects.first()
            assertTrue(effect is TestEffect.Message)
            assertEquals("Hello MVI", effect.text)
        }

    @Test
    fun teardownInvokesTeardownHook() {
        val viewModel = ConcreteTestViewModel()
        viewModel.clear()
        assertTrue(viewModel.cleared)
    }
}
