package com.example.lostfound.ui.bookmarks

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
import com.example.lostfound.databinding.FragmentBookmarksBinding
import com.example.lostfound.local.AppDatabase
import com.example.lostfound.local.BookmarkDao
import com.example.lostfound.local.toItem
import com.example.lostfound.model.Item
import com.example.lostfound.ui.detail.DetailItemFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var itemAdapter: ItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentBookmarksBinding.inflate(
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

        setupRecyclerView()
        loadBookmarks()
    }

    private fun setupRecyclerView() {

        itemAdapter = ItemAdapter(

            onItemClick = { item ->
                openItemDetail(item)
            },

            /*
             * Pada halaman Bookmark, menekan tombol
             * Tersimpan berarti menghapus Bookmark.
             */
            onBookmarkClick = { item ->
                removeBookmark(item)
            }
        )

        binding.rvBookmarks.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        binding.rvBookmarks.adapter =
            itemAdapter

        binding.rvBookmarks.setHasFixedSize(
            true
        )
    }


    private fun loadBookmarks() {

        val currentUser =
            auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                requireContext(),
                R.string.session_not_found,
                Toast.LENGTH_LONG
            ).show()

            findNavController().navigateUp()
            return
        }

        binding.progressBar.visibility =
            View.VISIBLE

        binding.rvBookmarks.visibility =
            View.GONE

        binding.tvEmptyState.visibility =
            View.GONE

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                bookmarkDao
                    .observeBookmarks(
                        currentUser.uid
                    )
                    .collectLatest { bookmarks ->

                        if (_binding == null) {
                            return@collectLatest
                        }

                        val items =
                            bookmarks.map {
                                it.toItem()
                            }

                        itemAdapter.setItems(
                            items
                        )

                        /*
                         * Semua item pada halaman ini
                         * adalah Bookmark.
                         */
                        itemAdapter
                            .setBookmarkedItemIds(
                                items.map {
                                    it.id
                                }.toSet()
                            )

                        displayBookmarksState(
                            items
                        )
                    }
            }
    }

    private fun displayBookmarksState(
        items: List<Item>
    ) {

        binding.progressBar.visibility =
            View.GONE

        if (items.isEmpty()) {

            binding.rvBookmarks.visibility =
                View.GONE

            binding.tvEmptyState.visibility =
                View.VISIBLE

            binding.tvEmptyState.text =
                getString(
                    R.string.empty_bookmarks
                )

        } else {

            binding.rvBookmarks.visibility =
                View.VISIBLE

            binding.tvEmptyState.visibility =
                View.GONE
        }
    }

    private fun removeBookmark(
        item: Item
    ) {

        val currentUser =
            auth.currentUser
                ?: return

        viewLifecycleOwner
            .lifecycleScope
            .launch {

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
            R.id.action_bookmarksFragment_to_detailItemFragment,
            itemBundle
        )
    }

    override fun onDestroyView() {

        binding.rvBookmarks.adapter =
            null

        super.onDestroyView()

        _binding = null
    }
}