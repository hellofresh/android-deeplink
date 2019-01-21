package com.hellofresh.deeplink.intent

import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import com.hellofresh.deeplink.Action
import com.hellofresh.deeplink.BaseRoute
import com.hellofresh.deeplink.DeepLinkParser
import com.hellofresh.deeplink.DeepLinkUri
import com.hellofresh.deeplink.Environment

fun main() {
    val env = EmptyEnvironment

    val parser = DeepLinkParser.of<IntentResult>(env)
        .addRoute(RecipeRoute)
        .addRoute(SubscriptionRoute)
        .addFallbackAction(Fallback)
        .build()

    val result = parser.parse(DeepLinkUri.parse("hellofresh://recipe"))


    // Later
    result.start()


    // In Authenticator Activity
    val intent = Intent() // authenticator's own intent
    if (!IntentResult.advance(intent)) {
        // Start regular flow after authentication
    }
    // Finish authenticator activity eventually right? Just as usual
}


object RecipeRoute : BaseRoute<IntentResult>("recipe") {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment): IntentResult {
        val intent = Intent()
            .putExtra("keyA", params["a"])
            .putExtra("keyB", params["b"])
            .putExtra("keyC", params["c"])
            .setClassName("com.hellofresh.androidapp", "RecipeActivity")

        return IntentResult.of(env, intent)
            .requiresAuth(true)
            .create()
    }
}

object SubscriptionRoute : BaseRoute<IntentResult>("subscription") {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment): IntentResult {
        val intent = Intent().setClassName("com.hellofresh.androidapp", "SubscriptionActivity")
        val parentIntent = Intent().setClassName("com.hellofresh.androidapp", "MainActivity")

        val taskStack = TaskStackBuilder.create(env.context)
            .addNextIntentWithParentStack(parentIntent)
            .addNextIntent(intent)

        return IntentResult.of(env, taskStack)
            .requiresAuth(true)
            .create()
    }
}

object Fallback : Action<IntentResult> {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment): IntentResult {
        val defaultIntent = Intent().setClassName("com.hellofresh.androidapp", "MainActivity")
        return IntentResult.of(env, defaultIntent)
            .requiresAuth(false)
            .isFallback(true)
            .create()
    }
}

object EmptyEnvironment : Environment {

    @Suppress("UNCHECKED_CAST")
    fun <T> nullOverride() = null as T

    override val context get() = nullOverride<Context>()

    override val isAuthenticated get() = true
}
