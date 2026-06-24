package com.example.lostfound.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentSplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSplashBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.root.postDelayed(
            {
                if (_binding != null) {
                    checkLoginSession()
                }
            },
            2000
        )
    }

    private fun checkLoginSession() {

        /*
         * currentUser berisi pengguna yang sedang login.
         * Jika belum ada pengguna, nilainya null.
         */
        val currentUser = auth.currentUser

        if (currentUser != null) {

            // Pengguna sudah login, langsung menuju Home
            findNavController().navigate(
                R.id.action_splashFragment_to_homeFragment
            )

        } else {

            // Pengguna belum login, menuju halaman Welcome
            findNavController().navigate(
                R.id.action_splashFragment_to_welcomeFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}