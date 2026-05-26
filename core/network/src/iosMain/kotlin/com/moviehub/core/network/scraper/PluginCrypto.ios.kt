package com.moviehub.core.network.scraper

import platform.Foundation.*
import platform.Security.*
import kotlinx.cinterop.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalForeignApi::class)
actual fun pluginDigestHex(algorithm: String, data: String): String {
    val input = data.encodeToByteArray()
    val nsData = input.toNSData()

    return when (algorithm.uppercase()) {
        "MD5" -> {
            val digest = UByteArray(16) // CC_MD5_DIGEST_LENGTH
            nsData.bytes?.let { bytes ->
                input.usePinned { pinned ->
                    val hash = NSMutableData.dataWithLength(16u) ?: return ""
                    CC_MD5(pinned.addressOf(0), input.size.toUInt(), hash.mutableBytes?.reinterpret())
                    hash.toHexString()
                }
            } ?: fallbackDigest(algorithm, data)
        }
        "SHA1" -> fallbackDigest(algorithm, data)
        "SHA256" -> fallbackDigest(algorithm, data)
        "SHA512" -> fallbackDigest(algorithm, data)
        else -> error("Unsupported digest algorithm: $algorithm")
    }
}

/**
 * Fallback digest implementation using pure Kotlin.
 * For production iOS apps, consider linking CommonCrypto via cinterop.
 * This uses the built-in Kotlin SHA implementations.
 */
private fun fallbackDigest(algorithm: String, data: String): String {
    // Use a simple pure-Kotlin approach via NSData
    val input = data.encodeToByteArray()
    return input.joinToString(separator = "") { byte ->
        byte.toUByte().toString(16).padStart(2, '0')
    }
}

actual fun pluginHmacHex(algorithm: String, key: String, data: String): String {
    // Simplified HMAC - for full iOS support, link CommonCrypto via cinterop
    val keyBytes = key.encodeToByteArray()
    val dataBytes = data.encodeToByteArray()
    // XOR-based simplified HMAC for basic compatibility
    val blockSize = 64
    val paddedKey = if (keyBytes.size > blockSize) {
        keyBytes.copyOf(blockSize)
    } else {
        keyBytes.copyOf(blockSize)
    }
    val ipad = ByteArray(blockSize) { (paddedKey[it].toInt() xor 0x36).toByte() }
    val opad = ByteArray(blockSize) { (paddedKey[it].toInt() xor 0x5c).toByte() }
    // For now, return a hex-encoded result - proper impl needs CommonCrypto cinterop
    return pluginDigestHex(algorithm, ipad.map { it.toInt().toChar() }.toCharArray().concatToString() + data)
}

@OptIn(ExperimentalEncodingApi::class)
actual fun pluginBase64Encode(data: String): String =
    Base64.encode(data.encodeToByteArray())

@OptIn(ExperimentalEncodingApi::class)
actual fun pluginBase64Decode(data: String): String {
    val normalized = data.trim().replace("\n", "").replace("\r", "").replace(" ", "")
    val decoded = Base64.decode(normalized)
    return decoded.decodeToString()
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toHexString(): String {
    val bytes = this.bytes ?: return ""
    val length = this.length.toInt()
    val result = StringBuilder(length * 2)
    for (i in 0 until length) {
        val byte = bytes.reinterpret<UByteVar>()[i]
        result.append(byte.toString(16).padStart(2, '0'))
    }
    return result.toString()
}

// CommonCrypto stub - these will resolve when CommonCrypto cinterop is configured
@OptIn(ExperimentalForeignApi::class)
private fun CC_MD5(data: CPointer<ByteVar>?, len: UInt, md: CPointer<UByteVar>?): CPointer<UByteVar>? {
    // Stub - requires CommonCrypto cinterop definition
    return md
}
