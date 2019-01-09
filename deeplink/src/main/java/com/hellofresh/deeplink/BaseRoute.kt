package com.hellofresh.deeplink

abstract class BaseRoute<out T>(private vararg val routes: String) : Action<T> {

    internal fun matchWith(uri: DeepLinkUri): MatchResult {
        val (_, inputParts) = retrieveHostAndPathSegments(uri)
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
            uri.queryParameterNames()
                .filter { it !in params }
                .forEach { key ->
                    val queryValue = uri.queryParameter(key) ?: error("""Query "$key" has a null value!""")
                    params[key] = queryValue
                }
            return MatchResult(true, params)
        }
        return MatchResult(false)
    }

    /**
     * Retrieves the host and path segments from a given [uri] and returns them as a pair.
     *
     * This is essentially exposed for use cases with custom URIs where the user might
     * treat the host as part of the path segments.
     * The default implementation just returns the actual [uri] host and path segments
     * accordingly.
     */
    protected open fun retrieveHostAndPathSegments(uri: DeepLinkUri): Pair<String, List<String>> {
        return uri.host() to uri.pathSegments()
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
