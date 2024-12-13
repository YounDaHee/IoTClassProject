package com.example.iotprojectapplication

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.Socket

class RegisterNewLocation : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiReceiver: BroadcastReceiver
    private val handler = Handler(Looper.getMainLooper())

    private val TAG = "TCPServer"
    private lateinit var file_name : String
    private lateinit var file :File
    private lateinit var socket: Socket
    private var serverJob: Job? = null

    // 데이터 저장 위치 설정
    val folderName = "iotSensingDataSave"
    val customDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), folderName)

    // class init 메서드와 유사. 처음 페이지 생성 시에 실행
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_new_location)

        // 데이터 저장 폴더 생성
        if (!customDir.exists()) {
            val isCreated = customDir.mkdirs()
            if (!isCreated) {
                println("Failed to create directory!")
                return
            }
        }
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    // 현 화면이 활성화 중에는 계속 실행
    override fun onResume() {
        super.onResume()

        val sampelingRate : Long = 500  // 데이터 모으는 시간 간격
        val countSet = 100 // 데이터 모으는 수 셋팅
        // sampleing rate 500ms*100 = 50초동안 데이터 수집

        var count = 0

        // 센싱 데이터 저장을 위해
        lateinit var fileWriter: FileWriter

        val regiButton: Button = findViewById(R.id.registration)// 등록 버튼 객체 선언
        val dataName : EditText = findViewById(R.id.dataName)// 위치 이름 등록
        val countNum: TextView = findViewById(R.id.CountNum)// 카운팅
        val apNum: TextView = findViewById(R.id.TotalNum)// 공유기 수
        val apText: TextView = findViewById(R.id.allDataSet)// 현재 확인 가능한 공유기 데이터


        // RSSI 데이터 수집하는 메서드
        val updateWifiInfo = object : Runnable {

            override fun run() {
                try {
                    // Permission 여부 확인
                    if (ActivityCompat.checkSelfPermission(
                            this@RegisterNewLocation,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // Wi-Fi 스캔 결과 가져오기
                        wifiManager.startScan()
                        var apLists = (wifiManager.scanResults).sortedByDescending { it.level }

                        // String 변수에 각 데이터들 저장
                        countNum.text = (--count).toString()
                        apNum.text = apLists.size.toString()
                        var checkAP = ""
                        fileWriter.append("[")
                        val lastIndex = apLists.size-1
                        for ((index, ap) in apLists.withIndex()) {
                            checkAP += "[${ap.SSID}] [${ap.BSSID}] ${ap.level}\n"
                            fileWriter.append("{\"SSID\": \"${ap.SSID}\", \"MAC\":\"${ap.BSSID}\", \"RSSI\":${ap.level}}")

                            if (index != lastIndex) {
                                fileWriter.append(",")
                            }
                        }

                        apText.text = checkAP // TextView 업데이트
                    } else {
                        // Permission에 문제 있음
                        apText.text = "Location permission required!"
                        return
                    }

                    //out of range 예방(데이터 모집 시간 연산에 이용할 예정)
                    if (count > 0) {
                        //sampelingRate 밀리 초 마다 반복 하도록
                        fileWriter.append("],\n")
                        handler.postDelayed(this, sampelingRate)
                    } else { //파일 작성 완료
                        fileWriter.append("]]")
                        fileWriter.flush()
                        fileWriter.close()
                        regiButton.isEnabled = true

                        serverJob = CoroutineScope(Dispatchers.IO).launch {
                            sendText("서버 ip 주소",9800)
                        }
                    }
                }catch(e: IOException){
                    //파일을 열고, 닫는 과정에서 문제가 발생함
                    apText.text = e.toString()
                }
            }

        }

        regiButton.setOnClickListener {
            count = countSet
            if(dataName.text.toString() != ""){
                regiButton.isEnabled = false
                file = File(customDir, dataName.text.toString()+".txt")
                fileWriter  = FileWriter(file, false)
                fileWriter.append("[")
                file_name = dataName.text.toString()+".txt"
                handler.post(updateWifiInfo)
            }

        }
    }

    // 페이지 종료 시 호출
    override fun onPause() {
        super.onPause()
        // 리시버 해제
        socket?.close()
        serverJob?.cancel()
        unregisterReceiver(wifiReceiver)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    private suspend fun sendText(serverIp:String, port: Int){
        withContext(Dispatchers.IO) {
            try {
                socket = Socket(serverIp, port)
                val outputStream: OutputStream = socket.getOutputStream()
                val writer = OutputStreamWriter(socket.getOutputStream())

                writer.write("0")

                // 컴퓨터로 데이터 송신
                writer.write("#${file_name}")
                writer.flush()


                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                Log.d(TAG, "Send text file")

            } catch (e: Exception) {
                Log.e(TAG, "Error in server: ${e.message}")
            } finally {
                socket?.close()
                serverJob?.cancel()
            }
        }
    }
}
