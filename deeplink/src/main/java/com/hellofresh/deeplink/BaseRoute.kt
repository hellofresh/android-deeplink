package com.hellofresh.deeplink

abstract class BaseRoute<out T>(vararg routes: String) {

    abstract fun run(uri: String, params: Map<String, String>): ParserResult<T>
}