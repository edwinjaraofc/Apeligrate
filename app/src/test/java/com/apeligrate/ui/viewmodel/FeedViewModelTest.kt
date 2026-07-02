package com.apeligrate.ui.viewmodel

import com.apeligrate.MainDispatcherRule
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.IncidentReportRepository
import com.apeligrate.domain.repository.UserProgressRepository
import com.apeligrate.domain.repository.VoteReportResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `validateReport requires logged user`() = runTest {
        val repository = FakeIncidentReportRepository()
        val viewModel = FeedViewModel(
            repository = repository,
            userProgressRepository = FakeUserProgressRepository(),
            currentUserId = null
        )

        viewModel.validateReport("r1", isReal = true)

        assertEquals("Debes iniciar sesion para votar.", viewModel.uiState.value.error)
    }

    @Test
    fun `validateReport updates state and records validation on success`() = runTest {
        val repository = FakeIncidentReportRepository(
            initialReports = listOf(
                IncidentReport(
                    id = "r1",
                    category = "Acoso",
                    description = "Descripcion"
                )
            )
        )
        val userProgressRepository = FakeUserProgressRepository()
        val viewModel = FeedViewModel(repository, userProgressRepository, "15")
        advanceUntilIdle()

        viewModel.validateReport("r1", isReal = true)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.reports.single()
        assertEquals(1, updated.validationCount)
        assertTrue("15" in updated.validationVoterIds)
        assertEquals(listOf("15"), userProgressRepository.recordedValidationUserIds)
    }

    @Test
    fun `validateReport awards justice points when report is cancelled`() = runTest {
        val repository = FakeIncidentReportRepository(
            initialReports = listOf(
                IncidentReport(
                    id = "r2",
                    category = "Acoso",
                    description = "Descripcion"
                )
            ),
            voteResult = VoteReportResult(
                accepted = true,
                reportCancelled = true,
                rewardedUserIds = listOf("2", "3")
            )
        )
        val userProgressRepository = FakeUserProgressRepository()
        val viewModel = FeedViewModel(repository, userProgressRepository, "20")
        advanceUntilIdle()

        viewModel.validateReport("r2", isReal = false)
        advanceUntilIdle()

        assertEquals(listOf(listOf("2", "3")), userProgressRepository.awardedJusticePoints)
    }

    @Test
    fun `validateReport does not send vote when user already voted`() = runTest {
        val repository = FakeIncidentReportRepository(
            initialReports = listOf(
                IncidentReport(
                    id = "r3",
                    category = "Acoso",
                    description = "Descripcion",
                    validationVoterIds = listOf("15")
                )
            )
        )
        val viewModel = FeedViewModel(repository, FakeUserProgressRepository(), "15")
        advanceUntilIdle()

        viewModel.validateReport("r3", isReal = true)
        advanceUntilIdle()

        assertEquals(0, repository.voteCalls)
    }

    @Test
    fun `validateReport exposes repository error and removes submitting state`() = runTest {
        val repository = FakeIncidentReportRepository(
            initialReports = listOf(
                IncidentReport(
                    id = "r4",
                    category = "Acoso",
                    description = "Descripcion"
                )
            ),
            voteResult = VoteReportResult(
                accepted = false,
                errorMessage = "No se pudo votar"
            )
        )
        val viewModel = FeedViewModel(repository, FakeUserProgressRepository(), "25")
        advanceUntilIdle()

        viewModel.validateReport("r4", isReal = true)
        advanceUntilIdle()

        assertEquals("No se pudo votar", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.submittingReportIds.isEmpty())
    }

    private class FakeIncidentReportRepository(
        initialReports: List<IncidentReport> = emptyList(),
        private val voteResult: VoteReportResult = VoteReportResult(accepted = true)
    ) : IncidentReportRepository {
        private val reportsFlow = MutableStateFlow(initialReports)
        var voteCalls = 0

        override suspend fun submitReport(report: IncidentReport): Result<IncidentReport> {
            error("Not needed in this test")
        }

        override suspend fun voteReport(reportId: String, userId: String, isReal: Boolean): VoteReportResult {
            voteCalls++
            return voteResult
        }

        override suspend fun refreshReports() = Unit

        override fun getReports(): Flow<List<IncidentReport>> = reportsFlow

        override fun getReportById(id: String): Flow<IncidentReport?> = flowOf(reportsFlow.value.find { it.id == id })
    }

    private class FakeUserProgressRepository : UserProgressRepository {
        val recordedValidationUserIds = mutableListOf<String?>()
        val awardedJusticePoints = mutableListOf<List<String>>()

        override fun getUser(userId: String): Flow<User?> = flowOf(null)

        override suspend fun ensureUser(userId: String) = Unit

        override suspend fun recordSubmittedReport(userId: String?) = Unit

        override suspend fun recordValidation(userId: String?) {
            recordedValidationUserIds += userId
        }

        override suspend fun awardJusticePoints(userIds: List<String>) {
            awardedJusticePoints += userIds
        }

        override suspend fun saveProfile(userId: String, name: String, city: String) = Unit
    }
}
