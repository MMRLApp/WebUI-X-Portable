package com.dergoogler.mmrl.wx.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dergoogler.mmrl.wx.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mmrlx.webui.CSSActions
import javax.inject.Inject

@HiltViewModel
class DevToolsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    var borderAllState by mutableStateOf<CSSActions?>(null)
    var editContentState by mutableStateOf<Boolean?>(null)
}