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
package com.hellofresh.deeplink

import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URI
import java.net.URL
import java.util.Arrays
import java.util.LinkedHashSet
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.fail

@RunWith(Parameterized::class)
class DeepLinkUriTest {

    @JvmField
    @Parameterized.Parameter
    var useGet: Boolean = false
    
    private fun parse(link: String): DeepLinkUri? {
        return when {
            useGet -> DeepLinkUri.parse(link)
            else -> DeepLinkUri.parseOrNull(link)
        }
    }

    @Test
    fun parseTrimsAsciiWhitespace() {
        val expected = parse("http://host/")
        assertEquals(expected, DeepLinkUri.parse("http://host/\u000c\n\t \r")) // Leading.
        assertEquals(expected, DeepLinkUri.parse("\r\n\u000c \thttp://host/")) // Trailing.
        assertEquals(expected, DeepLinkUri.parse(" http://host/ ")) // Both.
        assertEquals(expected, DeepLinkUri.parse("    http://host/    ")) // Both.
        assertEquals(expected, DeepLinkUri.parse("http://host/").resolve("   "))
        assertEquals(expected, DeepLinkUri.parse("http://host/").resolve("  .  "))
    }

    @Test
    fun parseHostAsciiNonPrintable() {
        val host = "host\u0001"
        assertInvalid("http://$host/", "Invalid URL host: \"host\u0001\"")
        // TODO make exception message escape non-printable characters
    }

    @Test
    fun parseDoesNotTrimOtherWhitespaceCharacters() {
        // Whitespace characters list from Google's Guava team: http://goo.gl/IcR9RD
        assertEquals("/%0B", DeepLinkUri.parse("http://h/\u000b").encodedPath()) // line tabulation
        assertEquals("/%1C", DeepLinkUri.parse("http://h/\u001c").encodedPath()) // information separator 4
        assertEquals("/%1D", DeepLinkUri.parse("http://h/\u001d").encodedPath()) // information separator 3
        assertEquals("/%1E", DeepLinkUri.parse("http://h/\u001e").encodedPath()) // information separator 2
        assertEquals("/%1F", DeepLinkUri.parse("http://h/\u001f").encodedPath()) // information separator 1
        assertEquals("/%C2%85", DeepLinkUri.parse("http://h/\u0085").encodedPath()) // next line
        assertEquals("/%C2%A0", DeepLinkUri.parse("http://h/\u00a0").encodedPath()) // non-breaking space
        assertEquals("/%E1%9A%80", DeepLinkUri.parse("http://h/\u1680").encodedPath()) // ogham space mark
        assertEquals("/%E1%A0%8E", DeepLinkUri.parse("http://h/\u180e").encodedPath()) // mongolian vowel separator
        assertEquals("/%E2%80%80", DeepLinkUri.parse("http://h/\u2000").encodedPath()) // en quad
        assertEquals("/%E2%80%81", DeepLinkUri.parse("http://h/\u2001").encodedPath()) // em quad
        assertEquals("/%E2%80%82", DeepLinkUri.parse("http://h/\u2002").encodedPath()) // en space
        assertEquals("/%E2%80%83", DeepLinkUri.parse("http://h/\u2003").encodedPath()) // em space
        assertEquals("/%E2%80%84", DeepLinkUri.parse("http://h/\u2004").encodedPath()) // three-per-em space
        assertEquals("/%E2%80%85", DeepLinkUri.parse("http://h/\u2005").encodedPath()) // four-per-em space
        assertEquals("/%E2%80%86", DeepLinkUri.parse("http://h/\u2006").encodedPath()) // six-per-em space
        assertEquals("/%E2%80%87", DeepLinkUri.parse("http://h/\u2007").encodedPath()) // figure space
        assertEquals("/%E2%80%88", DeepLinkUri.parse("http://h/\u2008").encodedPath()) // punctuation space
        assertEquals("/%E2%80%89", DeepLinkUri.parse("http://h/\u2009").encodedPath()) // thin space
        assertEquals("/%E2%80%8A", DeepLinkUri.parse("http://h/\u200a").encodedPath()) // hair space
        assertEquals("/%E2%80%8B", DeepLinkUri.parse("http://h/\u200b").encodedPath()) // zero-width space
        assertEquals("/%E2%80%8C", DeepLinkUri.parse("http://h/\u200c").encodedPath()) // zero-width non-joiner
        assertEquals("/%E2%80%8D", DeepLinkUri.parse("http://h/\u200d").encodedPath()) // zero-width joiner
        assertEquals("/%E2%80%8E", DeepLinkUri.parse("http://h/\u200e").encodedPath()) // left-to-right mark
        assertEquals("/%E2%80%8F", DeepLinkUri.parse("http://h/\u200f").encodedPath()) // right-to-left mark
        assertEquals("/%E2%80%A8", DeepLinkUri.parse("http://h/\u2028").encodedPath()) // line separator
        assertEquals("/%E2%80%A9", DeepLinkUri.parse("http://h/\u2029").encodedPath()) // paragraph separator
        assertEquals("/%E2%80%AF", DeepLinkUri.parse("http://h/\u202f").encodedPath()) // narrow non-breaking space
        assertEquals("/%E2%81%9F", DeepLinkUri.parse("http://h/\u205f").encodedPath()) // medium mathematical space
        assertEquals("/%E3%80%80", DeepLinkUri.parse("http://h/\u3000").encodedPath()) // ideographic space
    }

    @Test
    fun scheme() {
        assertEquals(parse("http://host/"), DeepLinkUri.parse("http://host/"))
        assertEquals(parse("http://host/"), DeepLinkUri.parse("Http://host/"))
        assertEquals(parse("http://host/"), DeepLinkUri.parse("http://host/"))
        assertEquals(parse("http://host/"), DeepLinkUri.parse("HTTP://host/"))
        assertEquals(parse("https://host/"), DeepLinkUri.parse("https://host/"))
        assertEquals(parse("https://host/"), DeepLinkUri.parse("HTTPS://host/"))

        assertEquals(parse("image640://480.png"), DeepLinkUri.parse("image640://480.png"))
        assertEquals(parse("httpp://host/"), DeepLinkUri.parse("httpp://host/"))
        assertEquals(parse("ht+tp://host/"), DeepLinkUri.parse("ht+tp://host/"))
        assertEquals(parse("ht.tp://host/"), DeepLinkUri.parse("ht.tp://host/"))
        assertEquals(parse("ht-tp://host/"), DeepLinkUri.parse("ht-tp://host/"))
        assertEquals(parse("ht1tp://host/"), DeepLinkUri.parse("ht1tp://host/"))
        assertEquals(parse("httpss://host/"), DeepLinkUri.parse("httpss://host/"))

        assertInvalid("0ttp://host/", "Expected URL scheme 'http' or 'https' but no colon was found")
    }

    @Test
    fun parseNoScheme() {
        assertInvalid("//host", "Expected URL scheme 'http' or 'https' but no colon was found")
        assertInvalid("/path", "Expected URL scheme 'http' or 'https' but no colon was found")
        assertInvalid("path", "Expected URL scheme 'http' or 'https' but no colon was found")
        assertInvalid("?query", "Expected URL scheme 'http' or 'https' but no colon was found")
        assertInvalid("#fragment", "Expected URL scheme 'http' or 'https' but no colon was found")
    }

    @Test
    fun newBuilderResolve() {
        // Non-exhaustive tests because implementation is the same as resolve.
        val base = DeepLinkUri.parse("http://host/a/b")
        assertEquals(parse("https://host2/"), base.resolve("https://host2"))
        assertEquals(parse("http://host2/"), base.resolve("//host2"))
        assertEquals(parse("http://host/path"), base.resolve("/path"))
        assertEquals(parse("http://host/a/path"), base.resolve("path"))
        assertEquals(parse("http://host/a/b?query"), base.resolve("?query"))
        assertEquals(parse("http://host/a/b#fragment"), base.resolve("#fragment"))
        assertEquals(parse("http://host/a/b"), base.resolve(""))

        assertEquals(parse("ftp://b"), base.resolve("ftp://b"))
        assertEquals(parse("ht+tp://b"), base.resolve("ht+tp://b"))
        assertEquals(parse("ht-tp://b"), base.resolve("ht-tp://b"))
        assertEquals(parse("ht.tp://b"), base.resolve("ht.tp://b"))
    }

    @Test
    fun resolveNoScheme() {
        val base = DeepLinkUri.parse("http://host/a/b")
        assertEquals(parse("http://host2/"), base.resolve("//host2"))
        assertEquals(parse("http://host/path"), base.resolve("/path"))
        assertEquals(parse("http://host/a/path"), base.resolve("path"))
        assertEquals(parse("http://host/a/b?query"), base.resolve("?query"))
        assertEquals(parse("http://host/a/b#fragment"), base.resolve("#fragment"))
        assertEquals(parse("http://host/a/b"), base.resolve(""))
        assertEquals(parse("http://host/path"), base.resolve("\\path"))
    }

    @Test
    fun resolveCustomScheme() {
        val base = DeepLinkUri.parse("http://a/")
        assertEquals(parse("ftp://b"), base.resolve("ftp://b"))
        assertEquals(parse("ht+tp://b"), base.resolve("ht+tp://b"))
        assertEquals(parse("ht-tp://b"), base.resolve("ht-tp://b"))
        assertEquals(parse("ht.tp://b"), base.resolve("ht.tp://b"))
    }

    @Test
    fun resolveSchemeLikePath() {
        val base = DeepLinkUri.parse("http://a/")
        assertEquals(parse("http://a/http//b/"), base.resolve("http//b/"))
        assertEquals(parse("http://a/ht+tp//b/"), base.resolve("ht+tp//b/"))
        assertEquals(parse("http://a/ht-tp//b/"), base.resolve("ht-tp//b/"))
        assertEquals(parse("http://a/ht.tp//b/"), base.resolve("ht.tp//b/"))
    }

