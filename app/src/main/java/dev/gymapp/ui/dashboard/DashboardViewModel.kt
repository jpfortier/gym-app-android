package dev.gymapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gymapp.api.ErrorBodyParser
import dev.gymapp.api.GymApi
import dev.gymapp.api.PrImageLoader
import dev.gymapp.api.models.ApiError
import dev.gymapp.api.models.ExerciseVariant
import dev.gymapp.api.models.Pr
import dev.gymapp.api.models.Session
import dev.gymapp.api.models.toPersonalRecord
import dev.gymapp.ui.chat.PrWithImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val latestPr: Pr? = null,
    val latestPrImage: ByteArray? = null,
    val allPrs: List<Pr> = emptyList(),
    val recentPrsByType: Map<String, List<Pr>> = emptyMap(),
    val exercises: List<ExerciseVariant> = emptyList(),
    val exerciseCategories: List<String> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val streakDays: Int = 0,
    val lastError: ApiError? = null,
    val selectedPrModal: PrWithImage? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DashboardUiState
        if (latestPr != other.latestPr) return false
        if (latestPrImage != null) {
            if (other.latestPrImage == null) return false
            if (!latestPrImage.contentEquals(other.latestPrImage)) return false
        } else if (other.latestPrImage != null) return false
        if (allPrs != other.allPrs) return false
        if (recentPrsByType != other.recentPrsByType) return false
        if (exercises != other.exercises) return false
        if (exerciseCategories != other.exerciseCategories) return false
        if (sessions != other.sessions) return false
        if (streakDays != other.streakDays) return false
        if (lastError != other.lastError) return false
        if (selectedPrModal != other.selectedPrModal) return false
        return true
    }

    override fun hashCode(): Int {
        var result = latestPr?.hashCode() ?: 0
        result = 31 * result + (latestPrImage?.contentHashCode() ?: 0)
        result = 31 * result + allPrs.hashCode()
        result = 31 * result + recentPrsByType.hashCode()
        result = 31 * result + exercises.hashCode()
        result = 31 * result + exerciseCategories.hashCode()
        result = 31 * result + sessions.hashCode()
        result = 31 * result + streakDays
        result = 31 * result + (lastError?.hashCode() ?: 0)
        result = 31 * result + (selectedPrModal?.hashCode() ?: 0)
        return result
    }
}

class DashboardViewModel(private val api: GymApi) : ViewModel() {

    private val imageLoader = PrImageLoader(api)
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        loadAll()
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    fun showPrModal(pr: Pr, imageBytes: ByteArray? = null) {
        val prWithImage = PrWithImage(pr = pr.toPersonalRecord(), imageBytes = imageBytes)
        _state.update { it.copy(selectedPrModal = prWithImage) }
        if (imageBytes == null) {
            viewModelScope.launch {
                imageLoader.loadPrImage(pr.id)
                    .onSuccess { bytes ->
                        _state.update { state ->
                            state.selectedPrModal?.let { current ->
                                if (current.pr.id == pr.id) {
                                    state.copy(selectedPrModal = current.copy(imageBytes = bytes))
                                } else state
                            } ?: state
                        }
                    }
            }
        }
    }

    fun dismissPrModal() {
        _state.update { it.copy(selectedPrModal = null) }
    }

    fun refresh() {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            val (data, apiError) = withContext(Dispatchers.IO) {
                runCatching {
                    coroutineScope {
                        val prsDeferred = async { api.prs() }
                        val exercisesDeferred = async { api.exercises() }
                        val sessionsDeferred = async { api.sessions(limit = 30) }

                        val prsResp = prsDeferred.await()
                        val exercisesResp = exercisesDeferred.await()
                        val sessionsResp = sessionsDeferred.await()

                        val prs = if (prsResp.isSuccessful) prsResp.body() ?: emptyList() else null
                        val exercises = if (exercisesResp.isSuccessful) exercisesResp.body() ?: emptyList() else null
                        val sessions = if (sessionsResp.isSuccessful) sessionsResp.body() ?: emptyList() else null

                        val error = when {
                            !prsResp.isSuccessful -> ErrorBodyParser.parse(prsResp.errorBody(), prsResp.code())
                            !exercisesResp.isSuccessful -> ErrorBodyParser.parse(exercisesResp.errorBody(), exercisesResp.code())
                            !sessionsResp.isSuccessful -> ErrorBodyParser.parse(sessionsResp.errorBody(), sessionsResp.code())
                            else -> null
                        }

                        Pair(Triple(prs, exercises, sessions), error)
                    }
                }.getOrElse {
                    Pair(Triple(null, null, null), ApiError(it.message ?: "Unknown error", "?", null))
                }
            }

            val (prList, exList, sessionList) = data
            if (prList != null) {
                val recentByType = prList
                    .groupBy { it.prType }
                    .mapValues { (_, list) -> list.sortedByDescending { it.createdAt }.take(3) }
                val categories = (exList ?: emptyList()).map { it.categoryName }.distinct().sorted()
                val streak = computeStreak(sessionList ?: emptyList())

                _state.update {
                    it.copy(
                        latestPr = prList.firstOrNull(),
                        latestPrImage = null,
                        allPrs = prList,
                        recentPrsByType = recentByType,
                        exercises = exList ?: emptyList(),
                        exerciseCategories = categories,
                        sessions = sessionList ?: emptyList(),
                        streakDays = streak,
                        lastError = null
                    )
                }
                prList.firstOrNull()?.let { loadPrImage(it.id) }
            } else if (apiError != null) {
                _state.update { it.copy(lastError = apiError) }
            }
        }
    }

    private fun computeStreak(sessions: List<Session>): Int {
        if (sessions.isEmpty()) return 0
        val sortedDates = sessions.map { it.date }.distinct().sortedDescending()
        val cal = java.util.Calendar.getInstance()
        val today = "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
        var streak = 0
        var checkDate = today
        for (d in sortedDates) {
            when {
                d > checkDate -> return streak
                d == checkDate -> {
                    streak++
                    val parts = d.split("-").map { it.toInt() }
                    cal.set(parts[0], parts[1] - 1, parts[2])
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                    checkDate = "%04d-%02d-%02d".format(
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH) + 1,
                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                }
                else -> return streak
            }
        }
        return streak
    }

    private fun loadPrImage(prId: String) {
        viewModelScope.launch {
            imageLoader.loadPrImage(prId)
                .onSuccess { bytes ->
                    _state.update { it.copy(latestPrImage = bytes) }
                }
        }
    }
}
