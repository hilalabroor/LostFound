package com.example.lostfound.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lostfound.R
import com.example.lostfound.adapter.ItemAdapter
import com.example.lostfound.databinding.FragmentSearchBinding
import com.example.lostfound.model.Item
import com.example.lostfound.ui.detail.DetailItemFragment
import com.example.lostfound.utils.FirebaseConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import androidx.lifecycle.lifecycleScope
import com.example.lostfound.local.AppDatabase
import com.example.lostfound.local.BookmarkDao
import com.example.lostfound.local.toBookmarkEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private lateinit var itemAdapter: ItemAdapter
    private lateinit var bookmarkDao: BookmarkDao

    /*
     * Menyimpan seluruh data asli dari Firebase.
     *
     * Data ini tidak langsung diubah saat melakukan
     * pencarian atau filter.
     */
    private val allItems =
        mutableListOf<Item>()

    /*
     * Query dan listener disimpan agar dapat dilepas
     * ketika tampilan Fragment dihancurkan.
     */
    private var itemsQuery: Query? = null
    private var itemsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSearchBinding.inflate(
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

        if (!checkCurrentUser()) {
            return
        }

        setupRecyclerView()
        setupSearchListener()
        setupFilterListeners()
        setupClickListeners()
        loadItemsFromFirebase()
        observeBookmarks()
    }

    /*
     * Memastikan pengguna masih login.
     */
    private fun checkCurrentUser(): Boolean {

        if (auth.currentUser == null) {

            Toast.makeText(
                requireContext(),
                R.string.session_not_found,
                Toast.LENGTH_LONG
            ).show()

            findNavController().navigate(
                R.id.welcomeFragment
            )

            return false
        }

        return true
    }

    /*
     * Menggunakan ItemAdapter yang sama seperti Home.
     */
    private fun setupRecyclerView() {

        itemAdapter = ItemAdapter(

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

    /*
     * Menjalankan filter setiap kali pengguna
     * mengetik atau menghapus tulisan pencarian.
     */
    private fun setupSearchListener() {

        binding.etSearch.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    text: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Tidak digunakan
                }

                override fun onTextChanged(
                    text: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    applyFilters()
                }

                override fun afterTextChanged(
                    editable: Editable?
                ) {
                    // Tidak digunakan
                }
            }
        )
    }

    /*
     * Satu listener dipakai untuk ketiga Spinner.
     */
    private fun setupFilterListeners() {

        val filterListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    applyFilters()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    // Tidak ada tindakan
                }
            }

        binding.spReportType.onItemSelectedListener =
            filterListener

        binding.spCategory.onItemSelectedListener =
            filterListener

        binding.spStatus.onItemSelectedListener =
            filterListener
    }

    private fun setupClickListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnResetFilter.setOnClickListener {
            resetFilters()
        }
    }

    /*
     * Membaca seluruh laporan dari Firebase.
     */
    private fun loadItemsFromFirebase() {

        showLoadingState()

        val query = FirebaseDatabase
            .getInstance(FirebaseConfig.DATABASE_URL)
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

                        if (item != null) {
                            allItems.add(item)
                        }
                    }

                    /*
                     * Data terbaru diletakkan di atas.
                     */
                    allItems.sortByDescending {
                        it.createdAt
                    }

                    binding.progressBar.visibility =
                        View.GONE

                    applyFilters()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    if (_binding == null) {
                        return
                    }

                    showErrorState(
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
     * Menggabungkan seluruh pencarian dan filter.
     */
    private fun applyFilters() {

        if (_binding == null) {
            return
        }

        val keyword =
            binding.etSearch.text
                .toString()
                .trim()

        val reportTypePosition =
            binding.spReportType
                .selectedItemPosition

        val categoryPosition =
            binding.spCategory
                .selectedItemPosition

        val statusPosition =
            binding.spStatus
                .selectedItemPosition

        val selectedCategory =
            binding.spCategory
                .selectedItem
                ?.toString()
                .orEmpty()

        val filteredItems =
            allItems.filter { item ->

                val matchesKeyword =
                    keyword.isEmpty() ||
                            item.title.contains(
                                keyword,
                                ignoreCase = true
                            ) ||
                            item.category.contains(
                                keyword,
                                ignoreCase = true
                            ) ||
                            item.location.contains(
                                keyword,
                                ignoreCase = true
                            ) ||
                            item.description.contains(
                                keyword,
                                ignoreCase = true
                            )

                val matchesReportType =
                    when (reportTypePosition) {

                        /*
                         * Posisi 1:
                         * Barang Hilang
                         */
                        1 -> {
                            item.reportType ==
                                    REPORT_TYPE_LOST
                        }

                        /*
                         * Posisi 2:
                         * Barang Ditemukan
                         */
                        2 -> {
                            item.reportType ==
                                    REPORT_TYPE_FOUND
                        }

                        /*
                         * Posisi 0:
                         * Semua jenis laporan
                         */
                        else -> {
                            true
                        }
                    }

                val matchesCategory =
                    categoryPosition == 0 ||
                            item.category.equals(
                                selectedCategory,
                                ignoreCase = true
                            )

                val matchesStatus =
                    when (statusPosition) {

                        /*
                         * Posisi 1:
                         * Belum selesai
                         */
                        1 -> {
                            item.status ==
                                    STATUS_OPEN
                        }

                        /*
                         * Posisi 2:
                         * Sudah dikembalikan
                         */
                        2 -> {
                            item.status ==
                                    STATUS_RETURNED
                        }

                        /*
                         * Posisi 0:
                         * Semua status
                         */
                        else -> {
                            true
                        }
                    }

                matchesKeyword &&
                        matchesReportType &&
                        matchesCategory &&
                        matchesStatus
            }

        itemAdapter.setItems(
            filteredItems
        )

        displayFilterResult(
            filteredItems
        )
    }

    /*
     * Menampilkan jumlah data atau empty state.
     */
    private fun displayFilterResult(
        filteredItems: List<Item>
    ) {

        binding.progressBar.visibility =
            View.GONE

        binding.tvResultCount.text =
            getString(
                R.string.search_result_count,
                filteredItems.size
            )

        if (filteredItems.isEmpty()) {

            binding.rvSearchResults.visibility =
                View.GONE

            binding.tvEmptyState.visibility =
                View.VISIBLE

            binding.tvEmptyState.text =
                getString(
                    R.string.empty_search_results
                )

        } else {

            binding.rvSearchResults.visibility =
                View.VISIBLE

            binding.tvEmptyState.visibility =
                View.GONE
        }
    }

    private fun resetFilters() {

        /*
         * Mengosongkan pencarian.
         */
        binding.etSearch.setText("")

        /*
         * Mengembalikan seluruh Spinner
         * ke pilihan pertama.
         */
        binding.spReportType.setSelection(0)
        binding.spCategory.setSelection(0)
        binding.spStatus.setSelection(0)

        applyFilters()
    }

    private fun showLoadingState() {

        binding.progressBar.visibility =
            View.VISIBLE

        binding.rvSearchResults.visibility =
            View.GONE

        binding.tvEmptyState.visibility =
            View.GONE

        binding.tvResultCount.text =
            getString(
                R.string.loading_search_results
            )
    }

    private fun showErrorState(
        message: String
    ) {

        binding.progressBar.visibility =
            View.GONE

        binding.rvSearchResults.visibility =
            View.GONE

        binding.tvEmptyState.visibility =
            View.VISIBLE

        binding.tvEmptyState.text =
            getString(
                R.string.failed_search_results,
                message
            )

        binding.tvResultCount.text =
            getString(
                R.string.search_result_count,
                0
            )
    }

    /*
     * Membuka halaman Detail.
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
            R.id.action_searchFragment_to_detailItemFragment,
            itemBundle
        )
    }

    override fun onDestroyView() {

        val query = itemsQuery
        val listener = itemsListener

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

        allItems.clear()

        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val REPORT_TYPE_LOST =
            "LOST"

        private const val REPORT_TYPE_FOUND =
            "FOUND"

        private const val STATUS_OPEN =
            "OPEN"

        private const val STATUS_RETURNED =
            "RETURNED"
    }

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

        if (currentUser == null) {

            Toast.makeText(
                requireContext(),
                R.string.session_not_found,
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (item.id.isEmpty()) {

            Toast.makeText(
                requireContext(),
                R.string.bookmark_item_id_missing,
                Toast.LENGTH_LONG
            ).show()

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

}

