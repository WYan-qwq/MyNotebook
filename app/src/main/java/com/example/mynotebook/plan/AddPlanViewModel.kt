package com.example.mynotebook.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.PlanCreateRequest
import com.example.mynotebook.api.PlanItem
import com.example.mynotebook.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AddPlanUiState(
    val date: LocalDate = LocalDate.now(),
    val showEditor: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0,
    val title: String = "",
    val details: String = "",
    val alarm: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val justCreated: PlanItem? = null
)

class AddPlanViewModel(private val userId: Int) : ViewModel() {

    private val _ui = MutableStateFlow(AddPlanUiState())
    val ui: StateFlow<AddPlanUiState> = _ui

    fun setDate(d: LocalDate) { _ui.value = _ui.value.copy(date = d) }
    fun setHour(h: Int) { _ui.value = _ui.value.copy(hour = h.coerceIn(0, 23)) }
    fun setMinute(m: Int) { _ui.value = _ui.value.copy(minute = m.coerceIn(0, 59)) }
    fun setTitle(t: String) { _ui.value = _ui.value.copy(title = t) }
    fun setDetails(d: String) { _ui.value = _ui.value.copy(details = d) }
    fun toggleAlarm() { _ui.value = _ui.value.copy(alarm = !_ui.value.alarm) }
    fun openEditor() { _ui.value = _ui.value.copy(showEditor = true, error = null, justCreated = null) }
    fun closeEditor() { _ui.value = _ui.value.copy(showEditor = false) }
    fun clearMessage() { _ui.value = _ui.value.copy(justCreated = null, error = null) }

    fun submit() {
        val s = _ui.value
        if (s.title.isBlank()) {
            _ui.value = s.copy(error = "Title is required.")
            return
        }
        viewModelScope.launch {
            _ui.value = s.copy(loading = true, error = null)
            try {
                val body = PlanCreateRequest(
                    userId = userId,
                    createTime = Instant.now().toString(),
                    date = s.date.format(DateTimeFormatter.ISO_DATE),
                    hour = s.hour,
                    minute = s.minute,
                    title = s.title.ifBlank { null },
                    details = s.details.ifBlank { null },
                    alarm = if (s.alarm) 1 else 0,
                    finished = 0
                )
                val resp = RetrofitClient.api.createPlan(body)
                if (resp.isSuccessful && resp.body() != null) {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        showEditor = false,
                        title = "",
                        details = "",
                        alarm = false,
                        justCreated = resp.body()
                    )
                } else {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = "Create failed (${resp.code()})."
                    )
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Network error.")
            }
        }
    }

    companion object {
        fun provideFactory(userId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AddPlanViewModel(userId) as T
                }
            }
    }
}
