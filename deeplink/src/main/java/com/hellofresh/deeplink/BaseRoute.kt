package com.hellofresh.deeplink

import android.net.Uri

abstract class BaseRoute<out T>(private vararg val routes: String) : Action<T> {

    internal fun matchWith(uri: Uri): MatchResult {
        val inputParts = uri.pathSegments
        routes.forEach { route ->
            val params = hashMapOf<String, String>()

            val parts = route.split("/").dropLastWhile { it.isEmpty() }
            if (inputParts.size != parts.size) {
                return@forEach
            }
            inputParts.zip(parts) { inPart, routePart ->
                if (routePart.startsWith(":")) {
                    params[routePart.substring(1)] = inPart
                } else if (inPart != routePart) {
                    return@forEach
                }
            }
            uri.queryParameterNames.forEach { key ->
                val queryValue = uri.getQueryParameter(key) ?: error("This should not happen!")
                if (key in params && params[key] != queryValue) {
                    // Warn: about to replace path param with query param
                    // Shall we rather skip this instead (since path should ideally trump query)?
                    // Or perhaps, just throw an exception and return?
                }
                params[key] = queryValue
            }
            return MatchResult(true, params)
        }
        return MatchResult(false)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseRoute<*>) return false

        return routes.contentEquals(other.routes)
    }

    override fun hashCode(): Int {
        return routes.contentHashCode()
    }

}
