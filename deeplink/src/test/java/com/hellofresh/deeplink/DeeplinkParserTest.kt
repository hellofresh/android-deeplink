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
