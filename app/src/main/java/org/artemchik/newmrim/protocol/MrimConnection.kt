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
                val line = reader.readLine()?.trim() ?: ""
                Log.d(TAG, "Redirector: $line")
                val parts = line.split(":")
                require(parts.size == 2) { "Invalid redirector response: $line" }
                Pair(parts[0], parts[1].toInt())
            } finally { runCatching { resolverSocket.close() } }
        }

    suspend fun connect(config: ServerConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val (targetHost, targetPort) = if (config.useRedirector) resolveServer(config) else Pair(config.host, config.port)
            Log.d(TAG, "Connecting to $targetHost:$targetPort")
            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(targetHost, targetPort), CONNECT_TIMEOUT)
            newSocket.soTimeout = 0
            newSocket.keepAlive = true
            newSocket.tcpNoDelay = true
            
            socket = newSocket
            outputStream = BufferedOutputStream(newSocket.getOutputStream())
            inputStream = BufferedInputStream(newSocket.getInputStream())
            isConnected = true
            startReading()
            Log.d(TAG, "Connected to $targetHost:$targetPort")
            Result.success(Unit)
        } catch (e: Exception) { 
            Log.e(TAG, "Connection failed: ${e.message}")
            disconnect()
            Result.failure(e) 
        }
    }

    fun canSend(): Boolean {
        return isConnected && socket?.let { it.isConnected && !it.isClosed && !it.isOutputShutdown } == true
    }

    suspend fun sendPacket(packet: MrimPacket): Boolean = withContext(Dispatchers.IO) {
        try {
            val out = outputStream ?: return@withContext false
            out.write(packet.toByteArray())
            out.flush()
            Log.d(TAG, ">>> 0x${packet.msgType.toString(16)} seq=${packet.seq}")
            true
        } catch (e: Exception) { 
            Log.e(TAG, "Send failed", e)
            disconnect()
            false
        }
    }

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            try { 
                while (isActive && isConnected) { 
                    val p = readPacket() ?: break
                    Log.d(TAG, "<<< 0x${p.msgType.toString(16)} seq=${p.seq}")
                    _incomingPackets.emit(p) 
                } 
            }
            catch (e: Exception) { 
                if (isActive) Log.e(TAG, "Read error", e) 
            }
            finally { 
                if (isConnected) disconnect() 
            }
        }
    }

    private fun readPacket(): MrimPacket? {
        val stream = inputStream ?: return null
        val headerBytes = readExact(stream, MrimConstants.HEADER_SIZE) ?: return null
        val dlenBuf = ByteBuffer.wrap(headerBytes, 16, 4).order(ByteOrder.LITTLE_ENDIAN)
        val dataLength = dlenBuf.int
        val bodyBytes = if (dataLength > 0) {
            if (dataLength > 1024 * 1024) return null // Protection against huge packets
            readExact(stream, dataLength) ?: return null 
        } else ByteArray(0)
        return MrimPacket.fromByteArray(headerBytes, bodyBytes)
    }

    private fun readExact(stream: InputStream, count: Int): ByteArray? {
        val buffer = ByteArray(count); var offset = 0
        while (offset < count) { 
            val read = try { stream.read(buffer, offset, count - offset) } catch (e: Exception) { -1 }
            if (read == -1) return null
            offset += read 
        }
        return buffer
    }

    fun disconnect() {
        isConnected = false
        readJob?.cancel()
        runCatching { inputStream?.close() }
        runCatching { outputStream?.close() }
        runCatching { socket?.close() }
        socket = null
        inputStream = null
        outputStream = null
        Log.d(TAG, "Disconnected")
    }
}
