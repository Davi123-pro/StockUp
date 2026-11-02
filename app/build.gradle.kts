plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.stockup"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.stockup"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
      // Necessário para os testes JVM de BackupManager: android.net.Uri é stub no
      // android.jar e, sem isso, qualquer chamada lança "not mocked". Com o retorno
      // padrão, Uri.parse devolve null e o código já trata esse caso.
      unitTests.isReturnDefaultValues = true
    }
}

base {
    archivesName.set("StockUp")
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
  implementation(libs.androidx.compose.material.icons.extended)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // WorkManager
  implementation(libs.androidx.work.runtime.ktx)

  // Coil (image loading)
  // Coil (image loading)
  implementation(libs.coil.compose)

  // Google Play Services Code Scanner (Barcode / QR Code)
  implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

  // Pastas escolhidas pelo usuário (cópia de segurança: listar/gravar na pasta das cópias)
  implementation(libs.androidx.documentfile)

  // JSON do arquivo de cópia (Gson em vez de org.json: testável na JVM)
  implementation(libs.gson)

  constraints {
    // O Gson 2.11.0 declara error_prone_annotations 2.27.0, cujo JAR não está no cache
    // offline desta máquina (só o POM). A 2.28.0 está e é compatível — são apenas
    // anotações de compilação, sem efeito em tempo de execução.
    // Numa máquina com internet esta constraint pode sair sem problema.
    implementation("com.google.errorprone:error_prone_annotations:2.28.0")
  }
}
