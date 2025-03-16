package com.hisham.removebg

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> get() = _state

    fun removeBackground(imageBitmap: ImageBitmap) = viewModelScope.launch(Dispatchers.IO) {
//        OnnxRemoval()
        PyTorchRemoval()
            .runInference(imageBitmap.asAndroidBitmap())
            .map { it.asImageBitmap() }
            .map { mergeImageWithMask(imageBitmap, it) }
            .onStart { _state.value = UiState(showLoading = true) }
            .collect { _state.value = UiState(image = it) }
    }
}

data class UiState(
    val image: ImageBitmap? = null,
    val showLoading: Boolean = false,
    val error: Boolean = false,
)