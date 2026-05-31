package com.moviehub.core.network.scraper

import kotlinx.cinterop.*
import platform.Foundation.*

// ─── SHA-256 ──────────────────────────────────────────────────────────────

private fun sha256(message: ByteArray): ByteArray {
    val k = uintArrayOf(
        0x428a2f98u, 0x71374491u, 0xb5c0fbcfu, 0xe9b5dba5u,
        0x3956c25bu, 0x59f111f1u, 0x923f82a4u, 0xab1c5ed5u,
        0xd807aa98u, 0x12835b01u, 0x243185beu, 0x550c7dc3u,
        0x72be5d74u, 0x80deb1feu, 0x9bdc06a7u, 0xc19bf174u,
        0xe49b69c1u, 0xefbe4786u, 0x0fc19dc6u, 0x240ca1ccu,
        0x2de92c6fu, 0x4a7484aau, 0x5cb0a9dcu, 0x76f988dau,
        0x983e5152u, 0xa831c66du, 0xb00327c8u, 0xbf597fc7u,
        0xc6e00bf3u, 0xd5a79147u, 0x06ca6351u, 0x14292967u,
        0x27b70a85u, 0x2e1b2138u, 0x4d2c6dfcu, 0x53380d13u,
        0x650a7354u, 0x766a0abbu, 0x81c2c92eu, 0x92722c85u,
        0xa2bfe8a1u, 0xa81a664bu, 0xc24b8b70u, 0xc76c51a3u,
        0xd192e819u, 0xd6990624u, 0xf40e3585u, 0x106aa070u,
        0x19a4c116u, 0x1e376c08u, 0x2748774cu, 0x34b0bcb5u,
        0x391c0cb3u, 0x4ed8aa4au, 0x5b9cca4fu, 0x682e6ff3u,
        0x748f82eeu, 0x78a5636fu, 0x84c87814u, 0x8cc70208u,
        0x90befffau, 0xa4506cebu, 0xbef9a3f7u, 0xc67178f2u,
    )
    fun rrot(x: UInt, n: Int) = (x shr n) or (x shl (32 - n))
    val ml = message.size.toULong() * 8uL
    val m = message + 0x80.toByte() + ByteArray(((56 - (message.size + 1) % 64 + 64) % 64)) +
        byteArrayOf(
            (ml shr 56).toByte(), (ml shr 48).toByte(), (ml shr 40).toByte(), (ml shr 32).toByte(),
            (ml shr 24).toByte(), (ml shr 16).toByte(), (ml shr 8).toByte(), ml.toByte(),
        )
    var h0 = 0x6a09e667u; var h1 = 0xbb67ae85u; var h2 = 0x3c6ef372u; var h3 = 0xa54ff53au
    var h4 = 0x510e527fu; var h5 = 0x9b05688cu; var h6 = 0x1f83d9abu; var h7 = 0x5be0cd19u
    for (chunk in m.indices step 64) {
        val w = UIntArray(64)
        for (t in 0..15) {
            val p = chunk + t * 4; val b0 = m[p].toUInt(); val b1 = m[p + 1].toUInt()
            val b2 = m[p + 2].toUInt(); val b3 = m[p + 3].toUInt()
            w[t] = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }
        for (t in 16..63) {
            val s0 = rrot(w[t - 15], 7) xor rrot(w[t - 15], 18) xor (w[t - 15] shr 3)
            val s1 = rrot(w[t - 2], 17) xor rrot(w[t - 2], 19) xor (w[t - 2] shr 10)
            w[t] = w[t - 16] + s0 + w[t - 7] + s1
        }
        var a = h0; var b = h1; var c = h2; var d = h3; var e = h4; var f = h5; var g = h6; var h = h7
        for (t in 0..63) {
            val s1 = rrot(e, 6) xor rrot(e, 11) xor rrot(e, 25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = h + s1 + ch + k[t] + w[t]
            val s0 = rrot(a, 2) xor rrot(a, 13) xor rrot(a, 22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = s0 + maj
            h = g; g = f; f = e; e = d + temp1; d = c; c = b; b = a; a = temp1 + temp2
        }
        h0 += a; h1 += b; h2 += c; h3 += d; h4 += e; h5 += f; h6 += g; h7 += h
    }
    return byteArrayOf(
        (h0 shr 24).toByte(), (h0 shr 16).toByte(), (h0 shr 8).toByte(), h0.toByte(),
        (h1 shr 24).toByte(), (h1 shr 16).toByte(), (h1 shr 8).toByte(), h1.toByte(),
        (h2 shr 24).toByte(), (h2 shr 16).toByte(), (h2 shr 8).toByte(), h2.toByte(),
        (h3 shr 24).toByte(), (h3 shr 16).toByte(), (h3 shr 8).toByte(), h3.toByte(),
        (h4 shr 24).toByte(), (h4 shr 16).toByte(), (h4 shr 8).toByte(), h4.toByte(),
        (h5 shr 24).toByte(), (h5 shr 16).toByte(), (h5 shr 8).toByte(), h5.toByte(),
        (h6 shr 24).toByte(), (h6 shr 16).toByte(), (h6 shr 8).toByte(), h6.toByte(),
        (h7 shr 24).toByte(), (h7 shr 16).toByte(), (h7 shr 8).toByte(), h7.toByte(),
    )
}

// ─── SHA-1 ────────────────────────────────────────────────────────────────

private fun sha1(message: ByteArray): ByteArray {
    fun lrot(x: UInt, n: Int) = (x shl n) or (x shr (32 - n))
    val ml = message.size.toULong() * 8uL
    val padded = message + 0x80.toByte() + ByteArray(((56 - (message.size + 1) % 64 + 64) % 64)) +
        byteArrayOf(
            (ml shr 56).toByte(), (ml shr 48).toByte(), (ml shr 40).toByte(), (ml shr 32).toByte(),
            (ml shr 24).toByte(), (ml shr 16).toByte(), (ml shr 8).toByte(), ml.toByte(),
        )
    var h0 = 0x67452301u; var h1 = 0xEFCDAB89u; var h2 = 0x98BADCFEu; var h3 = 0x10325476u; var h4 = 0xC3D2E1F0u
    for (chunk in padded.indices step 64) {
        val w = UIntArray(80)
        for (t in 0..15) {
            val p = chunk + t * 4; val b0 = padded[p].toUInt(); val b1 = padded[p + 1].toUInt()
            val b2 = padded[p + 2].toUInt(); val b3 = padded[p + 3].toUInt()
            w[t] = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }
        for (t in 16..79) w[t] = lrot(w[t - 3] xor w[t - 8] xor w[t - 14] xor w[t - 16], 1)
        var a = h0; var b = h1; var c = h2; var d = h3; var e = h4
        for (t in 0..79) {
            val f: UInt
            val kVal: UInt
            when {
                t < 20 -> { f = (b and c) or (b.inv() and d); kVal = 0x5A827999u }
                t < 40 -> { f = b xor c xor d; kVal = 0x6ED9EBA1u }
                t < 60 -> { f = (b and c) or (b and d) or (c and d); kVal = 0x8F1BBCDCu }
                else -> { f = b xor c xor d; kVal = 0xCA62C1D6u }
            }
            val temp = lrot(a, 5) + f + e + kVal + w[t]
            e = d; d = c; c = lrot(b, 30); b = a; a = temp
        }
        h0 += a; h1 += b; h2 += c; h3 += d; h4 += e
    }
    return byteArrayOf(
        (h0 shr 24).toByte(), (h0 shr 16).toByte(), (h0 shr 8).toByte(), h0.toByte(),
        (h1 shr 24).toByte(), (h1 shr 16).toByte(), (h1 shr 8).toByte(), h1.toByte(),
        (h2 shr 24).toByte(), (h2 shr 16).toByte(), (h2 shr 8).toByte(), h2.toByte(),
        (h3 shr 24).toByte(), (h3 shr 16).toByte(), (h3 shr 8).toByte(), h3.toByte(),
        (h4 shr 24).toByte(), (h4 shr 16).toByte(), (h4 shr 8).toByte(), h4.toByte(),
    )
}

// ─── SHA-512 ──────────────────────────────────────────────────────────────

private fun sha512(message: ByteArray): ByteArray {
    val k = ulongArrayOf(
        0x428a2f98d728ae22uL, 0x7137449123ef65cduL, 0xb5c0fbcfec4d3b2fuL, 0xe9b5dba58189dbbcuL,
        0x3956c25bf348b538uL, 0x59f111f1b605d019uL, 0x923f82a4af194f9buL, 0xab1c5ed5da6d8118uL,
        0xd807aa98a3030242uL, 0x12835b0145706fbeuL, 0x243185be4ee4b28cuL, 0x550c7dc3d5ffb4e2uL,
        0x72be5d74f27b896fuL, 0x80deb1fe3b1696b1uL, 0x9bdc06a725c71235uL, 0xc19bf174cf692694uL,
        0xe49b69c19ef14ad2uL, 0xefbe4786384f25e3uL, 0x0fc19dc68b8cd5b5uL, 0x240ca1cc77ac9c65uL,
        0x2de92c6f592b0275uL, 0x4a7484aa6ea6e483uL, 0x5cb0a9dcbd41fbd4uL, 0x76f988da831153b5uL,
        0x983e5152ee66dfabuL, 0xa831c66d2db43210uL, 0xb00327c898fb213fuL, 0xbf597fc7beef0ee4uL,
        0xc6e00bf33da88fc2uL, 0xd5a79147930aa725uL, 0x06ca6351e003826fuL, 0x142929670a0e6e70uL,
        0x27b70a8546d22ffcuL, 0x2e1b21385c26c926uL, 0x4d2c6dfc5ac42aeduL, 0x53380d139d95b3dfuL,
        0x650a73548baf63deuL, 0x766a0abb3c77b2a8uL, 0x81c2c92e47edaee6uL, 0x92722c851482353buL,
        0xa2bfe8a14cf10364uL, 0xa81a664bbc423001uL, 0xc24b8b70d0f89791uL, 0xc76c51a30654be30uL,
        0xd192e819d6ef5218uL, 0xd69906245565a910uL, 0xf40e35855771202auL, 0x106aa07032bbd1b8uL,
        0x19a4c116b8d2d0c8uL, 0x1e376c085141ab53uL, 0x2748774cdf8eeb99uL, 0x34b0bcb5e19b48a8uL,
        0x391c0cb3c5c95a63uL, 0x4ed8aa4ae3418acbuL, 0x5b9cca4f7763e373uL, 0x682e6ff3d6b2b8a3uL,
        0x748f82ee5defb2fcuL, 0x78a5636f43172f60uL, 0x84c87814a1f0ab72uL, 0x8cc702081a6439ecuL,
        0x90befffa23631e28uL, 0xa4506cebde82bde9uL, 0xbef9a3f7b2c67915uL, 0xc67178f2e372532buL,
        0xca273eceea26619cuL, 0xd186b8c721c0c207uL, 0xeada7dd6cde0eb1euL, 0xf57d4f7fee6ed178uL,
        0x06f067aa72176fbauL, 0x0a637dc5a2c898a6uL, 0x113f9804bef90daeuL, 0x1b710b35131c471buL,
        0x28db77f523047d84uL, 0x32caab7b40c72493uL, 0x3c9ebe0a15c9bebcuL, 0x431d67c49c100d4cuL,
        0x4cc5d4becb3e42b6uL, 0x597f299cfc657e2auL, 0x5fcb6fab3ad6faeCuL, 0x6c44198c4a475817uL,
    )
    fun rrot(x: ULong, n: Int) = (x shr n) or (x shl (64 - n))
    val ml = message.size.toULong() * 8uL
    val padded = message + 0x80.toByte() + ByteArray(((112 - (message.size + 1) % 128 + 128) % 128)) +
        ByteArray(8) + byteArrayOf(
            (ml shr 56).toByte(), (ml shr 48).toByte(), (ml shr 40).toByte(), (ml shr 32).toByte(),
            (ml shr 24).toByte(), (ml shr 16).toByte(), (ml shr 8).toByte(), ml.toByte(),
        )
    var h0 = 0x6a09e667f3bcc908uL; var h1 = 0xbb67ae8584caa73buL; var h2 = 0x3c6ef372fe94f82buL
    var h3 = 0xa54ff53a5f1d36f1uL; var h4 = 0x510e527fade682d1uL; var h5 = 0x9b05688c2b3e6c1fuL
    var h6 = 0x1f83d9abfb41bd6buL; var h7 = 0x5be0cd19137e2179uL
    for (chunk in padded.indices step 128) {
        val w = ULongArray(80)
        for (t in 0..15) {
            val p = chunk + t * 8; val b0 = padded[p].toULong(); val b1 = padded[p + 1].toULong()
            val b2 = padded[p + 2].toULong(); val b3 = padded[p + 3].toULong()
            val b4 = padded[p + 4].toULong(); val b5 = padded[p + 5].toULong()
            val b6 = padded[p + 6].toULong(); val b7 = padded[p + 7].toULong()
            w[t] = (b0 shl 56) or (b1 shl 48) or (b2 shl 40) or (b3 shl 32) or
                (b4 shl 24) or (b5 shl 16) or (b6 shl 8) or b7
        }
        for (t in 16..79) {
            val s0 = rrot(w[t - 15], 1) xor rrot(w[t - 15], 8) xor (w[t - 15] shr 7)
            val s1 = rrot(w[t - 2], 19) xor rrot(w[t - 2], 61) xor (w[t - 2] shr 6)
            w[t] = w[t - 16] + s0 + w[t - 7] + s1
        }
        var a = h0; var b = h1; var c = h2; var d = h3; var e = h4; var f = h5; var g = h6; var h = h7
        for (t in 0..79) {
            val s1 = rrot(e, 14) xor rrot(e, 18) xor rrot(e, 41)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = h + s1 + ch + k[t] + w[t]
            val s0 = rrot(a, 28) xor rrot(a, 34) xor rrot(a, 39)
            val maj = (a and b) xor (a and c) xor (b and c)
            h = g; g = f; f = e; e = d + temp1; d = c; c = b; b = a; a = temp1 + s0 + maj
        }
        h0 += a; h1 += b; h2 += c; h3 += d; h4 += e; h5 += f; h6 += g; h7 += h
    }
    return byteArrayOf(
        (h0 shr 56).toByte(), (h0 shr 48).toByte(), (h0 shr 40).toByte(), (h0 shr 32).toByte(),
        (h0 shr 24).toByte(), (h0 shr 16).toByte(), (h0 shr 8).toByte(), h0.toByte(),
        (h1 shr 56).toByte(), (h1 shr 48).toByte(), (h1 shr 40).toByte(), (h1 shr 32).toByte(),
        (h1 shr 24).toByte(), (h1 shr 16).toByte(), (h1 shr 8).toByte(), h1.toByte(),
        (h2 shr 56).toByte(), (h2 shr 48).toByte(), (h2 shr 40).toByte(), (h2 shr 32).toByte(),
        (h2 shr 24).toByte(), (h2 shr 16).toByte(), (h2 shr 8).toByte(), h2.toByte(),
        (h3 shr 56).toByte(), (h3 shr 48).toByte(), (h3 shr 40).toByte(), (h3 shr 32).toByte(),
        (h3 shr 24).toByte(), (h3 shr 16).toByte(), (h3 shr 8).toByte(), h3.toByte(),
        (h4 shr 56).toByte(), (h4 shr 48).toByte(), (h4 shr 40).toByte(), (h4 shr 32).toByte(),
        (h4 shr 24).toByte(), (h4 shr 16).toByte(), (h4 shr 8).toByte(), h4.toByte(),
        (h5 shr 56).toByte(), (h5 shr 48).toByte(), (h5 shr 40).toByte(), (h5 shr 32).toByte(),
        (h5 shr 24).toByte(), (h5 shr 16).toByte(), (h5 shr 8).toByte(), h5.toByte(),
        (h6 shr 56).toByte(), (h6 shr 48).toByte(), (h6 shr 40).toByte(), (h6 shr 32).toByte(),
        (h6 shr 24).toByte(), (h6 shr 16).toByte(), (h6 shr 8).toByte(), h6.toByte(),
        (h7 shr 56).toByte(), (h7 shr 48).toByte(), (h7 shr 40).toByte(), (h7 shr 32).toByte(),
        (h7 shr 24).toByte(), (h7 shr 16).toByte(), (h7 shr 8).toByte(), h7.toByte(),
    )
}

// ─── HMAC ─────────────────────────────────────────────────────────────────

private fun hmac(hash: (ByteArray) -> ByteArray, blockSize: Int, key: ByteArray, data: ByteArray): ByteArray {
    val k = if (key.size > blockSize) hash(key) else key
    val keyPadded = k + ByteArray(blockSize - k.size)
    val iPad = ByteArray(blockSize) { (keyPadded[it].toInt() xor 0x36).toByte() }
    val oPad = ByteArray(blockSize) { (keyPadded[it].toInt() xor 0x5c).toByte() }
    return hash(oPad + hash(iPad + data))
}

// ─── Hex encoding ──────────────────────────────────────────────────────────

private fun ByteArray.toHex() = joinToString("") { it.toUByte().toString(16).padStart(2, '0') }

// ─── Public API ────────────────────────────────────────────────────────────

actual fun pluginDigestHex(algorithm: String, data: String): String {
    val input = data.encodeToByteArray()
    val digest = when (algorithm.uppercase()) {
        "SHA1" -> sha1(input)
        "SHA256" -> sha256(input)
        "SHA512" -> sha512(input)
        "MD5" -> error("MD5 not available on iOS")
        else -> error("Unsupported digest algorithm: $algorithm")
    }
    return digest.toHex()
}

actual fun pluginHmacHex(algorithm: String, key: String, data: String): String {
    val keyBytes = key.encodeToByteArray()
    val dataBytes = data.encodeToByteArray()
    val digest = when (algorithm.uppercase()) {
        "SHA1" -> hmac(::sha1, 64, keyBytes, dataBytes)
        "SHA256" -> hmac(::sha256, 64, keyBytes, dataBytes)
        "SHA512" -> hmac(::sha512, 128, keyBytes, dataBytes)
        "MD5" -> error("MD5 not available on iOS")
        else -> error("Unsupported HMAC algorithm: $algorithm")
    }
    return digest.toHex()
}

actual fun pluginBase64Encode(data: String): String =
    (data as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        ?.base64EncodedStringWithOptions(0u) ?: error("Base64 encode failed")

actual fun pluginBase64Decode(data: String): String {
    val normalized = data.trim().replace("\n", "").replace("\r", "").replace(" ", "")
    val nsData = NSData.create(
        base64EncodedString = normalized,
        options = NSDataBase64DecodingIgnoreUnknownCharacters,
    ) ?: error("Base64 decode failed")
    return NSString.create(data = nsData, encoding = NSUTF8StringEncoding) as String
}
