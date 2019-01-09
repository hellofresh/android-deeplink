package com.hellofresh.deeplink

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaseRouteTest {

    @Test
    fun matchWith_UriWithNullQueryValue_ThrowsException() {
        assertFailsWith<IllegalStateException> {
            val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234?q")
            TestRoute.matchWith(uri)
        }
    }

    @Test
    fun matchWith_InputWithOnlyPathParams_ReturnsPathData() {
        val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234")
        val params = TestRoute.matchWith(uri).params

        assertEquals(1, params.size)
        assertEquals("1234", params["id"])
    }

    @Test
    fun matchWith_InputWithBothPathAndQueryParamsNoClash_ReturnsMixData() {
        val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234?token=XYZ")
        val params = TestRoute.matchWith(uri).params

        assertEquals(2, params.size)
        assertEquals("1234", params["id"])
        assertEquals("XYZ", params["token"])
    }

    @Test
    fun matchWith_InputWithBothPathAndQueryParamsClash_ReturnsMixDataWithPathPreferred() {
        val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234?token=XYZ&id=5678")
        val params = TestRoute.matchWith(uri).params

        assertEquals(2, params.size)
        assertEquals("1234", params["id"])
        assertEquals("XYZ", params["token"])
        // Query value can still be retrieved directly from the URI
        assertEquals("5678", uri.queryParameter("id"))
    }

    @Test
    fun matchWith_DefaultPathRetrieval() {
        val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234")
        assertTrue(TestRoute.matchWith(uri).isMatch)

        val customUriWithHost = DeepLinkUri.parse("hellofresh://host/recipe/1234")
        assertTrue(TestRoute.matchWith(customUriWithHost).isMatch)

        val customUriNoHost = DeepLinkUri.parse("hellofresh://recipe/1234")
        assertFalse(TestRoute.matchWith(customUriNoHost).isMatch)
    }

    @Test
    fun matchWith_OverridePathRetrieval() {
        val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234")
        assertTrue(PathOverrideRoute.matchWith(uri).isMatch)

        val customUriWithHost = DeepLinkUri.parse("hellofresh://host/recipe/1234")
        assertFalse(PathOverrideRoute.matchWith(customUriWithHost).isMatch)

        val customUriNoHost = DeepLinkUri.parse("hellofresh://recipe/1234")
        assertTrue(PathOverrideRoute.matchWith(customUriNoHost).isMatch)
    }

    object TestRoute : BaseRoute<String>("recipe/:id") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, environment: Environment) = TODO()
    }

    object PathOverrideRoute : BaseRoute<String>("recipe/:id") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, environment: Environment) = TODO()

        override fun retrieveHostAndPathSegments(uri: DeepLinkUri): Pair<String, List<String>> {
            if (uri.scheme() in arrayOf("http", "https")) {
                return super.retrieveHostAndPathSegments(uri)
            }
            val host = uri.host()
            val pathSegments = uri.pathSegments()
            return "" to listOf(host) + pathSegments
        }
    }
}
