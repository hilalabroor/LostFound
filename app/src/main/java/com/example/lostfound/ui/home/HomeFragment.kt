package com.example.lostfound.ui.home

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
import com.example.lostfound.databinding.FragmentHomeBinding
import com.example.lostfound.local.AppDatabase
import com.example.lostfound.local.BookmarkDao
import com.example.lostfound.local.toBookmarkEntity
import com.example.lostfound.model.Item
import com.example.lostfound.model.User
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

class HomeFragment : Fragment() {

    private var _binding:
            FragmentHomeBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var auth:
            FirebaseAuth

    private lateinit var itemAdapter:
            ItemAdapter

    private lateinit var bookmarkDao:
            BookmarkDao

    private var itemsQuery:
            Query? = null

    private var itemsListener:
            ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentHomeBinding.inflate(
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

        /*
         * Jangan lanjut menjalankan Home
         * jika session pengguna tidak ada.
         */
        if (!checkCurrentUser()) {
            return
        }

        setupRecyclerView()
        observeBookmarks()
        loadItemsFromFirebase()
    }

    /*
     * Memeriksa session dan menampilkan
     * email pengguna.
     */
    private fun checkCurrentUser():
            Boolean {

        val currentUser =
            auth.currentUser

        if (currentUser == null) {
            findNavController().navigate(
                R.id.action_homeFragment_to_welcomeFragment
            )
            return false
        }

        return true
        }



    /*
     * Menyiapkan RecyclerView laporan.
     */
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

        binding.rvItems.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        binding.rvItems.adapter =
            itemAdapter

        binding.rvItems.setHasFixedSize(
            true
        )
    }

    /*
     * Mengamati daftar ID laporan
     * yang sudah disimpan di Room.
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

    /*
     * Menyimpan atau menghapus Bookmark.
     */
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
                        userId =
                            currentUser.uid,
                        itemId =
                            item.id
                    )

                if (alreadyBookmarked) {

                    bookmarkDao.deleteBookmark(
                        userId =
                            currentUser.uid,
                        itemId =
                            item.id
                    )

                    if (_binding != null) {

                        Toast.makeText(
                            requireContext(),
                            R.string.bookmark_removed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    val bookmark =
                        item.toBookmarkEntity(
                            bookmarkUserId =
                                currentUser.uid
                        )

                    bookmarkDao.insertBookmark(
                        bookmark
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

    /*
     * Membaca semua laporan Firebase.
     */
    private fun loadItemsFromFirebase() {

        showLoadingState()

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

                    val loadedItems =
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
                            loadedItems.add(item)
                        }
                    }

                    loadedItems.sortByDescending {
                        it.createdAt
                    }

                    itemAdapter.setItems(
                        loadedItems
                    )

                    displayItemsState(
                        loadedItems
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

        itemsQuery = query
        itemsListener = listener

        query.addValueEventListener(
            listener
        )
    }

    private fun showLoadingState() {

        binding.rvItems.visibility =
            View.GONE

        binding.tvEmptyState.visibility =
            View.VISIBLE

        binding.tvEmptyState.text =
            getString(
                R.string.loading_reports
            )
    }

    private fun displayItemsState(
        items: List<Item>
    ) {

        if (items.isEmpty()) {

            binding.rvItems.visibility =
                View.GONE

            binding.tvEmptyState.visibility =
                View.VISIBLE

            binding.tvEmptyState.text =
                getString(
                    R.string.empty_reports
                )

        } else {

            binding.rvItems.visibility =
                View.VISIBLE

            binding.tvEmptyState.visibility =
                View.GONE
        }
    }

    private fun displayErrorState(
        errorMessage: String
    ) {

        binding.rvItems.visibility =
            View.GONE

        binding.tvEmptyState.visibility =
            View.VISIBLE

        binding.tvEmptyState.text =
            getString(
                R.string.failed_load_reports,
                errorMessage
            )

        Toast.makeText(
            requireContext(),
            errorMessage,
            Toast.LENGTH_LONG
        ).show()
    }

    /*
     * Membuka halaman Detail.
     */
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
            R.id.action_homeFragment_to_detailItemFragment,
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

        binding.rvItems.adapter =
            null

        itemsQuery = null
        itemsListener = null

        super.onDestroyView()

        _binding = null
    }
}