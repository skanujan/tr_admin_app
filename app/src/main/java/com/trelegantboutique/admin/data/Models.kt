package com.trelegantboutique.admin.data

import com.google.gson.annotations.SerializedName

/**
 * Plain data shapes returned by the PHP API (the api folder on the live site).
 * Field names and nullability here were checked against live-tested responses
 * from each endpoint, not guessed from the PHP source alone.
 */

// ---- Generic envelope ----
data class ApiError(
    val ok: Boolean = false,
    val error: String? = null
)

// ---- api/login.php ----
data class LoginResponse(
    val ok: Boolean,
    val token: String?,
    val admin: AdminInfo?,
    val perms: List<String>?,
    val error: String?
)

data class AdminInfo(
    val id: Int,
    val name: String,
    val email: String,
    @SerializedName("role_id") val roleId: Int?
)

// ---- api/dashboard.php ----
data class DashboardResponse(
    val ok: Boolean,
    @SerializedName("admin_name") val adminName: String?,
    val currency: String?,
    @SerializedName("today_orders") val todayOrders: Int?,
    @SerializedName("today_revenue") val todayRevenue: Double?,
    @SerializedName("completed_today") val completedToday: Int?,
    @SerializedName("pending_orders") val pendingOrders: Int?,
    @SerializedName("status_counts") val statusCounts: Map<String, Int>?,
    @SerializedName("low_stock") val lowStock: List<LowStockProduct>?
)

data class LowStockProduct(
    val id: Int,
    val name: String,
    val stock: Int,
    val image: String?
)

// ---- api/orders-list.php / orders-detail.php ----
data class OrdersListResponse(
    val ok: Boolean,
    val orders: List<OrderSummary>?,
    @SerializedName("has_more") val hasMore: Boolean?,
    val page: Int?
)

data class OrderSummary(
    val id: Int,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("customer_name") val customerName: String?,
    val phone: String?,
    val status: String,
    val total: Double?,
    @SerializedName("is_completed") val isCompleted: Boolean?,
    @SerializedName("created_at") val createdAt: String?
)

data class OrderDetailResponse(
    val ok: Boolean,
    val order: OrderDetail?,
    val items: List<OrderItem>?,
    val error: String?
)

data class OrderDetail(
    val id: Int,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("customer_name") val customerName: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val status: String,
    val total: Double?,
    @SerializedName("is_completed") val isCompleted: Boolean?,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class OrderItem(
    val id: Int,
    @SerializedName("product_id") val productId: Int?,
    val name: String?,
    val qty: Int?,
    val price: Double?,
    val image: String?
)

// ---- api/orders-update-status.php ----
data class UpdateStatusRequest(
    val status: String,
    @SerializedName("return_shipping_cost") val returnShippingCost: Double? = null
)

// ---- api/products-list.php / products-detail.php ----
data class ProductsListResponse(
    val ok: Boolean,
    val products: List<ProductSummary>?,
    @SerializedName("has_more") val hasMore: Boolean?,
    val page: Int?
)

data class ProductSummary(
    val id: Int,
    val name: String,
    val price: Double?,
    @SerializedName("sale_price") val salePrice: Double?,
    @SerializedName("effective_price") val effectivePrice: Double?,
    val stock: Int?,
    val active: Boolean?,
    val featured: Boolean?,
    val image: String?
)

data class ProductDetailResponse(
    val ok: Boolean,
    val product: ProductDetail?,
    val colors: List<ProductColor>?,
    val error: String?
)

data class ProductDetail(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double?,
    @SerializedName("sale_price") val salePrice: Double?,
    val stock: Int?,
    val active: Boolean?,
    val featured: Boolean?,
    val image: String?
)

data class ProductColor(
    val id: Int,
    val name: String?,
    @SerializedName("hex_code") val hexCode: String?
)

data class ProductUpdateRequest(
    val active: Boolean? = null,
    val stock: Int? = null,
    val price: Double? = null,
    @SerializedName("sale_price") val salePrice: Double? = null
)

// ---- api/push-register.php ----
data class PushRegisterRequest(
    @SerializedName("push_token") val pushToken: String,
    val platform: String = "android"
)

// ---- generic ok-only response (logout, update-status, products-update) ----
data class OkResponse(
    val ok: Boolean,
    val error: String? = null
)
