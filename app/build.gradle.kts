plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.kotlin.serialization)
}

base.archivesName.set("app")

android {
  namespace = "com.bp.wallet"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.bpwallet.app"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk {
      abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
    }
  }

  signingConfigs {
    getByName("debug") {
      // Keep default debug config
    }
    create("release") {
      val keystoreFile = rootProject.file("bpwallet.jks")
      val keystorePass = System.getenv("RELEASE_KEYSTORE_PASSWORD")
      val keyAliasVal = System.getenv("RELEASE_KEY_ALIAS")
      val keyPass = System.getenv("RELEASE_KEY_PASSWORD")

      if (keystoreFile.exists() && !keystorePass.isNullOrBlank()) {
        storeFile = keystoreFile
        storePassword = keystorePass
        keyAlias = if (!keyAliasVal.isNullOrBlank()) keyAliasVal else "bpwallet"
        keyPassword = if (!keyPass.isNullOrBlank()) keyPass else keystorePass
        println(">>> Using Custom Release Keystore: ${keystoreFile.absolutePath}")
      } else {
        println(">>> Release keystore or environment variables not found; falling back to debug signing for evaluation.")
        val debugConfig = signingConfigs.getByName("debug")
        storeFile = debugConfig.storeFile
        storePassword = debugConfig.storePassword
        keyAlias = debugConfig.keyAlias
        keyPassword = debugConfig.keyPassword
      }
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")

      lint {
        checkReleaseBuilds = false
        abortOnError = false
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.ui.text)
  implementation(libs.androidx.compose.ui.unit)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.lottie.compose)
  implementation(libs.okhttp)
  implementation(libs.jbcrypt)
  implementation(platform(libs.supabase.bom))
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.auth)
  implementation(libs.supabase.realtime)
  implementation(libs.supabase.storage)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.kotlinx.serialization.json)
  
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
