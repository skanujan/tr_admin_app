package com.trelegantboutique.admin.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.data.ProductSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductsListUiState(
    val loading: Boolean = true,
    val products: List<ProductSummary> = emptyList(),
    val query: String = "",
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false
)

class ProductsListViewModel(private val repo: ApiRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProductsListUiState())
    val state: StateFlow<ProductsListUiState> = _state.asStateFlow()

    init { refresh() }

    fun setQuery(q: String) { _state.value = _state.value.copy(query = q) }

    fun search() = refresh()

    fun refresh() {
        val current = _state.value
        _state.value = current.copy(loading = true, error = null, page = 1)
        viewModelScope.launch {
            when (val result = repo.productsList(current.query.ifBlank { null }, 1)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    loading = false,
                    products = result.data.products ?: emptyList(),
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
            when (val result = repo.productsList(current.query.ifBlank { null }, nextPage)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    loadingMore = false,
                    products = _state.value.products + (result.data.products ?: emptyList()),
                    hasMore = result.data.hasMore ?: false,
                    page = nextPage
                )
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    loadingMore = false, error = result.message, sessionExpired = result.unauthorized
                )
            }
        }
    }

    /** Fast path for the stock/active toggles on the list row itself, so the
     *  person doesn't have to open a product just to mark it out of stock. */
    fun quickToggleActive(product: ProductSummary) {
        viewModelScope.launch {
            val newActive = !(product.active ?: true)
            when (repo.updateProduct(product.id, com.trelegantboutique.admin.data.ProductUpdateRequest(active = newActive))) {
                is ApiResult.Success -> refresh()
                is ApiResult.Failure -> { /* silently ignored here; detail screen surfaces real errors */ }
            }
        }
    }
}
