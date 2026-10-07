package com.example.data.util

import java.security.MessageDigest

object PasswordSecurity {
  private const val SALT = "MEDRAD_STUDY_SQUAD_NIGERIA_SALT_2026"

  fun hashPassword(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val combined = "$password$SALT".toByteArray(Charsets.UTF_8)
    val digest = md.digest(combined)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
  }

  fun verifyPassword(password: String, storedHash: String): Boolean {
    val hash = hashPassword(password)
    return hash == storedHash
  }
}
