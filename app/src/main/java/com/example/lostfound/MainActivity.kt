package com.example.lostfound

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lostfound.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Variabel binding untuk akses komponen XML
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Membuat objek binding dari activity_main.xml
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Menampilkan layout activity_main.xml ke layar
        setContentView(binding.root)

        // Aksi ketika tombol Masuk ditekan
        binding.btnLogin.setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.login_clicked),
                Toast.LENGTH_SHORT
            ).show()
        }

        // Aksi ketika tombol Daftar ditekan
        binding.btnRegister.setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.register_clicked),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}