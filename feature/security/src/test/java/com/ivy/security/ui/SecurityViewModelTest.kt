package com.ivy.security.ui

import android.os.SystemClock
import com.ivy.security.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {
    private lateinit var session: AuthSessionController
    private lateinit var store: SecurityStore
    private lateinit var vm: SecurityViewModel
    private val capture = mockk<ScreenCapturePreferences>(relaxed = true)
    @Before fun prepare() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 100L
        session = mockk(relaxed = true)
        store = mockk(relaxed = true)
        every { session.generation } returns 7L
        every { session.settings } returns MutableStateFlow(SecuritySettings(UnlockMode.PIN, true, true))
        every { session.locked } returns MutableStateFlow(true)
        every { session.beginAuthentication() } returns 11L
        every { session.requestGeneration } returns 11L
        every { session.finishAuthentication(any(), any(), any()) } returns true
        coEvery { store.verify(any(), any()) } returns CredentialResult.Success
        coEvery { store.configure(any(), any()) } returns "NEW-RECOVERY-CODE"
        coEvery { store.generateRecoveryCode() } returns "REPLACEMENT-CODE"
        vm = SecurityViewModel(session, store, capture)
    }
    @After fun finish() { unmockkStatic(SystemClock::class); Dispatchers.resetMain() }

    @Test fun `capture preference cannot change while locked`() {
        clearMocks(capture)
        vm.setScreenCapture(true)
        verify(exactly = 0) { capture.setAllowed(any()) }
    }
    @Test fun `unlocked user can explicitly change capture preference`() {
        clearMocks(capture)
        every { session.locked } returns MutableStateFlow(false)
        vm.setScreenCapture(true)
        verify(exactly = 1) { capture.setAllowed(true) }
    }

    @Test fun `testing biometrics cannot replace the current PIN proof`() = runTest {
        assertTrue(vm.beginBiometric(settings = true, biometricOnly = true))
        assertTrue(vm.biometricResult(11L, true))
        assertFalse(vm.verifiedForSettings)
        vm.configure(UnlockMode.OFF, "", "")
        advanceUntilIdle()
        coVerify(exactly = 0) { store.configure(any(), any()) }
    }
    @Test fun `current PIN proof permits an explicit security change`() = runTest {
        vm.verify("123456", true)
        advanceUntilIdle()
        assertTrue(vm.verifiedForSettings)
        vm.configure(UnlockMode.OFF, "", "")
        advanceUntilIdle()
        coVerify(exactly = 1) { store.configure(UnlockMode.OFF, null) }
    }
    @Test fun `old callback cannot authorize the current request`() = runTest {
        vm.beginBiometric(true, true)
        assertFalse(vm.biometricResult(10L, true))
        verify(exactly = 0) { session.finishAuthentication(10L, any(), any()) }
        assertEquals(11L, vm.promptGeneration)
    }
    @Test fun `recovery proves access without unlocking until new PIN is saved`() = runTest {
        vm.useRecovery(true)
        vm.verify("RECOVERY", false)
        advanceUntilIdle()
        assertTrue(vm.recoveryVerified)
        verify { session.finishAuthentication(11L, true, unlock = false) }
        coVerify(exactly = 0) { store.configure(any(), any()) }
        vm.configure(UnlockMode.PIN, "123456", "123456")
        advanceUntilIdle()
        coVerify { store.configure(UnlockMode.PIN, "123456") }
        assertEquals("NEW-RECOVERY-CODE", vm.recoveryCode)
    }
    @Test fun `expired recovery proof returns to code entry`() = runTest {
        vm.useRecovery(true); vm.verify("RECOVERY", false); advanceUntilIdle()
        every { session.generation } returns 8L
        vm.configure(UnlockMode.PIN, "123456", "123456")
        assertFalse(vm.recoveryVerified)
        assertTrue(vm.recovering)
        coVerify(exactly = 0) { store.configure(any(), any()) }
    }
    @Test fun `legacy fingerprint user can create a code without changing their unlock method`() = runTest {
        every { session.settings } returns MutableStateFlow(SecuritySettings(UnlockMode.LEGACY_SYSTEM, false, false))
        every { session.locked } returns MutableStateFlow(false)
        vm.beginBiometric(settings = true, biometricOnly = false)
        assertTrue(vm.biometricResult(11L, true))
        vm.generateRecoveryCode()
        advanceUntilIdle()
        coVerify(exactly = 1) { store.generateRecoveryCode() }
        coVerify(exactly = 0) { store.configure(any(), any()) }
        assertEquals("REPLACEMENT-CODE", vm.recoveryCode)
        assertFalse(vm.verifiedForSettings)
        assertEquals(UnlockMode.LEGACY_SYSTEM, session.settings.value.mode)
    }
    @Test fun `unlocked alone is not enough to generate a code`() = runTest {
        every { session.locked } returns MutableStateFlow(false)
        vm.generateRecoveryCode(); advanceUntilIdle()
        coVerify(exactly = 0) { store.generateRecoveryCode() }
    }
    @Test fun `expired identity proof cannot replace a backup code`() = runTest {
        every { session.locked } returns MutableStateFlow(false)
        vm.verify("123456", true); advanceUntilIdle()
        every { SystemClock.elapsedRealtime() } returns 60_101L
        vm.generateRecoveryCode(); advanceUntilIdle()
        coVerify(exactly = 0) { store.generateRecoveryCode() }
        assertFalse(vm.verifiedForSettings)
    }
    @Test fun `a device lock between proof and generation prevents rotation`() = runTest {
        every { session.locked } returns MutableStateFlow(false)
        vm.verify("123456", true); advanceUntilIdle()
        every { session.locked } returns MutableStateFlow(true)
        vm.generateRecoveryCode(); advanceUntilIdle()
        coVerify(exactly = 0) { store.generateRecoveryCode() }
    }
    @Test fun `keyguard prevents generation even before the lock flow catches up`() = runTest {
        every { session.locked } returns MutableStateFlow(false)
        vm.verify("123456", true); advanceUntilIdle()
        every { session.deviceLocked } returns true
        vm.generateRecoveryCode(); advanceUntilIdle()
        coVerify(exactly = 0) { store.generateRecoveryCode() }
        assertFalse(vm.verifiedForSettings)
    }
    @Test fun `a changed session epoch invalidates backup code authorization`() = runTest {
        every { session.locked } returns MutableStateFlow(false)
        vm.verify("123456", true); advanceUntilIdle()
        every { session.generation } returns 8L
        vm.generateRecoveryCode(); advanceUntilIdle()
        coVerify(exactly = 0) { store.generateRecoveryCode() }
    }
    @Test fun `a recovery proof can only reset PIN not rotate recovery credentials`() = runTest {
        every { session.locked } returns MutableStateFlow(false)
        vm.useRecovery(true); vm.verify("RECOVERY", true); advanceUntilIdle()
        vm.generateRecoveryCode(); advanceUntilIdle()
        coVerify(exactly = 0) { store.generateRecoveryCode() }
    }
    @Test fun `visible code must be acknowledged before another is generated`() = runTest {
        every { session.locked } returns MutableStateFlow(false)
        vm.verify("123456", true); advanceUntilIdle()
        vm.generateRecoveryCode(); advanceUntilIdle()
        vm.generateRecoveryCode(); advanceUntilIdle()
        coVerify(exactly = 1) { store.generateRecoveryCode() }
    }
}
