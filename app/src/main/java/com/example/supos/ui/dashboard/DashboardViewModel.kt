package com.example.supos.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.supos.data.health.SystemHealthReport
import com.example.supos.data.health.SystemHealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(
    private val systemHealthRepository: SystemHealthRepository
) : ViewModel() {

    val healthReport: StateFlow<SystemHealthReport> = systemHealthRepository.healthReport

    private val _isDiagnosticsDialogOpen = MutableStateFlow(false)
    val isDiagnosticsDialogOpen: StateFlow<Boolean> = _isDiagnosticsDialogOpen.asStateFlow()

    fun openDiagnosticsDialog() {
        _isDiagnosticsDialogOpen.value = true
    }

    fun closeDiagnosticsDialog() {
        _isDiagnosticsDialogOpen.value = false
    }

    companion object {
        fun provideFactory(systemHealthRepository: SystemHealthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(systemHealthRepository) as T
                }
            }
    }
}
