package com.trelegantboutique.admin.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.data.OrderSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrdersListUiState(
    val loading: Boolean = true,
    val orders: List<OrderSummary> = emptyList(),
    val status: String? = null,
    val query: String = "",
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false
)

val ORDER_STATUS_FILTERS = listOf(null, "Pending", "Confirmed", "Shipped", "Delivered", "Returned", "Cancelled")

class OrdersListViewModel(private val repo: ApiRepository) : ViewModel() {
    private val _state = MutableStateFlow(OrdersListUiState())
    val state: StateFlow<OrdersListUiState> = _state.asStateFlow()

    init { refresh() }

    fun setStatus(status: String?) {
        _state.value = _state.value.copy(status = status)
        refresh()
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun search() {
        refresh()
    }

    fun refresh() {
        val current = _state.value
        _state.value = current.copy(loading = true, error = null, page = 1)
        viewModelScope.launch {
            when (val result = repo.ordersList(current.status, current.query.ifBlank { null }, 1)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    loading = false,
                    orders = result.data.orders ?: emptyList(),
                    hasMore = result.data.hasMore ?: false,
                    page = 1
                )
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    loading = false, error = result.message, sessionExpired = result.unauthorized
                )
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loadingMore || !current.hasMore) return
        _state.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            val nextPage = current.page + 1
            when (val result = repo.ordersList(current.status, current.query.ifBlank { null }, nextPage)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    loadingMore = false,
                    orders = _state.value.orders + (result.data.orders ?: emptyList()),
                    hasMore = result.data.hasMore ?: false,
                    page = nextPage
                )
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    loadingMore = false, error = result.message, sessionExpired = result.unauthorized
                )
            }
        }
    }
}
