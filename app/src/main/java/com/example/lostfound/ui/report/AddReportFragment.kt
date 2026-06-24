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
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentAddReportBinding
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

class AddReportFragment : Fragment() {

    private var _binding: FragmentAddReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    // Referensi menuju node items
    private lateinit var itemsReference: DatabaseReference

    // Gambar yang dipilih pengguna
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null && _binding != null) {

                selectedImageUri = uri

                binding.ivImagePreview.setImageURI(uri)

                binding.tvImageStatus.text =
                    getString(R.string.image_selected)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAddReportBinding.inflate(
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

        binding.btnSaveReport.setOnClickListener {
            validateForm()
        }
    }

    private fun showDatePicker() {

        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                val formattedDate = String.format(
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
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.maxDate =
            System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun validateForm() {

        clearInputErrors()

        val reportType =
            when (
                binding.rgReportType.checkedRadioButtonId
            ) {

                R.id.rbLost -> "LOST"
                R.id.rbFound -> "FOUND"
                else -> ""
            }

        val itemName =
            binding.etItemName.text
                .toString()
                .trim()

        val category =
            binding.spCategory.selectedItem
                .toString()

        val area =
            binding.spArea.selectedItem
                .toString()

        val locationDetail =
            binding.etLocationDetail.text
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
                "Pilih jenis laporan terlebih dahulu"
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
            binding.spCategory.selectedItemPosition == 0
        ) {

            showMessage("Pilih kategori barang")
            return
        }

        if (
            binding.spArea.selectedItemPosition == 0
        ) {

            showMessage("Pilih wilayah kejadian")
            return
        }

        if (locationDetail.isEmpty()) {

            binding.etLocationDetail.error =
                "Detail lokasi wajib diisi"

            binding.etLocationDetail.requestFocus()
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

        val imageUri = selectedImageUri

        if (imageUri == null) {

            showMessage(
                "Pilih foto barang terlebih dahulu"
            )

            return
        }

        createReportItem(
            reportType = reportType,
            itemName = itemName,
            category = category,
            area = area,
            locationDetail = locationDetail,
            eventDate = eventDate,
            description = description,
            contact = contact,
            imageUri = imageUri
        )
    }

    private fun createReportItem(
        reportType: String,
        itemName: String,
        category: String,
        area: String,
        locationDetail: String,
        eventDate: String,
        description: String,
        contact: String,
        imageUri: Uri
    ) {

        val currentUser = auth.currentUser

        if (currentUser == null) {

            showMessage(
                "Session pengguna tidak ditemukan"
            )

            return
        }

        val fullLocation =
            "$locationDetail, $area, Kota Mataram"

        val item = Item(
            id = "",
            userId = currentUser.uid,
            title = itemName,
            category = category,
            reportType = reportType,
            location = fullLocation,
            eventDate = eventDate,
            description = description,
            contact = contact,
            imageUrl = "",
            status = "OPEN",
            createdAt = System.currentTimeMillis()
        )

        uploadImage(
            item = item,
            imageUri = imageUri
        )
    }

    private fun uploadImage(
        item: Item,
        imageUri: Uri
    ) {

        val cloudName =
            FirebaseConfig.CLOUDINARY_CLOUD_NAME

        val uploadPreset =
            FirebaseConfig.CLOUDINARY_UPLOAD_PRESET

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

        // Maksimum ukuran gambar 5 MB
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

        setLoading(true)

        binding.tvImageStatus.text =
            "Mengunggah gambar..."

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
                "text/plain".toMediaTypeOrNull()
            )

        RetrofitClient.cloudinaryApi
            .uploadImage(
                cloudName = cloudName,
                imageFile = imagePart,
                uploadPreset = presetRequestBody
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
                            responseBody.secureUrl.isNotEmpty()
                        ) {

                            binding.tvImageStatus.text =
                                "Gambar berhasil diunggah"

                            val itemWithImage =
                                item.copy(
                                    imageUrl =
                                        responseBody.secureUrl
                                )

                            saveReportToDatabase(
                                itemWithImage
                            )

                        } else {

                            setLoading(false)

                            binding.tvImageStatus.text =
                                getString(
                                    R.string.image_selected
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

                        setLoading(false)

                        binding.tvImageStatus.text =
                            getString(
                                R.string.image_selected
                            )

                        showMessage(
                            throwable.localizedMessage
                                ?: "Upload gambar gagal"
                        )
                    }
                }
            )
    }

    private fun saveReportToDatabase(
        item: Item
    ) {

        binding.btnSaveReport.text =
            "Menyimpan laporan..."

        // Membuat ID unik dari Firebase
        val itemId =
            itemsReference.push().key

        if (itemId == null) {

            setLoading(false)

            showMessage(
                "ID laporan gagal dibuat"
            )

            return
        }

        val finalItem =
            item.copy(id = itemId)

        itemsReference
            .child(itemId)
            .setValue(finalItem)
            .addOnSuccessListener {

                if (_binding == null) {
                    return@addOnSuccessListener
                }

                setLoading(false)

                Toast.makeText(
                    requireContext(),
                    "Laporan berhasil disimpan",
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
                    exception.localizedMessage
                        ?: "Laporan gagal disimpan"
                )
            }
    }

    private fun setLoading(
        isLoading: Boolean
    ) {

        binding.btnSaveReport.isEnabled =
            !isLoading

        binding.btnChooseImage.isEnabled =
            !isLoading

        binding.btnBack.isEnabled =
            !isLoading

        binding.etItemName.isEnabled =
            !isLoading

        binding.spCategory.isEnabled =
            !isLoading

        binding.spArea.isEnabled =
            !isLoading

        binding.etLocationDetail.isEnabled =
            !isLoading

        binding.etEventDate.isEnabled =
            !isLoading

        binding.etDescription.isEnabled =
            !isLoading

        binding.etContact.isEnabled =
            !isLoading

        binding.rbLost.isEnabled =
            !isLoading

        binding.rbFound.isEnabled =
            !isLoading

        binding.btnSaveReport.text =
            if (isLoading) {
                "Memproses..."
            } else {
                getString(R.string.save_report)
            }
    }

    private fun clearInputErrors() {

        binding.etItemName.error = null
        binding.etLocationDetail.error = null
        binding.etEventDate.error = null
        binding.etDescription.error = null
        binding.etContact.error = null
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