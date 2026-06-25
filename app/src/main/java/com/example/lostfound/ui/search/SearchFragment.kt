package com.example.lostfound.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {

    private var _binding:
            FragmentSearchBinding? = null

    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentSearchBinding.inflate(
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

        setupClickListeners()
    }

    private fun setupClickListeners() {

        binding.btnSearchNow.setOnClickListener {
            openSearchResult()
        }

        binding.btnResetFilter.setOnClickListener {
            resetSearchForm()
        }
    }

    private fun openSearchResult() {

        val keyword =
            binding.etSearch.text
                .toString()
                .trim()

        /*
         * Spinner jenis:
         * 0 = Semua
         * 1 = LOST
         * 2 = FOUND
         */
        val reportType =
            when (
                binding.spReportType
                    .selectedItemPosition
            ) {

                1 -> TYPE_LOST
                2 -> TYPE_FOUND
                else -> FILTER_ALL
            }

        /*
         * Posisi pertama kategori adalah
         * Semua Kategori.
         */
        val category =
            if (
                binding.spCategory
                    .selectedItemPosition == 0
            ) {

                FILTER_ALL

            } else {

                binding.spCategory
                    .selectedItem
                    .toString()
            }

        /*
         * Spinner status:
         * 0 = Semua
         * 1 = OPEN
         * 2 = RETURNED
         */
        val status =
            when (
                binding.spStatus
                    .selectedItemPosition
            ) {

                1 -> STATUS_OPEN
                2 -> STATUS_RETURNED
                else -> FILTER_ALL
            }

        /*
         * Data pencarian dikirim ke
         * SearchResultFragment menggunakan Bundle.
         */
        val searchBundle =
            Bundle().apply {

                putString(
                    SearchResultFragment.ARG_KEYWORD,
                    keyword
                )

                putString(
                    SearchResultFragment.ARG_REPORT_TYPE,
                    reportType
                )

                putString(
                    SearchResultFragment.ARG_CATEGORY,
                    category
                )

                putString(
                    SearchResultFragment.ARG_STATUS,
                    status
                )
            }

        findNavController().navigate(
            R.id.action_searchFragment_to_searchResultFragment,
            searchBundle
        )
    }

    private fun resetSearchForm() {

        binding.etSearch.text?.clear()

        binding.spReportType
            .setSelection(0)

        binding.spCategory
            .setSelection(0)

        binding.spStatus
            .setSelection(0)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {

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