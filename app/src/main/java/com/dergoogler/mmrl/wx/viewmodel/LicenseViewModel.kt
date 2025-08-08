package com.dergoogler.mmrl.wx.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dergoogler.mmrl.ext.LoadData
import com.dergoogler.mmrl.ext.LoadData.Default.asLoadData
import com.dergoogler.mmrl.wx.model.license.Artifact
import com.dergoogler.mmrl.wx.model.license.UiLicense
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import javax.inject.Inject

@HiltViewModel
class LicenseViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    var data by mutableStateOf<LoadData<List<UiLicense>>>(LoadData.Loading)
        private set

    init {
        loadData()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            data = runCatching {
                context.assets.open("app/cash/licensee/artifacts.json").use { stream ->
                    Json.decodeFromStream<List<Artifact>>(stream)
                        .map(::UiLicense)
                }
            }.onFailure {
                Log.d(TAG, "Failed to load licenses", it)
            }.asLoadData()
        }
    }

    private companion object {
        const val TAG = "LicenseViewModel"
    }
}