    /** https://tools.ietf.org/html/rfc3986#section-5.4.1  */
    @Test
    fun rfc3886NormalExamples() {
        val url = DeepLinkUri.parse("http://a/b/c/d;p?q")
        assertEquals(parse("g://h/"), url.resolve("g:h"))
        assertEquals(parse("http://a/b/c/g"), url.resolve("g"))
        assertEquals(parse("http://a/b/c/g"), url.resolve("./g"))
        assertEquals(parse("http://a/b/c/g/"), url.resolve("g/"))
        assertEquals(parse("http://a/g"), url.resolve("/g"))
        assertEquals(parse("http://g"), url.resolve("//g"))
        assertEquals(parse("http://a/b/c/d;p?y"), url.resolve("?y"))
        assertEquals(parse("http://a/b/c/g?y"), url.resolve("g?y"))
        assertEquals(parse("http://a/b/c/d;p?q#s"), url.resolve("#s"))
        assertEquals(parse("http://a/b/c/g#s"), url.resolve("g#s"))
        assertEquals(parse("http://a/b/c/g?y#s"), url.resolve("g?y#s"))
        assertEquals(parse("http://a/b/c/;x"), url.resolve(";x"))
        assertEquals(parse("http://a/b/c/g;x"), url.resolve("g;x"))
        assertEquals(parse("http://a/b/c/g;x?y#s"), url.resolve("g;x?y#s"))
        assertEquals(parse("http://a/b/c/d;p?q"), url.resolve(""))
        assertEquals(parse("http://a/b/c/"), url.resolve("."))
        assertEquals(parse("http://a/b/c/"), url.resolve("./"))
        assertEquals(parse("http://a/b/"), url.resolve(".."))
        assertEquals(parse("http://a/b/"), url.resolve("../"))
        assertEquals(parse("http://a/b/g"), url.resolve("../g"))
        assertEquals(parse("http://a/"), url.resolve("../.."))
        assertEquals(parse("http://a/"), url.resolve("../../"))
        assertEquals(parse("http://a/g"), url.resolve("../../g"))
    }

    /** https://tools.ietf.org/html/rfc3986#section-5.4.2  */
    @Test
    fun rfc3886AbnormalExamples() {
        val url = DeepLinkUri.parse("http://a/b/c/d;p?q")
        assertEquals(parse("http://a/g"), url.resolve("../../../g"))
        assertEquals(parse("http://a/g"), url.resolve("../../../../g"))
        assertEquals(parse("http://a/g"), url.resolve("/./g"))
        assertEquals(parse("http://a/g"), url.resolve("/../g"))
        assertEquals(parse("http://a/b/c/g."), url.resolve("g."))
        assertEquals(parse("http://a/b/c/.g"), url.resolve(".g"))
        assertEquals(parse("http://a/b/c/g.."), url.resolve("g.."))
        assertEquals(parse("http://a/b/c/..g"), url.resolve("..g"))
        assertEquals(parse("http://a/b/g"), url.resolve("./../g"))
        assertEquals(parse("http://a/b/c/g/"), url.resolve("./g/."))
        assertEquals(parse("http://a/b/c/g/h"), url.resolve("g/./h"))
        assertEquals(parse("http://a/b/c/h"), url.resolve("g/../h"))
        assertEquals(parse("http://a/b/c/g;x=1/y"), url.resolve("g;x=1/./y"))
        assertEquals(parse("http://a/b/c/y"), url.resolve("g;x=1/../y"))
        assertEquals(parse("http://a/b/c/g?y/./x"), url.resolve("g?y/./x"))
        assertEquals(parse("http://a/b/c/g?y/../x"), url.resolve("g?y/../x"))
        assertEquals(parse("http://a/b/c/g#s/./x"), url.resolve("g#s/./x"))
        assertEquals(parse("http://a/b/c/g#s/../x"), url.resolve("g#s/../x"))
        assertEquals(parse("http://a/b/c/g"), url.resolve("http:g")) // "http:g" also okay.
    }

    @Test
    fun parseAuthoritySlashCountDoesntMatter() {
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:/host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:\\host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http://host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:\\/host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:/\\host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:\\\\host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:///host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:\\//host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:/\\/host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http://\\host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:\\\\/host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:/\\\\host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:\\\\\\host/path"))
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http:////host/path"))
    }

    @Test
    fun resolveAuthoritySlashCountDoesntMatterWithDifferentScheme() {
        val base = DeepLinkUri.parse("https://a/b/c")
        assertEquals(parse("http://host/path"), base.resolve("http:host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:/host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http://host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\/host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:/\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:///host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\//host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:/\\/host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http://\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\\\/host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:/\\\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\\\\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:////host/path"))
    }

    @Test
    fun resolveAuthoritySlashCountMattersWithSameScheme() {
        val base = DeepLinkUri.parse("http://a/b/c")
        assertEquals(parse("http://a/b/host/path"), base.resolve("http:host/path"))
        assertEquals(parse("http://a/host/path"), base.resolve("http:/host/path"))
        assertEquals(parse("http://a/host/path"), base.resolve("http:\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http://host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\/host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:/\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:///host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\//host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:/\\/host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http://\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\\\/host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:/\\\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:\\\\\\host/path"))
        assertEquals(parse("http://host/path"), base.resolve("http:////host/path"))
    }

    @Test
    fun username() {
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http://@host/path"))
        assertEquals(parse("http://user@host/path"), DeepLinkUri.parse("http://user@host/path"))
    }

    /** Given multiple '@' characters, the last one is the delimiter.  */
    @Test
    fun authorityWithMultipleAtSigns() {
        val deepLinkUri = DeepLinkUri.parse("http://foo@bar@baz/path")
        assertEquals("foo@bar", deepLinkUri.username())
        assertEquals("", deepLinkUri.password())
        assertEquals(parse("http://foo%40bar@baz/path"), deepLinkUri)
    }

    /** Given multiple ':' characters, the first one is the delimiter.  */
    @Test
    fun authorityWithMultipleColons() {
        val deepLinkUri = DeepLinkUri.parse("http://foo:pass1@bar:pass2@baz/path")
        assertEquals("foo", deepLinkUri.username())
        assertEquals("pass1@bar:pass2", deepLinkUri.password())
        assertEquals(parse("http://foo:pass1%40bar%3Apass2@baz/path"), deepLinkUri)
    }

    @Test
    fun usernameAndPassword() {
        assertEquals(
            parse("http://username:password@host/path"),
            DeepLinkUri.parse("http://username:password@host/path")
        )
        assertEquals(
            parse("http://username@host/path"),
            DeepLinkUri.parse("http://username:@host/path")
        )
    }

    @Test
    fun passwordWithEmptyUsername() {
        // Chrome doesn't mind, but Firefox rejects URLs with empty usernames and non-empty passwords.
        assertEquals(parse("http://host/path"), DeepLinkUri.parse("http://:@host/path"))
        assertEquals("password%40", DeepLinkUri.parse("http://:password@@host/path").encodedPassword())
    }

    @Test
    fun unprintableCharactersArePercentEncoded() {
        assertEquals("/%00", DeepLinkUri.parse("http://host/\u0000").encodedPath())
        assertEquals("/%08", DeepLinkUri.parse("http://host/\u0008").encodedPath())
        assertEquals("/%EF%BF%BD", DeepLinkUri.parse("http://host/\ufffd").encodedPath())
    }

    @Test
    fun hostContainsIllegalCharacter() {
        assertInvalid("http://\n/", "Invalid URL host: \"\n\"")
        assertInvalid("http:// /", "Invalid URL host: \" \"")
        assertInvalid("http://%20/", "Invalid URL host: \"%20\"")
    }

    @Test
    fun hostnameLowercaseCharactersMappedDirectly() {
        assertEquals("abcd", DeepLinkUri.parse("http://abcd").host())
        assertEquals("xn--4xa", DeepLinkUri.parse("http://σ").host())
    }

    @Test
    fun hostnameUppercaseCharactersConvertedToLowercase() {
        assertEquals("abcd", DeepLinkUri.parse("http://ABCD").host())
        assertEquals("xn--4xa", DeepLinkUri.parse("http://Σ").host())
    }

    @Test
    fun hostnameIgnoredCharacters() {
        // The soft hyphen (­) should be ignored.
        assertEquals("abcd", DeepLinkUri.parse("http://AB\u00adCD").host())
    }

    @Test
    fun hostnameMultipleCharacterMapping() {
        // Map the single character telephone symbol (℡) to the string "tel".
        assertEquals("tel", DeepLinkUri.parse("http://\u2121").host())
    }

    @Test
    fun hostnameMappingLastMappedCodePoint() {
        assertEquals("xn--pu5l", DeepLinkUri.parse("http://\uD87E\uDE1D").host())
    }

    @Ignore("The java.net.IDN implementation doesn't ignore characters that it should.")
    @Test
    fun hostnameMappingLastIgnoredCodePoint() {
        assertEquals("abcd", DeepLinkUri.parse("http://ab\uDB40\uDDEFcd").host())
    }

    @Test
    fun hostnameMappingLastDisallowedCodePoint() {
        assertInvalid("http://\uDBFF\uDFFF", "Invalid URL host: \"\uDBFF\uDFFF\"")
    }

    @Test
    fun hostIpv6() {
        // Square braces are absent from host()...
        assertEquals("::1", DeepLinkUri.parse("http://[::1]/").host())

        // ... but they're included in toString().
        assertEquals("http://[::1]/", DeepLinkUri.parse("http://[::1]/").toString())

        // IPv6 colons don't interfere with port numbers or passwords.
        assertEquals(8080, DeepLinkUri.parse("http://[::1]:8080/").port().toLong())
        assertEquals("password", DeepLinkUri.parse("http://user:password@[::1]/").password())
        assertEquals("::1", DeepLinkUri.parse("http://user:password@[::1]:8080/").host())

        // Permit the contents of IPv6 addresses to be percent-encoded...
        assertEquals("::1", DeepLinkUri.parse("http://[%3A%3A%31]/").host())

        // Including the Square braces themselves! (This is what Chrome does.)
        assertEquals("::1", DeepLinkUri.parse("http://%5B%3A%3A1%5D/").host())
    }

