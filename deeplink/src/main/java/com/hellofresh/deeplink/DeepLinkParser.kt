/*
 * Copyright (c) 2019.  The HelloFresh Android Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hellofresh.deeplink

class DeepLinkParser<T>(
    private val environment: Environment,
    private val routes: List<BaseRoute<T>>,
    private val fallback: Action<T>
) {

    fun parse(uri: DeepLinkUri): T {
        val (route, matchResult) = routes.asSequence()
            .map { it to it.matchWith(uri) }
            .firstOrNull { it.second.isMatch } ?: return fallback.run(uri, emptyMap(), environment)

        return route.run(uri, matchResult.params, environment)
    }


    class Builder<T>(private val environment: Environment) {

        private val routes: MutableSet<BaseRoute<T>> = LinkedHashSet()
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
