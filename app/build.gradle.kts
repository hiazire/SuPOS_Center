plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.supos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.supos.center"
        minSdk = 24
        targetSdk = 36
        versionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull() ?: 10000
        versionName = project.findProperty("versionName")?.toString() ?: "1.0.0"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
                ?: (project.findProperty("RELEASE_KEYSTORE_PATH") as? String)
            val keystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                ?: (project.findProperty("RELEASE_KEYSTORE_PASSWORD") as? String)
            val keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                ?: (project.findProperty("RELEASE_KEY_ALIAS") as? String)
            val keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                ?: (project.findProperty("RELEASE_KEY_PASSWORD") as? String)

            if (!keystorePath.isNullOrBlank() && File(keystorePath).exists()) {
                storeFile = File(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                // Zero-secret fallback for local dev and CI pull request checks
                val debugSigning = getByName("debug")
                storeFile = debugSigning.storeFile
                storePassword = debugSigning.storePassword
                this.keyAlias = debugSigning.keyAlias
                this.keyPassword = debugSigning.keyPassword
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            manifestPlaceholders["appName"] = "SuPOS (Staging)"
            buildConfigField("String", "ENVIRONMENT", "\"STAGING\"")
        }
        create("production") {
            dimension = "environment"
            manifestPlaceholders["appName"] = "SuPOS 中控臺"
            buildConfigField("String", "ENVIRONMENT", "\"PRODUCTION\"")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        ignoreTestSources = true
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    val gitShaProvider = providers.environmentVariable("GITHUB_SHA")
        .map { if (it.length >= 7) it.substring(0, 7) else it }
        .orElse(providers.gradleProperty("gitCommitSha"))
        .orElse(providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText.map { it.trim() })
        .orElse("unknown")

    applicationVariants.all {
        val variant = this
        val gitSha = gitShaProvider.get().ifEmpty { "unknown" }
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output?.outputFileName = "SuPOS-${variant.name}-v${variant.versionName}-${gitSha}.apk"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner, MockWebServer
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)

  // Instrumented tests: jUnit rules and runners, Room testing
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.room.testing)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room Database
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)

  // Retrofit
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.converter.gson)
}
