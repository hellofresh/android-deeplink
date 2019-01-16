/*
 * Copyright (c) 2019.  The HelloFresh Android Team
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

package com.hellofresh.deeplink.extension

import android.net.Uri
import com.hellofresh.deeplink.DeepLinkUri

fun DeepLinkUri.Companion.get(uri: Uri): DeepLinkUri {
    return parse(uri.toString())
}

fun DeepLinkUri.toAndroidUri(): Uri {
    return Uri.parse(toString())
}
