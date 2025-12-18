package com.sysmetrics.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase

/**
 * Factory for creating SettingsViewModel with dependencies.
 */
class SettingsViewModelFactory(
    private val manageOverlayConfigUseCase: ManageOverlayConfigUseCase
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(manageOverlayConfigUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
