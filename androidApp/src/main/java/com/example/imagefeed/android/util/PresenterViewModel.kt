package com.example.imagefeed.android.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

class PresenterViewModel<P : Any>(
    val presenter: P,
    private val onClear: (P) -> Unit,
) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        onClear(presenter)
    }
}

@Composable
inline fun <reified P : Any> rememberEntryPresenter(
    key: String? = null,
    noinline onClear: (P) -> Unit = {},
    noinline factory: () -> P,
): P {
    val vm =
        viewModel<PresenterViewModel<P>>(
            key = key,
            factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        PresenterViewModel(presenter = factory(), onClear = onClear) as T
                },
        )
    return vm.presenter
}
