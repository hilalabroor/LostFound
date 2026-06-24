package com.example.lostfound.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentDetailItemBinding

class DetailItemFragment : androidx.fragment.app.Fragment() {

    private var _binding: FragmentDetailItemBinding? = null
    private val binding get() = _binding!!

    // Data barang yang diterima dari Home
    private var itemTitle: String = ""
    private var itemCategory: String = ""
    private var itemReportType: String = ""
    private var itemLocation: String = ""
    private var itemDate: String = ""
    private var itemDescription: String = ""
    private var itemContact: String = ""
    private var itemImageUrl: String = ""
    private var itemStatus: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDetailItemBinding.inflate(
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

        readArguments()
        displayItemDetail()
        setupClickListeners()
    }

    /*
     * Membaca data yang sebelumnya dikirim
     * oleh HomeFragment melalui Bundle.
     */
    private fun readArguments() {

        itemTitle =
            arguments?.getString(ARG_TITLE).orEmpty()

        itemCategory =
            arguments?.getString(ARG_CATEGORY).orEmpty()

        itemReportType =
            arguments?.getString(ARG_REPORT_TYPE).orEmpty()

        itemLocation =
            arguments?.getString(ARG_LOCATION).orEmpty()

        itemDate =
            arguments?.getString(ARG_DATE).orEmpty()

        itemDescription =
            arguments?.getString(ARG_DESCRIPTION).orEmpty()

        itemContact =
            arguments?.getString(ARG_CONTACT).orEmpty()

        itemImageUrl =
            arguments?.getString(ARG_IMAGE_URL).orEmpty()

        itemStatus =
            arguments?.getString(ARG_STATUS).orEmpty()
    }

    private fun displayItemDetail() {

        binding.tvDetailTitle.text =
            itemTitle.ifEmpty {
                getString(R.string.value_not_available)
            }

        binding.tvDetailCategory.text =
            itemCategory.ifEmpty {
                getString(R.string.value_not_available)
            }

        binding.tvDetailLocation.text =
            itemLocation.ifEmpty {
                getString(R.string.value_not_available)
            }

        binding.tvDetailDate.text =
            itemDate.ifEmpty {
                getString(R.string.value_not_available)
            }

        binding.tvDetailDescription.text =
            itemDescription.ifEmpty {
                getString(R.string.value_not_available)
            }

        binding.tvDetailContact.text =
            itemContact.ifEmpty {
                getString(R.string.value_not_available)
            }

        displayReportType()
        displayStatus()
        displayImage()
    }

    private fun displayReportType() {

        if (itemReportType == REPORT_TYPE_FOUND) {

            binding.tvDetailReportType.text =
                getString(R.string.report_found)

            binding.tvDetailReportType.setBackgroundResource(
                R.drawable.bg_badge_found
            )

        } else {

            binding.tvDetailReportType.text =
                getString(R.string.report_lost)

            binding.tvDetailReportType.setBackgroundResource(
                R.drawable.bg_badge_lost
            )
        }
    }

    private fun displayStatus() {

        if (itemStatus == STATUS_RETURNED) {

            binding.tvDetailStatus.text =
                getString(R.string.status_returned)

            binding.tvDetailStatus.setTextColor(
                requireContext().getColor(
                    R.color.lf_status_returned
                )
            )

        } else {

            binding.tvDetailStatus.text =
                getString(R.string.status_open)

            binding.tvDetailStatus.setTextColor(
                requireContext().getColor(
                    R.color.lf_status_open
                )
            )
        }
    }

    private fun displayImage() {

        if (itemImageUrl.isNotEmpty()) {

            Glide.with(this)
                .load(itemImageUrl)
                .placeholder(
                    android.R.drawable.ic_menu_gallery
                )
                .error(
                    android.R.drawable.ic_menu_report_image
                )
                .centerCrop()
                .into(binding.ivDetailImage)

        } else {

            binding.ivDetailImage.setImageResource(
                android.R.drawable.ic_menu_gallery
            )
        }
    }

    private fun setupClickListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnContact.setOnClickListener {
            openWhatsApp()
        }
    }

    private fun openWhatsApp() {

        if (itemContact.isEmpty()) {

            Toast.makeText(
                requireContext(),
                R.string.value_not_available,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val normalizedPhone = normalizePhoneNumber(itemContact)

        val message =
            "Halo, saya melihat laporan \"$itemTitle\" " +
                    "di aplikasi LF Lost&Found."

        val whatsappUri = Uri.parse(
            "https://wa.me/$normalizedPhone" +
                    "?text=${Uri.encode(message)}"
        )

        val intent = Intent(
            Intent.ACTION_VIEW,
            whatsappUri
        )

        try {

            startActivity(intent)

        } catch (exception: Exception) {

            Toast.makeText(
                requireContext(),
                R.string.whatsapp_not_available,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
     * WhatsApp membutuhkan nomor internasional.
     *
     * Contoh:
     * 081234567890
     * menjadi:
     * 6281234567890
     */
    private fun normalizePhoneNumber(phone: String): String {

        val cleanPhone = phone
            .replace(" ", "")
            .replace("-", "")
            .replace("+", "")

        return when {

            cleanPhone.startsWith("0") -> {
                "62${cleanPhone.drop(1)}"
            }

            cleanPhone.startsWith("62") -> {
                cleanPhone
            }

            else -> {
                "62$cleanPhone"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        // Nama key Bundle
        const val ARG_TITLE = "item_title"
        const val ARG_CATEGORY = "item_category"
        const val ARG_REPORT_TYPE = "item_report_type"
        const val ARG_LOCATION = "item_location"
        const val ARG_DATE = "item_date"
        const val ARG_DESCRIPTION = "item_description"
        const val ARG_CONTACT = "item_contact"
        const val ARG_IMAGE_URL = "item_image_url"
        const val ARG_STATUS = "item_status"

        // Nilai jenis laporan dan status
        private const val REPORT_TYPE_FOUND = "FOUND"
        private const val STATUS_RETURNED = "RETURNED"
    }
}