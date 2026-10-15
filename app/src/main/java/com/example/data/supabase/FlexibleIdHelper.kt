package com.example.data.supabase

import java.util.UUID

object FlexibleIdHelper {
  fun toPositiveLong(raw: Any?, defaultVal: Long = 0L): Long {
    if (raw == null) return defaultVal
    when (raw) {
      is Number -> {
        val num = raw.toLong()
        return if (num > 0L) num else defaultVal
      }
      is String -> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return defaultVal
        trimmed.toLongOrNull()?.let { num ->
          return if (num > 0L) num else defaultVal
        }
        return try {
          val uuid = UUID.fromString(trimmed)
          val combined = uuid.mostSignificantBits xor uuid.leastSignificantBits
          val positive = combined and 0x7FFFFFFFFFFFFFFFL
          if (positive == 0L) 1L else positive
        } catch (_: Exception) {
          val hash = trimmed.hashCode().toLong()
          val positive = hash and 0x7FFFFFFFFFFFFFFFL
          if (positive == 0L) 1L else positive
        }
      }
      else -> {
        val hash = raw.hashCode().toLong()
        val positive = hash and 0x7FFFFFFFFFFFFFFFL
        return if (positive == 0L) 1L else positive
      }
    }
  }

  fun toNullablePositiveLong(raw: Any?): Long? {
    if (raw == null) return null
    val parsed = toPositiveLong(raw, 0L)
    return if (parsed > 0L) parsed else null
  }

  fun toUuidString(raw: Any?, namespace: String = "medrad"): String {
    if (raw == null) return UUID.randomUUID().toString()
    val str = raw.toString().trim()
    if (str.length == 36 && str.count { it == '-' } == 4) {
      return try {
        UUID.fromString(str).toString()
      } catch (_: Exception) {
        UUID.nameUUIDFromBytes("${namespace}_$str".toByteArray()).toString()
      }
    }
    return UUID.nameUUIDFromBytes("${namespace}_$str".toByteArray()).toString()
  }
}
