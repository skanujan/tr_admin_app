package com.trelegantboutique.admin.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Small helpers for reading the JSON the Catalog and Sales screens get back.
 * They never throw: a missing or null field simply reads as empty / zero /
 * false, so a slightly different server reply cannot crash the app.
 */
private fun JsonObject.elem(key: String): JsonElement? {
    val e = this.get(key)
    return if (e == null || e.isJsonNull) null else e
}

fun JsonObject.str(key: String): String {
    val e = elem(key) ?: return ""
    return try { if (e.isJsonPrimitive) e.asString else "" } catch (ex: Exception) { "" }
}

fun JsonObject.dbl(key: String): Double {
    val e = elem(key) ?: return 0.0
    return try { if (e.isJsonPrimitive) e.asDouble else 0.0 } catch (ex: Exception) { 0.0 }
}

fun JsonObject.int(key: String): Int = dbl(key).toInt()

fun JsonObject.bool(key: String): Boolean {
    val e = elem(key) ?: return false
    return try {
        if (!e.isJsonPrimitive) false
        else if (e.asJsonPrimitive.isBoolean) e.asBoolean
        else e.asString.let { it == "1" || it.equals("true", true) }
    } catch (ex: Exception) { false }
}

fun JsonObject.obj(key: String): JsonObject? {
    val e = elem(key) ?: return null
    return if (e.isJsonObject) e.asJsonObject else null
}

fun JsonObject.list(key: String): List<JsonObject> {
    val e = elem(key) ?: return emptyList()
    if (!e.isJsonArray) return emptyList()
    val out = ArrayList<JsonObject>()
    for (x in e.asJsonArray) if (x.isJsonObject) out.add(x.asJsonObject)
    return out
}

fun JsonObject.intList(key: String): List<Int> {
    val e = elem(key) ?: return emptyList()
    if (!e.isJsonArray) return emptyList()
    val out = ArrayList<Int>()
    for (x in e.asJsonArray) {
        try { out.add(x.asInt) } catch (ex: Exception) { }
    }
    return out
}

fun JsonObject.strList(key: String): List<String> {
    val e = elem(key) ?: return emptyList()
    if (!e.isJsonArray) return emptyList()
    val out = ArrayList<String>()
    for (x in e.asJsonArray) {
        try { out.add(x.asString) } catch (ex: Exception) { }
    }
    return out
}

/** Build a request body: jsonOf("id" to 5, "name" to "x", "on" to true). */
fun jsonOf(vararg pairs: Pair<String, Any?>): JsonObject {
    val o = JsonObject()
    for ((k, v) in pairs) {
        when (v) {
            null -> {}
            is Boolean -> o.addProperty(k, v)
            is Int -> o.addProperty(k, v)
            is Long -> o.addProperty(k, v)
            is Double -> o.addProperty(k, v)
            is Number -> o.addProperty(k, v)
            is JsonElement -> o.add(k, v)
            else -> o.addProperty(k, v.toString())
        }
    }
    return o
}

fun jsonIntArray(values: Collection<Int>): JsonArray {
    val a = JsonArray()
    for (v in values) a.add(v)
    return a
}

/** "12.50" style text, no trailing noise for whole numbers. */
fun money(v: Double): String = if (v == Math.floor(v)) String.format("%.0f", v) else String.format("%.2f", v)
