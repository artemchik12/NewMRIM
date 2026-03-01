package org.artemchik.newmrim.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class MrimPacket(
    val magic: UInt = MrimConstants.CS_MAGIC,
    val proto: UInt = ((MrimConstants.PROTO_VERSION_MAJOR shl 16) or
            MrimConstants.PROTO_VERSION_MINOR).toUInt(),
    val seq: UInt = 0u,
    val msgType: UInt,
    val from: UInt = 0u,
    val fromPort: UInt = 0u,
    val reserved: ByteArray = ByteArray(16),
    val data: ByteArray = ByteArray(0)
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(MrimConstants.HEADER_SIZE + data.size)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(magic.toInt())
        buffer.putInt(proto.toInt())
        buffer.putInt(seq.toInt())
        buffer.putInt(msgType.toInt())
        buffer.putInt(data.size)
        buffer.putInt(from.toInt())
        buffer.putInt(fromPort.toInt())
        buffer.put(reserved, 0, 16)
        if (data.isNotEmpty()) buffer.put(data)
        return buffer.array()
    }

    companion object {
        fun fromByteArray(headerBytes: ByteArray, bodyBytes: ByteArray = ByteArray(0)): MrimPacket {
            val buf = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buf.int.toUInt()
            val proto = buf.int.toUInt()
            val seq = buf.int.toUInt()
            val msgType = buf.int.toUInt()
            buf.int
            val from = buf.int.toUInt()
            val fromPort = buf.int.toUInt()
            val reserved = ByteArray(16)
            buf.get(reserved)
            return MrimPacket(magic, proto, seq, msgType, from, fromPort, reserved, bodyBytes)
        }
    }
}
