/*
* Copyright 2020 Matthew Nelson
* https://github.com/05nelsonm
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* */
package com.samourai.wallet.crypto

import com.google.common.io.BaseEncoding

class Base64 {

    @Throws(IllegalArgumentException::class)
    fun decode(chars: CharSequence): ByteArray =
        BaseEncoding.base64().decode(chars)

    @Throws(IndexOutOfBoundsException::class, AssertionError::class)
    fun encode(byteArray: ByteArray): String =
        BaseEncoding.base64().encode(byteArray)

    @Suppress("unused")
    @Throws(IndexOutOfBoundsException::class, AssertionError::class)
    fun encode(byteArray: ByteArray, off: Int, len: Int): String =
        BaseEncoding.base64().encode(byteArray, off, len)
}