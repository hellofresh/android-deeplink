package com.hellofresh.deeplink

interface Action<out T> {

    fun run(uri: DeepLinkUri, params: Map<String, String>, environment: Environment): ParserResult<T>
}
