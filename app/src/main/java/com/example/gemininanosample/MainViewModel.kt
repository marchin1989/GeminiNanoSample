package com.example.gemininanosample

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatedText = MutableStateFlow("")
    val generatedText: StateFlow<String> = _generatedText.asStateFlow()

    fun updateInputText(newText: String) {
        _inputText.value = newText
        _isGenerating.value = true
    }

    fun updateIsGenerating(isGenerating: Boolean) {
        if (isGenerating) {
            _generatedText.value = ""
        }
        _isGenerating.value = isGenerating
    }

    fun addGeneratedText(generatedText: String?) {
        _generatedText.value += generatedText
    }
}