    @Test
    fun hostIpv6AddressDifferentFormats() {
        // Multiple representations of the same address; see http://tools.ietf.org/html/rfc5952.
        val a3 = "2001:db8::1:0:0:1"
        assertEquals(a3, DeepLinkUri.parse("http://[2001:db8:0:0:1:0:0:1]").host())
        assertEquals(a3, DeepLinkUri.parse("http://[2001:0db8:0:0:1:0:0:1]").host())
        assertEquals(a3, DeepLinkUri.parse("http://[2001:db8::1:0:0:1]").host())
        assertEquals(a3, DeepLinkUri.parse("http://[2001:db8::0:1:0:0:1]").host())
        assertEquals(a3, DeepLinkUri.parse("http://[2001:0db8::1:0:0:1]").host())
        assertEquals(a3, DeepLinkUri.parse("http://[2001:db8:0:0:1::1]").host())
        assertEquals(a3, DeepLinkUri.parse("http://[2001:db8:0000:0:1::1]").host())
        assertEquals(a3, DeepLinkUri.parse("http://[2001:DB8:0:0:1::1]").host())
    }

    @Test
    fun hostIpv6AddressLeadingCompression() {
        assertEquals("::1", DeepLinkUri.parse("http://[::0001]").host())
        assertEquals("::1", DeepLinkUri.parse("http://[0000::0001]").host())
        assertEquals("::1", DeepLinkUri.parse("http://[0000:0000:0000:0000:0000:0000:0000:0001]").host())
        assertEquals("::1", DeepLinkUri.parse("http://[0000:0000:0000:0000:0000:0000::0001]").host())
    }

    @Test
    fun hostIpv6AddressTrailingCompression() {
        assertEquals("1::", DeepLinkUri.parse("http://[0001:0000::]").host())
        assertEquals("1::", DeepLinkUri.parse("http://[0001::0000]").host())
        assertEquals("1::", DeepLinkUri.parse("http://[0001::]").host())
        assertEquals("1::", DeepLinkUri.parse("http://[1::]").host())
    }

    @Test
    fun hostIpv6AddressTooManyDigitsInGroup() {
        assertInvalid(
            "http://[00000:0000:0000:0000:0000:0000:0000:0001]",
            "Invalid URL host: \"[00000:0000:0000:0000:0000:0000:0000:0001]\""
        )
        assertInvalid("http://[::00001]", "Invalid URL host: \"[::00001]\"")
    }

    @Test
    fun hostIpv6AddressMisplacedColons() {
        assertInvalid(
            "http://[:0000:0000:0000:0000:0000:0000:0000:0001]",
            "Invalid URL host: \"[:0000:0000:0000:0000:0000:0000:0000:0001]\""
        )
        assertInvalid(
            "http://[:::0000:0000:0000:0000:0000:0000:0000:0001]",
            "Invalid URL host: \"[:::0000:0000:0000:0000:0000:0000:0000:0001]\""
        )
        assertInvalid("http://[:1]", "Invalid URL host: \"[:1]\"")
        assertInvalid("http://[:::1]", "Invalid URL host: \"[:::1]\"")
        assertInvalid(
            "http://[0000:0000:0000:0000:0000:0000:0001:]",
            "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0001:]\""
        )
        assertInvalid(
            "http://[0000:0000:0000:0000:0000:0000:0000:0001:]",
            "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0000:0001:]\""
        )
        assertInvalid(
            "http://[0000:0000:0000:0000:0000:0000:0000:0001::]",
            "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0000:0001::]\""
        )
        assertInvalid(
            "http://[0000:0000:0000:0000:0000:0000:0000:0001:::]",
            "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0000:0001:::]\""
        )
        assertInvalid("http://[1:]", "Invalid URL host: \"[1:]\"")
        assertInvalid("http://[1:::]", "Invalid URL host: \"[1:::]\"")
        assertInvalid("http://[1:::1]", "Invalid URL host: \"[1:::1]\"")
        assertInvalid(
            "http://[0000:0000:0000:0000::0000:0000:0000:0001]",
            "Invalid URL host: \"[0000:0000:0000:0000::0000:0000:0000:0001]\""
        )
    }

    @Test
    fun hostIpv6AddressTooManyGroups() {
        assertInvalid(
            "http://[0000:0000:0000:0000:0000:0000:0000:0000:0001]",
            "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0000:0000:0001]\""
        )
    }

    @Test
    fun hostIpv6AddressTooMuchCompression() {
        assertInvalid(
            "http://[0000::0000:0000:0000:0000::0001]",
            "Invalid URL host: \"[0000::0000:0000:0000:0000::0001]\""
        )
        assertInvalid(
            "http://[::0000:0000:0000:0000::0001]",
            "Invalid URL host: \"[::0000:0000:0000:0000::0001]\""
        )
    }

    @Test
    fun hostIpv6ScopedAddress() {
        // java.net.InetAddress parses scoped addresses. These aren't valid in URLs.
        assertInvalid("http://[::1%2544]", "Invalid URL host: \"[::1%2544]\"")
    }

    @Test
    fun hostIpv6AddressTooManyLeadingZeros() {
        // Guava's been buggy on this case. https://github.com/google/guava/issues/3116
        assertInvalid(
            "http://[2001:db8:0:0:1:0:0:00001]",
            "Invalid URL host: \"[2001:db8:0:0:1:0:0:00001]\""
        )
    }

    @Test
    fun hostIpv6WithIpv4Suffix() {
        assertEquals("::1:ffff:ffff", DeepLinkUri.parse("http://[::1:255.255.255.255]/").host())
        assertEquals("::1:0:0", DeepLinkUri.parse("http://[0:0:0:0:0:1:0.0.0.0]/").host())
    }

    @Test
    fun hostIpv6WithIpv4SuffixWithOctalPrefix() {
        // Chrome interprets a leading '0' as octal; Firefox rejects them. (We reject them.)
        assertInvalid(
            "http://[0:0:0:0:0:1:0.0.0.000000]/",
            "Invalid URL host: \"[0:0:0:0:0:1:0.0.0.000000]\""
        )
        assertInvalid(
            "http://[0:0:0:0:0:1:0.010.0.010]/",
            "Invalid URL host: \"[0:0:0:0:0:1:0.010.0.010]\""
        )
        assertInvalid(
            "http://[0:0:0:0:0:1:0.0.0.000001]/",
            "Invalid URL host: \"[0:0:0:0:0:1:0.0.0.000001]\""
        )
    }

    @Test
    fun hostIpv6WithIpv4SuffixWithHexadecimalPrefix() {
        // Chrome interprets a leading '0x' as hexadecimal; Firefox rejects them. (We reject them.)
        assertInvalid(
            "http://[0:0:0:0:0:1:0.0x10.0.0x10]/",
            "Invalid URL host: \"[0:0:0:0:0:1:0.0x10.0.0x10]\""
        )
    }

    @Test
    fun hostIpv6WithMalformedIpv4Suffix() {
        assertInvalid("http://[0:0:0:0:0:1:0.0:0.0]/", "Invalid URL host: \"[0:0:0:0:0:1:0.0:0.0]\"")
        assertInvalid("http://[0:0:0:0:0:1:0.0-0.0]/", "Invalid URL host: \"[0:0:0:0:0:1:0.0-0.0]\"")
        assertInvalid(
            "http://[0:0:0:0:0:1:.255.255.255]/",
            "Invalid URL host: \"[0:0:0:0:0:1:.255.255.255]\""
        )
        assertInvalid(
            "http://[0:0:0:0:0:1:255..255.255]/",
            "Invalid URL host: \"[0:0:0:0:0:1:255..255.255]\""
        )
        assertInvalid(
            "http://[0:0:0:0:0:1:255.255..255]/",
            "Invalid URL host: \"[0:0:0:0:0:1:255.255..255]\""
        )
        assertInvalid(
            "http://[0:0:0:0:0:0:1:255.255]/",
            "Invalid URL host: \"[0:0:0:0:0:0:1:255.255]\""
        )
        assertInvalid(
            "http://[0:0:0:0:0:1:256.255.255.255]/",
            "Invalid URL host: \"[0:0:0:0:0:1:256.255.255.255]\""
        )
        assertInvalid(
            "http://[0:0:0:0:0:1:ff.255.255.255]/",
            "Invalid URL host: \"[0:0:0:0:0:1:ff.255.255.255]\""
        )
        assertInvalid(
            "http://[0:0:0:0:0:0:1:255.255.255.255]/",
            "Invalid URL host: \"[0:0:0:0:0:0:1:255.255.255.255]\""
        )
        assertInvalid(
            "http://[0:0:0:0:1:255.255.255.255]/",
            "Invalid URL host: \"[0:0:0:0:1:255.255.255.255]\""
        )
        assertInvalid("http://[0:0:0:0:1:0.0.0.0:1]/", "Invalid URL host: \"[0:0:0:0:1:0.0.0.0:1]\"")
        assertInvalid(
            "http://[0:0.0.0.0:1:0:0:0:0:1]/",
            "Invalid URL host: \"[0:0.0.0.0:1:0:0:0:0:1]\""
        )
        assertInvalid("http://[0.0.0.0:0:0:0:0:0:1]/", "Invalid URL host: \"[0.0.0.0:0:0:0:0:0:1]\"")
    }

    @Test
    fun hostIpv6WithIncompleteIpv4Suffix() {
        // To Chrome & Safari these are well-formed; Firefox disagrees. (We're consistent with Firefox).
        assertInvalid(
            "http://[0:0:0:0:0:1:255.255.255.]/",
            "Invalid URL host: \"[0:0:0:0:0:1:255.255.255.]\""
        )
        assertInvalid(
            "http://[0:0:0:0:0:1:255.255.255]/",
            "Invalid URL host: \"[0:0:0:0:0:1:255.255.255]\""
        )
    }

    @Test
    fun hostIpv6Malformed() {
        assertInvalid("http://[::g]/", "Invalid URL host: \"[::g]\"")
    }

