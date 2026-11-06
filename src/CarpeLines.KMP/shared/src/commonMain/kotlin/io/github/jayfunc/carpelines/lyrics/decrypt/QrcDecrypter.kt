package io.github.jayfunc.carpelines.lyrics.decrypt

import io.github.jayfunc.carpelines.lyrics.platform.inflateZlib

/**
 * QQ Music QRC decrypter: a hex-encoded block stream followed by a zlib stream.
 *
 * QQ does not use FIPS DESede here. Its DES treats each block as two little-endian 32-bit
 * words and ships a slightly different S2 table, so platform DESede output is garbage. The
 * three 8-byte keys are applied as D(k1) → E(k2) → D(k3).
 */
object QrcDecrypter {

    private val KEY1 = "!@#)(NHL".encodeToByteArray()
    private val KEY2 = "123ZXC!@".encodeToByteArray()
    private val KEY3 = "!@#)(*$%".encodeToByteArray()

    private val schedule1 by lazy { QqDes.keySchedule(KEY1, decrypt = true) }
    private val schedule2 by lazy { QqDes.keySchedule(KEY2, decrypt = false) }
    private val schedule3 by lazy { QqDes.keySchedule(KEY3, decrypt = true) }

    fun decrypt(hex: String?): String? {
        if (hex.isNullOrBlank()) return null
        return runCatching {
            val bytes = hexToBytes(hex.trim())
            if (bytes.isEmpty() || bytes.size % 8 != 0) return@runCatching null
            val decrypted = bytes.copyOf()
            for (offset in decrypted.indices step 8) {
                QqDes.cryptBlock(decrypted, offset, schedule1)
                QqDes.cryptBlock(decrypted, offset, schedule2)
                QqDes.cryptBlock(decrypted, offset, schedule3)
            }
            inflateZlib(decrypted).decodeToString().removePrefix("\uFEFF")
        }.getOrNull()
    }

    private fun hexToBytes(hex: String): ByteArray {
        if (hex.length % 2 != 0) return ByteArray(0)
        return ByteArray(hex.length / 2) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }
}

private object QqDes {

    private val SBOX1 = intArrayOf(
        14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7,
        0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8,
        4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0,
        15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13,
    )
    private val SBOX2 = intArrayOf(
        15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10,
        3, 13, 4, 7, 15, 2, 8, 15, 12, 0, 1, 10, 6, 9, 11, 5,
        0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15,
        13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9,
    )
    private val SBOX3 = intArrayOf(
        10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8,
        13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1,
        13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7,
        1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12,
    )
    private val SBOX4 = intArrayOf(
        7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15,
        13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9,
        10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4,
        3, 15, 0, 6, 10, 10, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14,
    )
    private val SBOX5 = intArrayOf(
        2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9,
        14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6,
        4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14,
        11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3,
    )
    private val SBOX6 = intArrayOf(
        12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11,
        10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8,
        9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6,
        4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13,
    )
    private val SBOX7 = intArrayOf(
        4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1,
        13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6,
        1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2,
        6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12,
    )
    private val SBOX8 = intArrayOf(
        13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7,
        1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2,
        7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8,
        2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11,
    )
    private val ROUND_SHIFT = intArrayOf(1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)
    private val PERM_C = intArrayOf(
        56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1,
        58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35,
    )
    private val PERM_D = intArrayOf(
        62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5,
        60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3,
    )
    private val COMPRESSION = intArrayOf(
        13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9, 22, 18, 11, 3,
        25, 7, 15, 6, 26, 19, 12, 1, 40, 51, 30, 36, 46, 54, 29, 39,
        50, 44, 32, 47, 43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31,
    )

    private val IP_LEFT = intArrayOf(
        57, 49, 41, 33, 25, 17, 9, 1, 59, 51, 43, 35, 27, 19, 11, 3,
        61, 53, 45, 37, 29, 21, 13, 5, 63, 55, 47, 39, 31, 23, 15, 7,
    )
    private val IP_RIGHT = intArrayOf(
        56, 48, 40, 32, 24, 16, 8, 0, 58, 50, 42, 34, 26, 18, 10, 2,
        60, 52, 44, 36, 28, 20, 12, 4, 62, 54, 46, 38, 30, 22, 14, 6,
    )

    private val TOP_BIT = 0x80000000.toInt()

