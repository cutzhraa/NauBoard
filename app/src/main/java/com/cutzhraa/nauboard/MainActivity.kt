package com.cutzhraa.nauboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Buat tampilan sederhana secara programatis tanpa XML tambahan
        val button = Button(this).apply {
            text = "Aktifkan NauBoard"
            setOnClickListener {
                // Membuka pengaturan keyboard sistem
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }
        setContentView(button)
    }
}