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
    fun matchWith_DefaultPathResolution() {
        val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234")
        assertTrue(TestRoute.matchWith(uri).isMatch)

        val customUriWithHost = DeepLinkUri.parse("hellofresh://host/recipe/1234")
        assertTrue(TestRoute.matchWith(customUriWithHost).isMatch)

        val customUriNoHost = DeepLinkUri.parse("hellofresh://recipe/1234")
        assertFalse(TestRoute.matchWith(customUriNoHost).isMatch)
    }

    @Test
    fun matchWith_OverridePathResolution() {
        val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234")
        assertTrue(PathOverrideRoute.matchWith(uri).isMatch)

        val customUriWithHost = DeepLinkUri.parse("hellofresh://host/recipe/1234")
        assertFalse(PathOverrideRoute.matchWith(customUriWithHost).isMatch)

        val customUriNoHost = DeepLinkUri.parse("hellofresh://recipe/1234")
        assertTrue(PathOverrideRoute.matchWith(customUriNoHost).isMatch)
    }

    @Test
    fun matchWith_namelessPathResolution() {
        var uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/me/1234")
        assertTrue(NamelessPathRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/customer-key/1234")
        assertTrue(NamelessPathRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/anything/1234")
        assertTrue(NamelessPathRoute.matchWith(uri).isMatch)
    }

    object TestRoute : BaseRoute<Unit>("recipe/:id") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment) = Unit
    }

    object PathOverrideRoute : BaseRoute<Unit>("recipe/:id") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment) = Unit

        override fun treatHostAsPath(uri: DeepLinkUri): Boolean {
            return uri.scheme() == "hellofresh"
        }
    }
    
    object NamelessPathRoute : BaseRoute<Unit>("recipes/*/:id") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment) = Unit
    }
}
