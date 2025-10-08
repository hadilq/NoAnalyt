package com.noanalyt.runtime

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log

class NoAnalytContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        try {
            NoAnalyt.setInstance(NoAnalytImpl())
        } catch (ex: Exception) {
            Log.i("NoAnalytContentProvider ", "Failed to auto initialize the NoAnalyt runtime", ex)
        }
        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }
}