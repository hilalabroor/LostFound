package com.example.lostfound.network

import com.google.gson.annotations.SerializedName

data class CloudinaryUploadResponse(

    @SerializedName("secure_url")
    val secureUrl: String = "",

    @SerializedName("public_id")
    val publicId: String = ""
)