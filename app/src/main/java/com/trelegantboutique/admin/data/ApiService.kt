package com.trelegantboutique.admin.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * One method per endpoint under /api on the live site. Every call except
 * login returns through Retrofit's Response<> wrapper rather than throwing,
 * so a 401/403/429 from the server is read as a normal (if unsuccessful)
 * response - ApiRepository turns that into a user-facing message instead of
 * a crash.
 */
interface ApiService {

    @POST("login.php")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @POST("logout.php")
    suspend fun logout(): Response<OkResponse>

    @GET("dashboard.php")
    suspend fun dashboard(): Response<DashboardResponse>

    @GET("orders-list.php")
    suspend fun ordersList(
        @Query("status") status: String? = null,
        @Query("q") q: String? = null,
        @Query("page") page: Int = 1
    ): Response<OrdersListResponse>

    @GET("orders-detail.php")
    suspend fun orderDetail(@Query("id") id: Int): Response<OrderDetailResponse>

    @POST("orders-update-status.php")
    suspend fun updateOrderStatus(
        @Query("id") id: Int,
        @Body body: UpdateStatusRequest
    ): Response<OkResponse>

    @GET("products-list.php")
    suspend fun productsList(
        @Query("q") q: String? = null,
        @Query("page") page: Int = 1
    ): Response<ProductsListResponse>

    @GET("products-detail.php")
    suspend fun productDetail(@Query("id") id: Int): Response<ProductDetailResponse>

    @POST("products-update.php")
    suspend fun updateProduct(
        @Query("id") id: Int,
        @Body body: ProductUpdateRequest
    ): Response<OkResponse>

    @POST("push-register.php")
    suspend fun registerPush(@Body body: PushRegisterRequest): Response<OkResponse>
}
