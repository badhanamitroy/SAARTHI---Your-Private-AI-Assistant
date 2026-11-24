package com.saarthi.assistant.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Interface for resolving phone numbers to contact display names.
 */
interface ContactResolver {
    fun resolveContactName(phoneNumber: String?): String?
}

/**
 * Production implementation using Android ContactsContract.PhoneLookup.
 * Strictly respects READ_CONTACTS permission.
 */
class AndroidContactResolver(private val context: Context) : ContactResolver {

    override fun resolveContactName(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) {
            return null
        }

        // Permission check
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return null
        }

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            return name.trim()
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Test fake for deterministic unit tests.
 */
class FakeContactResolver(
    private val contacts: Map<String, String> = emptyMap()
) : ContactResolver {

    override fun resolveContactName(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        return contacts[phoneNumber.trim()]
    }
}

