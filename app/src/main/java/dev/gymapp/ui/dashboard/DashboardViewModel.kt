package dev.gymapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gymapp.api.ErrorBodyParser
import dev.gymapp.api.GymApi
import dev.gymapp.api.PrImageLoader
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
    val latestPrImage: ByteArray? = null,
    val lastError: ApiError? = null
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
        if (lastError != other.lastError) return false
        return true
    }

    override fun hashCode(): Int {
        var result = latestPr?.hashCode() ?: 0
        result = 31 * result + (latestPrImage?.contentHashCode() ?: 0)
        result = 31 * result + (lastError?.hashCode() ?: 0)
        return result
    }
}

class DashboardViewModel(private val api: GymApi) : ViewModel() {

    private val imageLoader = PrImageLoader(api)
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
                    _state.update { it.copy(latestPr = pr, latestPrImage = null, lastError = null) }
                    loadPrImage(pr.id)
                }
            } else if (apiError != null) {
                _state.update { it.copy(lastError = apiError) }
            }
        }
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
