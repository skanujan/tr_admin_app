package com.trelegantboutique.admin.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.data.ProductColor
import com.trelegantboutique.admin.data.ProductDetail
import com.trelegantboutique.admin.data.ProductUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val loading: Boolean = true,
    val product: ProductDetail? = null,
    val colors: List<ProductColor> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
    val sessionExpired: Boolean = false
)

class ProductDetailViewModel(private val repo: ApiRepository, private val productId: Int) : ViewModel() {
    private val _state = MutableStateFlow(ProductDetailUiState())
    val state: StateFlow<ProductDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = repo.productDetail(productId)) {
                is ApiResult.Success -> _state.value = ProductDetailUiState(
                    loading = false,
                    product = result.data.product,
                    colors = result.data.colors ?: emptyList()
                )
                is ApiResult.Failure -> _state.value = ProductDetailUiState(
                    loading = false, error = result.message, sessionExpired = result.unauthorized
                )
            }
        }
    }

    fun save(active: Boolean, stock: Int, price: Double, salePrice: Double?) {
        _state.value = _state.value.copy(saving = true, saveError = null, saved = false)
        viewModelScope.launch {
            val req = ProductUpdateRequest(active = active, stock = stock, price = price, salePrice = salePrice)
            when (val result = repo.updateProduct(productId, req)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(saving = false, saved = true)
                    load()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    saving = false, saveError = result.message, sessionExpired = result.unauthorized
                )
            }
        }
    }
}