    fun keySchedule(key: ByteArray, decrypt: Boolean): Array<IntArray> {
        val schedule = Array(16) { IntArray(6) }
        var c = 0
        var d = 0
        for (i in 0 until 28) {
            c = c or bitNum(key, PERM_C[i], 31 - i)
            d = d or bitNum(key, PERM_D[i], 31 - i)
        }
        for (i in 0 until 16) {
            c = rotate(c, i)
            d = rotate(d, i)
            val target = if (decrypt) 15 - i else i
            val subKey = IntArray(6)
            for (j in 0 until 24) {
                subKey[j / 8] = subKey[j / 8] or bitNumIntR(c, COMPRESSION[j], 7 - (j % 8))
            }
            for (j in 24 until 48) {
                subKey[j / 8] = subKey[j / 8] or bitNumIntR(d, COMPRESSION[j] - 27, 7 - (j % 8))
            }
            schedule[target] = subKey
        }
        return schedule
    }

    private fun rotate(value: Int, round: Int): Int {
        val shift = ROUND_SHIFT[round]
        return ((value shl shift) or (value ushr (28 - shift))) and 0xFFFFFFF0.toInt()
    }

    fun cryptBlock(data: ByteArray, offset: Int, schedule: Array<IntArray>) {
        var left = 0
        var right = 0
        for (i in 0 until 32) {
            val dest = 31 - i
            left = left or (bit(data, offset, IP_LEFT[i]) shl dest)
            right = right or (bit(data, offset, IP_RIGHT[i]) shl dest)
        }
        for (round in 0 until 15) {
            val swap = right
            right = f(right, schedule[round]) xor left
            left = swap
        }
        left = f(right, schedule[15]) xor left

        data[offset + 3] = byteOf(
            right, 7, 7, left, 7, 6, right, 15, 5, left, 15, 4,
            right, 23, 3, left, 23, 2, right, 31, 1, left, 31, 0,
        )
        data[offset + 2] = byteOf(
            right, 6, 7, left, 6, 6, right, 14, 5, left, 14, 4,
            right, 22, 3, left, 22, 2, right, 30, 1, left, 30, 0,
        )
        data[offset + 1] = byteOf(
            right, 5, 7, left, 5, 6, right, 13, 5, left, 13, 4,
            right, 21, 3, left, 21, 2, right, 29, 1, left, 29, 0,
        )
        data[offset] = byteOf(
            right, 4, 7, left, 4, 6, right, 12, 5, left, 12, 4,
            right, 20, 3, left, 20, 2, right, 28, 1, left, 28, 0,
        )
        data[offset + 7] = byteOf(
            right, 3, 7, left, 3, 6, right, 11, 5, left, 11, 4,
            right, 19, 3, left, 19, 2, right, 27, 1, left, 27, 0,
        )
        data[offset + 6] = byteOf(
            right, 2, 7, left, 2, 6, right, 10, 5, left, 10, 4,
            right, 18, 3, left, 18, 2, right, 26, 1, left, 26, 0,
        )
        data[offset + 5] = byteOf(
            right, 1, 7, left, 1, 6, right, 9, 5, left, 9, 4,
            right, 17, 3, left, 17, 2, right, 25, 1, left, 25, 0,
        )
        data[offset + 4] = byteOf(
            right, 0, 7, left, 0, 6, right, 8, 5, left, 8, 4,
            right, 16, 3, left, 16, 2, right, 24, 1, left, 24, 0,
        )
    }

