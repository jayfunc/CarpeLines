package io.github.jayfunc.carpelines.lyrics.platform

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

actual fun inflateZlib(data: ByteArray): ByteArray {
    val inflater = Inflater()
    inflater.setInput(data)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    try {
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) break
                continue
            }
            output.write(buffer, 0, count)
        }
    } finally {
        inflater.end()
    }
    return output.toByteArray()
}

actual fun md5Hex(data: ByteArray): String =
    MessageDigest.getInstance("MD5").digest(data).toHexString().lowercase()

actual fun epochMillis(): Long = System.currentTimeMillis()

actual fun aesEcbPkcs7EncryptHex(data: ByteArray, key: ByteArray): String {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
    return cipher.doFinal(data).toHexString().uppercase()
}

private fun ByteArray.toHexString(): String = buildString(size * 2) {
    for (byte in this@toHexString) {
        append(((byte.toInt() and 0xFF).toString(16)).padStart(2, '0'))
    }
}
