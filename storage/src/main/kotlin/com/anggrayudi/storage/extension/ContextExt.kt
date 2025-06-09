@file:JvmName("ContextUtils")

package com.anggrayudi.storage.extension

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

fun Context.documentFileFromTreeUri(fileUri: Uri): DocumentFile? =
    try {
        DocumentFile.fromTreeUri(this, fileUri)
    } catch (_: Exception) {
        null
    }
