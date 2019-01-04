package com.hellofresh.deeplink


object RecipeRoute : BaseRoute<String>("recipes", "recipe/:id") {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, environment: Environment): ParserResult<String> {
        val id = params["id"] ?: return ParserResult(javaClass.simpleName)
        return ParserResult(id)
    }
}

object SubscriptionRoute : BaseRoute<String>("subscription") {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, environment: Environment): ParserResult<String> {
        return ParserResult(javaClass.simpleName)
    }
}