    @Test
    fun hostIpv6CanonicalForm() {
        assertEquals(
            "abcd:ef01:2345:6789:abcd:ef01:2345:6789",
            DeepLinkUri.parse("http://[abcd:ef01:2345:6789:abcd:ef01:2345:6789]/").host()
        )
        assertEquals("a::b:0:0:0", DeepLinkUri.parse("http://[a:0:0:0:b:0:0:0]/").host())
        assertEquals("a:b:0:0:c::", DeepLinkUri.parse("http://[a:b:0:0:c:0:0:0]/").host())
        assertEquals("a:b::c:0:0", DeepLinkUri.parse("http://[a:b:0:0:0:c:0:0]/").host())
        assertEquals("a::b:0:0:0", DeepLinkUri.parse("http://[a:0:0:0:b:0:0:0]/").host())
        assertEquals("::a:b:0:0:0", DeepLinkUri.parse("http://[0:0:0:a:b:0:0:0]/").host())
        assertEquals("::a:0:0:0:b", DeepLinkUri.parse("http://[0:0:0:a:0:0:0:b]/").host())
        assertEquals("0:a:b:c:d:e:f:1", DeepLinkUri.parse("http://[0:a:b:c:d:e:f:1]/").host())
        assertEquals("a:b:c:d:e:f:1:0", DeepLinkUri.parse("http://[a:b:c:d:e:f:1:0]/").host())
        assertEquals("ff01::101", DeepLinkUri.parse("http://[FF01:0:0:0:0:0:0:101]/").host())
        assertEquals("2001:db8::1", DeepLinkUri.parse("http://[2001:db8::1]/").host())
        assertEquals("2001:db8::2:1", DeepLinkUri.parse("http://[2001:db8:0:0:0:0:2:1]/").host())
        assertEquals("2001:db8:0:1:1:1:1:1", DeepLinkUri.parse("http://[2001:db8:0:1:1:1:1:1]/").host())
        assertEquals("2001:db8::1:0:0:1", DeepLinkUri.parse("http://[2001:db8:0:0:1:0:0:1]/").host())
        assertEquals("2001:0:0:1::1", DeepLinkUri.parse("http://[2001:0:0:1:0:0:0:1]/").host())
        assertEquals("1::", DeepLinkUri.parse("http://[1:0:0:0:0:0:0:0]/").host())
        assertEquals("::1", DeepLinkUri.parse("http://[0:0:0:0:0:0:0:1]/").host())
        assertEquals("::", DeepLinkUri.parse("http://[0:0:0:0:0:0:0:0]/").host())
        assertEquals("192.168.1.254", DeepLinkUri.parse("http://[::ffff:c0a8:1fe]/").host())
    }

    /** The builder permits square braces but does not require them.  */
    @Test
    fun hostIpv6Builder() {
        val base = DeepLinkUri.parse("http://example.com/")
        assertEquals("http://[::1]/", base.newBuilder().host("[::1]").build().toString())
        assertEquals("http://[::1]/", base.newBuilder().host("[::0001]").build().toString())
        assertEquals("http://[::1]/", base.newBuilder().host("::1").build().toString())
        assertEquals("http://[::1]/", base.newBuilder().host("::0001").build().toString())
    }

    @Test
    fun hostIpv4CanonicalForm() {
        assertEquals("255.255.255.255", DeepLinkUri.parse("http://255.255.255.255/").host())
        assertEquals("1.2.3.4", DeepLinkUri.parse("http://1.2.3.4/").host())
        assertEquals("0.0.0.0", DeepLinkUri.parse("http://0.0.0.0/").host())
    }

    @Test
    fun hostWithTrailingDot() {
        assertEquals("host.", DeepLinkUri.parse("http://host./").host())
    }

    @Test
    fun port() {
        assertEquals(parse("http://host/"), DeepLinkUri.parse("http://host:80/"))
        assertEquals(parse("http://host:99/"), DeepLinkUri.parse("http://host:99/"))
        assertEquals(parse("http://host/"), DeepLinkUri.parse("http://host:/"))
        assertEquals(65535, DeepLinkUri.parse("http://host:65535/").port().toLong())
        assertInvalid("http://host:0/", "Invalid URL port: \"0\"")
        assertInvalid("http://host:65536/", "Invalid URL port: \"65536\"")
        assertInvalid("http://host:-1/", "Invalid URL port: \"-1\"")
        assertInvalid("http://host:a/", "Invalid URL port: \"a\"")
        assertInvalid("http://host:%39%39/", "Invalid URL port: \"%39%39\"")
    }

    @Test
    fun fragmentNonAscii() {
        val url = DeepLinkUri.parse("http://host/#Σ")
        assertEquals("http://host/#Σ", url.toString())
        assertEquals("Σ", url.fragment())
        assertEquals("Σ", url.encodedFragment())
        assertEquals("http://host/#Σ", url.uri().toString())
    }

    @Test
    fun fragmentNonAsciiThatOffendsJavaNetUri() {
        val url = DeepLinkUri.parse("http://host/#\u0080")
        assertEquals("http://host/#\u0080", url.toString())
        assertEquals("\u0080", url.fragment())
        assertEquals("\u0080", url.encodedFragment())
        assertEquals(URI("http://host/#"), url.uri()) // Control characters may be stripped!
    }

    @Test
    fun fragmentPercentEncodedNonAscii() {
        val url = DeepLinkUri.parse("http://host/#%C2%80")
        assertEquals("http://host/#%C2%80", url.toString())
        assertEquals("\u0080", url.fragment())
        assertEquals("%C2%80", url.encodedFragment())
        assertEquals("http://host/#%C2%80", url.uri().toString())
    }

    @Test
    fun fragmentPercentEncodedPartialCodePoint() {
        val url = DeepLinkUri.parse("http://host/#%80")
        assertEquals("http://host/#%80", url.toString())
        assertEquals("\ufffd", url.fragment()) // Unicode replacement character.
        assertEquals("%80", url.encodedFragment())
        assertEquals("http://host/#%80", url.uri().toString())
    }

