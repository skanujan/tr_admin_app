package com.trelegantboutique.admin.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.data.bool
import com.trelegantboutique.admin.data.list
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JsonUiState(
    val loading: Boolean = true,
    val data: JsonObject? = null,
    val items: List<JsonObject> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val sessionExpired: Boolean = false
)

/**
 * One reusable view model behind most Catalog and Sales screens: it loads a
 * JSON page from [path] (with optional paging through the array named
 * [listKey]), and sends changes back with [post]. [message] carries both
 * success texts and error texts for the screen to show in a snackbar.
 */
class JsonLoaderViewModel(
    private val repo: ApiRepository,
    private val path: String,
    private val listKey: String? = null,
    initialQuery: Map<String, String> = emptyMap()
) : ViewModel() {

    private val _state = MutableStateFlow(JsonUiState())
    val state: StateFlow<JsonUiState> = _state.asStateFlow()

    private var query: Map<String, String> = initialQuery

    init { load() }

    fun setQuery(q: Map<String, String>) {
        query = q
        load()
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null, page = 1)
        viewModelScope.launch {
            val q = HashMap(query)
            if (listKey != null) q["page"] = "1"
            when (val r = repo.getJson(path, q)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    loading = false,
                    data = r.data,
                    items = if (listKey != null) r.data.list(listKey) else emptyList(),
                    hasMore = r.data.bool("has_more"),
                    page = 1
                )
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    loading = false, error = r.message, sessionExpired = r.unauthorized
                )
            }
        }
    }

    /** Reload quietly (no spinner over the whole screen) after a change. */
    private fun reloadQuiet() {
        viewModelScope.launch {
            val q = HashMap(query)
            if (listKey != null) q["page"] = "1"
            when (val r = repo.getJson(path, q)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    data = r.data,
                    items = if (listKey != null) r.data.list(listKey) else _state.value.items,
                    hasMore = r.data.bool("has_more"),
                    page = 1
                )
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    message = r.message, sessionExpired = r.unauthorized
                )
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (listKey == null || s.loadingMore || !s.hasMore || s.loading) return
        _state.value = s.copy(loadingMore = true)
        viewModelScope.launch {
            val next = s.page + 1
            val q = HashMap(query)
            q["page"] = next.toString()
            when (val r = repo.getJson(path, q)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    loadingMore = false,
                    items = _state.value.items + r.data.list(listKey),
                    hasMore = r.data.bool("has_more"),
                    page = next
                )
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    loadingMore = false, message = r.message, sessionExpired = r.unauthorized
                )
            }
        }
    }

    /** POST a change. On success the server's "message" is shown and the list is reloaded. */
    fun post(
        postPath: String,
        body: JsonObject,
        reload: Boolean = true,
        onDone: ((JsonObject) -> Unit)? = null
    ) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            when (val r = repo.postJson(postPath, body)) {
                is ApiResult.Success -> {
                    val msg = r.data.get("message")
                    _state.value = _state.value.copy(
                        busy = false,
                        message = if (msg != null && msg.isJsonPrimitive) msg.asString else "Saved."
                    )
                    if (onDone != null) onDone(r.data)
                    if (reload) reloadQuiet()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    busy = false, message = r.message, sessionExpired = r.unauthorized
                )
            }
        }
    }

    fun upload(
        uploadPath: String,
        fields: Map<String, String>,
        jpeg: ByteArray?,
        reload: Boolean = true
    ) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            when (val r = repo.uploadPhoto(uploadPath, fields, jpeg)) {
                is ApiResult.Success -> {
                    val msg = r.data.get("message")
                    _state.value = _state.value.copy(
                        busy = false,
                        message = if (msg != null && msg.isJsonPrimitive) msg.asString else "Saved."
                    )
                    if (reload) reloadQuiet()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    busy = false, message = r.message, sessionExpired = r.unauthorized
                )
            }
        }
    }

    fun showMessage(text: String) {
        _state.value = _state.value.copy(message = text)
    }

    fun messageShown() {
        _state.value = _state.value.copy(message = null)
    }
}
