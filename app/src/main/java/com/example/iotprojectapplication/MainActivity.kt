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

    // class init 메서드와 유사. 처음 페이지 생성 시에 실행
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    // 현 화면이 활성화 중에는 계속 실행
    override fun onResume() {
        super.onResume()

        var count = 0 // 화면이 갱신 되고 있음을 알려줌(이후 데이터 수집의 counting 변수로 사용 예정)
        val apText: TextView = findViewById(R.id.check)// 화면에 보여주는 Text 객체 선언

        // RSSI 데이터 수집하는 메서드
        val updateWifiInfo = object : Runnable {
            override fun run() {
                // Permission 여부 확인
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Wi-Fi 스캔 결과 가져오기
                    wifiManager.startScan()
                    var apLists = wifiManager.scanResults

                    // String 변수에 각 데이터들 저장
                    var checkAP = "${count++}: ${apLists.size}\n"
                    for (ap in apLists) {
                        checkAP += "[${ap.SSID}] [${ap.BSSID}] ${ap.level}\n"
                    }

                    apText.text = checkAP // TextView 업데이트
                } else {
                    // Permission에 문제 있음
                    apText.text = "Location permission required!"
                }

                //out of range 예방(데이터 모집 시간 연산에 이용할 예정)
                if(count == 100) count = 0;

                //500ms 마다 반복 하도록
                handler.postDelayed(this, 500)
            }
        }

        // 첫 수행
        handler.post(updateWifiInfo)
    }

    // 페이지 종료 시 호출
    override fun onPause() {
        super.onPause()
        // 리시버 해제
        unregisterReceiver(wifiReceiver)
    }
}
