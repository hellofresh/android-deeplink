/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.hellofresh.deeplink

import com.hellofresh.deeplink.DeepLinkUri.Builder.ParseResult
import okio.Buffer
import java.net.IDN
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.LinkedHashSet
import java.util.Locale

/**
 * Adapted from OkHttp's HttpUrl class.
 *
 * Changes:
 * - Allow any scheme, instead of just http or https
 * - Migrated the class to Kotlin
 * - Made a few API modifications for nullability support
 *
 * Original implementation:
 * https://github.com/square/okhttp/blob/master/okhttp/src/main/java/com/squareup/okhttp/HttpUri.java
 */
class DeepLinkUri private constructor(builder: Builder) {

    /** Either "http" or "https" or some custom value as the case may be.  */
    private val scheme: String

    /** Decoded username.  */
    private val username: String

    /** Decoded password.  */
    private val password: String

    /** Canonical hostname.  */
    private val host: String

    /** Either 80, 443 or a user-specified port. In range [1..65535].  */
    private val port: Int

    /**
     * A list of canonical path segments. This list always contains at least one element, which may
     * be the empty string. Each segment is formatted with a leading '/', so if path segments were
     * ["a", "b", ""], then the encoded path would be "/a/b/".
     */
    private val pathSegments: List<String>

    /**
     * Alternating, decoded query names and values, or null for no query. Names may be empty or
     * non-empty, but never null. Values are null if the name has no corresponding '=' separator, or
     * empty, or non-empty.
     */
    private val queryNamesAndValues: List<String?>?

    /** Decoded fragment.  */
    private val fragment: String?

    /** Canonical URL.  */
    private val url: String

    val isHttps: Boolean
        get() = scheme == "https"

    init {
        this.scheme = checkNotNull(builder.scheme)
        this.username = percentDecode(builder.encodedUsername, plusIsSpace = false)
        this.password = percentDecode(builder.encodedPassword, plusIsSpace = false)
        this.host = checkNotNull(builder.host)
        this.port = builder.effectivePort()
        this.pathSegments = percentDecode(builder.encodedPathSegments, plusIsSpace = false).filterNotNull()
        this.queryNamesAndValues = builder.encodedQueryNamesAndValues?.let { percentDecode(it, plusIsSpace = true) }
        this.fragment = builder.encodedFragment?.let { percentDecode(it, plusIsSpace = false) }
        this.url = builder.toString()
    }

