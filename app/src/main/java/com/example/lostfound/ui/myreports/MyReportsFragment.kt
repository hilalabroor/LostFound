package com.example.lostfound.ui.myreports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lostfound.R
import com.example.lostfound.adapter.MyReportAdapter
import com.example.lostfound.databinding.FragmentMyReportsBinding
import com.example.lostfound.model.Item
import com.example.lostfound.ui.detail.DetailItemFragment
import com.example.lostfound.utils.FirebaseConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.example.lostfound.ui.report.EditReportFragment

class MyReportsFragment : Fragment() {

    private var _binding: FragmentMyReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private lateinit var itemsReference: DatabaseReference

    private lateinit var myReportAdapter: MyReportAdapter

    /*
     * Query dan listener disimpan supaya dapat
     * dilepas ketika halaman ditutup.
     */
    private var myReportsQuery: Query? = null
    private var myReportsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMyReportsBinding.inflate(
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

        initializeFirebase()
        setupRecyclerView()
        setupClickListeners()
        checkUserAndLoadReports()
    }

    private fun initializeFirebase() {

        auth = FirebaseAuth.getInstance()

        itemsReference = FirebaseDatabase
            .getInstance(FirebaseConfig.DATABASE_URL)
            .getReference("items")
    }

    private fun setupRecyclerView() {

        myReportAdapter = MyReportAdapter(

            onDetailClick = { item ->
                openItemDetail(item)
            },

            onEditClick = { item ->
                openEditReport(item)
            },

            onStatusClick = { item ->
                updateReportStatus(item)
            },

            onDeleteClick = { item ->
                showDeleteConfirmation(item)
            }
        )

        binding.rvMyReports.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        binding.rvMyReports.adapter =
            myReportAdapter

        binding.rvMyReports.setHasFixedSize(
            true
        )
    }

    private fun setupClickListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun checkUserAndLoadReports() {

        val currentUser = auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                requireContext(),
                R.string.session_not_found,
                Toast.LENGTH_LONG
            ).show()

            findNavController().navigate(
                R.id.welcomeFragment
            )

