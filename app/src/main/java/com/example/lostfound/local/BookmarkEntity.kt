package com.example.lostfound.local

import androidx.room.Entity
import com.example.lostfound.model.Item

/*
 * Entity adalah tabel pada Room Database.
 *
 * Satu baris pada tabel bookmarks berisi
 * satu laporan yang disimpan pengguna.
 *
 * Primary key terdiri dari:
 * userId + itemId
 *
 * Artinya laporan yang sama dapat disimpan oleh
 * akun yang berbeda, tetapi tidak dapat diduplikasi
 * oleh akun yang sama.
 */
@Entity(
    tableName = "bookmarks",
    primaryKeys = ["userId", "itemId"]
)
data class BookmarkEntity(

    // UID akun Firebase yang menyimpan bookmark
    val userId: String,

    // ID laporan dari Firebase
    val itemId: String,

    val ownerId: String,

    val title: String,

    val category: String,

    val reportType: String,

    val location: String,

    val eventDate: String,

    val description: String,

    val contact: String,

    val imageUrl: String,

    val status: String,

    val createdAt: Long,

    // Waktu ketika bookmark disimpan
    val savedAt: Long
)

/*
 * Mengubah Item Firebase menjadi BookmarkEntity Room.
 */
fun Item.toBookmarkEntity(
    bookmarkUserId: String
): BookmarkEntity {

    return BookmarkEntity(
        userId = bookmarkUserId,
        itemId = id,
        ownerId = userId,
        title = title,
        category = category,
        reportType = reportType,
        location = location,
        eventDate = eventDate,
        description = description,
        contact = contact,
        imageUrl = imageUrl,
        status = status,
        createdAt = createdAt,
        savedAt = System.currentTimeMillis()
    )
}

/*
 * Mengubah BookmarkEntity kembali menjadi Item
 * agar dapat ditampilkan oleh ItemAdapter.
 */
fun BookmarkEntity.toItem(): Item {

    return Item(
        id = itemId,
        userId = ownerId,
        title = title,
        category = category,
        reportType = reportType,
        location = location,
        eventDate = eventDate,
        description = description,
        contact = contact,
        imageUrl = imageUrl,
        status = status,
        createdAt = createdAt
    )
}