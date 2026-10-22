package com.trelegantboutique.admin.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.data.OrderDetail
import com.trelegantboutique.admin.data.OrderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val ORDER_STATUS_OPTIONS = listOf("Pending", "Confirmed", "Shipped", "Delivered", "Returned", "Cancelled")

data class OrderDetailUiState(
    val loading: Boolean = true,
    val order: OrderDetail? = null,
    val items: List<OrderItem> = emptyList(),
    val error: String? = null,
    val updating: Boolean = false,
    val updateError: String? = null,
    val sessionExpired: Boolean = false
)

class OrderDetailViewModel(private val repo: ApiRepository, private val orderId: Int) : ViewModel() {
    private val _state = MutableStateFlow(OrderDetailUiState())
    val state: StateFlow<OrderDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = repo.orderDetail(orderId)) {
                is ApiResult.Success -> _state.value = OrderDetailUiState(
                    loading = false,
                    order = result.data.order,
                    items = result.data.items ?: emptyList()
                )
                is ApiResult.Failure -> _state.value = OrderDetailUiState(
                    loading = false, error = result.message, sessionExpired = result.unauthorized
                )
            }
        }
    }

    fun updateStatus(newStatus: String, returnShippingCost: Double? = null) {
        _state.value = _state.value.copy(updating = true, updateError = null)
        viewModelScope.launch {
            when (val result = repo.updateOrderStatus(orderId, newStatus, returnShippingCost)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(updating = false)
                    load()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    updating = false, updateError = result.message, sessionExpired = result.unauthorized
                )
            }
        }
    }
}
