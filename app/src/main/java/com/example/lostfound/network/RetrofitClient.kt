package com.example.lostfound.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val CLOUDINARY_BASE_URL =
        "https://api.cloudinary.com/"

    private val retrofit: Retrofit by lazy {

        Retrofit.Builder()
            .baseUrl(CLOUDINARY_BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    val cloudinaryApi: CloudinaryApiService by lazy {

        retrofit.create(
            CloudinaryApiService::class.java
        )
    }
}