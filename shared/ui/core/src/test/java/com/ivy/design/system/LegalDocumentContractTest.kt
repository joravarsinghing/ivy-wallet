package com.ivy.design.system

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class LegalDocumentContractTest {
    private val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }
    private fun text(path: String) = File(root, path).readText()
    @Test fun `offline legal documents retain the actual license and maintainer identity`() {
        val prefix = "shared/ui/core/src/main/res/raw/"
        assertTrue(text(prefix + "jvy_license.txt").contains(text("LICENSE").trim()))
        assertTrue(text(prefix + "jvy_terms.txt").contains("Joravar Singh"))
        assertTrue(text(prefix + "jvy_privacy.txt").contains("Joravar Singh"))
        val settings = text("feature/settings/src/main/java/com/ivy/settings/SettingsScreen.kt")
        assertTrue(settings.contains("JvyLegalLinks("))
        assertFalse(settings.contains("SupportOnLinkedIn("))
        assertFalse(settings.contains("Contributors("))
        assertFalse(settings.contains("Attributions("))
        assertTrue(text("feature/onboarding/src/main/java/com/ivy/onboarding/steps/OnboardingSplashLogin.kt").contains("JvyLegalLinks("))
    }
}
