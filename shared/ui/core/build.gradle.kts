import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test

plugins {
    id("ivy.feature")
}

android {
    namespace = "com.ivy.ui"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)
}

// Source-contract tests inspect other modules; their files must invalidate cached
// results even when this module's own production/test classes have not changed.
tasks.withType<Test>().configureEach {
    // Expand to source files, not a parent directory that also contains generated
    // task outputs: Gradle correctly rejects implicit dependencies on those.
    inputs.files(providers.provider {
        rootProject.fileTree("feature") {
            include("**/src/main/**/*.kt")
            exclude("**/build/**")
        }.files + listOf("app/src/main", "temp/legacy-code/src/main", "shared/ui/navigation/src/main").flatMap { path ->
            rootProject.fileTree(path) { include("**/*.kt"); exclude("**/build/**") }.files
        }
    }).withPropertyName("navigationContractSources").withPathSensitivity(PathSensitivity.RELATIVE)
}
