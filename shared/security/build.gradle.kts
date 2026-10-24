plugins { id("ivy.feature"); id("ivy.integration.testing") }
android { namespace = "com.ivy.security" }
dependencies { implementation(projects.shared.base) }
