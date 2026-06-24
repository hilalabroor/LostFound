package com.example.lostfound.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentProfileBinding
import com.example.lostfound.local.AppDatabase
import com.example.lostfound.local.BookmarkDao
import com.example.lostfound.model.Item
import com.example.lostfound.model.User
import com.example.lostfound.utils.FirebaseConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var bookmarkDao: BookmarkDao

    private var reportsQuery: Query? = null
    private var reportsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProfileBinding.inflate(
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

        bookmarkDao = AppDatabase
            .getDatabase(requireContext())
            .bookmarkDao()

        val currentUser = auth.currentUser

        if (currentUser == null) {

            findNavController().navigate(
                R.id.welcomeFragment
            )

            return
        }

        binding.tvEmailValue.text =
            currentUser.email
                ?: getString(
                    R.string.email_not_available
                )

        loadProfile(currentUser.uid)
        loadReportStatistics(currentUser.uid)
        observeBookmarkCount(currentUser.uid)
        setupClickListeners()
    }

    private fun loadProfile(uid: String) {

        FirebaseDatabase
            .getInstance(FirebaseConfig.DATABASE_URL)
            .getReference("users")
            .child(uid)
            .get()
            .addOnCompleteListener { task ->

                if (_binding == null) {
                    return@addOnCompleteListener
                }

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

                displayProfile(user)
            }
    }

    private fun displayProfile(user: User) {

        val displayName =
            user.name.ifBlank {
                "Pengguna"
            }

        binding.tvProfileName.text =
            displayName

        binding.tvNameValue.text =
            displayName

        binding.tvPhoneValue.text =
            user.phone.ifBlank {
                getString(
                    R.string.phone_not_available
                )
            }

        /*
         * Mengambil huruf pertama nama
         * untuk avatar.
         */
        binding.tvAvatar.text =
            displayName
                .trim()
                .firstOrNull()
                ?.uppercase()
                ?: "U"
    }

    private fun loadReportStatistics(uid: String) {

        val query =
            FirebaseDatabase
                .getInstance(
                    FirebaseConfig.DATABASE_URL
                )
                .getReference("items")
                .orderByChild("userId")
                .equalTo(uid)

        val listener =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    if (_binding == null) {
                        return
                    }

                    var totalReports = 0
                    var activeReports = 0
                    var completedReports = 0

                    for (
                    itemSnapshot
                    in snapshot.children
                    ) {

                        val item =
                            itemSnapshot.getValue(
                                Item::class.java
                            )
                                ?: continue

                        totalReports++

                        if (item.status == STATUS_RETURNED) {
                            completedReports++
                        } else {
                            activeReports++
                        }
                    }

                    binding.tvTotalReports.text =
                        totalReports.toString()

                    binding.tvActiveReports.text =
                        activeReports.toString()

                    binding.tvCompletedReports.text =
                        completedReports.toString()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    if (_binding == null) {
                        return
                    }

                    showMessage(error.message)
                }
            }

        reportsQuery = query
        reportsListener = listener

        query.addValueEventListener(listener)
    }

    private fun observeBookmarkCount(uid: String) {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                bookmarkDao
                    .observeBookmarks(uid)
                    .collectLatest { bookmarks ->

                        if (_binding == null) {
                            return@collectLatest
                        }

                        binding.tvBookmarkCount.text =
                            bookmarks.size.toString()
                    }
            }
    }

    private fun setupClickListeners() {

        binding.btnEditProfile.setOnClickListener {

            findNavController().navigate(
                R.id.action_profileFragment_to_editProfileFragment
            )
        }

        binding.btnLogout.setOnClickListener {

            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {

        AlertDialog.Builder(requireContext())
            .setTitle(
                R.string.logout_confirmation_title
            )
            .setMessage(
                R.string.logout_confirmation_message
            )
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .setPositiveButton(
                R.string.logout
            ) { _, _ ->

                logoutUser()
            }
            .show()
    }

    private fun logoutUser() {

        auth.signOut()

        Toast.makeText(
            requireContext(),
            R.string.logout_success,
            Toast.LENGTH_SHORT
        ).show()

        findNavController().navigate(
            R.id.action_profileFragment_to_welcomeFragment
        )
    }

    private fun showMessage(message: String) {

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {

        val query = reportsQuery
        val listener = reportsListener

        if (
            query != null &&
            listener != null
        ) {
            query.removeEventListener(listener)
        }

        reportsQuery = null
        reportsListener = null

        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val STATUS_RETURNED =
            "RETURNED"
    }
}