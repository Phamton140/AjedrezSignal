package com.example.ajedrezsignal

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.ajedrezsignal.databinding.ActivityMainBinding
import com.example.ajedrezsignal.service.HapticChessService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Verificamos el permiso de Overlay (imprescindible para el motor de gestos)
        checkOverlayPermission()

        // 2. Iniciamos el servicio invisible
        startHapticService()

        // Mostramos el estado en el texto de la pantalla
        binding.sampleText.text = "Motor Háptico Iniciado\n(Pantalla casi negra activa)"
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 123)
            }
        }
    }

    private fun startHapticService() {
        val intent = Intent(this, HapticChessService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // Volvemos a intentar iniciar el servicio si el usuario acaba de dar el permiso
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            startHapticService()
        }
    }
}