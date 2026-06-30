package com.credenceid.sdkapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class CardReaderViewModel {
    private val _cardResult = MutableLiveData<String>()

    val cardResult: LiveData<String> get() = _cardResult

    fun updateCardResult(newText: String) {
        _cardResult.value = newText
    }
}