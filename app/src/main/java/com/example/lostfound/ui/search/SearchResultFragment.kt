package com.example.lostfound.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lostfound.R
import com.example.lostfound.adapter.ItemAdapter
import com.example.lostfound.databinding.FragmentSearchResultBinding
import com.example.lostfound.local.AppDatabase
import com.example.lostfound.local.BookmarkDao
import com.example.lostfound.local.toBookmarkEntity
import com.example.lostfound.model.Item
import com.example.lostfound.ui.detail.DetailItemFragment
import com.example.lostfound.utils.FirebaseConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchResultFragment : Fragment() {

    private var _binding:
            FragmentSearchResultBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var auth:
            FirebaseAuth

    private lateinit var bookmarkDao:
            BookmarkDao

    private lateinit var itemAdapter:
            ItemAdapter

    private val allItems =
        mutableListOf<Item>()

    private var itemsQuery:
            Query? = null

    private var itemsListener:
            ValueEventListener? = null

    private var keyword: String = ""
    private var reportType: String = FILTER_ALL
    private var category: String = FILTER_ALL
    private var status: String = FILTER_ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentSearchResultBinding.inflate(
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
        super.onViewCreated(
            view,
            savedInstanceState
        )

        auth =
            FirebaseAuth.getInstance()

        bookmarkDao =
            AppDatabase
                .getDatabase(requireContext())
                .bookmarkDao()

        readSearchArguments()
        setupRecyclerView()
        setupClickListeners()
        displaySearchSummary()
        observeBookmarks()
        loadItemsFromFirebase()
    }

    /*
     * Membaca kata kunci dan filter
     * yang dikirim oleh SearchFragment.
     */
    private fun readSearchArguments() {

        keyword =
            arguments
                ?.getString(ARG_KEYWORD)
                .orEmpty()

        reportType =
            arguments
                ?.getString(ARG_REPORT_TYPE)
                ?: FILTER_ALL

        category =
            arguments
                ?.getString(ARG_CATEGORY)
                ?: FILTER_ALL

        status =
            arguments
                ?.getString(ARG_STATUS)
                ?: FILTER_ALL
    }

    private fun setupRecyclerView() {

        itemAdapter =
            ItemAdapter(

                onItemClick = { item ->
                    openItemDetail(item)
                },

                onBookmarkClick = { item ->
                    toggleBookmark(item)
                }
            )

        binding.rvSearchResults.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        binding.rvSearchResults.adapter =
            itemAdapter

        binding.rvSearchResults.setHasFixedSize(
            true
        )
    }

    private fun setupClickListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnChangeSearch.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /*
     * Menampilkan informasi mengenai filter
     * yang sedang digunakan.
     */
    private fun displaySearchSummary() {

        val summaries =
            mutableListOf<String>()

        if (keyword.isNotBlank()) {

            summaries.add(
                "Kata kunci: \"$keyword\""
            )
        }

        when (reportType) {

            TYPE_LOST ->
                summaries.add(
                    "Jenis: Barang Hilang"
                )

            TYPE_FOUND ->
                summaries.add(
                    "Jenis: Barang Ditemukan"
                )
        }

        if (category != FILTER_ALL) {

            summaries.add(
                "Kategori: $category"
            )
        }

        when (status) {

            STATUS_OPEN ->
                summaries.add(
                    "Status: Masih Aktif"
                )

            STATUS_RETURNED ->
                summaries.add(
                    "Status: Selesai"
                )
        }

        binding.tvSearchSummary.text =
            if (summaries.isEmpty()) {

                getString(
                    R.string.all_search_results
                )

            } else {

                summaries.joinToString(
                    separator = " • "
                )
            }
    }

    private fun loadItemsFromFirebase() {

        showLoading()

        val query =
            FirebaseDatabase
                .getInstance(
                    FirebaseConfig.DATABASE_URL
                )
                .getReference("items")
                .orderByChild("createdAt")

        val listener =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    if (_binding == null) {
                        return
                    }

                    allItems.clear()

                    for (
                    itemSnapshot
                    in snapshot.children
                    ) {

                        val item =
                            itemSnapshot.getValue(
                                Item::class.java
                            )
                                ?: continue

                        /*
                         * Menggunakan key Firebase jika
                         * field ID pada data kosong.
                         */
                        if (item.id.isBlank()) {
                            item.id =
                                itemSnapshot.key.orEmpty()
                        }

                        allItems.add(item)
                    }

                    applyFilters()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    if (_binding == null) {
                        return
                    }

                    showError(
                        error.message
                    )
                }
            }

        itemsQuery = query
        itemsListener = listener

        query.addValueEventListener(
            listener
        )
    }

    /*
     * Menerapkan seluruh filter pada data Firebase.
     */
    private fun applyFilters() {

        val normalizedKeyword =
            keyword.lowercase()

        val filteredItems =
            allItems
                .filter { item ->

                    val searchableText =
                        listOf(
                            item.title,
                            item.category,
                            item.location,
                            item.description
                        )
                            .joinToString(" ")
                            .lowercase()

                    val matchesKeyword =
                        keyword.isBlank() ||
                                searchableText.contains(
                                    normalizedKeyword
                                )

                    val matchesReportType =
                        reportType == FILTER_ALL ||
                                item.reportType.equals(
                                    reportType,
                                    ignoreCase = true
                                )

                    val matchesCategory =
                        category == FILTER_ALL ||
                                item.category.equals(
                                    category,
                                    ignoreCase = true
                                )

                    val matchesStatus =
                        status == FILTER_ALL ||
                                item.status.equals(
                                    status,
                                    ignoreCase = true
                                )

                    matchesKeyword &&
                            matchesReportType &&
                            matchesCategory &&
                            matchesStatus
                }
                .sortedByDescending {
                    it.createdAt
                }

        itemAdapter.setItems(
            filteredItems
        )

        binding.tvResultCount.text =
            getString(
                R.string.search_result_count,
                filteredItems.size
            )

        displayResultState(
            filteredItems
        )
    }

    private fun showLoading() {

        binding.progressBar.visibility =
            View.VISIBLE

        binding.rvSearchResults.visibility =
            View.GONE

        binding.tvEmptyState.visibility =
            View.GONE
    }

    private fun displayResultState(
        results: List<Item>
    ) {

        binding.progressBar.visibility =
            View.GONE

        if (results.isEmpty()) {

            binding.rvSearchResults.visibility =
                View.GONE

            binding.tvEmptyState.visibility =
                View.VISIBLE

        } else {

            binding.rvSearchResults.visibility =
                View.VISIBLE

            binding.tvEmptyState.visibility =
                View.GONE
        }
    }

    private fun showError(
        message: String
    ) {

        binding.progressBar.visibility =
            View.GONE

        binding.rvSearchResults.visibility =
            View.GONE

        binding.tvEmptyState.visibility =
            View.VISIBLE

        binding.tvEmptyState.text =
            message

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    /*
     * Mengamati ID laporan yang sudah
     * tersimpan pada Room Bookmark.
     */
    private fun observeBookmarks() {

        val currentUser =
            auth.currentUser
                ?: return

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                bookmarkDao
                    .observeBookmarkedItemIds(
                        currentUser.uid
                    )
                    .collectLatest { itemIds ->

                        if (_binding == null) {
                            return@collectLatest
                        }

                        itemAdapter
                            .setBookmarkedItemIds(
                                itemIds.toSet()
                            )
                    }
            }
    }

    private fun toggleBookmark(
        item: Item
    ) {

        val currentUser =
            auth.currentUser
                ?: return

        if (item.id.isBlank()) {
            return
        }

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                val alreadyBookmarked =
                    bookmarkDao.isBookmarked(
                        userId = currentUser.uid,
                        itemId = item.id
                    )

                if (alreadyBookmarked) {

                    bookmarkDao.deleteBookmark(
                        userId = currentUser.uid,
                        itemId = item.id
                    )

                    if (_binding != null) {

                        Toast.makeText(
                            requireContext(),
                            R.string.bookmark_removed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    bookmarkDao.insertBookmark(
                        item.toBookmarkEntity(
                            bookmarkUserId =
                                currentUser.uid
                        )
                    )

                    if (_binding != null) {

                        Toast.makeText(
                            requireContext(),
                            R.string.bookmark_saved,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun openItemDetail(
        item: Item
    ) {

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
            R.id.action_searchResultFragment_to_detailItemFragment,
            itemBundle
        )
    }

    override fun onDestroyView() {

        val query =
            itemsQuery

        val listener =
            itemsListener

        if (
            query != null &&
            listener != null
        ) {

            query.removeEventListener(
                listener
            )
        }

        binding.rvSearchResults.adapter =
            null

        itemsQuery = null
        itemsListener = null

        super.onDestroyView()

        _binding = null
    }

    companion object {

        const val ARG_KEYWORD =
            "search_keyword"

        const val ARG_REPORT_TYPE =
            "search_report_type"

        const val ARG_CATEGORY =
            "search_category"

        const val ARG_STATUS =
            "search_status"

        private const val FILTER_ALL =
            "ALL"

        private const val TYPE_LOST =
            "LOST"

        private const val TYPE_FOUND =
            "FOUND"

        private const val STATUS_OPEN =
            "OPEN"

        private const val STATUS_RETURNED =
            "RETURNED"
    }
}