    /** Returns this URL as a [java.net.URL][URL].  */
    fun url(): URL {
        try {
            return URL(url)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e) // Unexpected!
        }

    }

    /**
     * Attempt to convert this URL to a [java.net.URI][URI]. This method throws an unchecked
     * [IllegalStateException] if the URL it holds isn't valid by URI's overly-stringent
     * standard. For example, URI rejects paths containing the '[' character. Consult that class for
     * the exact rules of what URLs are permitted.
     */
    fun uri(): URI {
        try {
            val uri = newBuilder().reencodeForUri().toString()
            return URI(uri)
        } catch (e: URISyntaxException) {
            throw IllegalStateException("not valid as a java.net.URI: $url")
        }

    }

    /** Returns either "http" or "https" or some custom scheme as the case may be.  */
    fun scheme(): String {
        return scheme
    }

    /** Returns the username, or an empty string if none is set.  */
    fun encodedUsername(): String {
        if (username.isEmpty()) return ""
        val usernameStart = scheme.length + 3 // "://".length() == 3.
        val usernameEnd = delimiterOffset(url, usernameStart, url.length, ":@")
        return url.substring(usernameStart, usernameEnd)
    }

    fun username(): String {
        return username
    }

    /** Returns the password, or an empty string if none is set.  */
    fun encodedPassword(): String {
        if (password.isEmpty()) return ""
        val passwordStart = url.indexOf(':', scheme.length + 3) + 1
        val passwordEnd = url.indexOf('@')
        return url.substring(passwordStart, passwordEnd)
    }

    /** Returns the decoded password, or an empty string if none is present.  */
    fun password(): String {
        return password
    }

    /**
     * Returns the host address suitable for use with [InetAddress.getAllByName]. May
     * be:
     *
     *  * A regular host name, like `android.com`.
     *  * An IPv4 address, like `127.0.0.1`.
     *  * An IPv6 address, like `::1`. Note that there are no square braces.
     *  * An encoded IDN, like `xn--n3h.net`.
     *
     */
    fun host(): String {
        return host
    }

    /**
     * Returns the explicitly-specified port if one was provided, or the default port for this URL's
     * scheme. For example, this returns 8443 for `https://square.com:8443/` and 443 for `https://square.com/`. The result is in `[1..65535]`.
     */
    fun port(): Int {
        return port
    }

    fun pathSize(): Int {
        return pathSegments.size
    }

    /**
     * Returns the entire path of this URL, encoded for use in HTTP resource resolution. The
     * returned path is always nonempty and is prefixed with `/`.
     */
    fun encodedPath(): String {
        val pathStart = url.indexOf('/', scheme.length + 3) // "://".length() == 3.
        val pathEnd = delimiterOffset(url, pathStart, url.length, "?#")
        return url.substring(pathStart, pathEnd)
    }

    fun encodedPathSegments(): List<String> {
        val pathStart = url.indexOf('/', scheme.length + 3)
        val pathEnd = delimiterOffset(url, pathStart, url.length, "?#")
        val result = ArrayList<String>()
        var i = pathStart
        while (i < pathEnd) {
            i++ // Skip the '/'.
            val segmentEnd = delimiterOffset(url, i, pathEnd, "/")
            result.add(url.substring(i, segmentEnd))
            i = segmentEnd
        }
        return result
    }

    fun pathSegments(): List<String> {
        return pathSegments
    }

    /**
     * Returns the query of this URL, encoded for use in HTTP resource resolution. The returned string
     * may be null (for URLs with no query), empty (for URLs with an empty query) or non-empty (all
     * other URLs).
     */
    fun encodedQuery(): String? {
        if (queryNamesAndValues == null) return null // No query.
        val queryStart = url.indexOf('?') + 1
        val queryEnd = delimiterOffset(url, queryStart + 1, url.length, "#")
        return url.substring(queryStart, queryEnd)
    }

    fun query(): String? {
        if (queryNamesAndValues == null) return null // No query.
        val result = StringBuilder()
        namesAndValuesToQueryString(result, queryNamesAndValues)
        return result.toString()
    }

    fun querySize(): Int {
        val queryNameAndValuesSize = queryNamesAndValues?.size ?: return 0
        return queryNameAndValuesSize / 2
    }

    /**
     * Returns the first query parameter named `name` decoded using UTF-8, or null if there is
     * no such query parameter.
     */
    fun queryParameter(name: String): String? {
        queryNamesAndValues ?: return null
        var i = 0
        val size = queryNamesAndValues.size
        while (i < size) {
            if (name == queryNamesAndValues[i]) {
                return queryNamesAndValues[i + 1]
            }
            i += 2
        }
        return null
    }

    fun queryParameterNames(): Set<String> {
        queryNamesAndValues ?: return emptySet()
        val result = LinkedHashSet<String>()
        var i = 0
        val size = queryNamesAndValues.size
        while (i < size) {
            result.add(queryNamesAndValues[i]!!)
            i += 2
        }
        return Collections.unmodifiableSet(result)
    }

    fun queryParameterValues(name: String): List<String?> {
        queryNamesAndValues ?: return emptyList()
        val result = ArrayList<String?>()
        var i = 0
        val size = queryNamesAndValues.size
        while (i < size) {
            if (name == queryNamesAndValues[i]) {
                result.add(queryNamesAndValues[i + 1])
            }
            i += 2
        }
        return Collections.unmodifiableList(result)
    }

    fun queryParameterName(index: Int): String? {
        return queryNamesAndValues?.get(index * 2)
    }

    fun queryParameterValue(index: Int): String? {
        return queryNamesAndValues?.get(index * 2 + 1)
    }

    fun encodedFragment(): String? {
        fragment ?: return null
        val fragmentStart = url.indexOf('#') + 1
        return url.substring(fragmentStart)
    }

    fun fragment(): String? {
        return fragment
    }

    /** Returns the URL that would be retrieved by following `link` from this URL.  */
    fun resolve(link: String): DeepLinkUri? {
        val builder = Builder()
        val result = builder.parse(this, link)
        return if (result == ParseResult.SUCCESS) builder.build() else null
    }

    fun newBuilder(): Builder {
        val result = Builder()
        result.scheme = scheme
        result.encodedUsername = encodedUsername()
        result.encodedPassword = encodedPassword()
        result.host = host
        result.port = if (port != defaultPort(scheme)) port else -1
        result.encodedPathSegments.clear()
        result.encodedPathSegments.addAll(encodedPathSegments())
        result.encodedQuery(encodedQuery())
        result.encodedFragment = encodedFragment()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is DeepLinkUri && other.url == url
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

    override fun toString(): String {
        return url
    }

    class Builder {
        var scheme: String? = null
        var encodedUsername = ""
        var encodedPassword = ""
        var host: String? = null
        var port = -1
        val encodedPathSegments: MutableList<String> = ArrayList()
        var encodedQueryNamesAndValues: MutableList<String?>? = null
        var encodedFragment: String? = null

        init {
            encodedPathSegments.add("") // The default path is '/' which needs a trailing space.
        }

        fun scheme(scheme: String): Builder = apply {
            this.scheme = scheme.toLowerCase(Locale.US)
        }

        fun username(username: String): Builder = apply {
            this.encodedUsername = canonicalize(
                username,
                USERNAME_ENCODE_SET,
                alreadyEncoded = false,
                plusIsSpace = false,
                asciiOnly = true
            )
        }

        fun encodedUsername(encodedUsername: String): Builder = apply {
            this.encodedUsername = canonicalize(
                encodedUsername, USERNAME_ENCODE_SET,
                alreadyEncoded = true,
                plusIsSpace = false,
                asciiOnly = true
            )
        }

        fun password(password: String): Builder = apply {
            this.encodedPassword = canonicalize(
                password,
                PASSWORD_ENCODE_SET,
                alreadyEncoded = false,
                plusIsSpace = false,
                asciiOnly = true
            )
        }

        fun encodedPassword(encodedPassword: String): Builder = apply {
            this.encodedPassword = canonicalize(
                encodedPassword, PASSWORD_ENCODE_SET,
                alreadyEncoded = true,
                plusIsSpace = false,
                asciiOnly = true
            )
        }

        /**
         * @param host either a regular hostname, International Domain Name, IPv4 address, or IPv6
         * address.
         */
        fun host(host: String): Builder = apply {
            val encoded =
                canonicalizeHost(host, 0, host.length) ?: throw IllegalArgumentException("unexpected host: $host")
            this.host = encoded
        }

        fun port(port: Int): Builder = apply {
            if (port !in 1..65535) throw IllegalArgumentException("unexpected port: $port")
            this.port = port
        }

        fun effectivePort(): Int {
            return if (port != -1) port else defaultPort(scheme!!)
        }

        fun addPathSegment(pathSegment: String): Builder = apply {
            push(pathSegment, 0, pathSegment.length, addTrailingSlash = false, alreadyEncoded = false)
        }

        fun addEncodedPathSegment(encodedPathSegment: String): Builder = apply {
            push(encodedPathSegment, 0, encodedPathSegment.length, addTrailingSlash = false, alreadyEncoded = true)
        }


        /**
         * Adds a set of path segments separated by a slash (either `\` or `/`). If
         * `pathSegments` starts with a slash, the resulting URL will have empty path segment.
         */
        fun addPathSegments(pathSegments: String): Builder {
            return addPathSegments(pathSegments, false)
        }

        /**
         * Adds a set of encoded path segments separated by a slash (either `\` or `/`). If
         * `encodedPathSegments` starts with a slash, the resulting URL will have empty path
         * segment.
         */
        fun addEncodedPathSegments(encodedPathSegments: String): Builder {
            return addPathSegments(encodedPathSegments, true)
        }

        private fun addPathSegments(pathSegments: String, alreadyEncoded: Boolean): Builder = apply {
            var offset = 0
            do {
                val segmentEnd = delimiterOffset(pathSegments, offset, pathSegments.length, "/\\")
                val addTrailingSlash = segmentEnd < pathSegments.length
                push(pathSegments, offset, segmentEnd, addTrailingSlash, alreadyEncoded)
                offset = segmentEnd + 1
            } while (offset <= pathSegments.length)
        }

        fun setPathSegment(index: Int, pathSegment: String): Builder = apply {
            val canonicalPathSegment = canonicalize(
                pathSegment,
                0,
                pathSegment.length,
                PATH_SEGMENT_ENCODE_SET,
                alreadyEncoded = false,
                plusIsSpace = false,
                asciiOnly = true
            )
            if (isDot(canonicalPathSegment) || isDotDot(canonicalPathSegment)) {
                throw IllegalArgumentException("unexpected path segment: $pathSegment")
            }
            encodedPathSegments[index] = canonicalPathSegment
        }

        fun setEncodedPathSegment(index: Int, encodedPathSegment: String): Builder = apply {
            val canonicalPathSegment = canonicalize(
                encodedPathSegment,
                0,
                encodedPathSegment.length,
                PATH_SEGMENT_ENCODE_SET,
                alreadyEncoded = true,
                plusIsSpace = false,
                asciiOnly = true
            )
            encodedPathSegments[index] = canonicalPathSegment
            if (isDot(canonicalPathSegment) || isDotDot(canonicalPathSegment)) {
                throw IllegalArgumentException("unexpected path segment: $encodedPathSegment")
            }
        }

        fun removePathSegment(index: Int): Builder = apply {
            encodedPathSegments.removeAt(index)
            if (encodedPathSegments.isEmpty()) {
                encodedPathSegments.add("") // Always leave at least one '/'.
            }
        }

        fun encodedPath(encodedPath: String): Builder = apply {
            if (!encodedPath.startsWith("/")) {
                throw IllegalArgumentException("unexpected encodedPath: $encodedPath")
            }
            resolvePath(encodedPath, 0, encodedPath.length)
        }

        fun query(query: String?): Builder = apply {
            this.encodedQueryNamesAndValues = if (query != null)
                queryStringToNamesAndValues(
                    canonicalize(
                        query,
                        QUERY_ENCODE_SET,
                        alreadyEncoded = false,
                        plusIsSpace = true,
                        asciiOnly = true
                    )
                )
            else
                null
        }

        fun encodedQuery(encodedQuery: String?): Builder = apply {
            this.encodedQueryNamesAndValues = if (encodedQuery != null)
                queryStringToNamesAndValues(
                    canonicalize(
                        encodedQuery, QUERY_ENCODE_SET,
                        alreadyEncoded = true,
                        plusIsSpace = true,
                        asciiOnly = true
                    )
                )
            else
                null
        }

        /** Encodes the query parameter using UTF-8 and adds it to this URL's query string.  */
        fun addQueryParameter(name: String, value: String?): Builder = apply {
            val queries = encodedQueryNamesAndValues ?: arrayListOf<String?>().also {
                encodedQueryNamesAndValues = it
            }
            queries.add(
                canonicalize(
                    name,
                    QUERY_COMPONENT_ENCODE_SET,
                    alreadyEncoded = false,
                    plusIsSpace = true,
                    asciiOnly = true
                )
            )
            queries.add(
                if (value != null)
                    canonicalize(
                        value,
                        QUERY_COMPONENT_ENCODE_SET,
                        alreadyEncoded = false,
                        plusIsSpace = true,
                        asciiOnly = true
                    )
                else
                    null
            )
        }

        /** Adds the pre-encoded query parameter to this URL's query string.  */
        fun addEncodedQueryParameter(encodedName: String, encodedValue: String?): Builder = apply {
            val queries = encodedQueryNamesAndValues ?: arrayListOf<String?>().also {
                encodedQueryNamesAndValues = it
            }
            queries.add(
                canonicalize(
                    encodedName,
                    QUERY_COMPONENT_ENCODE_SET,
                    alreadyEncoded = true,
                    plusIsSpace = true,
                    asciiOnly = true
                )
            )
            queries.add(
                if (encodedValue != null)
                    canonicalize(
                        encodedValue,
                        QUERY_COMPONENT_ENCODE_SET,
                        alreadyEncoded = true,
                        plusIsSpace = true,
                        asciiOnly = true
                    )
                else
                    null
            )
        }

        fun setQueryParameter(name: String, value: String): Builder = apply {
            removeAllQueryParameters(name)
            addQueryParameter(name, value)
        }

        fun setEncodedQueryParameter(encodedName: String, encodedValue: String): Builder = apply {
            removeAllEncodedQueryParameters(encodedName)
            addEncodedQueryParameter(encodedName, encodedValue)
        }

        fun removeAllQueryParameters(name: String): Builder = apply {
            if (encodedQueryNamesAndValues == null) return@apply
            val nameToRemove = canonicalize(
                name,
                QUERY_COMPONENT_ENCODE_SET,
                alreadyEncoded = false,
                plusIsSpace = true,
                asciiOnly = true
            )
            removeAllCanonicalQueryParameters(nameToRemove)
        }

        fun removeAllEncodedQueryParameters(encodedName: String): Builder = apply {
            if (encodedQueryNamesAndValues == null) return@apply
            removeAllCanonicalQueryParameters(
                canonicalize(
                    encodedName,
                    QUERY_COMPONENT_ENCODE_SET,
                    alreadyEncoded = true,
                    plusIsSpace = true,
                    asciiOnly = true
                )
            )
        }

        private fun removeAllCanonicalQueryParameters(canonicalName: String) {
            val queries = encodedQueryNamesAndValues ?: return
            var i = queries.size - 2
            while (i >= 0) {
                if (canonicalName == queries[i]) {
                    queries.removeAt(i + 1)
                    queries.removeAt(i)
                    if (queries.isEmpty()) {
                        encodedQueryNamesAndValues = null
                        return
                    }
                }
                i -= 2
            }
        }

        fun fragment(fragment: String?): Builder = apply {
            this.encodedFragment = fragment?.let {
                canonicalize(it, FRAGMENT_ENCODE_SET, alreadyEncoded = false, plusIsSpace = false, asciiOnly = false)
            }
        }

        fun encodedFragment(encodedFragment: String?): Builder = apply {
            this.encodedFragment = encodedFragment?.let {
                canonicalize(it, FRAGMENT_ENCODE_SET, alreadyEncoded = true, plusIsSpace = false, asciiOnly = false)
            }
        }

        /**
         * Re-encodes the components of this URL so that it satisfies (obsolete) RFC 2396, which is
         * particularly strict for certain components.
         */
        fun reencodeForUri(): Builder {
            run {
                var i = 0
                val size = encodedPathSegments.size
                while (i < size) {
                    val pathSegment = encodedPathSegments[i]
                    encodedPathSegments[i] = canonicalize(
                        pathSegment, PATH_SEGMENT_ENCODE_SET_URI,
                        alreadyEncoded = true,
                        plusIsSpace = false,
                        asciiOnly = true
                    )
                    i++
                }
            }
            encodedQueryNamesAndValues?.let {
                var i = 0
                val size = it.size
                while (i < size) {
                    val component = it[i]
                    if (component != null) {
                        it[i] = canonicalize(
                            component, QUERY_COMPONENT_ENCODE_SET_URI,
                            alreadyEncoded = true,
                            plusIsSpace = true,
                            asciiOnly = true
                        )
                    }
                    i++
                }
            }
            encodedFragment = encodedFragment?.let {
                canonicalize(it, FRAGMENT_ENCODE_SET_URI, alreadyEncoded = true, plusIsSpace = false, asciiOnly = false)
            }
            return this
        }

        fun build(): DeepLinkUri {
            if (scheme == null) throw IllegalStateException("scheme == null")
            if (host == null) throw IllegalStateException("host == null")
            return DeepLinkUri(this)
        }

        override fun toString(): String {
            val scheme = scheme ?: ""
            val host = host ?: ""
            val result = StringBuilder()

            result.append(scheme)
            result.append("://")

            if (!encodedUsername.isEmpty() || !encodedPassword.isEmpty()) {
                result.append(encodedUsername)
                if (!encodedPassword.isEmpty()) {
                    result.append(':')
                    result.append(encodedPassword)
                }
                result.append('@')
            }

            if (host.indexOf(':') != -1) {
                // Host is an IPv6 address.
                result.append('[')
                result.append(host)
                result.append(']')
            } else {
                result.append(host)
            }

            val effectivePort = effectivePort()
            if (effectivePort != defaultPort(scheme)) {
                result.append(':')
                result.append(effectivePort)
            }

            pathSegmentsToString(result, encodedPathSegments)

            if (encodedQueryNamesAndValues != null) {
                result.append('?')
                namesAndValuesToQueryString(result, encodedQueryNamesAndValues!!)
            }

            if (encodedFragment != null) {
                result.append('#')
                result.append(encodedFragment)
            }

            return result.toString()
        }

        internal enum class ParseResult {
            SUCCESS,
            MISSING_SCHEME,
            UNSUPPORTED_SCHEME,
            INVALID_PORT,
            INVALID_HOST
        }

        internal fun parse(base: DeepLinkUri?, input: String): ParseResult {
            var pos = skipLeadingAsciiWhitespace(input, 0, input.length)
            val limit = skipTrailingAsciiWhitespace(input, pos, input.length)

            // Scheme.
            val schemeDelimiterOffset = schemeDelimiterOffset(input, pos, limit)
            when {
                schemeDelimiterOffset != -1 -> when {
                    input.regionMatches(pos, "https:", 0, 6, ignoreCase = true) -> {
                        this.scheme = "https"
                        pos += "https:".length
                    }
                    input.regionMatches(pos, "http:", 0, 5, ignoreCase = true) -> {
                        this.scheme = "http"
                        pos += "http:".length
                    }
                    else -> {
                        this.scheme = input.substring(pos, schemeDelimiterOffset)
                        pos += scheme!!.length + 1
                    }
                }
                base != null -> this.scheme = base.scheme
                else -> return ParseResult.MISSING_SCHEME // No scheme.
            }

            // Authority.
            var hasUsername = false
            var hasPassword = false
            val slashCount = slashCount(input, pos, limit)
            if (slashCount >= 2 || base == null || base.scheme != this.scheme) {
                // Read an authority if either:
                //  * The input starts with 2 or more slashes. These follow the scheme if it exists.
                //  * The input scheme exists and is different from the base URL's scheme.
                //
                // The structure of an authority is:
                //   username:password@host:port
                //
                // Username, password and port are optional.
                //   [username[:password]@]host[:port]
                pos += slashCount
                authority@ while (true) {
                    val componentDelimiterOffset = delimiterOffset(input, pos, limit, "@/\\?#")
                    val c = if (componentDelimiterOffset != limit)
                        input[componentDelimiterOffset]
                    else
                        (-1).toChar()
                    when (c) {
                        '@' -> {
                            // User info precedes.
                            if (!hasPassword) {
                                val passwordColonOffset = delimiterOffset(
                                    input, pos, componentDelimiterOffset, ":"
                                )
                                val canonicalUsername = canonicalize(
                                    input,
                                    pos,
                                    passwordColonOffset,
                                    USERNAME_ENCODE_SET,
                                    alreadyEncoded = true,
                                    plusIsSpace = false,
                                    asciiOnly = true
                                )
                                this.encodedUsername = if (hasUsername)
                                    this.encodedUsername + "%40" + canonicalUsername
                                else
                                    canonicalUsername
                                if (passwordColonOffset != componentDelimiterOffset) {
                                    hasPassword = true
                                    this.encodedPassword = canonicalize(
                                        input,
                                        passwordColonOffset + 1,
                                        componentDelimiterOffset,
                                        PASSWORD_ENCODE_SET,
                                        alreadyEncoded = true,
                                        plusIsSpace = false,
                                        asciiOnly = true
                                    )
                                }
                                hasUsername = true
                            } else {
                                this.encodedPassword = this.encodedPassword + "%40" + canonicalize(
                                    input, pos, componentDelimiterOffset, PASSWORD_ENCODE_SET,
                                    alreadyEncoded = true,
                                    plusIsSpace = false, asciiOnly = true
                                )
                            }
                            pos = componentDelimiterOffset + 1
                        }

                        (-1).toChar(), '/', '\\', '?', '#' -> {
                            // Host info precedes.
                            val portColonOffset = portColonOffset(input, pos, componentDelimiterOffset)
                            if (portColonOffset + 1 < componentDelimiterOffset) {
                                this.host = canonicalizeHost(input, pos, portColonOffset)
                                this.port = parsePort(input, portColonOffset + 1, componentDelimiterOffset)
                                if (this.port == -1) return ParseResult.INVALID_PORT // Invalid port.
                            } else {
                                this.host = canonicalizeHost(input, pos, portColonOffset)
                                this.port = defaultPort(this.scheme!!)
                            }
                            if (this.host == null) return ParseResult.INVALID_HOST // Invalid host.
                            pos = componentDelimiterOffset
                            break@authority
                        }
                        else -> {
                        }
                    }
                }
            } else {
                // This is a relative link. Copy over all authority components. Also maybe the path & query.
                this.encodedUsername = base.encodedUsername()
                this.encodedPassword = base.encodedPassword()
                this.host = base.host
                this.port = base.port
                this.encodedPathSegments.clear()
                this.encodedPathSegments.addAll(base.encodedPathSegments())
                if (pos == limit || input[pos] == '#') {
                    encodedQuery(base.encodedQuery())
                }
            }

            // Resolve the relative path.
            val pathDelimiterOffset = delimiterOffset(input, pos, limit, "?#")
            resolvePath(input, pos, pathDelimiterOffset)
            pos = pathDelimiterOffset

            // Query.
            if (pos < limit && input[pos] == '?') {
                val queryDelimiterOffset = delimiterOffset(input, pos, limit, "#")
                this.encodedQueryNamesAndValues = queryStringToNamesAndValues(
                    canonicalize(
                        input,
                        pos + 1,
                        queryDelimiterOffset,
                        QUERY_ENCODE_SET,
                        alreadyEncoded = true,
                        plusIsSpace = true,
                        asciiOnly = true
                    )
                )
                pos = queryDelimiterOffset
            }

            // Fragment.
            if (pos < limit && input[pos] == '#') {
                this.encodedFragment = canonicalize(
                    input,
                    pos + 1,
                    limit,
                    FRAGMENT_ENCODE_SET,
                    alreadyEncoded = true,
                    plusIsSpace = false,
                    asciiOnly = false
                )
            }

            return ParseResult.SUCCESS
        }

        private fun resolvePath(input: String, pos: Int, limit: Int) {
            var mutablePos = pos
            // Read a delimiter.
            if (mutablePos == limit) {
                // Empty path: keep the base path as-is.
                return
            }
            val c = input[mutablePos]
            if (c == '/' || c == '\\') {
                // Absolute path: reset to the default "/".
                encodedPathSegments.clear()
                encodedPathSegments.add("")
                mutablePos++
            } else {
                // Relative path: clear everything after the last '/'.
                encodedPathSegments[encodedPathSegments.size - 1] = ""
            }

            // Read path segments.
            var i = mutablePos
            while (i < limit) {
                val pathSegmentDelimiterOffset = delimiterOffset(input, i, limit, "/\\")
                val segmentHasTrailingSlash = pathSegmentDelimiterOffset < limit
                push(input, i, pathSegmentDelimiterOffset, segmentHasTrailingSlash, true)
                i = pathSegmentDelimiterOffset
                if (segmentHasTrailingSlash) i++
            }
        }

        /** Adds a path segment. If the input is ".." or equivalent, this pops a path segment.  */
        private fun push(input: String, pos: Int, limit: Int, addTrailingSlash: Boolean, alreadyEncoded: Boolean) {
            val segment = canonicalize(
                input, pos, limit, PATH_SEGMENT_ENCODE_SET, alreadyEncoded, false, asciiOnly = true
            )
            if (isDot(segment)) {
                return  // Skip '.' path segments.
            }
            if (isDotDot(segment)) {
                pop()
                return
            }
            if (encodedPathSegments[encodedPathSegments.size - 1].isEmpty()) {
                encodedPathSegments[encodedPathSegments.size - 1] = segment
            } else {
                encodedPathSegments.add(segment)
            }
            if (addTrailingSlash) {
                encodedPathSegments.add("")
            }
        }

        private fun isDot(input: String): Boolean {
            return input == "." || input.equals("%2e", ignoreCase = true)
        }

        private fun isDotDot(input: String): Boolean {
            return (input == ".."
                    || input.equals("%2e.", ignoreCase = true)
                    || input.equals(".%2e", ignoreCase = true)
                    || input.equals("%2e%2e", ignoreCase = true))
        }

        /**
         * Removes a path segment. When this method returns the last segment is always "", which means
         * the encoded path will have a trailing '/'.
         *
         *
         * Popping "/a/b/c/" yields "/a/b/". In this case the list of path segments goes from
         * ["a", "b", "c", ""] to ["a", "b", ""].
         *
         *
         * Popping "/a/b/c" also yields "/a/b/". The list of path segments goes from ["a", "b", "c"]
         * to ["a", "b", ""].
         */
        private fun pop() {
            val removed = encodedPathSegments.removeAt(encodedPathSegments.size - 1)

            // Make sure the path ends with a '/' by either adding an empty string or clearing a segment.
            if (removed.isEmpty() && !encodedPathSegments.isEmpty()) {
                encodedPathSegments[encodedPathSegments.size - 1] = ""
            } else {
                encodedPathSegments.add("")
            }
        }

        /**
         * Increments `pos` until `input[pos]` is not ASCII whitespace. Stops at `limit`.
         */
        private fun skipLeadingAsciiWhitespace(input: String, pos: Int, limit: Int): Int {
            loop@ for (i in pos until limit) {
                when (input[i]) {
                    '\t', '\n', '\u000c', '\r', ' ' -> continue@loop
                    else -> return i
                }
            }
            return limit
        }

        /**
         * Decrements `limit` until `input[limit - 1]` is not ASCII whitespace. Stops at
         * `pos`.
         */
        private fun skipTrailingAsciiWhitespace(input: String, pos: Int, limit: Int): Int {
            loop@ for (i in limit - 1 downTo pos) {
                when (input[i]) {
                    '\t', '\n', '\u000c', '\r', ' ' -> continue@loop
                    else -> return i + 1
                }
            }
            return pos
        }

        /**
         * Returns the index of the ':' in `input` that is after scheme characters. Returns -1 if
         * `input` does not have a scheme that starts at `pos`.
         */
        private fun schemeDelimiterOffset(input: String, pos: Int, limit: Int): Int {
            if (limit - pos < 2) return -1

            val c0 = input[pos]
            if ((c0 < 'a' || c0 > 'z') && (c0 < 'A' || c0 > 'Z')) return -1 // Not a scheme start char.

            for (i in pos + 1 until limit) {
                val c = input[i]

                return if (c in 'a'..'z'
                    || c in 'A'..'Z'
                    || c in '0'..'9'
                    || c == '+'
                    || c == '-'
                    || c == '.'
                ) {
                    continue // Scheme character. Keep going.
                } else if (c == ':') {
                    i // Scheme prefix!
                } else {
                    -1 // Non-scheme character before the first ':'.
                }
            }

            return -1 // No ':'; doesn't start with a scheme.
        }

        /** Returns the number of '/' and '\' slashes in `input`, starting at `pos`.  */
        private fun slashCount(input: String, pos: Int, limit: Int): Int {
            var mutablePos = pos
            var slashCount = 0
            while (mutablePos < limit) {
                val c = input[mutablePos]
                if (c == '\\' || c == '/') {
                    slashCount++
                    mutablePos++
                } else {
                    break
                }
            }
            return slashCount
        }

        /** Finds the first ':' in `input`, skipping characters between square braces "[...]".  */
        private fun portColonOffset(input: String, pos: Int, limit: Int): Int {
            var i = pos
            while (i < limit) {
                when (input[i]) {
                    '[' -> while (++i < limit) {
                        if (input[i] == ']') break
                    }
                    ':' -> return i
                    else -> {
                    }
                }
                i++
            }
            return limit // No colon.
        }

        private fun canonicalizeHost(input: String, pos: Int, limit: Int): String? {
            // Start by percent decoding the host. The WHATWG spec suggests doing this only after we've
            // checked for IPv6 square braces. But Chrome does it first, and that's more lenient.
            val percentDecoded = percentDecode(input, pos, limit, false)

            // If the input is encased in square braces "[...]", drop 'em. We have an IPv6 address.
            if (percentDecoded.startsWith("[") && percentDecoded.endsWith("]")) {
                val inetAddress = decodeIpv6(percentDecoded, 1, percentDecoded.length - 1) ?: return null
                val address = inetAddress.address
                if (address.size == 16) return inet6AddressToAscii(address)
                throw AssertionError()
            }

            return domainToAscii(percentDecoded)
        }

        /** Decodes an IPv6 address like 1111:2222:3333:4444:5555:6666:7777:8888 or ::1.  */
        private fun decodeIpv6(input: String, pos: Int, limit: Int): InetAddress? {
            val address = ByteArray(16)
            var b = 0
            var compress = -1
            var groupOffset = -1

            var i = pos
            while (i < limit) {
                if (b == address.size) return null // Too many groups.

                // Read a delimiter.
                if (i + 2 <= limit && input.regionMatches(i, "::", 0, 2)) {
                    // Compression "::" delimiter, which is anywhere in the input, including its prefix.
                    if (compress != -1) return null // Multiple "::" delimiters.
                    i += 2
                    b += 2
                    compress = b
                    if (i == limit) break
                } else if (b != 0) {
                    // Group separator ":" delimiter.
                    if (input.regionMatches(i, ":", 0, 1)) {
                        i++
                    } else if (input.regionMatches(i, ".", 0, 1)) {
                        // If we see a '.', rewind to the beginning of the previous group and parse as IPv4.
                        if (!decodeIpv4Suffix(input, groupOffset, limit, address, b - 2)) return null
                        b += 2 // We rewound two bytes and then added four.
                        break
                    } else {
                        return null // Wrong delimiter.
                    }
                }

                // Read a group, one to four hex digits.
                var value = 0
                groupOffset = i
                while (i < limit) {
                    val c = input[i]
                    val hexDigit = decodeHexDigit(c)
                    if (hexDigit == -1) break
                    value = (value shl 4) + hexDigit
                    i++
                }
                val groupLength = i - groupOffset
                if (groupLength == 0 || groupLength > 4) return null // Group is the wrong size.

                // We've successfully read a group. Assign its value to our byte array.
                address[b++] = (value.ushr(8) and 0xff).toByte()
                address[b++] = (value and 0xff).toByte()
            }

            // All done. If compression happened, we need to move bytes to the right place in the
            // address. Here's a sample:
            //
            //      input: "1111:2222:3333::7777:8888"
            //     before: { 11, 11, 22, 22, 33, 33, 00, 00, 77, 77, 88, 88, 00, 00, 00, 00  }
            //   compress: 6
            //          b: 10
            //      after: { 11, 11, 22, 22, 33, 33, 00, 00, 00, 00, 00, 00, 77, 77, 88, 88 }
            //
            if (b != address.size) {
                if (compress == -1) return null // Address didn't have compression or enough groups.
                System.arraycopy(address, compress, address, address.size - (b - compress), b - compress)
                Arrays.fill(address, compress, compress + (address.size - b), 0.toByte())
            }

            try {
                return InetAddress.getByAddress(address)
            } catch (e: UnknownHostException) {
                throw AssertionError()
            }

        }

        /** Decodes an IPv4 address suffix of an IPv6 address, like 1111::5555:6666:192.168.0.1.  */
        private fun decodeIpv4Suffix(
            input: String, pos: Int, limit: Int, address: ByteArray, addressOffset: Int
        ): Boolean {
            var b = addressOffset

            var i = pos
            while (i < limit) {
                if (b == address.size) return false // Too many groups.

                // Read a delimiter.
                if (b != addressOffset) {
                    if (input[i] != '.') return false // Wrong delimiter.
                    i++
                }

                // Read 1 or more decimal digits for a value in 0..255.
                var value = 0
                val groupOffset = i
                while (i < limit) {
                    val c = input[i]
                    if (c < '0' || c > '9') break
                    if (value == 0 && groupOffset != i) return false // Reject unnecessary leading '0's.
                    value = value * 10 + c.toInt() - '0'.toInt()
                    if (value > 255) return false // Value out of range.
                    i++
                }
                val groupLength = i - groupOffset
                if (groupLength == 0) return false // No digits.

                // We've successfully read a byte.
                address[b++] = value.toByte()
            }

            return b == addressOffset + 4 // Too few groups. We wanted exactly four.
            // Success.
        }

        /**
         * Performs IDN ToASCII encoding and canonicalize the result to lowercase. e.g. This converts
         * `☃.net` to `xn--n3h.net`, and `WwW.GoOgLe.cOm` to `www.google.com`.
         * `null` will be returned if the input cannot be ToASCII encoded or if the result
         * contains unsupported ASCII characters.
         */
        private fun domainToAscii(input: String): String? {
            try {
                val result = IDN.toASCII(input).toLowerCase(Locale.US)
                if (result.isEmpty()) return null

                // Confirm that the IDN ToASCII result doesn't contain any illegal characters.
                return if (containsInvalidHostnameAsciiCodes(result)) {
                    null
                } else result
                // TODO: implement all label limits.
            } catch (e: IllegalArgumentException) {
                return null
            }

        }

        private fun containsInvalidHostnameAsciiCodes(hostnameAscii: String): Boolean {
            for (i in 0 until hostnameAscii.length) {
                val c = hostnameAscii[i]
                // The WHATWG Host parsing rules accepts some character codes which are invalid by
                // definition for OkHttp's host header checks (and the WHATWG Host syntax definition). Here
                // we rule out characters that would cause problems in host headers.
                if (c <= '\u001f' || c >= '\u007f') {
                    return true
                }
                // Check for the characters mentioned in the WHATWG Host parsing spec:
                // U+0000, U+0009, U+000A, U+000D, U+0020, "#", "%", "/", ":", "?", "@", "[", "\", and "]"
                // (excluding the characters covered above).
                if (" #%/:?@[\\]".indexOf(c) != -1) {
                    return true
                }
            }
            return false
        }

        private fun inet6AddressToAscii(address: ByteArray): String {
            // Go through the address looking for the longest run of 0s. Each group is 2-bytes.
            var longestRunOffset = -1
            var longestRunLength = 0
            run {
                var i = 0
                while (i < address.size) {
                    val currentRunOffset = i
                    while (i < 16 && address[i].toInt() == 0 && address[i + 1].toInt() == 0) {
                        i += 2
                    }
                    val currentRunLength = i - currentRunOffset
                    if (currentRunLength > longestRunLength) {
                        longestRunOffset = currentRunOffset
                        longestRunLength = currentRunLength
                    }
                    i += 2
                }
            }

            // Emit each 2-byte group in hex, separated by ':'. The longest run of zeroes is "::".
            val result = Buffer()
            var i = 0
            while (i < address.size) {
                if (i == longestRunOffset) {
                    result.writeByte(':'.toInt())
                    i += longestRunLength
                    if (i == 16) result.writeByte(':'.toInt())
                } else {
                    if (i > 0) result.writeByte(':'.toInt())
                    val group = ((address[i].toInt() and 0xff) shl 8) or (address[i + 1].toInt() and 0xff)
                    result.writeHexadecimalUnsignedLong(group.toLong())
                    i += 2
                }
            }
            return result.readUtf8()
        }

        private fun parsePort(input: String, pos: Int, limit: Int): Int {
            return try {
                // Canonicalize the port string to skip '\n' etc.
                val portString =
                    canonicalize(input, pos, limit, "", alreadyEncoded = false, plusIsSpace = false, asciiOnly = true)
                val i = Integer.parseInt(portString)
                if (i in 1..65535) i else -1
            } catch (e: NumberFormatException) {
                -1 // Invalid port.
            }

        }
    }

    private fun percentDecode(list: List<String?>, plusIsSpace: Boolean): List<String?> {
        val result = ArrayList<String?>(list.size)
        for (s in list) {
            result.add(if (s != null) percentDecode(s, plusIsSpace = plusIsSpace) else null)
        }
        return Collections.unmodifiableList(result)
    }

    companion object {
        private val HEX_DIGITS =
            charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
        private const val USERNAME_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#"
        private const val PASSWORD_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#"
        private const val PATH_SEGMENT_ENCODE_SET = " \"<>^`{}|/\\?#"
        private const val PATH_SEGMENT_ENCODE_SET_URI = "[]"
        private const val QUERY_ENCODE_SET = " \"'<>#"
        private const val QUERY_COMPONENT_ENCODE_SET = " \"'<>#&="
        private const val QUERY_COMPONENT_ENCODE_SET_URI = "\\^`{|}"
        private const val CONVERT_TO_URI_ENCODE_SET = "^`{}|\\"
        private const val FORM_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#&!$(),~"
        private const val FRAGMENT_ENCODE_SET = ""
        private const val FRAGMENT_ENCODE_SET_URI = " \"#<>\\^`{|}"

        /**
         * Returns 80 if `scheme.equals("http")`, 443 if `scheme.equals("https")` and -1
         * otherwise.
         */
        internal fun defaultPort(scheme: String): Int {
            return when (scheme) {
                "http" -> 80
                "https" -> 443
                else -> -1
            }
        }

        internal fun pathSegmentsToString(out: StringBuilder, pathSegments: List<String>) {
            var i = 0
            val size = pathSegments.size
            while (i < size) {
                out.append('/')
                out.append(pathSegments[i])
                i++
            }
        }

        internal fun namesAndValuesToQueryString(out: StringBuilder, namesAndValues: List<String?>) {
            var i = 0
            val size = namesAndValues.size
            while (i < size) {
                val name = namesAndValues[i]
                val value = namesAndValues[i + 1]
                if (i > 0) out.append('&')
                out.append(name)
                if (value != null) {
                    out.append('=')
                    out.append(value)
                }
                i += 2
            }
        }

        /**
         * Cuts `encodedQuery` up into alternating parameter names and values. This divides a
         * query string like `subject=math&easy&problem=5-2=3` into the list `["subject",
         * "math", "easy", null, "problem", "5-2=3"]`. Note that values may be null and may contain
         * '=' characters.
         */
        internal fun queryStringToNamesAndValues(encodedQuery: String): MutableList<String?> {
            val result = ArrayList<String?>()
            var pos = 0
            while (pos <= encodedQuery.length) {
                var ampersandOffset = encodedQuery.indexOf('&', pos)
                if (ampersandOffset == -1) ampersandOffset = encodedQuery.length

                val equalsOffset = encodedQuery.indexOf('=', pos)
                if (equalsOffset == -1 || equalsOffset > ampersandOffset) {
                    result.add(encodedQuery.substring(pos, ampersandOffset))
                    result.add(null) // No value for this name.
                } else {
                    result.add(encodedQuery.substring(pos, equalsOffset))
                    result.add(encodedQuery.substring(equalsOffset + 1, ampersandOffset))
                }
                pos = ampersandOffset + 1
            }
            return result
        }

        /**
         * Returns a new `DeepLinkUri` representing `uri` if it is a well-formed
         * URI, or throws an `IllegalArgumentException` if it isn't.
         */
        @JvmStatic
        fun parse(uri: String): DeepLinkUri {
            val builder = Builder()
            val result = builder.parse(null, uri)
            return when (result) {
                ParseResult.SUCCESS -> builder.build()
                else -> throw IllegalArgumentException("Invalid URL: $result for $uri")
            }
        }

        /**
         * Returns a new `DeepLinkUri` representing `uri` if it is a well-formed
         * URI, or null if it isn't.
         */
        @JvmStatic
        fun parseOrNull(uri: String): DeepLinkUri? {
            val builder = Builder()
            val result = builder.parse(null, uri)
            return if (result == ParseResult.SUCCESS) builder.build() else null
        }

        /**
         * Returns an [DeepLinkUri] for `url` or throw.
         *
         * Use `parseOrNull(url.toString())` if you'd prefer a nullable version than throwing.
         */
        @JvmStatic
        fun get(url: URL): DeepLinkUri {
            return parse(url.toString())
        }

        /**
         * Returns an [DeepLinkUri] for `uri` or throw.
         *
         * Use `parseOrNull(uri.toString())` if you'd prefer a nullable version than throwing.
         */
        @JvmStatic
        fun get(uri: URI): DeepLinkUri {
            return parse(uri.toString())
        }

        /**
         * Returns the index of the first character in `input` that contains a character in `delimiters`. Returns limit if there is no such character.
         */
        private fun delimiterOffset(input: String, pos: Int, limit: Int, delimiters: String): Int {
            for (i in pos until limit) {
                if (delimiters.indexOf(input[i]) != -1) return i
            }
            return limit
        }

        @JvmOverloads
        internal fun percentDecode(
            encoded: String,
            pos: Int = 0,
            limit: Int = encoded.length,
            plusIsSpace: Boolean = false
        ): String {
            for (i in pos until limit) {
                val c = encoded[i]
                if (c == '%' || c == '+' && plusIsSpace) {
                    // Slow path: the character at i requires decoding!
                    val out = Buffer()
                    out.writeUtf8(encoded, pos, i)
                    percentDecode(out, encoded, i, limit, plusIsSpace)
                    return out.readUtf8()
                }
            }

            // Fast path: no characters in [pos..limit) required decoding.
            return encoded.substring(pos, limit)
        }

        private fun percentDecode(out: Buffer, encoded: String, pos: Int, limit: Int, plusIsSpace: Boolean) {
            var codePoint: Int
            var i = pos
            while (i < limit) {
                codePoint = encoded.codePointAt(i)
                if (codePoint == '%'.toInt() && i + 2 < limit) {
                    val d1 = decodeHexDigit(encoded[i + 1])
                    val d2 = decodeHexDigit(encoded[i + 2])
                    if (d1 != -1 && d2 != -1) {
                        out.writeByte((d1 shl 4) + d2)
                        i += 2
                        i += Character.charCount(codePoint)
                        continue
                    }
                } else if (codePoint == '+'.toInt() && plusIsSpace) {
                    out.writeByte(' '.toInt())
                    i += Character.charCount(codePoint)
                    continue
                }
                out.writeUtf8CodePoint(codePoint)
                i += Character.charCount(codePoint)
            }
        }

        internal fun decodeHexDigit(c: Char): Int {
            return when (c) {
                in '0'..'9' -> c - '0'
                in 'a'..'f' -> c - 'a' + 10
                in 'A'..'F' -> c - 'A' + 10
                else -> -1
            }
        }

        /**
         * Returns a substring of `input` on the range `[pos..limit)` with the following
         * transformations:
         *
         *  * Tabs, newlines, form feeds and carriage returns are skipped.
         *  * In queries, ' ' is encoded to '+' and '+' is encoded to "%2B".
         *  * Characters in `encodeSet` are percent-encoded.
         *  * Control characters and non-ASCII characters are percent-encoded.
         *  * All other characters are copied without transformation.
         *
         *
         * @param alreadyEncoded true to leave '%' as-is; false to convert it to '%25'.
         * @param plusIsSpace true to encode '+' as "%2B" if it is not already encoded.
         * @param asciiOnly true to encode all non-ASCII codepoints.
         */
        internal fun canonicalize(
            input: String, pos: Int, limit: Int, encodeSet: String,
            alreadyEncoded: Boolean, plusIsSpace: Boolean, asciiOnly: Boolean
        ): String {
            var codePoint: Int
            var i = pos
            while (i < limit) {
                codePoint = input.codePointAt(i)
                if (codePoint < 0x20
                    || codePoint == 0x7f
                    || (codePoint >= 0x80 && asciiOnly)
                    || encodeSet.indexOf(codePoint.toChar()) != -1
                    || codePoint == '%'.toInt() && !alreadyEncoded
                    || codePoint == '+'.toInt() && plusIsSpace
                ) {
                    // Slow path: the character at i requires encoding!
                    val out = Buffer()
                    out.writeUtf8(input, pos, i)
                    canonicalize(out, input, i, limit, encodeSet, alreadyEncoded, plusIsSpace, asciiOnly)
                    return out.readUtf8()
                }
                i += Character.charCount(codePoint)
            }

            // Fast path: no characters in [pos..limit) required encoding.
            return input.substring(pos, limit)
        }

        private fun canonicalize(
            out: Buffer, input: String, pos: Int, limit: Int,
            encodeSet: String, alreadyEncoded: Boolean, plusIsSpace: Boolean, asciiOnly: Boolean
        ) {
            var utf8Buffer: Buffer? = null // Lazily allocated.
            var codePoint: Int
            var i = pos
            while (i < limit) {
                codePoint = input.codePointAt(i)
                if (alreadyEncoded && (codePoint == '\t'.toInt() || codePoint == '\n'.toInt() || codePoint == '\u000c'.toInt() || codePoint == '\r'.toInt())) {
                    // Skip this character.
                } else if (codePoint == '+'.toInt() && plusIsSpace) {
                    // Encode '+' as '%2B' since we permit ' ' to be encoded as either '+' or '%20'.
                    out.writeUtf8(if (alreadyEncoded) "+" else "%2B")
                } else if (codePoint < 0x20
                    || codePoint == 0x7f
                    || (codePoint >= 0x80 && asciiOnly)
                    || encodeSet.indexOf(codePoint.toChar()) != -1
                    || codePoint == '%'.toInt() && !alreadyEncoded
                ) {
                    // Percent encode this character.
                    if (utf8Buffer == null) {
                        utf8Buffer = Buffer()
                    }
                    utf8Buffer.writeUtf8CodePoint(codePoint)
                    while (!utf8Buffer.exhausted()) {
                        val b = utf8Buffer.readByte().toInt() and 0xff
                        out.writeByte('%'.toInt())
                        out.writeByte(HEX_DIGITS[b shr 4 and 0xf].toInt())
                        out.writeByte(HEX_DIGITS[b and 0xf].toInt())
                    }
                } else {
                    // This character doesn't need encoding. Just copy it over.
                    out.writeUtf8CodePoint(codePoint)
                }
                i += Character.charCount(codePoint)
            }
        }

        internal fun canonicalize(
            input: String,
            encodeSet: String,
            alreadyEncoded: Boolean,
            plusIsSpace: Boolean,
            asciiOnly: Boolean
        ): String {
            return canonicalize(input, 0, input.length, encodeSet, alreadyEncoded, plusIsSpace, asciiOnly)
        }
    }
}
