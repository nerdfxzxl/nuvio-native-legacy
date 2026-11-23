plugins {
    id("com.android.library") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
}

android {
    namespace = "space.nuvio.nativelegacy.p4test"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets["main"].java.srcDir("src/production/kotlin")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.media3:media3-datasource:1.8.0")
    implementation("androidx.media3:media3-database:1.8.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}


tasks.withType<Test>().configureEach {
    // Follow repository's existing tests/android-card mirror fallback for 429s.
    systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
    testLogging {
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
