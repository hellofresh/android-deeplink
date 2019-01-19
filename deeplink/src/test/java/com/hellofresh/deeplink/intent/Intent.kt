package com.hellofresh.deeplink.intent

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import com.hellofresh.deeplink.Environment

class IntentResult private constructor(
    private val environment: Environment,
    private val taskStackBuilder: TaskStackBuilder,
    private val requiresAuth: Boolean
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

    // Do we even need a builder? :thinking:
    class Builder internal constructor(private val env: Environment) {

        private var taskStackBuilder: TaskStackBuilder? = null
        private var requiresAuth: Boolean = false

        fun withIntent(intent: Intent, requiresAuth: Boolean = false): Builder = apply {
            this.taskStackBuilder = TaskStackBuilder.create(env.context).addNextIntent(intent)
            this.requiresAuth = requiresAuth
        }

        fun withTaskStack(taskStackBuilder: TaskStackBuilder, requiresAuth: Boolean): Builder = apply {
            this.taskStackBuilder = taskStackBuilder
            this.requiresAuth = requiresAuth
        }

        fun build(): IntentResult {
            val taskStack = taskStackBuilder ?: error("No intent/task stack builder has been set!")
            return IntentResult(env, taskStack, requiresAuth)
        }

    }

    companion object {

        const val NEXT_INTENT = "com.hellofresh.deeplink.intent.next"

        fun builder(env: Environment): Builder {
            return Builder(env)
        }

        // Do we need this method?
        fun advance(authIntent: Intent): Boolean {
            val pendingIntent = authIntent.getParcelableExtra<PendingIntent>(IntentResult.NEXT_INTENT) ?: return false
            pendingIntent.send()
            return true
        }
    }
}
