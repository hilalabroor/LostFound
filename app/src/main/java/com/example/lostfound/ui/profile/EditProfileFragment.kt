package com.example.lostfound.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentEditProfileBinding
import com.example.lostfound.model.User
import com.example.lostfound.utils.FirebaseConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EditProfileFragment : Fragment() {

    private var _binding:
            FragmentEditProfileBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var auth:
            FirebaseAuth

    private lateinit var userReference:
            DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentEditProfileBinding.inflate(
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

        val currentUser = auth.currentUser

        if (currentUser == null) {

            findNavController().navigateUp()
            return
        }

        userReference =
            FirebaseDatabase
                .getInstance(
                    FirebaseConfig.DATABASE_URL
                )
                .getReference("users")
                .child(currentUser.uid)

        binding.etEmail.setText(
            currentUser.email.orEmpty()
        )

        setupClickListeners()
        loadCurrentProfile()
    }

    private fun setupClickListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSaveProfile.setOnClickListener {
            validateAndSaveProfile()
        }
    }

    private fun loadCurrentProfile() {

        setLoading(true)

        userReference
            .get()
            .addOnCompleteListener { task ->

                if (_binding == null) {
                    return@addOnCompleteListener
                }

                setLoading(false)

                if (!task.isSuccessful) {

                    showMessage(
                        task.exception?.localizedMessage
                            ?: getString(
                                R.string.profile_not_found
                            )
                    )

                    return@addOnCompleteListener
                }

                val user =
                    task.result.getValue(
                        User::class.java
                    )

                if (user == null) {

                    showMessage(
                        getString(
                            R.string.profile_not_found
                        )
                    )

                    return@addOnCompleteListener
                }

                binding.etName.setText(
                    user.name
                )

                binding.etPhone.setText(
                    user.phone
                )
            }
    }

    private fun validateAndSaveProfile() {

        clearErrors()

        val name =
            binding.etName.text
                .toString()
                .trim()

        val phone =
            binding.etPhone.text
                .toString()
                .trim()

        if (name.length < 3) {

            binding.etName.error =
                "Nama minimal 3 karakter"

            binding.etName.requestFocus()
            return
        }

        if (phone.length < 10) {

            binding.etPhone.error =
                "Nomor WhatsApp minimal 10 angka"

            binding.etPhone.requestFocus()
            return
        }

        /*
         * Memastikan nomor hanya berisi angka
         * atau karakter + pada bagian awal.
         */
        val validPhone =
            phone.matches(
                Regex("^\\+?[0-9]{10,15}$")
            )

        if (!validPhone) {

            binding.etPhone.error =
                "Format nomor WhatsApp tidak valid"

            binding.etPhone.requestFocus()
            return
        }

        saveProfile(
            name = name,
            phone = phone
        )
    }

    private fun saveProfile(
        name: String,
        phone: String
    ) {

        setLoading(true)

        val updates =
            mapOf<String, Any>(
                "name" to name,
                "phone" to phone
            )

        userReference
            .updateChildren(updates)
            .addOnSuccessListener {

                if (_binding == null) {
                    return@addOnSuccessListener
                }

                setLoading(false)

                Toast.makeText(
                    requireContext(),
                    R.string.profile_updated,
                    Toast.LENGTH_LONG
                ).show()

                findNavController().navigateUp()
            }
            .addOnFailureListener { exception ->

                if (_binding == null) {
                    return@addOnFailureListener
                }

                setLoading(false)

                showMessage(
                    getString(
                        R.string.profile_update_failed,
                        exception.localizedMessage
                            ?: "Kesalahan database"
                    )
                )
            }
    }

    private fun setLoading(
        isLoading: Boolean
    ) {

        binding.progressBar.visibility =
            if (isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnSaveProfile.isEnabled =
            !isLoading

        binding.btnBack.isEnabled =
            !isLoading

        binding.etName.isEnabled =
            !isLoading

        binding.etPhone.isEnabled =
            !isLoading

        binding.btnSaveProfile.text =
            if (isLoading) {
                getString(
                    R.string.saving_profile
                )
            } else {
                getString(
                    R.string.save_profile
                )
            }
    }

    private fun clearErrors() {

        binding.etName.error = null
        binding.etPhone.error = null
    }

    private fun showMessage(
        message: String
    ) {

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}