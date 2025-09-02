package com.example.mynotebook.me

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.BuildConfig
import com.example.mynotebook.api.*
import com.example.mynotebook.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class MeUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val profile: ProfileResponse? = null,
    val nameInput: String = "",
    val passwordInput: String = "",
    val toast: String? = null
)

class MeViewModel : ViewModel() {
    private val _ui = MutableStateFlow(MeUiState())
    val ui: StateFlow<MeUiState> = _ui
    private fun ensureAbsolute(url: String?): String? {
        if (url.isNullOrBlank()) return url
        if (url.startsWith("http", ignoreCase = true)) return url
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val path = if (url.startsWith("/")) url else "/$url"
        return base + path
    }
    private fun normalizeProfile(p: ProfileResponse?): ProfileResponse? =
        p?.copy(picture = ensureAbsolute(p.picture))

    fun load(userId: Int) = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        try {
            val r = RetrofitClient.api.getProfile(userId)
            if (r.isSuccessful) {
                val p = r.body()
                _ui.update {
                    it.copy(
                        loading = false,
                        profile = p,
                        nameInput = p?.userName.orEmpty()
                    )
                }
            } else {
                _ui.update { it.copy(loading = false, error = "Load failed: ${r.code()}") }
            }
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = e.message ?: "Network error") }
        }
    }

    fun setName(name: String) { _ui.update { it.copy(nameInput = name) } }
    fun setPwd(pwd: String) { _ui.update { it.copy(passwordInput = pwd) } }
    fun clearToast() { _ui.update { it.copy(toast = null) } }

    fun saveName(userId: Int) = viewModelScope.launch {
        val name = _ui.value.nameInput.trim()
        if (name.isEmpty()) return@launch
        _ui.update { it.copy(saving = true, error = null) }
        try {
            val r = RetrofitClient.api.updateProfile(userId, UpdateProfileRequest(userName = name))
            if (r.isSuccessful) {
                _ui.update { it.copy(saving = false, profile = r.body(), toast = "Name updated") }
            } else {
                _ui.update { it.copy(saving = false, error = "Update failed: ${r.code()}") }
            }
        } catch (e: Exception) {
            _ui.update { it.copy(saving = false, error = e.message ?: "Network error") }
        }
    }

    fun savePassword(userId: Int) = viewModelScope.launch {
        val pwd = _ui.value.passwordInput
        if (pwd.isEmpty()) return@launch
        _ui.update { it.copy(saving = true, error = null) }
        try {
            val r = RetrofitClient.api.changePassword(userId, ChangePasswordRequest(pwd))
            if (r.isSuccessful) {
                _ui.update { it.copy(saving = false, passwordInput = "", toast = "Password updated") }
            } else {
                _ui.update { it.copy(saving = false, error = "Update failed: ${r.code()}") }
            }
        } catch (e: Exception) {
            _ui.update { it.copy(saving = false, error = e.message ?: "Network error") }
        }
    }

    fun uploadAvatar(userId: Int, uri: Uri, cr: ContentResolver) = viewModelScope.launch {
        _ui.update { it.copy(saving = true, error = null) }
        try {
            val part = withContext(Dispatchers.IO) { uriToPart("file", uri, cr) }
            val r = RetrofitClient.api.uploadAvatar(userId, part)
            if (r.isSuccessful) {
                val body = r.body()
                _ui.update {
                    it.copy(
                        saving = false,
                        profile = body?.user ?: it.profile?.copy(picture = body?.url),
                        toast = "Avatar updated"
                    )
                }
            } else {
                _ui.update { it.copy(saving = false, error = "Upload failed: ${r.code()}") }
            }
        } catch (e: Exception) {
            _ui.update { it.copy(saving = false, error = e.message ?: "Network error") }
        }
    }

    // ---- helper: 把 Uri 变成 Multipart.Part ----
    private fun uriToPart(formKey: String, uri: Uri, cr: ContentResolver): MultipartBody.Part {
        val mime = cr.getType(uri) ?: "image/*"
        val fileName = queryDisplayName(cr, uri) ?: "avatar.jpg"
        val bytes = cr.openInputStream(uri)!!.use { it.readBytes() }
        val rb = bytes.toRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(formKey, fileName, rb)
    }

    private fun queryDisplayName(cr: ContentResolver, uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = cr.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) name = it.getString(index)
        }
        return name
    }
}