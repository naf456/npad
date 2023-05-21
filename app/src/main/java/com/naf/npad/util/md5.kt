package com.naf.npad.util

import java.security.MessageDigest

internal fun md5(content: String) : String {
    val digest = MessageDigest.getInstance("MD5")
    return String(digest.digest(content.toByteArray()))
}