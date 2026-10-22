package com.trelegantboutique.admin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.data.DashboardResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = true,
    val data: DashboardResponse? = null,
    val error: String? = null,
    val sessionExpired: Boolean = false
)

class DashboardViewModel(private val repo: ApiRepository) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = repo.dashboard()) {
                is ApiResult.Success -> _state.value = DashboardUiState(loading = false, data = result.data)
                is ApiResult.Failure -> _state.value = DashboardUiState(
                    loading = false, error = result.message, sessionExpired = result.unauthorized
                )
            }
        }
    }
}
