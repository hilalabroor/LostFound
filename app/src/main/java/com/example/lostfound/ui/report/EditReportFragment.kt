package com.example.lostfound.ui.report

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentEditReportBinding
import com.example.lostfound.model.Item
import com.example.lostfound.network.CloudinaryUploadResponse
import com.example.lostfound.network.RetrofitClient
import com.example.lostfound.utils.FileUtils
import com.example.lostfound.utils.FirebaseConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

class EditReportFragment : Fragment() {

    private var _binding: FragmentEditReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var itemsReference: DatabaseReference

    private var itemId: String = ""
    private var currentItem: Item? = null
    private var selectedImageUri: Uri? = null

    /*
     * Membuka pemilih gambar.
     */
    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (
                uri != null &&
                _binding != null
            ) {

                selectedImageUri = uri

                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(binding.ivImagePreview)

                binding.tvImageStatus.text =
                    getString(
                        R.string.new_image_selected
                    )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEditReportBinding.inflate(
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
        setupClickListeners()
        readItemId()
    }

    private fun initializeFirebase() {

        auth = FirebaseAuth.getInstance()

        itemsReference = FirebaseDatabase
            .getInstance(FirebaseConfig.DATABASE_URL)
            .getReference("items")
    }

    private fun setupClickListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.etEventDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnChooseImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnUpdateReport.setOnClickListener {
            validateAndUpdateReport()
        }
    }

    private fun readItemId() {

        itemId =
            arguments
                ?.getString(ARG_ITEM_ID)
                .orEmpty()

        if (itemId.isEmpty()) {

            showMessage(
                getString(
                    R.string.report_id_not_found
                )
            )

            findNavController().navigateUp()
            return
        }

        loadReport()
    }

    /*
     * Membaca data terbaru langsung dari Firebase.
     */
    private fun loadReport() {

        showInitialLoading()

        itemsReference
            .child(itemId)
            .get()
            .addOnCompleteListener { task ->

                if (_binding == null) {
                    return@addOnCompleteListener
                }

                if (!task.isSuccessful) {

                    showMessage(
                        task.exception?.localizedMessage
                            ?: getString(
                                R.string.report_not_found
                            )
                    )

                    findNavController().navigateUp()
                    return@addOnCompleteListener
                }

                val item =
                    task.result.getValue(
                        Item::class.java
                    )

                if (item == null) {

                    showMessage(
                        getString(
                            R.string.report_not_found
                        )
                    )

                    findNavController().navigateUp()
                    return@addOnCompleteListener
                }

                val currentUser =
                    auth.currentUser

                if (currentUser == null) {

                    showMessage(
                        getString(
                            R.string.session_not_found
                        )
                    )

                    findNavController().navigateUp()
                    return@addOnCompleteListener
                }

                /*
                 * Memastikan laporan benar-benar milik
                 * pengguna yang sedang login.
                 */
                if (item.userId != currentUser.uid) {

                    showMessage(
                        getString(
                            R.string.no_edit_permission
                        )
                    )

                    findNavController().navigateUp()
                    return@addOnCompleteListener
                }

                currentItem = item

                populateForm(item)
                showForm()
            }
    }

    private fun populateForm(item: Item) {

        if (item.reportType == REPORT_TYPE_FOUND) {

            binding.rbFound.isChecked = true

        } else {

            binding.rbLost.isChecked = true
        }

        binding.etItemName.setText(
            item.title
        )

        selectCategory(
            item.category
        )

        binding.etLocation.setText(
            item.location
        )

        binding.etEventDate.setText(
            item.eventDate
        )

        binding.etDescription.setText(
            item.description
        )

        binding.etContact.setText(
            item.contact
        )

        if (item.imageUrl.isNotEmpty()) {

            Glide.with(this)
                .load(item.imageUrl)
                .placeholder(
                    android.R.drawable.ic_menu_gallery
                )
                .error(
                    android.R.drawable.ic_menu_report_image
                )
                .centerCrop()
                .into(binding.ivImagePreview)

            binding.tvImageStatus.text =
                getString(
                    R.string.keeping_old_image
                )

        } else {

            binding.ivImagePreview.setImageResource(
                android.R.drawable.ic_menu_gallery
            )

            binding.tvImageStatus.text =
                getString(
                    R.string.no_image_selected
                )
        }
    }

    /*
     * Memilih posisi Spinner berdasarkan
     * kategori laporan sebelumnya.
     */
    private fun selectCategory(category: String) {

        val adapter =
            binding.spCategory.adapter

        for (position in 0 until adapter.count) {

            val value =
                adapter.getItem(position)
                    .toString()

            if (
                value.equals(
                    category,
                    ignoreCase = true
                )
            ) {

                binding.spCategory.setSelection(
                    position
                )

                break
            }
        }
    }

