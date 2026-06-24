package com.example.lostfound.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Objek Firebase Authentication
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(
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

        // Menghubungkan Fragment dengan Firebase Authentication
        auth = FirebaseAuth.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {

        // Tombol kembali
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Tombol masuk
        binding.btnLogin.setOnClickListener {
            validateLoginForm()
        }

        // Pindah ke halaman Register
        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment
            )
        }
    }

    private fun validateLoginForm() {

        // Mengambil email dari EditText
        val email = binding.etEmail.text
            .toString()
            .trim()

        // Mengambil password dari EditText
        val password = binding.etPassword.text
            .toString()

        var isValid = true

        // Validasi email kosong
        if (email.isEmpty()) {
            binding.etEmail.error = "Email wajib diisi"
            binding.etEmail.requestFocus()
            isValid = false

        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            // Validasi format email
            binding.etEmail.error = "Format email tidak valid"
            binding.etEmail.requestFocus()
            isValid = false
        }

        // Validasi password
        if (password.isEmpty()) {
            binding.etPassword.error = "Password wajib diisi"

            if (isValid) {
                binding.etPassword.requestFocus()
            }

            isValid = false
        }

        // Jika input valid, jalankan proses login
        if (isValid) {
            loginUser(
                email = email,
                password = password
            )
        }
    }

    private fun loginUser(
        email: String,
        password: String
    ) {

        // Mengubah tampilan menjadi sedang memproses
        setLoading(true)

        /*
         * Firebase memeriksa email dan password.
         * Proses ini berjalan secara asynchronous.
         */
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { loginTask ->

                /*
                 * Callback Firebase mungkin selesai setelah pengguna
                 * meninggalkan halaman. Karena itu periksa binding dahulu.
                 */
                if (_binding == null) {
                    return@addOnCompleteListener
                }

                setLoading(false)

                if (loginTask.isSuccessful) {

                    Toast.makeText(
                        requireContext(),
                        "Login berhasil",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Berpindah ke halaman Home
                    findNavController().navigate(
                        R.id.action_loginFragment_to_homeFragment
                    )

                } else {

                    val errorMessage =
                        when (loginTask.exception) {

                            is FirebaseAuthInvalidCredentialsException -> {
                                "Email atau password salah"
                            }

                            else -> {
                                loginTask.exception?.localizedMessage
                                    ?: "Login gagal"
                            }
                        }

                    Toast.makeText(
                        requireContext(),
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {

        // Mencegah tombol ditekan berkali-kali
        binding.btnLogin.isEnabled = !isLoading

        // Menonaktifkan input saat proses login berjalan
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading

        // Mengubah tulisan tombol
        binding.btnLogin.text =
            if (isLoading) {
                "Memproses..."
            } else {
                "Masuk"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}