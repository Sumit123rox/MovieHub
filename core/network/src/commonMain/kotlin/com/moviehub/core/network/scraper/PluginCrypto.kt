package com.moviehub.core.network.scraper

/**
 * Platform-specific cryptographic helpers used by the QuickJS PluginRuntime.
 * Each platform (Android/iOS) provides its own actual implementation.
 */
expect fun pluginDigestHex(algorithm: String, data: String): String

expect fun pluginHmacHex(algorithm: String, key: String, data: String): String

expect fun pluginBase64Encode(data: String): String

expect fun pluginBase64Decode(data: String): String

fun pluginUtf8ToHex(value: String): String =
    value.encodeToByteArray().joinToString(separator = "") { byte ->
        byte.toUByte().toString(16).padStart(2, '0')
    }

fun pluginHexToUtf8(hex: String): String {
    val normalized = hex.trim().lowercase()
        .replace(" ", "")
        .removePrefix("0x")
    if (normalized.isEmpty()) return ""

    val evenHex = if (normalized.length % 2 == 0) normalized else "0$normalized"
    val out = ByteArray(evenHex.length / 2)
    for (index in out.indices) {
        val part = evenHex.substring(index * 2, index * 2 + 2)
        out[index] = part.toInt(16).toByte()
    }
    return out.decodeToString()
}
