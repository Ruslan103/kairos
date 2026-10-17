package com.example.kaizenkanban.ui.board

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

data class PhoneContactDraft(
    val name: String,
    val phone: String,
    val email: String,
    val social: String
)

@Composable
fun rememberPhoneContactImport(onDraft: (PhoneContactDraft) -> Unit): () -> Unit {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val draft = readPhoneContact(context, uri)
        if (draft == null || draft.name.isBlank()) {
            Toast.makeText(context, s.cardNoContact, Toast.LENGTH_SHORT).show()
        } else {
            onDraft(draft)
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pick.launch(null) else {
            Toast.makeText(context, s.cardContactsDenied, Toast.LENGTH_SHORT).show()
        }
    }
    return {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) pick.launch(null) else permission.launch(Manifest.permission.READ_CONTACTS)
    }
}

fun readPhoneContact(context: Context, uri: Uri): PhoneContactDraft? {
    val resolver = context.contentResolver
    var id: String? = null
    var name = ""
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return null
        val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
        val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
        if (idIdx >= 0) id = cursor.getString(idIdx)
        if (nameIdx >= 0) name = cursor.getString(nameIdx).orEmpty()
    }
    val contactId = id ?: return null
    var phone = ""
    resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) phone = cursor.getString(0).orEmpty()
    }
    var email = ""
    resolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
        arrayOf(contactId),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) email = cursor.getString(0).orEmpty()
    }
    val socials = mutableListOf<String>()
    val websites = mutableListOf<String>()
    resolver.query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.CommonDataKinds.Im.PROTOCOL,
            ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL
        ),
        "${ContactsContract.Data.CONTACT_ID} = ?",
        arrayOf(contactId),
        null
    )?.use { cursor ->
        val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
        val dataIdx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
        val protocolIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL)
        val customIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL)
        while (cursor.moveToNext()) {
            val data = if (dataIdx >= 0) cursor.getString(dataIdx).orEmpty().trim() else ""
            if (data.isEmpty()) continue
            when (if (mimeIdx >= 0) cursor.getString(mimeIdx) else null) {
                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                    val custom = if (customIdx >= 0) cursor.getString(customIdx).orEmpty() else ""
                    val protocol = if (protocolIdx >= 0) cursor.getInt(protocolIdx) else -1
                    val label = imLabel(protocol, custom)
                    socials += if (label.isBlank()) data else "$label $data"
                }
                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> websites += data
            }
        }
    }
    websites.filter { isSocialUrl(it) }.forEach { socials += it }
    if (socials.isEmpty()) websites.firstOrNull()?.let { socials += it }
    return PhoneContactDraft(
        name = name.trim(),
        phone = phone.trim(),
        email = email.trim(),
        social = socials.distinct().joinToString(", ")
    )
}

private fun imLabel(protocol: Int, custom: String): String {
    if (custom.isNotBlank()) return custom.trim()
    return when (protocol) {
        ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE -> "Skype"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK -> "Google Talk"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ -> "QQ"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ -> "ICQ"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER -> "Jabber"
        else -> ""
    }
}

private fun isSocialUrl(url: String): Boolean {
    val value = url.lowercase()
    return listOf(
        "t.me", "telegram", "wa.me", "whatsapp", "instagram", "vk.com",
        "facebook", "fb.com", "twitter", "x.com", "tiktok", "ok.ru"
    ).any { value.contains(it) }
}
