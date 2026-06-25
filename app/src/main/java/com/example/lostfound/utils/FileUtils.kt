package com.example.lostfound.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

object FileUtils {

    fun createTempFileFromUri(
        context: Context,
        uri: Uri
    ): File? {

        val contentResolver =
            context.contentResolver

        val mimeType =
            contentResolver.getType(uri)
                ?: "image/jpeg"

        val extension =
            MimeTypeMap
                .getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?: "jpg"

        val tempFile = File.createTempFile(
            "lostfound_upload_",
            ".$extension",
            context.cacheDir
        )

        return try {

            val inputStream =
                contentResolver.openInputStream(uri)
                    ?: return null

            inputStream.use { input ->

                tempFile
                    .outputStream()
                    .use { output ->

                        input.copyTo(output)
                    }
            }

            tempFile

        } catch (exception: Exception) {

            tempFile.delete()
            null
        }
    }
}