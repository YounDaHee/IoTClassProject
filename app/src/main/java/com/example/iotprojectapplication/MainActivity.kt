package com.example.iotprojectapplication

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiReceiver: BroadcastReceiver // 리시버 선언
    private val handler = Handler(Looper.getMainLooper())
    var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    override fun onResume() {
        super.onResume()
        val checkResult: TextView = findViewById(R.id.check)


        // BroadcastReceiver 정의
        val updateWifiInfo = object : Runnable {
            override fun run() {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Wi-Fi 스캔 결과 가져오기
                    wifiManager.startScan()
                    var apLists = wifiManager.scanResults
                    var checkAP = "${count++}: ${apLists.size}\n"
                    for (ap in apLists) {
                        checkAP += "[${ap.SSID}] [${ap.BSSID}] ${ap.level}\n"
                    }
                    checkResult.text = checkAP // TextView 업데이트
                } else {
                    checkResult.text = "Location permission required!"
                }

                if(count == 100) count = 0;
                handler.postDelayed(this, 500)


            }
        }

        handler.post(updateWifiInfo)
    }

    override fun onPause() {
        super.onPause()
        // 리시버 해제
        unregisterReceiver(wifiReceiver)
    }
}
