package io.github.jayfunc.carpelines.lyrics.platform

/** Platform compression/crypto needed by Lyricify-compatible provider payloads. */
expect fun inflateZlib(data: ByteArray): ByteArray

expect fun md5Hex(data: ByteArray): String

expect fun epochMillis(): Long

/** AES/ECB/PKCS5Padding, returned as uppercase hex (NetEase EAPI `params`). */
expect fun aesEcbPkcs7EncryptHex(data: ByteArray, key: ByteArray): String
