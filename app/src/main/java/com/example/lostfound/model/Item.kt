package com.example.lostfound.model

data class Item(
    var id: String = "",
    var userId: String = "",
    var title: String = "",
    var category: String = "",
    var reportType: String = "",
    var location: String = "",
    var eventDate: String = "",
    var description: String = "",
    var contact: String = "",
    var imageUrl: String = "",
    var status: String = "OPEN",
    var createdAt: Long = 0L
)