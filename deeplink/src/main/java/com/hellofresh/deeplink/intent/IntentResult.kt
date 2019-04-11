package com.hellofresh.deeplink.intent

import android.app.Activity
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import com.hellofresh.deeplink.Environment

class IntentResult private constructor(
    private val env: Environment,
    private val taskStackBuilder: TaskStackBuilder,
    private val requiresAuth: Boolean,
    val isFallback: Boolean
) {

    fun start() {
        // NEEDS ANSWERS! (Some things already resolved and removed in this commit)
        // ====
        // Start intent or task stack builder, as the case may be! (everything now wrapped inside a task stack builder)
        // Unfortunately, task stack always restarts the whole stack
        // regardless of whether or not stuff is already in the foreground, which makes the UX quite shitty :(

        // Do we need to expose a way to update the actual intent? Fill-in style
        // HF app adds BUNDLE_APPINDEXING_URI to intent post-parse

        // How to expose API for authenticator intent? (done)
        // ====

        val context = env.context
        val alreadyAuthenticated = env.isAuthenticated

        if (requiresAuth && !alreadyAuthenticated) {
            env as AuthEnvironment // Major key - Only cast when auth is actually needed :)
            val authIntent = Intent(context, env.authenticator) // Works like this
                .putExtra(
                    NEXT_INTENT,
                    taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
                )
            context.startActivity(authIntent)
        } else {
            taskStackBuilder.startActivities()
        }
    }

    class Builder internal constructor(private val env: Environment, private val taskStackBuilder: TaskStackBuilder) {

        private var requiresAuth: Boolean = false
        private var isFallback: Boolean = false

        fun requiresAuth(requiresAuth: Boolean): Builder = apply {
            this.requiresAuth = requiresAuth
        }

        fun isFallback(isFallback: Boolean): Builder = apply {
            this.isFallback = isFallback
        }

        fun create(): IntentResult {
            return IntentResult(env, taskStackBuilder, requiresAuth, isFallback)
        }

    }

    companion object {

        const val NEXT_INTENT = "com.hellofresh.deeplink.intent.next"

        fun of(env: Environment, intent: Intent): Builder {
            val taskStack = TaskStackBuilder.create(env.context).addNextIntent(intent)
            return of(env, taskStack)
        }

        fun of(env: Environment, taskStackBuilder: TaskStackBuilder): Builder {
            return Builder(env, taskStackBuilder)
        }

        fun advance(authIntent: Intent): Boolean {
            val pendingIntent = authIntent.getParcelableExtra<PendingIntent>(IntentResult.NEXT_INTENT) ?: return false
            pendingIntent.send()
            return true
        }
    }
}

interface AuthEnvironment : Environment {

    val authenticator: Class<out Activity> // Should we require a full intent instead?
}
