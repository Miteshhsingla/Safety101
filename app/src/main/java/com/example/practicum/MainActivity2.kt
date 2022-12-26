package com.example.practicum

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.practicum.databinding.ActivityMain2Binding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityMain2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        initview()
        binding.b1.setOnClickListener{
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

        }
        binding.bt2.setOnClickListener{
            Firebase.auth.signOut()
            var intent = Intent(this, GetStarted::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun initview() {
        binding.mapview.settings.javaScriptEnabled = true
        binding.mapview.loadUrl("https://www.google.com/maps/search/police+station/")
    }
}