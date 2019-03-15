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
    fun matchWith_InputWithOnlyPathParams_ReturnsPathData() {
        val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/1234")
        val params = TestRoute.matchWith(uri).params

        assertEquals(1, params.size)
        assertEquals("1234", params["id"])
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

    @Test
    fun matchWith_regexPathResolution() {
        var uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/detail/abc-1234")
        var res = RegexPathRoute.matchWith(uri)
        assertTrue(res.isMatch)
        assertEquals("detail", res.params["action"])
        assertEquals("abc-1234", res.params["id"])

        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/info/abc-1234")
        res = RegexPathRoute.matchWith(uri)
        assertTrue(res.isMatch)
        assertEquals("info", res.params["action"])
        assertEquals("abc-1234", res.params["id"])

        // action does not match
        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/invalid/abc-1234")
        assertFalse(RegexPathRoute.matchWith(uri).isMatch)

        // id does not match
        uri = DeepLinkUri.parse("http://www.hellofresh.com/recipes/detail/1234")
        assertFalse(RegexPathRoute.matchWith(uri).isMatch)
    }

    @Test
    fun matchWith_regexPathResolutionUnnamed() {
        val uri = DeepLinkUri.parse("http://www.hellofresh.com/recipe/abc-1234")
        val res = UnnamedRegexPathRoute.matchWith(uri)
        assertTrue(res.isMatch)
        assertTrue(res.params.isEmpty())
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

    object RegexPathRoute : BaseRoute<Unit>("recipes/:action(detail|info)/:id(.*-\\w+)") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment) = Unit
    }

    object UnnamedRegexPathRoute : BaseRoute<Unit>("recipe/:(.*-\\w+)") {

        override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment) = Unit
    }
}
