package com.hellofresh.deeplink

import android.content.Context

object EmptyEnvironment : Environment {

    override val context: Context
        get() = nullOverride()

    override val isAuthenticated: Boolean
        get() = true

    @Suppress("UNCHECKED_CAST")
    private fun <T> nullOverride() = null as T
}
