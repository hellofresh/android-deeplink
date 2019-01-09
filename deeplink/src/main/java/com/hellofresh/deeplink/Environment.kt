package com.hellofresh.deeplink

import android.content.Context

interface Environment {

    val context: Context
    val isAuthenticated: Boolean
}
