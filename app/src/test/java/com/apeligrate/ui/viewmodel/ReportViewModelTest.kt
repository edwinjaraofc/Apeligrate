package com.apeligrate.ui.viewmodel

import com.apeligrate.MainDispatcherRule
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.IncidentReportRepository
import com.apeligrate.domain.repository.UserProgressRepository
import com.apeligrate.domain.repository.VoteReportResult
import com.apeligrate.domain.use_case.SubmitIncidentReportUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submitReport rejects missing category`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onDescriptionChange("Descripcion valida")
        viewModel.submitReport(userId = "123")

        assertEquals("Selecciona una categoría", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `submitReport rejects blank description`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onCategoryChange("Acoso")
        viewModel.submitReport(userId = "123")

        assertEquals("Ingresa una descripción", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `submitReport sends anonymous report without user id and records progress`() = runTest {
        val repository = FakeIncidentReportRepository()
        val userProgressRepository = FakeUserProgressRepository()
        val viewModel = buildViewModel(repository, userProgressRepository)

        viewModel.onCategoryChange("Robo a mano armada")
        viewModel.onDescriptionChange("Descripcion del incidente")
        viewModel.onAnonymousChange(true)
        viewModel.onLocationPicked(-12.05, -77.04)

        viewModel.submitReport(userId = "999", latitude = 1.0, longitude = 2.0, address = "Direccion")
        advanceUntilIdle()

        val submitted = repository.submittedReports.single()
        assertNull(submitted.userId)
        assertEquals(-12.05, submitted.latitude)
        assertEquals(-77.04, submitted.longitude)
        assertEquals("critical", submitted.status)
        assertTrue(viewModel.uiState.value.isSuccess)
        assertEquals("999", userProgressRepository.recordedSubmittedReportUserIds.single())
    }

    @Test
    fun `submitReport keeps user id when report is not anonymous and uses fallback coordinates`() = runTest {
        val repository = FakeIncidentReportRepository()
        val userProgressRepository = FakeUserProgressRepository()
        val viewModel = buildViewModel(repository, userProgressRepository)

        viewModel.onCategoryChange("Acoso")
        viewModel.onDescriptionChange("Descripcion del incidente")

        viewModel.submitReport(userId = "123", latitude = -11.9, longitude = -77.1, address = "Direccion")
        advanceUntilIdle()

        val submitted = repository.submittedReports.single()
        assertEquals("123", submitted.userId)
        assertEquals(-11.9, submitted.latitude)
        assertEquals(-77.1, submitted.longitude)
        assertEquals("warning", submitted.status)
        assertNotNull(viewModel.uiState.value.lastReportId)
    }

    @Test
    fun `resetState clears form state after changes`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onCategoryChange("Acoso")
        viewModel.onDescriptionChange("Descripcion")
        viewModel.onAnonymousChange(true)
        viewModel.onLocationPicked(-12.0, -77.0)

        viewModel.resetState()

        val state = viewModel.uiState.value
        assertEquals("", state.category)
        assertEquals("", state.description)
        assertFalse(state.isAnonymous)
        assertTrue(state.selectedImages.isEmpty())
        assertNull(state.pickedLocation)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `submitReport exposes repository failure and does not record progress`() = runTest {
        val repository = FakeIncidentReportRepository(
            submitResult = Result.failure(IllegalStateException("Fallo al persistir"))
        )
        val userProgressRepository = FakeUserProgressRepository()
        val viewModel = buildViewModel(repository, userProgressRepository)

        viewModel.onCategoryChange("Acoso")
        viewModel.onDescriptionChange("Descripcion del incidente")
        viewModel.submitReport(userId = "123")
        advanceUntilIdle()

        assertEquals("Fallo al persistir", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSuccess)
        assertTrue(userProgressRepository.recordedSubmittedReportUserIds.isEmpty())
    }

    private fun buildViewModel(
        repository: FakeIncidentReportRepository = FakeIncidentReportRepository(),
        userProgressRepository: FakeUserProgressRepository = FakeUserProgressRepository()
    ): ReportViewModel {
        return ReportViewModel(
            submitIncidentReportUseCase = SubmitIncidentReportUseCase(repository),
            userProgressRepository = userProgressRepository
        )
    }

    private class FakeIncidentReportRepository(
        private val submitResult: Result<IncidentReport>? = null
    ) : IncidentReportRepository {
        val submittedReports = mutableListOf<IncidentReport>()
        private val reportsFlow = MutableStateFlow<List<IncidentReport>>(emptyList())

        override suspend fun submitReport(report: IncidentReport): Result<IncidentReport> {
            submitResult?.let { return it }
            val stored = report.copy(id = "generated-id")
            submittedReports += stored
            reportsFlow.value = listOf(stored) + reportsFlow.value
            return Result.success(stored)
        }

        override suspend fun voteReport(reportId: String, userId: String, isReal: Boolean): VoteReportResult {
            error("Not needed in this test")
        }

        override suspend fun refreshReports() = Unit

        override fun getReports(): Flow<List<IncidentReport>> = reportsFlow

        override fun getReportById(id: String): Flow<IncidentReport?> = flowOf(null)
    }

    private class FakeUserProgressRepository : UserProgressRepository {
        val recordedSubmittedReportUserIds = mutableListOf<String?>()

        override fun getUser(userId: String): Flow<User?> = flowOf(null)

        override suspend fun ensureUser(userId: String) = Unit

        override suspend fun recordSubmittedReport(userId: String?) {
            recordedSubmittedReportUserIds += userId
        }

        override suspend fun recordValidation(userId: String?) = Unit

        override suspend fun awardJusticePoints(userIds: List<String>) = Unit

        override suspend fun saveProfile(userId: String, name: String, city: String) = Unit
    }
}
