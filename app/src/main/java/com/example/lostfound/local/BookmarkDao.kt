package com.example.lostfound.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/*
 * DAO adalah kumpulan operasi yang diperbolehkan
 * pada tabel bookmarks.
 */
@Dao
interface BookmarkDao {

    /*
     * Mengambil seluruh bookmark milik satu akun.
     *
     * Flow membuat tampilan otomatis diperbarui
     * ketika isi database berubah.
     */
    @Query(
        """
        SELECT * FROM bookmarks
        WHERE userId = :userId
        ORDER BY savedAt DESC
        """
    )
    fun observeBookmarks(
        userId: String
    ): Flow<List<BookmarkEntity>>

    /*
     * Mengambil seluruh ID laporan yang disimpan.
     *
     * Digunakan agar Adapter mengetahui laporan
     * mana yang sudah ditandai bookmark.
     */
    @Query(
        """
        SELECT itemId FROM bookmarks
        WHERE userId = :userId
        """
    )
    fun observeBookmarkedItemIds(
        userId: String
    ): Flow<List<String>>

    /*
     * Memeriksa satu laporan sudah disimpan
     * atau belum.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM bookmarks
            WHERE userId = :userId
            AND itemId = :itemId
        )
        """
    )
    suspend fun isBookmarked(
        userId: String,
        itemId: String
    ): Boolean

    /*
     * Menyimpan bookmark.
     *
     * REPLACE berarti jika datanya sudah ada,
     * Room akan memperbarui data lama.
     */
    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertBookmark(
        bookmark: BookmarkEntity
    )

    /*
     * Menghapus satu bookmark.
     */
    @Query(
        """
        DELETE FROM bookmarks
        WHERE userId = :userId
        AND itemId = :itemId
        """
    )
    suspend fun deleteBookmark(
        userId: String,
        itemId: String
    )

    /*
     * Menghapus seluruh bookmark milik akun.
     * Belum dipakai sekarang, tetapi berguna nanti.
     */
    @Query(
        """
        DELETE FROM bookmarks
        WHERE userId = :userId
        """
    )
    suspend fun deleteAllBookmarks(
        userId: String
    )
}