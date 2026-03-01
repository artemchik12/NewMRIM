package org.artemchik.newmrim.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

class MrimPacketReader(data: ByteArray) {
    private val buffer: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    val remaining: Int get() = buffer.remaining()
    fun hasRemaining(): Boolean = buffer.hasRemaining()

    fun readUL(): UInt = buffer.int.toUInt()

    fun readLPS(): String {
        val length = buffer.int
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, Charset.forName("windows-1251"))
    }

    fun readLPSAscii(): String {
        val length = buffer.int
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    fun readLPSUtf16(): String {
        val length = buffer.int
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_16LE)
    }

    fun readBytes(count: Int): ByteArray {
        val bytes = ByteArray(count)
        buffer.get(bytes)
        return bytes
    }

    fun skip(count: Int) {
        buffer.position(buffer.position() + count)
    }
}
