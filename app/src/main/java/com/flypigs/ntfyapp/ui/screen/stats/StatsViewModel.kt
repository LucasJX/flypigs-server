package com.flypigs.ntfyapp.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flypigs.ntfyapp.data.local.dao.CategoryCount
import com.flypigs.ntfyapp.data.local.dao.DailyCount
import com.flypigs.ntfyapp.data.local.dao.TopicCount
import com.flypigs.ntfyapp.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    // Today's start timestamp
    private val startOfDay: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    // 7 days ago timestamp
    private val sevenDaysAgo: Long
        get() {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -7)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val todayCount: StateFlow<Int> = messageRepository.getMessageCountSince(startOfDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categoryStats: StateFlow<List<CategoryCount>> = messageRepository.getCategoryStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyStats: StateFlow<List<DailyCount>> = messageRepository.getDailyStats(sevenDaysAgo)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topicStats: StateFlow<List<TopicCount>> = messageRepository.getTopicStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
