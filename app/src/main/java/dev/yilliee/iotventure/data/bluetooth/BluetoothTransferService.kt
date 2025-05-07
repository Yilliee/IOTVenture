package dev.yilliee.iotventure.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import dev.yilliee.iotventure.data.model.Challenge
import dev.yilliee.iotventure.data.model.TeamMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.*

class BluetoothTransferService(private val context: Context) {
    companion object {
        private const val TAG = "BluetoothTransferService"
        private val SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val json = Json { ignoreUnknownKeys = true }
        private const val BUFFER_SIZE = 8192
        private const val CONNECTION_TIMEOUT = 10000L // 10 seconds timeout
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var isConnected = false

    @Serializable
    data class TransferData(
        val deviceToken: String,
        val username: String,
        val teamName: String,
        val password: String,
        val solvedChallenges: List<Int>,
        val challenges: List<Challenge>,
        val teamMessages: List<TeamMessage>,
        val serverIp: String,
        val serverPort: String
    )

    suspend fun startServer(onDataReceived: (TransferData) -> Unit) = withContext(Dispatchers.IO) {
        try {
            cleanup()
            
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("IOTVenture", SERVICE_UUID)
            Log.d(TAG, "Server socket created, waiting for connection...")
            
            val socket = serverSocket?.accept()
            Log.d(TAG, "Connection accepted")
            
            socket?.let {
                isConnected = true
                try {
                    // Wait a bit to ensure connection is stable
                    delay(1000)
                    
                    val inputStream = it.inputStream
                    val buffer = ByteArray(BUFFER_SIZE)
                    var totalBytesRead = 0
                    var bytesRead = 0
                    
                    // Read data with timeout
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < CONNECTION_TIMEOUT) {
                        if (inputStream.available() > 0) {
                            bytesRead = inputStream.read(buffer, totalBytesRead, BUFFER_SIZE - totalBytesRead)
                            if (bytesRead == -1) break
                            totalBytesRead += bytesRead
                            if (totalBytesRead >= BUFFER_SIZE) break
                        }
                        delay(100) // Small delay to prevent busy waiting
                    }
                    
                    if (totalBytesRead > 0) {
                        val jsonString = String(buffer, 0, totalBytesRead)
                        Log.d(TAG, "Received data length: $totalBytesRead")
                        Log.d(TAG, "Received JSON: $jsonString")
                        
                        try {
                            val transferData = json.decodeFromString<TransferData>(jsonString)
                            // Validate received data
                            if (transferData.deviceToken.isBlank() || 
                                transferData.username.isBlank() || 
                                transferData.teamName.isBlank() || 
                                transferData.password.isBlank()) {
                                throw IOException("Invalid transfer data: missing required fields")
                            }
                            Log.d(TAG, "Data validation successful")
                            onDataReceived(transferData)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing received data: ${e.message}")
                            throw e
                        }
                    } else {
                        throw IOException("No data received within timeout")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling data: ${e.message}")
                    throw e
                } finally {
                    isConnected = false
                    it.close()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error in server: ${e.message}")
            throw e
        } finally {
            cleanup()
        }
    }

    suspend fun startClient(device: BluetoothDevice, data: TransferData): Boolean = withContext(Dispatchers.IO) {
        try {
            cleanup()
            
            // Validate data before sending
            if (data.deviceToken.isBlank() || 
                data.username.isBlank() || 
                data.teamName.isBlank() || 
                data.password.isBlank()) {
                Log.e(TAG, "Invalid data: missing required fields")
                return@withContext false
            }
            
            clientSocket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
            Log.d(TAG, "Connecting to device: ${device.name}")
            
            clientSocket?.connect()
            Log.d(TAG, "Connected to device")
            
            // Wait a bit to ensure connection is stable
            delay(1000)
            
            isConnected = true
            
            val outputStream = clientSocket?.outputStream
            val jsonString = json.encodeToString(data)
            Log.d(TAG, "Sending JSON: $jsonString")
            val dataBytes = jsonString.toByteArray()
            
            // Write data in chunks with delay between chunks
            var bytesWritten = 0
            while (bytesWritten < dataBytes.size) {
                val remaining = dataBytes.size - bytesWritten
                val chunkSize = minOf(remaining, 1024) // Smaller chunks
                outputStream?.write(dataBytes, bytesWritten, chunkSize)
                bytesWritten += chunkSize
                delay(100) // Small delay between chunks
            }
            
            outputStream?.flush()
            Log.d(TAG, "Data sent successfully, total bytes: $bytesWritten")
            
            // Wait a bit to ensure data is sent
            delay(1000)
            
            return@withContext true
        } catch (e: IOException) {
            Log.e(TAG, "Error in client: ${e.message}")
            return@withContext false
        } finally {
            cleanup()
        }
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun cleanup() {
        try {
            if (isConnected) {
                isConnected = false
            }
            serverSocket?.close()
            clientSocket?.close()
            serverSocket = null
            clientSocket = null
        } catch (e: IOException) {
            Log.e(TAG, "Error cleaning up: ${e.message}")
        }
    }
} 