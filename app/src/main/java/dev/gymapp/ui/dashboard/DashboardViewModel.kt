package dev.gymapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gymapp.api.ErrorBodyParser
import dev.gymapp.api.GymApi
import dev.gymapp.api.models.ApiError
import dev.gymapp.api.models.Pr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val latestPr: Pr? = null,
    val lastError: ApiError? = null
)

class DashboardViewModel(private val api: GymApi) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        loadPrs()
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    private fun loadPrs() {
        viewModelScope.launch {
            val (prs, apiError) = withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.prs()
                    if (response.isSuccessful) {
                        response.body() to null
                    } else {
                        null to ErrorBodyParser.parse(response.errorBody(), response.code())
                    }
                }.getOrElse {
                    null to ApiError(
                        error = it.message ?: "Unknown error",
                        code = "?",
                        errorToken = null
                    )
                }
            }
            if (prs != null) {
                prs.firstOrNull()?.let { pr ->
                    _state.update { it.copy(latestPr = pr, lastError = null) }
                }
            } else if (apiError != null) {
                _state.update { it.copy(lastError = apiError) }
            }
        }
    }
}
