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
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _image = MutableStateFlow<ImageBitmap?>(null)
    val image: StateFlow<ImageBitmap?> get() = _image

    fun removeBackground(imageBitmap: ImageBitmap) = viewModelScope.launch(Dispatchers.IO) {
//        OnnxRemoval()
        PyTorchRemoval()
            .runInference(imageBitmap.asAndroidBitmap())
            .map { it.asImageBitmap() }
            .map { mergeImageWithMask(imageBitmap, it) }
            .collect { _image.value = it }
    }
}