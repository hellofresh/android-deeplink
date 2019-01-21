package com.hellofresh.deeplink.intent

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import com.hellofresh.deeplink.Environment

class IntentResult private constructor(
    private val environment: Environment,
    private val taskStackBuilder: TaskStackBuilder,
    private val requiresAuth: Boolean,
    val isFallback: Boolean
) {

    fun start() {
        // NEEDS ANSWERS! (Some things changed already. See below)
        // ====
        // Start intent or task stack builder, as the case may be! (everything now wrapped inside a task stack builder)
        // Needs environment to know: (done)
        // - If already authenticated: just start the direct intent
        // - Else start the authenticator intent with direct forwarded
        // If we have environment, then we probably don't need this context parameter? (done)
        // Can/should we support intents other than activities? Services, broadcasts? (I think we shouldn't)
        // Can we advance from auth activity to direct intent transparently? (I think not)
        // What other alternatives do we have if we can't? (implemented a potential alternative)

        // Do we need to expose a way to update the actual intent? Fill-in style
        // HF app adds BUNDLE_APPINDEXING_URI to intent post-parse

        // How to expose API for authenticator intent?
        // Do we need a singleton that already contains the env and auth intent?
        // ====

        val context = environment.context
        val alreadyAuthenticated = environment.isAuthenticated

        if (requiresAuth && !alreadyAuthenticated) {
            val authIntent = Intent() // Use actual intent. Should we expose an API for this? I think so
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
