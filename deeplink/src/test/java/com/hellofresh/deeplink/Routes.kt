package com.hellofresh.deeplink


object RecipeRoute : BaseRoute<String>("recipes", "recipe/:id") {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment): String {
        return params["id"] ?: javaClass.simpleName
    }
}

object SubscriptionRoute : BaseRoute<String>("subscription") {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, env: Environment): String {
        return javaClass.simpleName
    }
}
