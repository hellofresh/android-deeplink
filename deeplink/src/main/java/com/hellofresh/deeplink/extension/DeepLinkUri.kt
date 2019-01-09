package com.hellofresh.deeplink.extension

import android.net.Uri
import com.hellofresh.deeplink.DeepLinkUri

fun DeepLinkUri.Companion.get(uri: Uri): DeepLinkUri {
    return parse(uri.toString())
}

fun DeepLinkUri.toAndroidUri(): Uri {
    return Uri.parse(toString())
}
