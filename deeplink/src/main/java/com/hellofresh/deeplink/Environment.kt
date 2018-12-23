package com.hellofresh.deeplink

import android.content.Context

interface Environment

object EmptyEnvironment : Environment

class ContextEnvironment(val context: Context) : Environment
