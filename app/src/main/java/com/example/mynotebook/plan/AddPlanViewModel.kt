package com.example.mynotebook.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.PlanCreateRequest
import com.example.mynotebook.api.PlanItem
import com.example.mynotebook.api.PlanUpdateRequest
import com.example.mynotebook.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

data class DraftPlan(
    val id: Long,
    val hour: Int = 9,
    val minute: Int = 0,
    val title: String = "",
    val details: String = "",
    val alarm: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null
)

data class AddPlanUiState(
    val date: LocalDate = LocalDate.now(),
    val existing: List<PlanItem> = emptyList(),   // 当天已有计划
    val loadingList: Boolean = false,
    val listError: String? = null,

    val drafts: List<DraftPlan> = emptyList(),    // 正在编辑的多条草稿
    val createdAny: Boolean = false               // 本次进入页面是否成功创建过
)

class AddPlanViewModel(private val userId: Int) : ViewModel() {

    private val _ui = MutableStateFlow(AddPlanUiState())
    val ui: StateFlow<AddPlanUiState> = _ui

    private val idGen = AtomicLong(1L)

    init {
        // 进入页面默认加载今天
        loadForDate(_ui.value.date)
    }

    fun setDate(d: LocalDate) {
        _ui.value = _ui.value.copy(date = d, listError = null)
        loadForDate(d)
    }

    private fun sortPlans(list: List<PlanItem>) =
        list.sortedWith(compareBy({ it.hour ?: 0 }, { it.minute ?: 0 }, { it.id }))

    fun loadForDate(d: LocalDate) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loadingList = true, listError = null)
            try {
                val dateStr = d.format(DateTimeFormatter.ISO_DATE)
                val resp = RetrofitClient.api.getPlansForDay(userId, dateStr)
                if (resp.isSuccessful) {
                    _ui.value = _ui.value.copy(
                        loadingList = false,
                        existing = sortPlans(resp.body().orEmpty())
                    )
                } else {
                    _ui.value = _ui.value.copy(
                        loadingList = false,
                        listError = "Load failed (${resp.code()})"
                    )
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loadingList = false, listError = e.message ?: "Network error")
            }
        }
    }

    // ===== 草稿编辑操作 =====
    fun addDraft() {
        val newDraft = DraftPlan(id = idGen.getAndIncrement())
        _ui.value = _ui.value.copy(drafts = _ui.value.drafts + newDraft)
    }

    fun removeDraft(id: Long) {
        _ui.value = _ui.value.copy(drafts = _ui.value.drafts.filterNot { it.id == id })
    }

    fun setDraftHour(id: Long, h: Int) =
        updateDraft(id) { it.copy(hour = h.coerceIn(0, 23), error = null) }

    fun setDraftMinute(id: Long, m: Int) =
        updateDraft(id) { it.copy(minute = m.coerceIn(0, 59), error = null) }

    fun setDraftTitle(id: Long, t: String) =
        updateDraft(id) { it.copy(title = t, error = null) }

    fun setDraftDetails(id: Long, d: String) =
        updateDraft(id) { it.copy(details = d) }

    fun toggleDraftAlarm(id: Long) =
        updateDraft(id) { it.copy(alarm = !it.alarm) }

    private fun updateDraft(id: Long, block: (DraftPlan) -> DraftPlan) {
        _ui.value = _ui.value.copy(
            drafts = _ui.value.drafts.map { if (it.id == id) block(it) else it }
        )
    }

    /** 提交某一条草稿（提交成功：草稿删掉 → 新记录追加到 existing 并排序） */
    fun submitDraft(id: Long) {
        val s = _ui.value
        val draft = s.drafts.firstOrNull { it.id == id } ?: return
        if (draft.title.isBlank()) {
            updateDraft(id) { it.copy(error = "Title is required.") }
            return
        }
        viewModelScope.launch {
            updateDraft(id) { it.copy(submitting = true, error = null) }
            try {
                val body = PlanCreateRequest(
                    userId = userId,
                    createTime = Instant.now().toString(),
                    date = s.date.format(DateTimeFormatter.ISO_DATE),
                    hour = draft.hour,
                    minute = draft.minute,
                    title = draft.title.ifBlank { null },
                    details = draft.details.ifBlank { null },
                    alarm = if (draft.alarm) 1 else 0,
                    finished = 0
                )
                val resp = RetrofitClient.api.createPlan(body)
                if (resp.isSuccessful && resp.body() != null) {
                    val newItem = resp.body()!!
                    val newExisting = sortPlans(s.existing + newItem)
                    _ui.value = _ui.value.copy(
                        existing = newExisting,
                        drafts = _ui.value.drafts.filterNot { it.id == id },
                        createdAny = true
                    )
                } else {
                    updateDraft(id) { it.copy(submitting = false, error = "Create failed (${resp.code()}).") }
                }
            } catch (e: Exception) {
                updateDraft(id) { it.copy(submitting = false, error = e.message ?: "Network error.") }
            }
        }
    }

    fun updatePlan(id: Int, hour: Int, minute: Int, title: String, details: String?, alarm: Int) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.updatePlan(
                    id,
                    PlanUpdateRequest(
                        hour = hour,
                        minute = minute,
                        title = title,
                        details = details,
                        alarm = alarm
                    )
                )
                if (resp.isSuccessful) {
                    // 重新拉取当前日期的 existing 列表
                    loadForDate(_ui.value.date)
                } else {
                    // 可选：错误提示
                }
            } catch (_: Exception) { /* 可选：错误提示 */ }
        }
    }


    fun deletePlan(id: Int) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.deletePlan(id)
                if (resp.isSuccessful) {
                    // 删完刷新当前日期的 existing 列表
                    loadForDate(_ui.value.date)
                } else {
                    // 可选：错误处理
                }
            } catch (_: Exception) {
                // 可选：异常处理
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