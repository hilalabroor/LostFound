package com.example.lostfound

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.lostfound.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {

        /*
         * Mengambil NavHostFragment dari Activity.
         * Menggunakan "as?" supaya aplikasi tidak langsung
         * crash jika Fragment tidak ditemukan.
         */
        val navHostFragment =
            supportFragmentManager.findFragmentById(
                R.id.navHostFragment
            ) as? NavHostFragment

        /*
         * Jika null, berarti ID activity_main.xml
         * tidak sesuai dengan MainActivity.
         */
        if (navHostFragment == null) {
            binding.bottomNavigation.visibility = View.GONE
            return
        }

        val navController =
            navHostFragment.navController

        /*
         * Menghubungkan Bottom Navigation dengan NavController.
         */
        binding.bottomNavigation.setupWithNavController(
            navController
        )

        /*
         * Bottom Navigation hanya tampil
         * pada lima halaman utama.
         */
        val mainDestinations = setOf(
            R.id.homeFragment,
            R.id.searchFragment,
            R.id.addReportFragment,
            R.id.bookmarksFragment,
            R.id.profileFragment
        )

        navController.addOnDestinationChangedListener {
                _,
                destination,
                _ ->

            binding.bottomNavigation.visibility =
                if (destination.id in mainDestinations) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }
}