[![Build Status](https://travis-ci.org/hellofresh/android-deeplink.svg?branch=master)](https://travis-ci.org/hellofresh/android-deeplink) [ ![Download](https://api.bintray.com/packages/hellofresh/maven/android-deeplink/images/download.svg) ](https://bintray.com/hellofresh/maven/android-deeplink/_latestVersion)

# DEPRECATED

The project is no longer maintained. Please consider copying the source code and creating the local library module if you are still relying on it. 

# android-deeplink

Deeplink parser library

## Deep Link Handler Resolution
This section describes the approach taken by the parser to resolve the handler that processes a deep link.

A parser is instantiated with a set of Routes and a fallback Action.
The parser goes through these routes (in the order of registration) until it finds one that is able to handle the link.
The order is important in a case where multiple routes can handle the same deep link. In such cases, the first match is ALWAYS selected.  
The fallback action gets executed if there are no registered routes, or if non of the registered routes are able to handle the deep link.


Each route reports whether or not it's able to handle a link to the parser. The report also contains a map of key/value pairs that will be discussed in the path matching sub-section.
Once a suitable handler is found, the parser then forwards the URI and the key/value map back to the handler for actual processing.  
The output of this is then returned as the final result of the parse request.

The logic to determine whether/not a route can handle a link is described below

### Path matching
A Route is made up of an array of route patterns. The deep link will be tested against each of these patterns to determine whether/not the Route can handle the link.  
If a match is found, we return immediately, otherwise, we test the link against all patterns, and fallback to NO-MATCH if none of them matches the link.

A route pattern is a string representing the path segments of a deep link URI. The pattern is broken down into its constituent parts/segments and each part is tested against the corresponding part of the deep link path segments.  
We take a fast path by immediately returning a NO-MATCH if the size of the pattern segments is not the same as that of the URI path segments.

A route pattern part can be one of the following:

1. **Simple string:**
This is a static string defined at compile time that must be exactly the same as the corresponding part of the deep link.

2. **Wildcard (\*)**:
This is quite straightforward, as the wildcard always passes unconditionally

3. **Parameterized string**: `:parameterName(REGEX)`  
Simple regexes are officially supported, however, the library does not attempt any sophisticated logic, hence complex regexes are highly discouraged.  
The `REGEX` is matched against the entire URI part and if successful, `parameterName` will be stored as a key in the param map, and the corresponding URI part as its value
  - `(REGEX)` is optional, and omitting it makes the syntax like `:parameterName`. This is basically equivalent to `:parameterName(.*)` and it passes unconditionally, just like the wildcard.  
  - `parameterName` can be omitted as well in a situation where the regex matching functionality alone is required. The syntax then becomes `:(REGEX)`. Since there is no parameter name, the matched value will not be added to the param map.

A Route can be configured such that it treats the host of the input deep link as the first part of the path segments. This can come handy for use with deep links made up of custom schemes and that do not necessarily define an explicit host.

## Sample Usage
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

## Installation

To add `android-deeplink` to your project, include the following in your app's `build.gradle.kts` or `build.gradle` file respectively:

`build.gradle.kts`

```
dependencies {
    implementation("com.hellofresh.android:deeplink:${latest.version}")
}
```

`build.gradle`

```
dependencies {
    implementation "com.hellofresh.android:deeplink:${latest.version}"
}
```

**Snapshots** of the development version are available in JFrog's snapshots repository. Add the repo below to download `SNAPSHOT` releases.

`build.gradle.kts`

```
repositories {
    maven(url = "http://oss.jfrog.org/artifactory/oss-snapshot-local/")
}
```

`build.gradle`

```
repositories {
    maven { url 'http://oss.jfrog.org/artifactory/oss-snapshot-local/' }
}
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
