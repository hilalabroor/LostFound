package com.example.lostfound.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    /*
     * _binding dapat bernilai null karena tampilan Fragment
     * bisa dihancurkan ketika pengguna berpindah halaman.
     */
    private var _binding: FragmentWelcomeBinding? = null

    /*
     * Properti binding hanya boleh digunakan
     * ketika tampilan Fragment masih aktif.
     */
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Menghubungkan Fragment dengan fragment_welcome.xml
        _binding = FragmentWelcomeBinding.inflate(
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

        setupClickListeners()
    }

    private fun setupClickListeners() {

        // Pindah dari Welcome ke Login
        binding.btnLogin.setOnClickListener {
            findNavController().navigate(
                R.id.action_welcomeFragment_to_loginFragment
            )
        }

        // Pindah dari Welcome ke Register
        binding.btnRegister.setOnClickListener {
            findNavController().navigate(
                R.id.action_welcomeFragment_to_registerFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        /*
         * Membersihkan binding saat tampilan Fragment dihancurkan
         * untuk mencegah kebocoran memori.
         */
        _binding = null
    }
}