    private fun f(state: Int, key: IntArray): Int {
        val t1 = bitNumIntL(state, 31, 0) or
            ((state and 0xF0000000.toInt()) ushr 1) or
            bitNumIntL(state, 4, 5) or
            bitNumIntL(state, 3, 6) or
            ((state and 0x0F000000) ushr 3) or
            bitNumIntL(state, 8, 11) or
            bitNumIntL(state, 7, 12) or
            ((state and 0x00F00000) ushr 5) or
            bitNumIntL(state, 12, 17) or
            bitNumIntL(state, 11, 18) or
            ((state and 0x000F0000) ushr 7) or
            bitNumIntL(state, 16, 23)

        val t2 = bitNumIntL(state, 15, 0) or
            ((state and 0x0000F000) shl 15) or
            bitNumIntL(state, 20, 5) or
            bitNumIntL(state, 19, 6) or
            ((state and 0x00000F00) shl 13) or
            bitNumIntL(state, 24, 11) or
            bitNumIntL(state, 23, 12) or
            ((state and 0x000000F0) shl 11) or
            bitNumIntL(state, 28, 17) or
            bitNumIntL(state, 27, 18) or
            ((state and 0x0000000F) shl 9) or
            bitNumIntL(state, 0, 23)

        val lrg = intArrayOf(
            ((t1 ushr 24) and 0xFF) xor key[0],
            ((t1 ushr 16) and 0xFF) xor key[1],
            ((t1 ushr 8) and 0xFF) xor key[2],
            ((t2 ushr 24) and 0xFF) xor key[3],
            ((t2 ushr 16) and 0xFF) xor key[4],
            ((t2 ushr 8) and 0xFF) xor key[5],
        )

        var result = (SBOX1[sBoxBit(lrg[0] shr 2)] shl 28) or
            (SBOX2[sBoxBit(((lrg[0] and 0x03) shl 4) or (lrg[1] shr 4))] shl 24) or
            (SBOX3[sBoxBit(((lrg[1] and 0x0F) shl 2) or (lrg[2] shr 6))] shl 20) or
            (SBOX4[sBoxBit(lrg[2] and 0x3F)] shl 16) or
            (SBOX5[sBoxBit(lrg[3] shr 2)] shl 12) or
            (SBOX6[sBoxBit(((lrg[3] and 0x03) shl 4) or (lrg[4] shr 4))] shl 8) or
            (SBOX7[sBoxBit(((lrg[4] and 0x0F) shl 2) or (lrg[5] shr 6))] shl 4) or
            SBOX8[sBoxBit(lrg[5] and 0x3F)]

        result = bitNumIntL(result, 15, 0) or
            bitNumIntL(result, 6, 1) or
            bitNumIntL(result, 19, 2) or
            bitNumIntL(result, 20, 3) or
            bitNumIntL(result, 28, 4) or
            bitNumIntL(result, 11, 5) or
            bitNumIntL(result, 27, 6) or
            bitNumIntL(result, 16, 7) or
            bitNumIntL(result, 0, 8) or
            bitNumIntL(result, 14, 9) or
            bitNumIntL(result, 22, 10) or
            bitNumIntL(result, 25, 11) or
            bitNumIntL(result, 4, 12) or
            bitNumIntL(result, 17, 13) or
            bitNumIntL(result, 30, 14) or
            bitNumIntL(result, 9, 15) or
            bitNumIntL(result, 1, 16) or
            bitNumIntL(result, 7, 17) or
            bitNumIntL(result, 23, 18) or
            bitNumIntL(result, 13, 19) or
            bitNumIntL(result, 31, 20) or
            bitNumIntL(result, 26, 21) or
            bitNumIntL(result, 2, 22) or
            bitNumIntL(result, 8, 23) or
            bitNumIntL(result, 18, 24) or
            bitNumIntL(result, 12, 25) or
            bitNumIntL(result, 29, 26) or
            bitNumIntL(result, 5, 27) or
            bitNumIntL(result, 21, 28) or
            bitNumIntL(result, 10, 29) or
            bitNumIntL(result, 3, 30) or
            bitNumIntL(result, 24, 31)

        return result
    }

    /** Composes one byte of the inverse IP from eight (state, bit index, target shift) triples. */
    private fun byteOf(vararg triples: Int): Byte {
        var value = 0
        var index = 0
        while (index < triples.size) {
            value = value or bitNumIntR(triples[index], triples[index + 1], triples[index + 2])
            index += 3
        }
        return value.toByte()
    }

    private fun bit(input: ByteArray, offset: Int, index: Int): Int {
        val byteIndex = index / 32 * 4 + 3 - index % 32 / 8
        return (input[offset + byteIndex].toInt() shr (7 - index % 8)) and 1
    }

    private fun bitNum(input: ByteArray, index: Int, shift: Int): Int =
        bit(input, 0, index) shl shift

    private fun bitNumIntR(value: Int, index: Int, shift: Int): Int =
        ((value ushr (31 - index)) and 1) shl shift

    private fun bitNumIntL(value: Int, index: Int, shift: Int): Int =
        ((value shl index) and TOP_BIT) ushr shift

    private fun sBoxBit(value: Int): Int =
        (value and 0x20) or ((value and 0x1F) shr 1) or ((value and 0x01) shl 4)
}
