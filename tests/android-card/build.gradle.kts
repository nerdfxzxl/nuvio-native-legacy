plugins {
    id("com.android.library") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
}

// Exercise the shipping renderer and font with Android graphics, independently
// of the player's NDK, FFmpeg, Rust and private login configuration.
val appMain = file("../../android/app/src/main")
val prepareRenderer by tasks.registering(Sync::class) {
    from(appMain) {
        include("java/**/StreamCardText.kt", "res/font/selector_inter.ttf", "res/values/strings.xml")
    }
    into(layout.buildDirectory.dir("renderer"))
}
android {
    namespace = "space.nuvio.nativelegacy"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    sourceSets["main"].java.srcDir(layout.buildDirectory.dir("renderer/java"))
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("renderer/res"))
    testOptions.unitTests.isIncludeAndroidResources = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions.jvmTarget = "17"
}
tasks.named("preBuild") { dependsOn(prepareRenderer) }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
