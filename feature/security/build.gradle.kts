plugins { id("ivy.feature") }
android { namespace = "com.ivy.security.ui" }
dependencies {
    implementation(projects.shared.security)
    implementation(projects.shared.ui.navigation)
    implementation(projects.shared.ui.core)
    implementation(libs.androidx.biometrics)
    implementation(libs.appcompat.activity)
}
