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

package com.hellofresh.deeplink.extension

import android.net.Uri
import com.hellofresh.deeplink.DeepLinkUri
import org.junit.Test
import kotlin.test.assertEquals

class DeepLinkUriKtTest {

    @Test
    fun get() {
        val androidUri = Uri.parse("http://www.hellofresh.com/test")
        val deepUri = DeepLinkUri.get(androidUri)

        assertEquals(androidUri.toString(), deepUri.toString())
    }

    @Test
    fun toAndroidUri() {
        val deepUri = DeepLinkUri.parse("custom://host.com/path/to/resource")
        val androidUri = deepUri.toAndroidUri()

        assertEquals(deepUri.toString(), androidUri.toString())
    }
}
