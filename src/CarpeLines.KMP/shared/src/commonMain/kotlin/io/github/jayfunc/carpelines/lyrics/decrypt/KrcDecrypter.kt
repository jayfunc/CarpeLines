package io.github.jayfunc.carpelines.lyrics.decrypt

import io.github.jayfunc.carpelines.lyrics.platform.inflateZlib
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Port of Lyricify-Lyrics-Helper's KRC decrypter (Kugou lyrics). */
object KrcDecrypter {

    private val KEY = byteArrayOf(
        0x40, 0x47, 0x61, 0x77, 0x5e, 0x32, 0x74, 0x47,
        0x51, 0x36, 0x31, 0x2d, 0xce.toByte(), 0xd2.toByte(), 0x6e, 0x69,
    )

    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(encrypted: String?): String? {
        if (encrypted.isNullOrBlank()) return null
        return runCatching {
            val raw = Base64.decode(encrypted.trim())
            if (raw.size < 5) return@runCatching null
            val body = raw.copyOfRange(4, raw.size)
            for (index in body.indices) {
                body[index] = (body[index].toInt() xor KEY[index % KEY.size].toInt()).toByte()
            }
            val inflated = inflateZlib(body).decodeToString()
            inflated.drop(1)
        }.getOrNull()
    }
}
