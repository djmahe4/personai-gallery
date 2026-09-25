package com.personai.match

import java.security.MessageDigest

/** SHA-256 hasher with per-install salt for local identifier protection. */
class PrivacyHasher(private val saltProvider: () -> ByteArray) {

  fun hash(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(saltProvider())
    digest.update(value.toByteArray())
    return digest.digest().joinToString("") { "%02x".format(it) }
  }
}
