[![Build Status](https://travis-ci.org/hellofresh/android-deeplink.svg?branch=master)](https://travis-ci.org/hellofresh/android-deeplink) [![codecov](https://codecov.io/gh/hellofresh/android-deeplink/branch/master/graph/badge.svg?token=pYXRTssjCY)](https://codecov.io/gh/hellofresh/android-deeplink) [ ![Download](https://api.bintray.com/packages/hellofresh/maven/android-deeplink/images/download.svg) ](https://bintray.com/hellofresh/maven/android-deeplink/_latestVersion)

# android-deeplink

Deeplink parser library

## Sample usage
The most important entry points are the `DeepLinkParser<T>` and `BaseRoute<T>` classes.

You will typically define all routes and their corresponding actions by creating 
concrete implementations of the `BaseRoute`.

The parser is flexible in such a way that you can freely define whatever its parameter should be. 
For instance, a typical Android application might want to return an activity Intent for each route, 
so that this is simply started as required. The parser and routes will then be defined as 
`DeepLinkParser<Intent>` and `BaseRoute<Intent>` respectively.

A very simplistic example of a `DeepLinkParser<String>` is given below

```kotlin
object RecipeRoute : BaseRoute<String>("recipes", "recipe/:id") {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, environment: Environment): String {
        return params["id"] ?: javaClass.simpleName
    }
}

object SubscriptionRoute : BaseRoute<String>("subscription") {

    override fun run(uri: DeepLinkUri, params: Map<String, String>, environment: Environment): String {
        return javaClass.simpleName
    }
}

val parser = DeepLinkParser.of<String>(EmptyEnvironment)
    .addRoute(RecipeRoute)
    .addRoute(SubscriptionRoute)
    .addFallbackAction(object : Action<String> {
        override fun run(uri: DeepLinkUri, params: Map<String, String>, environment: Environment): String {
            return "fallback"
        }
    })
    .build()

val stringResult = parser.parse("http://www.hellofresh.com/recipe/1234")
assertEquals("1234", stringResult)
```

License
-------

    Copyright (C) 2019 The Hellofresh Android Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
