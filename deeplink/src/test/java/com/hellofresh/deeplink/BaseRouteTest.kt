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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaseRouteTest {

    @Test
    fun matchWith_pathVariations() {
        var uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes")
        assertTrue(TestRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/")
        assertTrue(TestRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/x")
        assertFalse(TestRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234")
        assertTrue(TestRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/")
        assertFalse(TestRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("hellofresh://host/recipes")
        assertTrue(TestRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("hellofresh://host/recipes/")
        assertTrue(TestRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("hellofresh://host/recipes/x")
        assertFalse(TestRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("hellofresh://host/recipe/1234")
        assertTrue(TestRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("hellofresh://host/recipe/")
        assertFalse(TestRoute.matchWith(uri).isMatch)
    }

    @Test
    fun matchWith_pathVariationsWithOverride() {
        var uri = DeepLinkUri.parse("hellofresh://recipes")
        assertTrue(PathOverrideRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("hellofresh://recipes/")
        assertTrue(PathOverrideRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("hellofresh://recipes/x")
        assertFalse(PathOverrideRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("hellofresh://recipe/1234")
        assertTrue(PathOverrideRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("hellofresh://recipe/")
        assertFalse(PathOverrideRoute.matchWith(uri).isMatch)
    }

    @Test
    fun matchWith_pathVariationsWithNamelessParameter() {
        var uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/x/1234")
        assertTrue(NamelessPathRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes//1234")
        assertTrue(NamelessPathRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/")
        assertFalse(NamelessPathRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes//")
        assertFalse(NamelessPathRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/x")
        assertTrue(NamelessPathRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234")
        assertTrue(NamelessPathRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/")
        assertFalse(NamelessPathRoute.matchWith(uri).isMatch)

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe")
        assertFalse(NamelessPathRoute.matchWith(uri).isMatch)
    }

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

object TestRoute : BaseRoute<Unit>("recipes", "recipe/:id") {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment) = Unit
}

    object PathOverrideRoute : BaseRoute<Unit>("recipes", "recipe/:id") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment) = Unit

        override fun treatHostAsPath(uri: DeepLinkUri): Boolean {
            return uri.scheme() == "hellofresh"
        }
    }
    
    object NamelessPathRoute : BaseRoute<Unit>("recipe/*", "recipes/*/:id") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment) = Unit
    }
}