            return
        }

        loadMyReports(currentUser.uid)
    }

    /*
     * Mengambil laporan dengan userId
     * yang sama seperti UID pengguna login.
     */
    private fun loadMyReports(uid: String) {

        showLoadingState()

        val query = itemsReference
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

                    val reports =
                        mutableListOf<Item>()

                    for (
                    itemSnapshot
                    in snapshot.children
                    ) {

                        val item =
                            itemSnapshot.getValue(
                                Item::class.java
                            )

                        if (item != null) {
                            reports.add(item)
                        }
                    }

                    /*
                     * Laporan terbaru ditampilkan
                     * paling atas.
                     */
                    reports.sortByDescending {
                        it.createdAt
                    }

                    myReportAdapter.setReports(
                        reports
                    )

                    displayReportsState(
                        reports
                    )
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    if (_binding == null) {
                        return
                    }

                    displayErrorState(
                        error.message
                    )
                }
            }

        myReportsQuery = query
        myReportsListener = listener

        query.addValueEventListener(
            listener
        )
    }

    private fun showLoadingState() {

        binding.progressBar.visibility =
            View.VISIBLE

        binding.rvMyReports.visibility =
            View.GONE

        binding.tvEmptyState.visibility =
            View.GONE
    }

    private fun displayReportsState(
        reports: List<Item>
    ) {

        binding.progressBar.visibility =
            View.GONE

        if (reports.isEmpty()) {

            binding.rvMyReports.visibility =
                View.GONE

            binding.tvEmptyState.visibility =
                View.VISIBLE

            binding.tvEmptyState.text =
                getString(
                    R.string.empty_my_reports
                )

        } else {

            binding.rvMyReports.visibility =
                View.VISIBLE

            binding.tvEmptyState.visibility =
                View.GONE
        }
    }

    private fun displayErrorState(
        message: String
    ) {

        binding.progressBar.visibility =
            View.GONE

        binding.rvMyReports.visibility =
            View.GONE

        binding.tvEmptyState.visibility =
            View.VISIBLE

        binding.tvEmptyState.text =
            getString(
                R.string.failed_load_my_reports,
                message
            )
    }

    /*
     * Mengubah status:
     *
     * OPEN     → RETURNED
     * RETURNED → OPEN
     */
    private fun updateReportStatus(item: Item) {

        if (item.id.isEmpty()) {

            showMessage(
                "ID laporan tidak ditemukan"
            )

            return
        }

        val newStatus =
            if (item.status == STATUS_RETURNED) {
                STATUS_OPEN
            } else {
                STATUS_RETURNED
            }

        itemsReference
            .child(item.id)
            .child("status")
            .setValue(newStatus)
            .addOnSuccessListener {

                if (_binding == null) {
                    return@addOnSuccessListener
                }

                showMessage(
                    getString(
                        R.string.report_status_updated
                    )
                )
            }
            .addOnFailureListener { exception ->

                if (_binding == null) {
                    return@addOnFailureListener
                }

                showMessage(
                    getString(
                        R.string.report_status_update_failed,
                        exception.localizedMessage
                            ?: "Kesalahan database"
                    )
                )
            }
    }

    /*
     * Menampilkan dialog konfirmasi agar laporan
     * tidak langsung terhapus saat tombol tersentuh.
     */
    private fun showDeleteConfirmation(item: Item) {

        AlertDialog.Builder(requireContext())
            .setTitle(
                R.string.delete_report_title
            )
            .setMessage(
                getString(
                    R.string.delete_report_confirmation,
                    item.title
                )
            )
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .setPositiveButton(
                R.string.delete
            ) { _, _ ->

                deleteReport(item)
            }
            .show()
    }

    private fun deleteReport(item: Item) {

        if (item.id.isEmpty()) {

            showMessage(
                "ID laporan tidak ditemukan"
            )

            return
        }

        itemsReference
            .child(item.id)
            .removeValue()
            .addOnSuccessListener {

                if (_binding == null) {
                    return@addOnSuccessListener
                }

                showMessage(
                    getString(
                        R.string.report_deleted
                    )
                )
            }
            .addOnFailureListener { exception ->

                if (_binding == null) {
                    return@addOnFailureListener
                }

                showMessage(
                    getString(
                        R.string.report_delete_failed,
                        exception.localizedMessage
                            ?: "Kesalahan database"
                    )
                )
            }
    }

    /*
     * Membuka halaman Detail menggunakan Bundle.
     */
    private fun openItemDetail(item: Item) {

        val itemBundle =
            Bundle().apply {

                putString(
                    DetailItemFragment.ARG_TITLE,
                    item.title
                )

                putString(
                    DetailItemFragment.ARG_CATEGORY,
                    item.category
                )

                putString(
                    DetailItemFragment.ARG_REPORT_TYPE,
                    item.reportType
                )

                putString(
                    DetailItemFragment.ARG_LOCATION,
                    item.location
                )

                putString(
                    DetailItemFragment.ARG_DATE,
                    item.eventDate
                )

                putString(
                    DetailItemFragment.ARG_DESCRIPTION,
                    item.description
                )

                putString(
                    DetailItemFragment.ARG_CONTACT,
                    item.contact
                )

                putString(
                    DetailItemFragment.ARG_IMAGE_URL,
                    item.imageUrl
                )

                putString(
                    DetailItemFragment.ARG_STATUS,
                    item.status
                )
            }

        findNavController().navigate(
            R.id.action_myReportsFragment_to_detailItemFragment,
            itemBundle
        )
    }

    private fun openEditReport(item: Item) {

        if (item.id.isEmpty()) {

            showMessage(
                getString(
                    R.string.report_id_not_found
                )
            )

            return
        }

        val editBundle =
            Bundle().apply {

                putString(
                    EditReportFragment.ARG_ITEM_ID,
                    item.id
                )
            }

        findNavController().navigate(
            R.id.action_myReportsFragment_to_editReportFragment,
            editBundle
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

        val query = myReportsQuery
        val listener = myReportsListener

        if (
            query != null &&
            listener != null
        ) {

            query.removeEventListener(
                listener
            )
        }

        binding.rvMyReports.adapter = null

        myReportsQuery = null
        myReportsListener = null

        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val STATUS_OPEN =
            "OPEN"

        private const val STATUS_RETURNED =
            "RETURNED"
    }
}