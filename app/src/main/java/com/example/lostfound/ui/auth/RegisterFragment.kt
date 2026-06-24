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
import com.example.lostfound.databinding.FragmentRegisterBinding
import com.example.lostfound.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.lostfound.utils.FirebaseConfig

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Referensi menuju node "users" di Realtime Database
    private lateinit var usersReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRegisterBinding.inflate(
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

        // Menghubungkan kode dengan Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Menunjuk node "users" pada Realtime Database
        usersReference = FirebaseDatabase
            .getInstance(FirebaseConfig.DATABASE_URL)
            .getReference("users")

        setupClickListeners()
    }

    private fun setupClickListeners() {

        // Kembali ke halaman sebelumnya
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Jalankan validasi ketika tombol daftar ditekan
        binding.btnRegister.setOnClickListener {
            validateRegisterForm()
        }

        // Pindah ke halaman Login
        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(
                R.id.action_registerFragment_to_loginFragment
            )
        }
    }

    private fun validateRegisterForm() {

        // Mengambil nilai dari setiap EditText
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword =
            binding.etConfirmPassword.text.toString()

        var isValid = true

        // Validasi nama
        if (name.isEmpty()) {
            binding.etName.error = "Nama wajib diisi"
            binding.etName.requestFocus()
            isValid = false
        }

        // Validasi email
        if (email.isEmpty()) {
            binding.etEmail.error = "Email wajib diisi"

            if (isValid) {
                binding.etEmail.requestFocus()
            }

            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Format email tidak valid"

            if (isValid) {
                binding.etEmail.requestFocus()
            }

            isValid = false
        }

        // Validasi nomor WhatsApp
        if (phone.isEmpty()) {
            binding.etPhone.error = "Nomor WhatsApp wajib diisi"

            if (isValid) {
                binding.etPhone.requestFocus()
            }

            isValid = false
        } else if (phone.length < 10) {
            binding.etPhone.error =
                "Nomor WhatsApp minimal 10 angka"

            if (isValid) {
                binding.etPhone.requestFocus()
            }

            isValid = false
        }

        // Validasi password
        if (password.isEmpty()) {
            binding.etPassword.error = "Password wajib diisi"

            if (isValid) {
                binding.etPassword.requestFocus()
            }

            isValid = false
        } else if (password.length < 6) {
            binding.etPassword.error =
                "Password minimal 6 karakter"

            if (isValid) {
                binding.etPassword.requestFocus()
            }

            isValid = false
        }

        // Validasi konfirmasi password
        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error =
                "Konfirmasi password wajib diisi"

            if (isValid) {
                binding.etConfirmPassword.requestFocus()
            }

            isValid = false
        } else if (confirmPassword != password) {
            binding.etConfirmPassword.error =
                "Konfirmasi password tidak sama"

            if (isValid) {
                binding.etConfirmPassword.requestFocus()
            }

            isValid = false
        }

        // Jika semua input valid, mulai proses register
        if (isValid) {
            registerUser(
                name = name,
                email = email,
                phone = phone,
                password = password
            )
        }
    }

    private fun registerUser(
        name: String,
        email: String,
        phone: String,
        password: String
    ) {
        setLoading(true)

        /*
         * Tahap pertama:
         * membuat akun pada Firebase Authentication.
         */
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->

                if (_binding == null) {
                    return@addOnCompleteListener
                }

                if (!authTask.isSuccessful) {
                    setLoading(false)

                    val message =
                        authTask.exception?.localizedMessage
                            ?: "Pendaftaran akun gagal"

                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnCompleteListener
                }

                /*
                 * createUserWithEmailAndPassword berhasil.
                 * Pengguna otomatis menjadi pengguna aktif.
                 */
                val firebaseUser = auth.currentUser

                if (firebaseUser == null) {
                    setLoading(false)

                    Toast.makeText(
                        requireContext(),
                        "Data akun Firebase tidak ditemukan",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnCompleteListener
                }

                val uid = firebaseUser.uid

                /*
                 * Membuat objek profil yang akan disimpan
                 * ke Realtime Database.
                 */
                val user = User(
                    uid = uid,
                    name = name,
                    email = email,
                    phone = phone
                )

                /*
                 * Tahap kedua:
                 * menyimpan data ke users/UID.
                 */
                usersReference
                    .child(uid)
                    .setValue(user)
                    .addOnSuccessListener {

                        if (_binding == null) {
                            return@addOnSuccessListener
                        }

                        setLoading(false)

                        Toast.makeText(
                            requireContext(),
                            "Pendaftaran berhasil. Silakan masuk.",
                            Toast.LENGTH_LONG
                        ).show()

                        /*
                         * Akun otomatis login setelah register.
                         * Kita logout karena pengguna diarahkan ke Login.
                         */
                        auth.signOut()

                        findNavController().navigate(
                            R.id.action_registerFragment_to_loginFragment
                        )
                    }
                    .addOnFailureListener { exception ->

                        if (_binding == null) {
                            return@addOnFailureListener
                        }

                        setLoading(false)

                        /*
                         * Authentication berhasil tetapi profil gagal.
                         * Akun dihapus agar pengguna dapat mendaftar ulang
                         * dengan email yang sama.
                         */
                        firebaseUser.delete()
                            .addOnCompleteListener {
                                auth.signOut()
                            }

                        Toast.makeText(
                            requireContext(),
                            "Profil gagal disimpan: " +
                                    (exception.localizedMessage
                                        ?: "Kesalahan database"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }

    private fun setLoading(isLoading: Boolean) {

        // Mencegah tombol ditekan berkali-kali
        binding.btnRegister.isEnabled = !isLoading

        // Mengubah teks tombol
        binding.btnRegister.text =
            if (isLoading) {
                "Memproses..."
            } else {
                "Daftar Akun"
            }

        // Menonaktifkan input saat sedang memproses
        binding.etName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}