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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeepLinkParserTest {

    // DeepLinkParser is immutable, so the same instance can be shared across tests
    private val parser = DeepLinkParser.of<String>(EmptyEnvironment)
        .addRoute(RecipeRoute)
        .addRoute(SubscriptionRoute)
        .addFallbackAction(object : Action<String> {
            override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment): String {
                return "fallback"
            }
        })
        .build()

    @Test
    fun newParser_NoFallback_ThrowsException() {
        assertFailsWith<IllegalStateException> {
            DeepLinkParser.of<String>(EmptyEnvironment)
                .addRoute(RecipeRoute)
                .build()
        }
    }

    @Test
    fun parseSimple() {
        assertEquals("RecipeRoute", parser.parse(DeepLinkUri.parse("http://world.com/recipes")))
    }

    @Test
    fun parseWithParam() {
        assertEquals("1234", parser.parse(DeepLinkUri.parse("http://world.com/recipe/1234")))
    }

    @Test
    fun parseWithNextRouter() {
        assertEquals("SubscriptionRoute", parser.parse(DeepLinkUri.parse("hellofresh://host/subscription")))
    }

    @Test
    fun parseFallback() {
        assertEquals("fallback", parser.parse(DeepLinkUri.parse("http://world.com/unknown")))
    }
}
