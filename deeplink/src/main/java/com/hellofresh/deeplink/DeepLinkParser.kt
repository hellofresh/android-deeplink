package com.hellofresh.deeplink

import android.net.Uri

class DeepLinkParser<T>(
    private val environment: Environment,
    private val routes: List<BaseRoute<T>>,
    private val fallback: Action<T>
) {

    fun parse(uri: Uri): ParserResult<T> {
        val (route, matchResult) = routes.asSequence()
            .map { it to it.matchWith(uri) }
            .firstOrNull { it.second.isMatch } ?: return fallback.run(uri, emptyMap(), environment)

        return route.run(uri, matchResult.params, environment)
    }


    class Builder<T>(private val environment: Environment) {

        private val routes: MutableSet<BaseRoute<T>> = hashSetOf()
        private var defaultFallback: Action<T>? = null

        fun addRoute(route: BaseRoute<T>) = apply {
            routes.add(route)
        }

        fun addFallbackAction(action: Action<T>) = apply {
            defaultFallback = action
        }

        fun build(): DeepLinkParser<T> {
            val fallback = defaultFallback ?: error("Default fallback is not provided!")

            return DeepLinkParser(environment, routes.toList(), fallback)
        }
    }

    companion object {

        @JvmStatic
        fun <T> of(environment: Environment): Builder<T> {
            return Builder(environment)
        }
    }
}