    @Test
    fun relativePath() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals(parse("http://host/a/b/d/e/f"), base.resolve("d/e/f"))
        assertEquals(parse("http://host/d/e/f"), base.resolve("../../d/e/f"))
        assertEquals(parse("http://host/a/"), base.resolve(".."))
        assertEquals(parse("http://host/"), base.resolve("../.."))
        assertEquals(parse("http://host/"), base.resolve("../../.."))
        assertEquals(parse("http://host/a/b/"), base.resolve("."))
        assertEquals(parse("http://host/a/"), base.resolve("././.."))
        assertEquals(parse("http://host/a/b/c/"), base.resolve("c/d/../e/../"))
        assertEquals(parse("http://host/a/b/..e/"), base.resolve("..e/"))
        assertEquals(parse("http://host/a/b/e/f../"), base.resolve("e/f../"))
        assertEquals(parse("http://host/a/"), base.resolve("%2E."))
        assertEquals(parse("http://host/a/"), base.resolve(".%2E"))
        assertEquals(parse("http://host/a/"), base.resolve("%2E%2E"))
        assertEquals(parse("http://host/a/"), base.resolve("%2e."))
        assertEquals(parse("http://host/a/"), base.resolve(".%2e"))
        assertEquals(parse("http://host/a/"), base.resolve("%2e%2e"))
        assertEquals(parse("http://host/a/b/"), base.resolve("%2E"))
        assertEquals(parse("http://host/a/b/"), base.resolve("%2e"))
    }

    @Test
    fun relativePathWithTrailingSlash() {
        val base = DeepLinkUri.parse("http://host/a/b/c/")
        assertEquals(parse("http://host/a/b/"), base.resolve(".."))
        assertEquals(parse("http://host/a/b/"), base.resolve("../"))
        assertEquals(parse("http://host/a/"), base.resolve("../.."))
        assertEquals(parse("http://host/a/"), base.resolve("../../"))
        assertEquals(parse("http://host/"), base.resolve("../../.."))
        assertEquals(parse("http://host/"), base.resolve("../../../"))
        assertEquals(parse("http://host/"), base.resolve("../../../.."))
        assertEquals(parse("http://host/"), base.resolve("../../../../"))
        assertEquals(parse("http://host/a"), base.resolve("../../../../a"))
        assertEquals(parse("http://host/"), base.resolve("../../../../a/.."))
        assertEquals(parse("http://host/a/"), base.resolve("../../../../a/b/.."))
    }

    @Test
    fun pathWithBackslash() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals(parse("http://host/a/b/d/e/f"), base.resolve("d\\e\\f"))
        assertEquals(parse("http://host/d/e/f"), base.resolve("../..\\d\\e\\f"))
        assertEquals(parse("http://host/"), base.resolve("..\\.."))
    }

    @Test
    fun relativePathWithSameScheme() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals(parse("http://host/a/b/d/e/f"), base.resolve("http:d/e/f"))
        assertEquals(parse("http://host/d/e/f"), base.resolve("http:../../d/e/f"))
    }

    @Test
    fun decodeUsername() {
        assertEquals("user", DeepLinkUri.parse("http://user@host/").username())
        assertEquals("\uD83C\uDF69", DeepLinkUri.parse("http://%F0%9F%8D%A9@host/").username())
    }

    @Test
    fun decodePassword() {
        assertEquals("password", DeepLinkUri.parse("http://user:password@host/").password())
        assertEquals("", DeepLinkUri.parse("http://user:@host/").password())
        assertEquals("\uD83C\uDF69", DeepLinkUri.parse("http://user:%F0%9F%8D%A9@host/").password())
    }

    @Test
    fun decodeSlashCharacterInDecodedPathSegment() {
        assertEquals(
            Arrays.asList("a/b/c"),
            DeepLinkUri.parse("http://host/a%2Fb%2Fc").pathSegments()
        )
    }

    @Test
    fun decodeEmptyPathSegments() {
        assertEquals(
            Arrays.asList(""),
            DeepLinkUri.parse("http://host/").pathSegments()
        )
    }

    @Test
    fun percentDecode() {
        assertEquals(
            Arrays.asList("\u0000"),
            DeepLinkUri.parse("http://host/%00").pathSegments()
        )
        assertEquals(
            Arrays.asList("a", "\u2603", "c"),
            DeepLinkUri.parse("http://host/a/%E2%98%83/c").pathSegments()
        )
        assertEquals(
            Arrays.asList("a", "\uD83C\uDF69", "c"),
            DeepLinkUri.parse("http://host/a/%F0%9F%8D%A9/c").pathSegments()
        )
        assertEquals(
            Arrays.asList("a", "b", "c"),
            DeepLinkUri.parse("http://host/a/%62/c").pathSegments()
        )
        assertEquals(
            Arrays.asList("a", "z", "c"),
            DeepLinkUri.parse("http://host/a/%7A/c").pathSegments()
        )
        assertEquals(
            Arrays.asList("a", "z", "c"),
            DeepLinkUri.parse("http://host/a/%7a/c").pathSegments()
        )
    }

    @Test
    fun malformedPercentEncoding() {
        assertEquals(
            Arrays.asList("a%f", "b"),
            DeepLinkUri.parse("http://host/a%f/b").pathSegments()
        )
        assertEquals(
            Arrays.asList("%", "b"),
            DeepLinkUri.parse("http://host/%/b").pathSegments()
        )
        assertEquals(
            Arrays.asList("%"),
            DeepLinkUri.parse("http://host/%").pathSegments()
        )
        assertEquals(
            Arrays.asList("%00"),
            DeepLinkUri.parse("http://github.com/%%30%30").pathSegments()
        )
    }

    @Test
    fun malformedUtf8Encoding() {
        // Replace a partial UTF-8 sequence with the Unicode replacement character.
        assertEquals(
            Arrays.asList("a", "\ufffdx", "c"),
            DeepLinkUri.parse("http://host/a/%E2%98x/c").pathSegments()
        )
    }

    @Test
    fun incompleteUrlComposition() {
        try {
            DeepLinkUri.Builder().scheme("http").build()
            fail()
        } catch (expected: IllegalStateException) {
            assertEquals("host == null", expected.message)
        }

        try {
            DeepLinkUri.Builder().host("host").build()
            fail()
        } catch (expected: IllegalStateException) {
            assertEquals("scheme == null", expected.message)
        }

    }

    @Test
    fun builderToString() {
        assertEquals("https://host.com/path", DeepLinkUri.parse("https://host.com/path").newBuilder().toString())
    }

    @Test
    fun incompleteBuilderToString() {
        assertEquals(
            "https:///path",
            DeepLinkUri.Builder().scheme("https").encodedPath("/path").toString()
        )
        assertEquals(
            "//host.com/path",
            DeepLinkUri.Builder().host("host.com").encodedPath("/path").toString()
        )
        assertEquals(
            "//host.com:8080/path",
            DeepLinkUri.Builder().host("host.com").encodedPath("/path").port(8080).toString()
        )
    }

    @Test
    fun minimalUrlComposition() {
        val url = DeepLinkUri.Builder().scheme("http").host("host").build()
        assertEquals("http://host/", url.toString())
        assertEquals("http", url.scheme())
        assertEquals("", url.username())
        assertEquals("", url.password())
        assertEquals("host", url.host())
        assertEquals(80, url.port().toLong())
        assertEquals("/", url.encodedPath())
        assertNull(url.query())
        assertNull(url.fragment())
    }

    @Test
    fun fullUrlComposition() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .username("username")
            .password("password")
            .host("host")
            .port(8080)
            .addPathSegment("path")
            .query("query")
            .fragment("fragment")
            .build()
        assertEquals("http://username:password@host:8080/path?query#fragment", url.toString())
        assertEquals("http", url.scheme())
        assertEquals("username", url.username())
        assertEquals("password", url.password())
        assertEquals("host", url.host())
        assertEquals(8080, url.port().toLong())
        assertEquals("/path", url.encodedPath())
        assertEquals("query", url.query())
        assertEquals("fragment", url.fragment())
    }

    @Test
    fun changingSchemeChangesDefaultPort() {
        assertEquals(
            443, DeepLinkUri.parse("http://example.com")
                .newBuilder()
                .scheme("https")
                .build().port()
        )

        assertEquals(
            80, DeepLinkUri.parse("https://example.com")
                .newBuilder()
                .scheme("http")
                .build().port()
        )

        assertEquals(
            1234, DeepLinkUri.parse("https://example.com:1234")
                .newBuilder()
                .scheme("http")
                .build().port()
        )
    }

    @Test
    fun composeEncodesWhitespace() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .username("a\r\n\u000c\t b")
            .password("c\r\n\u000c\t d")
            .host("host")
            .addPathSegment("e\r\n\u000c\t f")
            .query("g\r\n\u000c\t h")
            .fragment("i\r\n\u000c\t j")
            .build()
        assertEquals(
            "http://a%0D%0A%0C%09%20b:c%0D%0A%0C%09%20d@host" + "/e%0D%0A%0C%09%20f?g%0D%0A%0C%09%20h#i%0D%0A%0C%09 j",
            url.toString()
        )
        assertEquals("a\r\n\u000c\t b", url.username())
        assertEquals("c\r\n\u000c\t d", url.password())
        assertEquals("e\r\n\u000c\t f", url.pathSegments()[0])
        assertEquals("g\r\n\u000c\t h", url.query())
        assertEquals("i\r\n\u000c\t j", url.fragment())
    }

    @Test
    fun composeFromUnencodedComponents() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .username("a:\u0001@/\\?#%b")
            .password("c:\u0001@/\\?#%d")
            .host("ef")
            .port(8080)
            .addPathSegment("g:\u0001@/\\?#%h")
            .query("i:\u0001@/\\?#%j")
            .fragment("k:\u0001@/\\?#%l")
            .build()
        assertEquals(
            "http://a%3A%01%40%2F%5C%3F%23%25b:c%3A%01%40%2F%5C%3F%23%25d@ef:8080/" + "g:%01@%2F%5C%3F%23%25h?i:%01@/\\?%23%25j#k:%01@/\\?#%25l",
            url.toString()
        )
        assertEquals("http", url.scheme())
        assertEquals("a:\u0001@/\\?#%b", url.username())
        assertEquals("c:\u0001@/\\?#%d", url.password())
        assertEquals(Arrays.asList("g:\u0001@/\\?#%h"), url.pathSegments())
        assertEquals("i:\u0001@/\\?#%j", url.query())
        assertEquals("k:\u0001@/\\?#%l", url.fragment())
        assertEquals("a%3A%01%40%2F%5C%3F%23%25b", url.encodedUsername())
        assertEquals("c%3A%01%40%2F%5C%3F%23%25d", url.encodedPassword())
        assertEquals("/g:%01@%2F%5C%3F%23%25h", url.encodedPath())
        assertEquals("i:%01@/\\?%23%25j", url.encodedQuery())
        assertEquals("k:%01@/\\?#%25l", url.encodedFragment())
    }

    @Test
    fun composeFromEncodedComponents() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .encodedUsername("a:\u0001@/\\?#%25b")
            .encodedPassword("c:\u0001@/\\?#%25d")
            .host("ef")
            .port(8080)
            .addEncodedPathSegment("g:\u0001@/\\?#%25h")
            .encodedQuery("i:\u0001@/\\?#%25j")
            .encodedFragment("k:\u0001@/\\?#%25l")
            .build()
        assertEquals(
            "http://a%3A%01%40%2F%5C%3F%23%25b:c%3A%01%40%2F%5C%3F%23%25d@ef:8080/" + "g:%01@%2F%5C%3F%23%25h?i:%01@/\\?%23%25j#k:%01@/\\?#%25l",
            url.toString()
        )
        assertEquals("http", url.scheme())
        assertEquals("a:\u0001@/\\?#%b", url.username())
        assertEquals("c:\u0001@/\\?#%d", url.password())
        assertEquals(Arrays.asList("g:\u0001@/\\?#%h"), url.pathSegments())
        assertEquals("i:\u0001@/\\?#%j", url.query())
        assertEquals("k:\u0001@/\\?#%l", url.fragment())
        assertEquals("a%3A%01%40%2F%5C%3F%23%25b", url.encodedUsername())
        assertEquals("c%3A%01%40%2F%5C%3F%23%25d", url.encodedPassword())
        assertEquals("/g:%01@%2F%5C%3F%23%25h", url.encodedPath())
        assertEquals("i:%01@/\\?%23%25j", url.encodedQuery())
        assertEquals("k:%01@/\\?#%25l", url.encodedFragment())
    }

    @Test
    fun composeWithEncodedPath() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .encodedPath("/a%2Fb/c")
            .build()
        assertEquals("http://host/a%2Fb/c", url.toString())
        assertEquals("/a%2Fb/c", url.encodedPath())
        assertEquals(Arrays.asList("a/b", "c"), url.pathSegments())
    }

    @Test
    fun composeMixingPathSegments() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .encodedPath("/a%2fb/c")
            .addPathSegment("d%25e")
            .addEncodedPathSegment("f%25g")
            .build()
        assertEquals("http://host/a%2fb/c/d%2525e/f%25g", url.toString())
        assertEquals("/a%2fb/c/d%2525e/f%25g", url.encodedPath())
        assertEquals(Arrays.asList("a%2fb", "c", "d%2525e", "f%25g"), url.encodedPathSegments())
        assertEquals(Arrays.asList("a/b", "c", "d%25e", "f%g"), url.pathSegments())
    }

    @Test
    fun composeWithAddSegment() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals("/a/b/c/", base.newBuilder().addPathSegment("").build().encodedPath())
        assertEquals(
            "/a/b/c/d",
            base.newBuilder().addPathSegment("").addPathSegment("d").build().encodedPath()
        )
        assertEquals("/a/b/", base.newBuilder().addPathSegment("..").build().encodedPath())
        assertEquals(
            "/a/b/", base.newBuilder().addPathSegment("").addPathSegment("..").build()
                .encodedPath()
        )
        assertEquals(
            "/a/b/c/", base.newBuilder().addPathSegment("").addPathSegment("").build()
                .encodedPath()
        )
    }

    @Test
    fun pathSize() {
        assertEquals(1, DeepLinkUri.parse("http://host/").pathSize().toLong())
        assertEquals(3, DeepLinkUri.parse("http://host/a/b/c").pathSize().toLong())
    }

    @Test
    fun addPathSegments() {
        val base = DeepLinkUri.parse("http://host/a/b/c")

        // Add a string with zero slashes: resulting URL gains one slash.
        assertEquals("/a/b/c/", base.newBuilder().addPathSegments("").build().encodedPath())
        assertEquals("/a/b/c/d", base.newBuilder().addPathSegments("d").build().encodedPath())

        // Add a string with one slash: resulting URL gains two slashes.
        assertEquals("/a/b/c//", base.newBuilder().addPathSegments("/").build().encodedPath())
        assertEquals("/a/b/c/d/", base.newBuilder().addPathSegments("d/").build().encodedPath())
        assertEquals("/a/b/c//d", base.newBuilder().addPathSegments("/d").build().encodedPath())

        // Add a string with two slashes: resulting URL gains three slashes.
        assertEquals("/a/b/c///", base.newBuilder().addPathSegments("//").build().encodedPath())
        assertEquals("/a/b/c//d/", base.newBuilder().addPathSegments("/d/").build().encodedPath())
        assertEquals("/a/b/c/d//", base.newBuilder().addPathSegments("d//").build().encodedPath())
        assertEquals("/a/b/c///d", base.newBuilder().addPathSegments("//d").build().encodedPath())
        assertEquals("/a/b/c/d/e/f", base.newBuilder().addPathSegments("d/e/f").build().encodedPath())
    }

    @Test
    fun addPathSegmentsOntoTrailingSlash() {
        val base = DeepLinkUri.parse("http://host/a/b/c/")

        // Add a string with zero slashes: resulting URL gains zero slashes.
        assertEquals("/a/b/c/", base.newBuilder().addPathSegments("").build().encodedPath())
        assertEquals("/a/b/c/d", base.newBuilder().addPathSegments("d").build().encodedPath())

        // Add a string with one slash: resulting URL gains one slash.
        assertEquals("/a/b/c//", base.newBuilder().addPathSegments("/").build().encodedPath())
        assertEquals("/a/b/c/d/", base.newBuilder().addPathSegments("d/").build().encodedPath())
        assertEquals("/a/b/c//d", base.newBuilder().addPathSegments("/d").build().encodedPath())

        // Add a string with two slashes: resulting URL gains two slashes.
        assertEquals("/a/b/c///", base.newBuilder().addPathSegments("//").build().encodedPath())
        assertEquals("/a/b/c//d/", base.newBuilder().addPathSegments("/d/").build().encodedPath())
        assertEquals("/a/b/c/d//", base.newBuilder().addPathSegments("d//").build().encodedPath())
        assertEquals("/a/b/c///d", base.newBuilder().addPathSegments("//d").build().encodedPath())
        assertEquals("/a/b/c/d/e/f", base.newBuilder().addPathSegments("d/e/f").build().encodedPath())
    }

    @Test
    fun addPathSegmentsWithBackslash() {
        val base = DeepLinkUri.parse("http://host/")
        assertEquals("/d/e", base.newBuilder().addPathSegments("d\\e").build().encodedPath())
        assertEquals("/d/e", base.newBuilder().addEncodedPathSegments("d\\e").build().encodedPath())
    }

    @Test
    fun addPathSegmentsWithEmptyPaths() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals(
            "/a/b/c//d/e///f",
            base.newBuilder().addPathSegments("/d/e///f").build().encodedPath()
        )
    }

    @Test
    fun addEncodedPathSegments() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals(
            "/a/b/c/d/e/%20/",
            base.newBuilder().addEncodedPathSegments("d/e/%20/\n").build().encodedPath()
        )
    }

    @Test
    fun addPathSegmentDotDoesNothing() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals("/a/b/c", base.newBuilder().addPathSegment(".").build().encodedPath())
    }

    @Test
    fun addPathSegmentEncodes() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals(
            "/a/b/c/%252e",
            base.newBuilder().addPathSegment("%2e").build().encodedPath()
        )
        assertEquals(
            "/a/b/c/%252e%252e",
            base.newBuilder().addPathSegment("%2e%2e").build().encodedPath()
        )
    }

    @Test
    fun addPathSegmentDotDotPopsDirectory() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals("/a/b/", base.newBuilder().addPathSegment("..").build().encodedPath())
    }

    @Test
    fun addPathSegmentDotAndIgnoredCharacter() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals("/a/b/c/.%0A", base.newBuilder().addPathSegment(".\n").build().encodedPath())
    }

    @Test
    fun addEncodedPathSegmentDotAndIgnoredCharacter() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals("/a/b/c", base.newBuilder().addEncodedPathSegment(".\n").build().encodedPath())
    }

    @Test
    fun addEncodedPathSegmentDotDotAndIgnoredCharacter() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals("/a/b/", base.newBuilder().addEncodedPathSegment("..\n").build().encodedPath())
    }

    @Test
    fun setPathSegment() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals("/d/b/c", base.newBuilder().setPathSegment(0, "d").build().encodedPath())
        assertEquals("/a/d/c", base.newBuilder().setPathSegment(1, "d").build().encodedPath())
        assertEquals("/a/b/d", base.newBuilder().setPathSegment(2, "d").build().encodedPath())
    }

    @Test
    fun setPathSegmentEncodes() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals("/%2525/b/c", base.newBuilder().setPathSegment(0, "%25").build().encodedPath())
        assertEquals("/.%0A/b/c", base.newBuilder().setPathSegment(0, ".\n").build().encodedPath())
        assertEquals("/%252e/b/c", base.newBuilder().setPathSegment(0, "%2e").build().encodedPath())
    }

    @Test
    fun setPathSegmentAcceptsEmpty() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals("//b/c", base.newBuilder().setPathSegment(0, "").build().encodedPath())
        assertEquals("/a/b/", base.newBuilder().setPathSegment(2, "").build().encodedPath())
    }

    @Test
    fun setPathSegmentRejectsDot() {
        val base = DeepLinkUri.parse("http://host/a/b/c")

        assertFailsWith<IllegalArgumentException> {
            base.newBuilder().setPathSegment(0, ".")
        }

    }

    @Test
    fun setPathSegmentRejectsDotDot() {
        val base = DeepLinkUri.parse("http://host/a/b/c")

        assertFailsWith<IllegalArgumentException> {
            base.newBuilder().setPathSegment(0, "..")
        }

    }

    @Test
    fun setPathSegmentWithSlash() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        val url = base.newBuilder().setPathSegment(1, "/").build()
        assertEquals("/a/%2F/c", url.encodedPath())
    }

    @Test
    fun setPathSegmentOutOfBounds() {
        assertFailsWith<IndexOutOfBoundsException> {
            DeepLinkUri.Builder().setPathSegment(1, "a")
        }

    }

    @Test
    fun setEncodedPathSegmentEncodes() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        assertEquals(
            "/%25/b/c",
            base.newBuilder().setEncodedPathSegment(0, "%25").build().encodedPath()
        )
    }

    @Test
    fun setEncodedPathSegmentRejectsDot() {
        val base = DeepLinkUri.parse("http://host/a/b/c")

        assertFailsWith<IllegalArgumentException> {
            base.newBuilder().setEncodedPathSegment(0, ".")
        }
    }

    @Test
    fun setEncodedPathSegmentRejectsDotAndIgnoredCharacter() {
        val base = DeepLinkUri.parse("http://host/a/b/c")

        assertFailsWith<IllegalArgumentException> {
            base.newBuilder().setEncodedPathSegment(0, ".\n")
        }

    }

    @Test
    fun setEncodedPathSegmentRejectsDotDot() {
        val base = DeepLinkUri.parse("http://host/a/b/c")

        assertFailsWith<IllegalArgumentException> {
            base.newBuilder().setEncodedPathSegment(0, "..")
        }

    }

    @Test
    fun setEncodedPathSegmentRejectsDotDotAndIgnoredCharacter() {
        val base = DeepLinkUri.parse("http://host/a/b/c")

        assertFailsWith<IllegalArgumentException> {
            base.newBuilder().setEncodedPathSegment(0, "..\n")
        }

    }

    @Test
    fun setEncodedPathSegmentWithSlash() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        val url = base.newBuilder().setEncodedPathSegment(1, "/").build()
        assertEquals("/a/%2F/c", url.encodedPath())
    }

    @Test
    fun setEncodedPathSegmentOutOfBounds() {
        assertFailsWith<IndexOutOfBoundsException> {
            DeepLinkUri.Builder().setEncodedPathSegment(1, "a")
            fail()
        }

    }

    @Test
    fun removePathSegment() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        val url = base.newBuilder()
            .removePathSegment(0)
            .build()
        assertEquals("/b/c", url.encodedPath())
    }

    @Test
    fun removePathSegmentDoesntRemovePath() {
        val base = DeepLinkUri.parse("http://host/a/b/c")
        val url = base.newBuilder()
            .removePathSegment(0)
            .removePathSegment(0)
            .removePathSegment(0)
            .build()
        assertEquals(Arrays.asList(""), url.pathSegments())
        assertEquals("/", url.encodedPath())
    }

    @Test
    fun removePathSegmentOutOfBounds() {
        assertFailsWith<IndexOutOfBoundsException> {
            DeepLinkUri.Builder().removePathSegment(1)
            fail()
        }

    }

    @Test
    fun toJavaNetUrl() {
        val deepLinkUri = DeepLinkUri.parse("http://username:password@host/path?query#fragment")
        val javaNetUrl = deepLinkUri.url()
        assertEquals("http://username:password@host/path?query#fragment", javaNetUrl.toString())
    }

    @Test
    fun toUri() {
        val deepLinkUri = DeepLinkUri.parse("http://username:password@host/path?query#fragment")
        val uri = deepLinkUri.uri()
        assertEquals("http://username:password@host/path?query#fragment", uri.toString())
    }

    @Test
    fun toUriSpecialQueryCharacters() {
        val deepLinkUri = DeepLinkUri.parse("http://host/?d=abc!@[]^`{}|\\")
        val uri = deepLinkUri.uri()
        assertEquals("http://host/?d=abc!@[]%5E%60%7B%7D%7C%5C", uri.toString())
    }

    @Test
    fun toUriWithUsernameNoPassword() {
        val deepLinkUri = DeepLinkUri.Builder()
            .scheme("http")
            .username("user")
            .host("host")
            .build()
        assertEquals("http://user@host/", deepLinkUri.toString())
        assertEquals("http://user@host/", deepLinkUri.uri().toString())
    }

    @Test
    fun toUriUsernameSpecialCharacters() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .username("=[]:;\"~|?#@^/$%*")
            .build()
        assertEquals("http://%3D%5B%5D%3A%3B%22~%7C%3F%23%40%5E%2F$%25*@host/", url.toString())
        assertEquals("http://%3D%5B%5D%3A%3B%22~%7C%3F%23%40%5E%2F$%25*@host/", url.uri().toString())
    }

    @Test
    fun toUriPasswordSpecialCharacters() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .username("user")
            .password("=[]:;\"~|?#@^/$%*")
            .build()
        assertEquals("http://user:%3D%5B%5D%3A%3B%22~%7C%3F%23%40%5E%2F$%25*@host/", url.toString())
        assertEquals(
            "http://user:%3D%5B%5D%3A%3B%22~%7C%3F%23%40%5E%2F$%25*@host/",
            url.uri().toString()
        )
    }

    @Test
    fun toUriPathSpecialCharacters() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .addPathSegment("=[]:;\"~|?#@^/$%*")
            .build()
        assertEquals("http://host/=[]:;%22~%7C%3F%23@%5E%2F$%25*", url.toString())
        assertEquals("http://host/=%5B%5D:;%22~%7C%3F%23@%5E%2F$%25*", url.uri().toString())
    }

    @Test
    fun toUriQueryParameterNameSpecialCharacters() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .addQueryParameter("=[]:;\"~|?#@^/$%*", "a")
            .build()
        assertEquals(
            "http://host/?%3D%5B%5D%3A%3B%22%7E%7C%3F%23%40%5E%2F%24%25*=a",
            url.toString()
        )
        assertEquals(
            "http://host/?%3D%5B%5D%3A%3B%22%7E%7C%3F%23%40%5E%2F%24%25*=a",
            url.uri().toString()
        )
        assertEquals("a", url.queryParameter("=[]:;\"~|?#@^/$%*"))
    }

    @Test
    fun toUriQueryParameterValueSpecialCharacters() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .addQueryParameter("a", "=[]:;\"~|?#@^/$%*")
            .build()
        assertEquals(
            "http://host/?a=%3D%5B%5D%3A%3B%22%7E%7C%3F%23%40%5E%2F%24%25*",
            url.toString()
        )
        assertEquals(
            "http://host/?a=%3D%5B%5D%3A%3B%22%7E%7C%3F%23%40%5E%2F%24%25*",
            url.uri().toString()
        )
        assertEquals("=[]:;\"~|?#@^/$%*", url.queryParameter("a"))
    }

    @Test
    fun toUriQueryValueSpecialCharacters() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .query("=[]:;\"~|?#@^/$%*")
            .build()
        assertEquals("http://host/?=[]:;%22~|?%23@^/$%25*", url.toString())
        assertEquals("http://host/?=[]:;%22~%7C?%23@%5E/$%25*", url.uri().toString())
    }

    @Test
    fun queryCharactersEncodedWhenComposed() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .addQueryParameter("a", "!$(),/:;?@[]\\^`{|}~")
            .build()
        assertEquals(
            "http://host/?a=%21%24%28%29%2C%2F%3A%3B%3F%40%5B%5D%5C%5E%60%7B%7C%7D%7E",
            url.toString()
        )
        assertEquals("!$(),/:;?@[]\\^`{|}~", url.queryParameter("a"))
    }

    /**
     * When callers use `addEncodedQueryParameter()` we only encode what's strictly required.
     * We retain the encoded (or non-encoded) state of the input.
     */
    @Test
    fun queryCharactersNotReencodedWhenComposedWithAddEncoded() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .addEncodedQueryParameter("a", "!$(),/:;?@[]\\^`{|}~")
            .build()
        assertEquals(
            "http://host/?a=!$(),/:;?@[]\\^`{|}~",
            url.toString()
        )
        assertEquals("!$(),/:;?@[]\\^`{|}~", url.queryParameter("a"))
    }

    /**
     * When callers parse a URL with query components that aren't encoded, we shouldn't convert them
     * into a canonical form because doing so could be semantically different.
     */
    @Test
    fun queryCharactersNotReencodedWhenParsed() {
        val url = DeepLinkUri.parse("http://host/?a=!$(),/:;?@[]\\^`{|}~")
        assertEquals("http://host/?a=!$(),/:;?@[]\\^`{|}~", url.toString())
        assertEquals("!$(),/:;?@[]\\^`{|}~", url.queryParameter("a"))
    }

    @Test
    fun toUriFragmentSpecialCharacters() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .host("host")
            .fragment("=[]:;\"~|?#@^/$%*")
            .build()
        assertEquals("http://host/#=[]:;\"~|?#@^/$%25*", url.toString())
        assertEquals("http://host/#=[]:;%22~%7C?%23@%5E/$%25*", url.uri().toString())
    }

    @Test
    fun toUriWithControlCharacters() {
        // Percent-encoded in the path.
        assertEquals(URI("http://host/a%00b"), DeepLinkUri.parse("http://host/a\u0000b").uri())
        assertEquals(URI("http://host/a%C2%80b"), DeepLinkUri.parse("http://host/a\u0080b").uri())
        assertEquals(URI("http://host/a%C2%9Fb"), DeepLinkUri.parse("http://host/a\u009fb").uri())
        // Percent-encoded in the query.
        assertEquals(URI("http://host/?a%00b"), DeepLinkUri.parse("http://host/?a\u0000b").uri())
        assertEquals(URI("http://host/?a%C2%80b"), DeepLinkUri.parse("http://host/?a\u0080b").uri())
        assertEquals(URI("http://host/?a%C2%9Fb"), DeepLinkUri.parse("http://host/?a\u009fb").uri())
        // Stripped from the fragment.
        assertEquals(URI("http://host/#a%00b"), DeepLinkUri.parse("http://host/#a\u0000b").uri())
        assertEquals(URI("http://host/#ab"), DeepLinkUri.parse("http://host/#a\u0080b").uri())
        assertEquals(URI("http://host/#ab"), DeepLinkUri.parse("http://host/#a\u009fb").uri())
    }

    @Test
    fun toUriWithSpaceCharacters() {
        // Percent-encoded in the path.
        assertEquals(URI("http://host/a%0Bb"), DeepLinkUri.parse("http://host/a\u000bb").uri())
        assertEquals(URI("http://host/a%20b"), DeepLinkUri.parse("http://host/a b").uri())
        assertEquals(URI("http://host/a%E2%80%89b"), DeepLinkUri.parse("http://host/a\u2009b").uri())
        assertEquals(URI("http://host/a%E3%80%80b"), DeepLinkUri.parse("http://host/a\u3000b").uri())
        // Percent-encoded in the query.
        assertEquals(URI("http://host/?a%0Bb"), DeepLinkUri.parse("http://host/?a\u000bb").uri())
        assertEquals(URI("http://host/?a%20b"), DeepLinkUri.parse("http://host/?a b").uri())
        assertEquals(URI("http://host/?a%E2%80%89b"), DeepLinkUri.parse("http://host/?a\u2009b").uri())
        assertEquals(URI("http://host/?a%E3%80%80b"), DeepLinkUri.parse("http://host/?a\u3000b").uri())
        // Stripped from the fragment.
        assertEquals(URI("http://host/#a%0Bb"), DeepLinkUri.parse("http://host/#a\u000bb").uri())
        assertEquals(URI("http://host/#a%20b"), DeepLinkUri.parse("http://host/#a b").uri())
        assertEquals(URI("http://host/#ab"), DeepLinkUri.parse("http://host/#a\u2009b").uri())
        assertEquals(URI("http://host/#ab"), DeepLinkUri.parse("http://host/#a\u3000b").uri())
    }

    @Test
    fun toUriWithNonHexPercentEscape() {
        assertEquals(URI("http://host/%25xx"), DeepLinkUri.parse("http://host/%xx").uri())
    }

    @Test
    fun toUriWithTruncatedPercentEscape() {
        assertEquals(URI("http://host/%25a"), DeepLinkUri.parse("http://host/%a").uri())
        assertEquals(URI("http://host/%25"), DeepLinkUri.parse("http://host/%").uri())
    }

    @Test
    fun fromJavaNetUrl() {
        val javaNetUrl = URL("http://username:password@host/path?query#fragment")
        val deepLinkUri = DeepLinkUri.get(javaNetUrl)
        assertEquals("http://username:password@host/path?query#fragment", deepLinkUri.toString())
    }

    @Test
    fun fromJavaNetUrlCustomScheme() {
        val javaNetUrl = URL("mailto:user@example.com")
        assertEquals(parse("mailto:user@example.com"), DeepLinkUri.get(javaNetUrl))
    }

    @Test
    fun fromUri() {
        val uri = URI("http://username:password@host/path?query#fragment")
        val deepLinkUri = DeepLinkUri.get(uri)
        assertEquals("http://username:password@host/path?query#fragment", deepLinkUri.toString())
    }

    @Test
    fun fromUriCustomScheme() {
        val uri = URI("mailto:user@example.com")
        assertEquals(parse("mailto:user@example.com"), DeepLinkUri.get(uri))
    }

    @Test
    fun fromUriPartial() {
        val uri = URI("/path")
        assertFailsWith<IllegalArgumentException> { (DeepLinkUri.get(uri)) }
    }

    @Test
    fun composeQueryWithComponents() {
        val base = DeepLinkUri.parse("http://host/")
        val url = base.newBuilder().addQueryParameter("a+=& b", "c+=& d").build()
        assertEquals("http://host/?a%2B%3D%26%20b=c%2B%3D%26%20d", url.toString())
        assertEquals("c+=& d", url.queryParameterValue(0))
        assertEquals("a+=& b", url.queryParameterName(0))
        assertEquals("c+=& d", url.queryParameter("a+=& b"))
        assertEquals(setOf("a+=& b"), url.queryParameterNames())
        assertEquals(listOf("c+=& d"), url.queryParameterValues("a+=& b"))
        assertEquals(1, url.querySize().toLong())
        assertEquals("a+=& b=c+=& d", url.query()) // Ambiguous! (Though working as designed.)
        assertEquals("a%2B%3D%26%20b=c%2B%3D%26%20d", url.encodedQuery())
    }

    @Test
    fun composeQueryWithEncodedComponents() {
        val base = DeepLinkUri.parse("http://host/")
        val url = base.newBuilder().addEncodedQueryParameter("a+=& b", "c+=& d").build()
        assertEquals("http://host/?a+%3D%26%20b=c+%3D%26%20d", url.toString())
        assertEquals("c =& d", url.queryParameter("a =& b"))
    }

    @Test
    fun composeQueryRemoveQueryParameter() {
        val url = DeepLinkUri.parse("http://host/").newBuilder()
            .addQueryParameter("a+=& b", "c+=& d")
            .removeAllQueryParameters("a+=& b")
            .build()
        assertEquals("http://host/", url.toString())
        assertNull(url.queryParameter("a+=& b"))
    }

    @Test
    fun composeQueryRemoveEncodedQueryParameter() {
        val url = DeepLinkUri.parse("http://host/").newBuilder()
            .addEncodedQueryParameter("a+=& b", "c+=& d")
            .removeAllEncodedQueryParameters("a+=& b")
            .build()
        assertEquals("http://host/", url.toString())
        assertNull(url.queryParameter("a =& b"))
    }

    @Test
    fun composeQuerySetQueryParameter() {
        val url = DeepLinkUri.parse("http://host/").newBuilder()
            .addQueryParameter("a+=& b", "c+=& d")
            .setQueryParameter("a+=& b", "ef")
            .build()
        assertEquals("http://host/?a%2B%3D%26%20b=ef", url.toString())
        assertEquals("ef", url.queryParameter("a+=& b"))
    }

    @Test
    fun composeQuerySetEncodedQueryParameter() {
        val url = DeepLinkUri.parse("http://host/").newBuilder()
            .addEncodedQueryParameter("a+=& b", "c+=& d")
            .setEncodedQueryParameter("a+=& b", "ef")
            .build()
        assertEquals("http://host/?a+%3D%26%20b=ef", url.toString())
        assertEquals("ef", url.queryParameter("a =& b"))
    }

    @Test
    fun composeQueryMultipleEncodedValuesForParameter() {
        val url = DeepLinkUri.parse("http://host/").newBuilder()
            .addQueryParameter("a+=& b", "c+=& d")
            .addQueryParameter("a+=& b", "e+=& f")
            .build()
        assertEquals(
            "http://host/?a%2B%3D%26%20b=c%2B%3D%26%20d&a%2B%3D%26%20b=e%2B%3D%26%20f",
            url.toString()
        )
        assertEquals(2, url.querySize().toLong())
        assertEquals(setOf("a+=& b"), url.queryParameterNames())
        assertEquals(Arrays.asList("c+=& d", "e+=& f"), url.queryParameterValues("a+=& b"))
    }

    @Test
    fun absentQueryIsZeroNameValuePairs() {
        val url = DeepLinkUri.parse("http://host/").newBuilder()
            .query(null)
            .build()
        assertEquals(0, url.querySize().toLong())
    }

    @Test
    fun emptyQueryIsSingleNameValuePairWithEmptyKey() {
        val url = DeepLinkUri.parse("http://host/").newBuilder()
            .query("")
            .build()
        assertEquals(1, url.querySize().toLong())
        assertEquals("", url.queryParameterName(0))
        assertNull(url.queryParameterValue(0))
    }

    @Test
    fun ampersandQueryIsTwoNameValuePairsWithEmptyKeys() {
        val url = DeepLinkUri.parse("http://host/").newBuilder()
            .query("&")
            .build()
        assertEquals(2, url.querySize().toLong())
        assertEquals("", url.queryParameterName(0))
        assertNull(url.queryParameterValue(0))
        assertEquals("", url.queryParameterName(1))
        assertNull(url.queryParameterValue(1))
    }

    @Test
    fun removeAllDoesNotRemoveQueryIfNoParametersWereRemoved() {
        val url = DeepLinkUri.parse("http://host/").newBuilder()
            .query("")
            .removeAllQueryParameters("a")
            .build()
        assertEquals("http://host/?", url.toString())
    }

    @Test
    fun queryParametersWithoutValues() {
        val url = DeepLinkUri.parse("http://host/?foo&bar&baz")
        assertEquals(3, url.querySize().toLong())
        assertEquals(
            LinkedHashSet(Arrays.asList("foo", "bar", "baz")),
            url.queryParameterNames()
        )
        assertNull(url.queryParameterValue(0))
        assertNull(url.queryParameterValue(1))
        assertNull(url.queryParameterValue(2))
        assertEquals(listOf<String?>(null), url.queryParameterValues("foo"))
        assertEquals(listOf<String?>(null), url.queryParameterValues("bar"))
        assertEquals(listOf<String?>(null), url.queryParameterValues("baz"))
    }

    @Test
    fun queryParametersWithEmptyValues() {
        val url = DeepLinkUri.parse("http://host/?foo=&bar=&baz=")
        assertEquals(3, url.querySize().toLong())
        assertEquals(
            LinkedHashSet(Arrays.asList("foo", "bar", "baz")),
            url.queryParameterNames()
        )
        assertEquals("", url.queryParameterValue(0))
        assertEquals("", url.queryParameterValue(1))
        assertEquals("", url.queryParameterValue(2))
        assertEquals(listOf(""), url.queryParameterValues("foo"))
        assertEquals(listOf(""), url.queryParameterValues("bar"))
        assertEquals(listOf(""), url.queryParameterValues("baz"))
    }

    @Test
    fun queryParametersWithRepeatedName() {
        val url = DeepLinkUri.parse("http://host/?foo[]=1&foo[]=2&foo[]=3")
        assertEquals(3, url.querySize().toLong())
        assertEquals(setOf("foo[]"), url.queryParameterNames())
        assertEquals("1", url.queryParameterValue(0))
        assertEquals("2", url.queryParameterValue(1))
        assertEquals("3", url.queryParameterValue(2))
        assertEquals(Arrays.asList("1", "2", "3"), url.queryParameterValues("foo[]"))
    }

    @Test
    fun queryParameterLookupWithNonCanonicalEncoding() {
        val url = DeepLinkUri.parse("http://host/?%6d=m&+=%20")
        assertEquals("m", url.queryParameterName(0))
        assertEquals(" ", url.queryParameterName(1))
        assertEquals("m", url.queryParameter("m"))
        assertEquals(" ", url.queryParameter(" "))
    }

    @Test
    fun parsedQueryDoesntIncludeFragment() {
        val url = DeepLinkUri.parse("http://host/?#fragment")
        assertEquals("fragment", url.fragment())
        assertEquals("", url.query())
        assertEquals("", url.encodedQuery())
    }

    @Test
    fun roundTripBuilder() {
        val url = DeepLinkUri.Builder()
            .scheme("http")
            .username("%")
            .password("%")
            .host("host")
            .addPathSegment("%")
            .query("%")
            .fragment("%")
            .build()
        assertEquals("http://%25:%25@host/%25?%25#%25", url.toString())
        assertEquals("http://%25:%25@host/%25?%25#%25", url.newBuilder().build().toString())
        assertEquals("http://%25:%25@host/%25?%25", url.resolve("")!!.toString())
    }

    /**
     * Although DeepLinkUri prefers percent-encodings in uppercase, it should preserve the exact structure
     * of the original encoding.
     */
    @Test
    fun rawEncodingRetained() {
        val urlString = "http://%6d%6D:%6d%6D@host/%6d%6D?%6d%6D#%6d%6D"
        val url = DeepLinkUri.parse(urlString)
        assertEquals("%6d%6D", url.encodedUsername())
        assertEquals("%6d%6D", url.encodedPassword())
        assertEquals("/%6d%6D", url.encodedPath())
        assertEquals(Arrays.asList("%6d%6D"), url.encodedPathSegments())
        assertEquals("%6d%6D", url.encodedQuery())
        assertEquals("%6d%6D", url.encodedFragment())
        assertEquals(urlString, url.toString())
        assertEquals(urlString, url.newBuilder().build().toString())
        assertEquals("http://%6d%6D:%6d%6D@host/%6d%6D?%6d%6D", url.resolve("")!!.toString())
    }

    @Test
    fun clearFragment() {
        val url = DeepLinkUri.parse("http://host/#fragment")
            .newBuilder()
            .fragment(null)
            .build()
        assertEquals("http://host/", url.toString())
        assertNull(url.fragment())
        assertNull(url.encodedFragment())
    }

    @Test
    fun clearEncodedFragment() {
        val url = DeepLinkUri.parse("http://host/#fragment")
            .newBuilder()
            .encodedFragment(null)
            .build()
        assertEquals("http://host/", url.toString())
        assertNull(url.fragment())
        assertNull(url.encodedFragment())
    }

    private fun assertInvalid(string: String, exceptionMessage: String?) {
        if (useGet) {
            assertFailsWith<IllegalArgumentException>(exceptionMessage) {
                DeepLinkUri.parse(string)
            }

        } else {
            assertNull(DeepLinkUri.parseOrNull(string), string)
        }
    }

    companion object {

        @Suppress("unused", "BooleanLiteralArgument")
        @Parameterized.Parameters(name = "Use get = {0}")
        @JvmStatic
        fun parameters(): Collection<*> {
            return listOf(true, false)
        }
    }
}
