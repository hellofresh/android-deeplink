package com.hellofresh.deeplink

import android.net.Uri

interface Action<out T> {

    fun run(uri: Uri, params: Map<String, String>, environment: Environment): ParserResult<T>
}
