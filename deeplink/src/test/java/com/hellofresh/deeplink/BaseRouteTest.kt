package com.hellofresh.deeplink

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

    object TestRoute : BaseRoute<String>("recipe/:id") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, environment: Environment) = TODO()
    }
}
