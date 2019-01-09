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
