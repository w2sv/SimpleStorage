package com.anggrayudi.storage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat.checkSelfPermission
import com.anggrayudi.storage.extension.documentFileFromTreeUri
import com.anggrayudi.storage.extension.isExternalStorageDocument
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.StorageId.PRIMARY
import com.anggrayudi.storage.file.isWritable

object SimpleStorage {

    private const val TAG = "SimpleStorage"

    @JvmStatic
    val externalStoragePath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    @JvmStatic
    val isSdCardPresent: Boolean
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    @JvmStatic
    @SuppressLint("InlinedApi")
    fun getDefaultExternalStorageIntent(context: Context): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            if (Build.VERSION.SDK_INT >= 26) {
                putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    context.documentFileFromTreeUri(DocumentFileCompat.createDocumentUri(PRIMARY))?.uri
                )
            }
        }
    }

    /**
     * For read and write permissions
     */
    @JvmStatic
    fun hasStoragePermission(context: Context): Boolean {
        return checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                hasStorageReadPermission(context)
    }

    /**
     * For read permission only
     */
    @JvmStatic
    fun hasStorageReadPermission(context: Context): Boolean {
        return checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun hasFullDiskAccess(context: Context, storageId: String): Boolean {
        return hasStorageAccess(
            context,
            DocumentFileCompat.buildAbsolutePath(context, storageId, "")
        )
    }

    /**
     * In API 29+, `/storage/emulated/0` may not be granted for URI permission,
     * but all directories under `/storage/emulated/0/Download` are granted and accessible.
     *
     * @param requiresWriteAccess `true` if you expect this path should be writable
     * @return `true` if you have URI access to this path
     * @see [DocumentFileCompat.buildAbsolutePath]
     * @see [DocumentFileCompat.buildSimplePath]
     */
    @JvmStatic
    @JvmOverloads
    fun hasStorageAccess(
        context: Context,
        fullPath: String,
        requiresWriteAccess: Boolean = true
    ): Boolean {
        return DocumentFileCompat.getAccessibleRootDocumentFile(
            context,
            fullPath,
            requiresWriteAccess
        ) != null &&
                (
                        Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
                                requiresWriteAccess && hasStoragePermission(context) || !requiresWriteAccess && hasStorageReadPermission(
                            context
                        )
                        )
    }

    /**
     * Max persistable URI per app is 128, so cleanup redundant URI permissions. Given the following URIs:
     * 1) `content://com.android.externalstorage.documents/tree/primary%3AMovies`
     * 2) `content://com.android.externalstorage.documents/tree/primary%3AMovies%2FHorror`
     *
     * Then remove the second URI, because it has been covered by the first URI.
     *
     * Read [Count Your SAF Uri Persisted Permissions!](https://commonsware.com/blog/2020/06/13/count-your-saf-uri-permission-grants.html)
     */
    @JvmStatic
    @WorkerThread
    fun cleanupRedundantUriPermissions(context: Context) {
        val resolver = context.contentResolver
        // e.g. content://com.android.externalstorage.documents/tree/primary%3AMusic
        val persistedUris = resolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission && it.uri.isExternalStorageDocument }
            .map { it.uri }
        val writeFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val uniqueUriParents = DocumentFileCompat.findUniqueParents(
            context,
            persistedUris.mapNotNull { it.path?.substringAfter("/tree/") }
        )
        persistedUris.forEach {
            if (DocumentFileCompat.buildAbsolutePath(
                    context,
                    it.path.orEmpty().substringAfter("/tree/")
                ) !in uniqueUriParents
            ) {
                resolver.releasePersistableUriPermission(it, writeFlags)
                Log.d(TAG, "Removed redundant URI permission => $it")
            }
        }
    }

    /**
     * It will remove URI permissions that are no longer writable.
     * Maybe you have access to the URI once, but the access is gone now for some reasons, for example
     * when the SD card is changed/replaced. Each SD card has their own unique storage ID.
     */
    @JvmStatic
    @WorkerThread
    fun removeObsoleteUriPermissions(context: Context) {
        val resolver = context.contentResolver
        val persistedUris = resolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission && it.uri.isExternalStorageDocument }
            .map { it.uri }
        val writeFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        persistedUris.forEach {
            if (DocumentFileCompat.fromUri(context, it)?.isWritable(context) != true) {
                resolver.releasePersistableUriPermission(it, writeFlags)
                Log.d(TAG, "Removed invalid URI permission => $it")
            }
        }
    }
}
