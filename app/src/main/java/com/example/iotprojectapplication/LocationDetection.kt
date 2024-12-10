package com.example.iotprojectapplication

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class LocationDetection : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiReceiver: BroadcastReceiver
    private val handler = Handler(Looper.getMainLooper())

    private val TAG = "TCPServer"
    private val time_slice:Long = 500
    private var serverJob: Job? = null

    private lateinit var socket: Socket
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_location_detection)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // 코루틴을 사용해 별도 스레드에서 서버 소켓 시작
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            startTcp("125.177.165.67",9800)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 액티비티 종료 시 서버도 종료
        socket?.close()
        serverJob?.cancel()
    }

    private fun wifiData():String{
        var detectWifi = ""
        if(ActivityCompat.checkSelfPermission(
            this@LocationDetection,Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED){
            wifiManager.startScan()
            var apLists = (wifiManager.scanResults).sortedByDescending { it.level }

            detectWifi += "[[["
            val lastIndex = apLists.size-1
            for ((index, ap) in apLists.withIndex()) {
                detectWifi += "{\"SSID\": \"${ap.SSID}\", \"MAC\":\"${ap.BSSID}\", \"RSSI\":${ap.level}}"

                if (index != lastIndex) {
                    detectWifi += ","
                }
            }
            detectWifi += "]]]"
        }
        return detectWifi
    }

    private suspend fun startTcp(serverIp:String, port: Int) {
        val resultPosition: TextView = findViewById(R.id.position_result)

        withContext(Dispatchers.IO) {
            try {
                socket = Socket(serverIp, port)
                val writer = OutputStreamWriter(socket.getOutputStream())
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                while (true){
                    writer.write("#1")

                    // 컴퓨터로 데이터 송신
                    val response = wifiData()
                    writer.write("${response.length}")
                    writer.flush()
                    writer.write(response)
                    writer.flush()
                    Log.d(TAG, "Send Data to PC : ${response.length}")

                    //컴퓨터에서 핸드폰으로 데이터 송신
                    val receivedData = reader.readLine()
                    Log.d(TAG, "Received from PC: $receivedData")

                    withContext(Dispatchers.Main) {
                       resultPosition.text = "$receivedData"
                    }

                    delay(time_slice)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in server: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }
}