    private fun showDatePicker() {

        val calendar =
            Calendar.getInstance()

        val dialog =
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->

                    val formattedDate =
                        String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            dayOfMonth,
                            month + 1,
                            year
                        )

                    binding.etEventDate.setText(
                        formattedDate
                    )
                },
                calendar.get(
                    Calendar.YEAR
                ),
                calendar.get(
                    Calendar.MONTH
                ),
                calendar.get(
                    Calendar.DAY_OF_MONTH
                )
            )

        dialog.datePicker.maxDate =
            System.currentTimeMillis()

        dialog.show()
    }

    private fun validateAndUpdateReport() {

        clearErrors()

        val oldItem =
            currentItem

        if (oldItem == null) {

            showMessage(
                getString(
                    R.string.report_not_found
                )
            )

            return
        }

        val reportType =
            when (
                binding.rgReportType
                    .checkedRadioButtonId
            ) {

                R.id.rbLost ->
                    REPORT_TYPE_LOST

                R.id.rbFound ->
                    REPORT_TYPE_FOUND

                else ->
                    ""
            }

        val itemName =
            binding.etItemName.text
                .toString()
                .trim()

        val category =
            binding.spCategory.selectedItem
                .toString()

        val location =
            binding.etLocation.text
                .toString()
                .trim()

        val eventDate =
            binding.etEventDate.text
                .toString()
                .trim()

        val description =
            binding.etDescription.text
                .toString()
                .trim()

        val contact =
            binding.etContact.text
                .toString()
                .trim()

        if (reportType.isEmpty()) {

            showMessage(
                "Pilih jenis laporan"
            )

            return
        }

        if (itemName.length < 3) {

            binding.etItemName.error =
                "Nama barang minimal 3 karakter"

            binding.etItemName.requestFocus()
            return
        }

        if (
            binding.spCategory
                .selectedItemPosition == 0
        ) {

            showMessage(
                "Pilih kategori barang"
            )

            return
        }

        if (location.length < 5) {

            binding.etLocation.error =
                "Lokasi wajib diisi dengan lengkap"

            binding.etLocation.requestFocus()
            return
        }

        if (eventDate.isEmpty()) {

            binding.etEventDate.error =
                "Tanggal kejadian wajib dipilih"

            return
        }

        if (description.length < 10) {

            binding.etDescription.error =
                "Deskripsi minimal 10 karakter"

            binding.etDescription.requestFocus()
            return
        }

        if (contact.length < 10) {

            binding.etContact.error =
                "Nomor WhatsApp minimal 10 angka"

            binding.etContact.requestFocus()
            return
        }

        /*
         * Membuat salinan Item dengan data edit.
         *
         * id, userId, status dan createdAt
         * tetap menggunakan nilai lama.
         */
        val editedItem =
            oldItem.copy(
                reportType = reportType,
                title = itemName,
                category = category,
                location = location,
                eventDate = eventDate,
                description = description,
                contact = contact
            )

        val newImageUri =
            selectedImageUri

        if (newImageUri != null) {

            uploadNewImage(
                item = editedItem,
                imageUri = newImageUri
            )

        } else {

            if (editedItem.imageUrl.isEmpty()) {

                showMessage(
                    "Pilih gambar laporan"
                )

                return
            }

            updateReportInFirebase(
                editedItem
            )
        }
    }

    private fun uploadNewImage(
        item: Item,
        imageUri: Uri
    ) {

        val cloudName =
            FirebaseConfig
                .CLOUDINARY_CLOUD_NAME

        val uploadPreset =
            FirebaseConfig
                .CLOUDINARY_UPLOAD_PRESET

        if (
            cloudName.isBlank() ||
            uploadPreset.isBlank()
        ) {

            showMessage(
                "Konfigurasi Cloudinary belum lengkap"
            )

            return
        }

        val tempFile =
            FileUtils.createTempFileFromUri(
                requireContext(),
                imageUri
            )

        if (tempFile == null) {

            showMessage(
                "Gambar gagal dibaca"
            )

            return
        }

        val maximumSize =
            5L * 1024L * 1024L

        if (tempFile.length() > maximumSize) {

            tempFile.delete()

            showMessage(
                "Ukuran gambar maksimal 5 MB"
            )

            return
        }

        val mimeType =
            requireContext()
                .contentResolver
                .getType(imageUri)
                ?: "image/jpeg"

        if (!mimeType.startsWith("image/")) {

            tempFile.delete()

            showMessage(
                "File yang dipilih bukan gambar"
            )

            return
        }

        setUpdating(true)

        binding.tvImageStatus.text =
            "Mengunggah gambar baru..."

        val imageRequestBody =
            tempFile.asRequestBody(
                mimeType.toMediaTypeOrNull()
            )

        val imagePart =
            MultipartBody.Part.createFormData(
                "file",
                tempFile.name,
                imageRequestBody
            )

        val presetRequestBody =
            uploadPreset.toRequestBody(
                "text/plain"
                    .toMediaTypeOrNull()
            )

        RetrofitClient
            .cloudinaryApi
            .uploadImage(
                cloudName = cloudName,
                imageFile = imagePart,
                uploadPreset =
                    presetRequestBody
            )
            .enqueue(
                object :
                    Callback<CloudinaryUploadResponse> {

                    override fun onResponse(
                        call: Call<CloudinaryUploadResponse>,
                        response: Response<CloudinaryUploadResponse>
                    ) {

                        tempFile.delete()

                        if (_binding == null) {
                            return
                        }

                        val responseBody =
                            response.body()

                        if (
                            response.isSuccessful &&
                            responseBody != null &&
                            responseBody.secureUrl
                                .isNotEmpty()
                        ) {

                            val itemWithNewImage =
                                item.copy(
                                    imageUrl =
                                        responseBody.secureUrl
                                )

                            updateReportInFirebase(
                                itemWithNewImage
                            )

                        } else {

                            setUpdating(false)

                            binding.tvImageStatus.text =
                                getString(
                                    R.string.new_image_selected
                                )

                            showMessage(
                                "Upload gambar gagal. Kode: ${response.code()}"
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<CloudinaryUploadResponse>,
                        throwable: Throwable
                    ) {

                        tempFile.delete()

                        if (_binding == null) {
                            return
                        }

                        setUpdating(false)

                        binding.tvImageStatus.text =
                            getString(
                                R.string.new_image_selected
                            )

                        showMessage(
                            throwable.localizedMessage
                                ?: "Upload gambar gagal"
                        )
                    }
                }
            )
    }

    /*
     * Hanya field yang dapat diedit yang diperbarui.
     *
     * userId, status dan createdAt tidak disentuh.
     */
    private fun updateReportInFirebase(
        item: Item
    ) {

        setUpdating(true)

        val updates =
            mapOf<String, Any>(
                "reportType" to item.reportType,
                "title" to item.title,
                "category" to item.category,
                "location" to item.location,
                "eventDate" to item.eventDate,
                "description" to item.description,
                "contact" to item.contact,
                "imageUrl" to item.imageUrl
            )

        itemsReference
            .child(itemId)
            .updateChildren(updates)
            .addOnSuccessListener {

                if (_binding == null) {
                    return@addOnSuccessListener
                }

                setUpdating(false)

                Toast.makeText(
                    requireContext(),
                    R.string.report_updated,
                    Toast.LENGTH_LONG
                ).show()

                findNavController().navigateUp()
            }
            .addOnFailureListener { exception ->

                if (_binding == null) {
                    return@addOnFailureListener
                }

                setUpdating(false)

                showMessage(
                    getString(
                        R.string.report_update_failed,
                        exception.localizedMessage
                            ?: "Kesalahan database"
                    )
                )
            }
    }

    private fun showInitialLoading() {

        binding.progressBar.visibility =
            View.VISIBLE

        binding.formContent.visibility =
            View.INVISIBLE
    }

    private fun showForm() {

        binding.progressBar.visibility =
            View.GONE

        binding.formContent.visibility =
            View.VISIBLE
    }

    private fun setUpdating(
        isUpdating: Boolean
    ) {

        binding.progressBar.visibility =
            if (isUpdating) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.formContent.alpha =
            if (isUpdating) {
                0.6f
            } else {
                1f
            }

        binding.btnBack.isEnabled =
            !isUpdating

        binding.rbLost.isEnabled =
            !isUpdating

        binding.rbFound.isEnabled =
            !isUpdating

        binding.etItemName.isEnabled =
            !isUpdating

        binding.spCategory.isEnabled =
            !isUpdating

        binding.etLocation.isEnabled =
            !isUpdating

        binding.etEventDate.isEnabled =
            !isUpdating

        binding.etDescription.isEnabled =
            !isUpdating

        binding.etContact.isEnabled =
            !isUpdating

        binding.btnChooseImage.isEnabled =
            !isUpdating

        binding.btnUpdateReport.isEnabled =
            !isUpdating

        binding.btnUpdateReport.text =
            if (isUpdating) {
                getString(
                    R.string.updating_report
                )
            } else {
                getString(
                    R.string.update_report
                )
            }
    }

    private fun clearErrors() {

        binding.etItemName.error = null
        binding.etLocation.error = null
        binding.etEventDate.error = null
        binding.etDescription.error = null
        binding.etContact.error = null
    }

    private fun showMessage(message: String) {

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

    companion object {

        const val ARG_ITEM_ID =
            "edit_item_id"

        private const val REPORT_TYPE_LOST =
            "LOST"

        private const val REPORT_TYPE_FOUND =
            "FOUND"
    }
}