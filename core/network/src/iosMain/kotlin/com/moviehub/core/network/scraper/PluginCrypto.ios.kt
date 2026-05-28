package com.moviehub.core.network.scraper

import com.moviehub.core.network.commoncrypto.*
import kotlinx.cinterop.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalForeignApi::class)
actual fun pluginDigestHex(algorithm: String, data: String): String {
    val input = data.encodeToByteArray()
    val digestLen: Int
    val compute: (CPointer<ByteVar>, UInt, CPointer<UByteVar>) -> Unit

    when (algorithm.uppercase()) {
        "MD5" -> {
            digestLen = 16
            compute = { d, len, md -> CC_MD5(d, len, md) }
        }
        "SHA1" -> {
            digestLen = 20
            compute = { d, len, md -> CC_SHA1(d, len, md) }
        }
        "SHA256" -> {
            digestLen = 32
            compute = { d, len, md -> CC_SHA256(d, len, md) }
        }
        "SHA512" -> {
            digestLen = 64
            compute = { d, len, md -> CC_SHA512(d, len, md) }
        }
        else -> error("Unsupported digest algorithm: $algorithm")
    }

    return memScoped {
        val digest = allocArray<UByteVar>(digestLen)
        input.usePinned { pinned ->
            compute(pinned.addressOf(0), input.size.toUInt(), digest)
        }
        (0 until digestLen).joinToString("") {
            digest[it].toString(16).padStart(2, '0')
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun pluginHmacHex(algorithm: String, key: String, data: String): String {
    val algoConst = when (algorithm.uppercase()) {
        "SHA1" -> 1u   // kCCHmacAlgSHA1
        "MD5" -> 2u    // kCCHmacAlgMD5
        "SHA256" -> 3u // kCCHmacAlgSHA256
        "SHA512" -> 4u // kCCHmacAlgSHA512
        else -> error("Unsupported HMAC algorithm: $algorithm")
    }

    val digestLen = when (algorithm.uppercase()) {
        "MD5" -> 16
        "SHA1" -> 20
        "SHA256" -> 32
        "SHA512" -> 64
        else -> error("Unsupported digest length")
    }

    val keyBytes = key.encodeToByteArray()
    val dataBytes = data.encodeToByteArray()

    return memScoped {
        val macOut = allocArray<UByteVar>(digestLen)
        keyBytes.usePinned { keyPinned ->
            dataBytes.usePinned { dataPinned ->
                CCHmac(
                    algoConst,
                    keyPinned.addressOf(0), keyBytes.size.toULong(),
                    dataPinned.addressOf(0), dataBytes.size.toULong(),
                    macOut
                )
            }
        }
        (0 until digestLen).joinToString("") {
            macOut[it].toString(16).padStart(2, '0')
        }
    }
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
