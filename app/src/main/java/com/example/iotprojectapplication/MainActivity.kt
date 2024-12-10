package com.example.iotprojectapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

    // 현 화면이 활성화 중에는 계속 실행
    override fun onResume() {
        super.onResume()

        val register: Button = findViewById(R.id.register)
        val detection : Button = findViewById(R.id.detection)// 위치 이름 등록


        register.setOnClickListener {
            startActivity(Intent(this, RegisterNewLocation::class.java))
        }

        detection.setOnClickListener {
            startActivity(Intent(this, LocationDetection::class.java))
        }
    }

}
