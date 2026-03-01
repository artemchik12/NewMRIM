package org.artemchik.newmrim.protocol

import android.util.Log
import org.artemchik.newmrim.protocol.data.ServerConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MrimConnection {

    companion object {
        private const val TAG = "MrimConnection"
        private const val CONNECT_TIMEOUT = 15_000
    }

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private val _incomingPackets = MutableSharedFlow<MrimPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<MrimPacket> = _incomingPackets

    @Volatile
    var isConnected = false
        private set

    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private suspend fun resolveServer(config: ServerConfig): Pair<String, Int> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Resolving via ${config.host}:${config.port}")
            val resolverSocket = Socket()
            try {
                resolverSocket.connect(InetSocketAddress(config.host, config.port), CONNECT_TIMEOUT)
                val reader = BufferedReader(InputStreamReader(resolverSocket.getInputStream()))
                val line = reader.readLine().trim()
                Log.d(TAG, "Redirector: $line")
                val parts = line.split(":")
                require(parts.size == 2) { "Invalid redirector response: $line" }
                Pair(parts[0], parts[1].toInt())
            } finally { resolverSocket.close() }
        }

    suspend fun connect(config: ServerConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (targetHost, targetPort) = if (config.useRedirector) resolveServer(config) else Pair(config.host, config.port)
            Log.d(TAG, "Connecting to $targetHost:$targetPort")
            socket = Socket().apply { connect(InetSocketAddress(targetHost, targetPort), CONNECT_TIMEOUT); soTimeout = 0; keepAlive = true; tcpNoDelay = true }
            outputStream = BufferedOutputStream(socket!!.getOutputStream())
            inputStream = BufferedInputStream(socket!!.getInputStream())
            isConnected = true; startReading()
            Log.d(TAG, "Connected to $targetHost:$targetPort")
            Result.success(Unit)
        } catch (e: Exception) { Log.e(TAG, "Connection failed", e); disconnect(); Result.failure(e) }
    }

    suspend fun sendPacket(packet: MrimPacket) = withContext(Dispatchers.IO) {
        try {
            outputStream?.let { it.write(packet.toByteArray()); it.flush() }
            Log.d(TAG, ">>> 0x${packet.msgType.toString(16)} seq=${packet.seq}")
        } catch (e: Exception) { Log.e(TAG, "Send failed", e); disconnect(); throw e }
    }

    private fun startReading() {
        readJob = scope.launch {
            try { while (isActive && isConnected) { val p = readPacket() ?: break; Log.d(TAG, "<<< 0x${p.msgType.toString(16)} seq=${p.seq}"); _incomingPackets.emit(p) } }
            catch (e: Exception) { if (isActive) Log.e(TAG, "Read error", e) }
            finally { disconnect() }
        }
    }

    private fun readPacket(): MrimPacket? {
        val stream = inputStream ?: return null
        val headerBytes = readExact(stream, MrimConstants.HEADER_SIZE) ?: return null
        val dlenBuf = ByteBuffer.wrap(headerBytes, 16, 4).order(ByteOrder.LITTLE_ENDIAN)
        val dataLength = dlenBuf.int
        val bodyBytes = if (dataLength > 0) readExact(stream, dataLength) ?: return null else ByteArray(0)
        return MrimPacket.fromByteArray(headerBytes, bodyBytes)
    }

    private fun readExact(stream: InputStream, count: Int): ByteArray? {
        val buffer = ByteArray(count); var offset = 0
        while (offset < count) { val read = stream.read(buffer, offset, count - offset); if (read == -1) return null; offset += read }
        return buffer
    }

    fun disconnect() {
        if (!isConnected) return; isConnected = false; readJob?.cancel()
        runCatching { inputStream?.close() }; runCatching { outputStream?.close() }; runCatching { socket?.close() }
        socket = null; inputStream = null; outputStream = null; Log.d(TAG, "Disconnected")
    }
}
