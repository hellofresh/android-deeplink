package com.hellofresh.deeplink.intent

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.hellofresh.deeplink.Environment

class IntentResult private constructor(
    private val env: Environment,
    private val intents: List<Intent>,
    private val requiresAuth: Boolean,
    val isFallback: Boolean
) {

    fun start() {
        // NEEDS ANSWERS! (Some things already resolved and removed in this commit)
        // ====
        // Start intent or task stack builder, as the case may be!
        // Now using intent list instead of task stack builder because
        // task stack always restarts the whole stack regardless of whether or not stuff is already in the foreground,
        // which makes the UX quite shitty :(

        // Do we need to expose a way to update the actual intent? Fill-in style
        // HF app adds BUNDLE_APPINDEXING_URI to intent post-parse

        // How to expose API for authenticator intent? (done)
        // ====

        val context = env.context
        val alreadyAuthenticated = env.isAuthenticated

        if (requiresAuth && !alreadyAuthenticated) {
            env as AuthEnvironment
            val authIntent = Intent(context, env.authenticator)
                .putExtra(NEXT_INTENT, intents.toTypedArray())
            context.startActivity(authIntent)
        } else {
            context.startActivities(intents.toLaunchArray())
        }
    }

    class Builder internal constructor(private val env: Environment) {

        private val intents = arrayListOf<Intent>()

        private var requiresAuth: Boolean = false
        private var isFallback: Boolean = false

        // API TBD
        fun intentStack(vararg intentStack: Intent) = apply {
            intents.addAll(intentStack)
        }

        fun requiresAuth(requiresAuth: Boolean): Builder = apply {
            this.requiresAuth = requiresAuth
        }

        fun isFallback(isFallback: Boolean): Builder = apply {
            this.isFallback = isFallback
        }

        fun create(): IntentResult {
            check(intents.isNotEmpty()) { "At least one intent must be supplied!" }
            return IntentResult(env, intents, requiresAuth, isFallback)
        }

    }

    companion object {

        const val NEXT_INTENT = "com.hellofresh.deeplink.intent.next"

        fun of(env: Environment): Builder {
            return Builder(env)
        }

        fun advance(context: Context, authIntent: Intent): Boolean {
            val p = authIntent.getParcelableArrayExtra(IntentResult.NEXT_INTENT) ?: return false
            val intents = p.filterIsInstance<Intent>().ifEmpty { return false }
            context.startActivities(intents.toLaunchArray())
            return true
        }

        // API TBD
        private fun List<Intent>.toLaunchArray(): Array<Intent> {
            val intents = toTypedArray().ifEmpty { return emptyArray() }
            intents[0].addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intents
        }
    }
}

interface AuthEnvironment : Environment {

    val authenticator: Class<out Activity> // Should we require a full intent instead?
}
