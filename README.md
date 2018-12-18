# android-deeplink

Deeplink library for HF Android projects


## Sample usage
```kotlin

val deepLinkParser = DeepParser<IntentParseResult>()
    .addRoute(RecipeRouter<IntentParseResult>(
        "recipe/all/",
        "recipe/week/next/",
        "recipe/:id"))
    .addRoute(SubscriptionRouter<IntentParseResult>(
        "subscriptions/:subscriptionId/tracking/:weekId",
        "subscriptions/:subscriptionId/weeks/:weekId"
    ))

val intentParseResult = deepLinkParser.parse("http://www.hellofresh.com/recipe/1234")
```