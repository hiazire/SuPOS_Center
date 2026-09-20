package com.example.supos.ui.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.supos.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PerformanceUiState {
    data object Loading : PerformanceUiState
    data class Success(val report: SalesReport) : PerformanceUiState
    data class Error(val message: String) : PerformanceUiState
}

class PerformanceViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {
    private val _selectedRange = MutableStateFlow(ReportRange.TODAY)
    val selectedRange: StateFlow<ReportRange> = _selectedRange.asStateFlow()

    private val _uiState = MutableStateFlow<PerformanceUiState>(PerformanceUiState.Loading)
    val uiState: StateFlow<PerformanceUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun selectRange(range: ReportRange) {
        if (_selectedRange.value == range) return
        _selectedRange.value = range
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = PerformanceUiState.Loading
            orderRepository.fetchHistoryOrders(_selectedRange.value.requestDays)
                .onSuccess { orders ->
                    _uiState.value = PerformanceUiState.Success(SalesReportCalculator.calculate(orders))
                }
                .onFailure { error ->
                    _uiState.value = PerformanceUiState.Error(error.message ?: "無法取得戰報")
                }
        }
    }
}
