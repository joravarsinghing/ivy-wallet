package com.ivy.importdata.csv

import com.ivy.navigation.Navigation
import com.ivy.onboarding.viewmodel.OnboardingViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CSVNavigationTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `finishing manual import exits once from onboarding or settings`() = runTest(dispatcher) {
        for (fromOnboarding in listOf(false, true)) {
            val navigation = mockk<Navigation>(relaxed = true)
            val onboarding = mockk<OnboardingViewModel>(relaxed = true)
            val vm = CSVViewModel(mockk(), mockk(), navigation)
            runCurrent()
            vm.onEvent(CSVEvent.FinishImport(fromOnboarding, onboarding))
            advanceUntilIdle()
            verify(exactly = 1) { navigation.back() }
            verify(exactly = if (fromOnboarding) 1 else 0) { onboarding.importFinished(false) }
        }
    }
}
