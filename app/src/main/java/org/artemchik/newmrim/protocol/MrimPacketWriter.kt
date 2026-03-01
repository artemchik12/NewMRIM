package org.artemchik.newmrim.protocol

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

class MrimPacketWriter {
    private val stream = ByteArrayOutputStream()

    fun writeUL(value: UInt): MrimPacketWriter {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(value.toInt())
        stream.write(buf.array())
        return this
    }

    fun writeLPS(value: String): MrimPacketWriter {
        val charset = Charset.forName("windows-1251")
        val bytes = value.toByteArray(charset)
        writeUL(bytes.size.toUInt())
        if (bytes.isNotEmpty()) stream.write(bytes)
        return this
    }

    fun writeLPSAscii(value: String): MrimPacketWriter {
        val bytes = value.toByteArray(Charsets.US_ASCII)
        writeUL(bytes.size.toUInt())
        if (bytes.isNotEmpty()) stream.write(bytes)
        return this
    }

    fun writeLPSUtf16(value: String): MrimPacketWriter {
        val bytes = value.toByteArray(Charsets.UTF_16LE)
        writeUL(bytes.size.toUInt())
        if (bytes.isNotEmpty()) stream.write(bytes)
        return this
    }

    fun writeEmptyLPS(): MrimPacketWriter {
        writeUL(0u)
        return this
    }

    fun writeBytes(bytes: ByteArray): MrimPacketWriter {
        stream.write(bytes)
        return this
    }

    fun toByteArray(): ByteArray = stream.toByteArray()
    fun size(): Int = stream.size()
}
