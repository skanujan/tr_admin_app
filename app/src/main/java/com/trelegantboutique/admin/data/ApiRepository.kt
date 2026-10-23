package com.trelegantboutique.admin.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException

/** What a screen gets back from the repository - either the data it asked
 *  for, or a short message already fit to show the person (no stack traces). */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String, val unauthorized: Boolean = false) : ApiResult<Nothing>()
}

/**
 * Every screen goes through here rather than calling ApiService directly, so
 * network errors, HTTP error bodies and "your session expired" all get turned
 * into one consistent ApiResult the UI can switch on.
 */
class ApiRepository(private val client: ApiClient, private val session: SessionStore) {

    private val gson = Gson()

    suspend fun login(email: String, password: String, deviceName: String): ApiResult<LoginResponse> {
        return call {
            client.service.login(
                mapOf("email" to email, "password" to password, "device_name" to deviceName)
            )
        }
    }

    suspend fun logout(): ApiResult<OkResponse> = call { client.service.logout() }

    suspend fun dashboard(): ApiResult<DashboardResponse> = call { client.service.dashboard() }

    suspend fun ordersList(status: String?, q: String?, page: Int): ApiResult<OrdersListResponse> =
        call { client.service.ordersList(status, q, page) }

    suspend fun orderDetail(id: Int): ApiResult<OrderDetailResponse> =
        call { client.service.orderDetail(id) }

    suspend fun updateOrderStatus(id: Int, status: String, returnShippingCost: Double?): ApiResult<OkResponse> =
        call { client.service.updateOrderStatus(id, UpdateStatusRequest(status, returnShippingCost)) }

    suspend fun productsList(q: String?, page: Int): ApiResult<ProductsListResponse> =
        call { client.service.productsList(q, page) }

    suspend fun productDetail(id: Int): ApiResult<ProductDetailResponse> =
        call { client.service.productDetail(id) }

    suspend fun updateProduct(id: Int, req: ProductUpdateRequest): ApiResult<OkResponse> =
        call { client.service.updateProduct(id, req) }

    suspend fun registerPush(token: String): ApiResult<OkResponse> =
        call { client.service.registerPush(PushRegisterRequest(token)) }

    suspend fun getJson(path: String, query: Map<String, String> = emptyMap()): ApiResult<JsonObject> =
        call { client.service.getJson(path, query) }

    suspend fun postJson(path: String, body: JsonObject): ApiResult<JsonObject> =
        call { client.service.postJson(path, body) }

    /** Sends one photo (already shrunk to a JPEG) plus text fields as multipart/form-data. */
    suspend fun uploadPhoto(path: String, fields: Map<String, String>, jpeg: ByteArray?): ApiResult<JsonObject> {
        val textType = "text/plain".toMediaType()
        val parts = HashMap<String, RequestBody>()
        for ((k, v) in fields) parts[k] = v.toRequestBody(textType)
        val filePart = if (jpeg != null) {
            val body = jpeg.toRequestBody("image/jpeg".toMediaType())
            MultipartBody.Part.createFormData("file", "photo.jpg", body)
        } else null
        return call { client.service.postMultipart(path, parts, filePart) }
    }

    private suspend fun <T> call(block: suspend () -> Response<T>): ApiResult<T> {
        return try {
            val response = block()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body)
            } else {
                val errorMessage = parseError(response)
                if (response.code() == 401) {
                    // Token revoked or expired server-side - clear the local
                    // session so the app drops back to the login screen
                    // instead of silently failing every call from here on.
                    session.clear()
                    ApiResult.Failure(errorMessage ?: "Your session has expired. Please sign in again.", unauthorized = true)
                } else {
                    ApiResult.Failure(errorMessage ?: "Something went wrong (code ${response.code()}).")
                }
            }
        } catch (e: IOException) {
            ApiResult.Failure("Couldn't reach the server. Check your internet connection and try again.")
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Unexpected error.")
        }
    }

    private fun <T> parseError(response: Response<T>): String? {
        return try {
            val raw = response.errorBody()?.string() ?: return null
            gson.fromJson(raw, ApiError::class.java)?.error
        } catch (e: Exception) {
            null
        }
    }
}
