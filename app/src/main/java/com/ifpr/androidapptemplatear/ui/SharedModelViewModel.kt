21' -

package com.ifpr.androidapptemplatear.uiimport androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedModelViewModel : ViewModel() {
    val selectedModel = MutableLiveData<String>()

    fun selectModel(modelPath: String) {
        selectedModel.value = modelPath
    }
}78