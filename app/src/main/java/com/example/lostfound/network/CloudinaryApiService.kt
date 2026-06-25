package com.example.lostfound.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface CloudinaryApiService {

    @Multipart
    @POST("v1_1/{cloudName}/image/upload")
    fun uploadImage(

        @Path("cloudName")
        cloudName: String,

        @Part
        imageFile: MultipartBody.Part,

        @Part("upload_preset")
        uploadPreset: RequestBody

    ): Call<CloudinaryUploadResponse>
}