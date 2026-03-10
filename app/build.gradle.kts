plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    config.setFrom(files("${rootProject.projectDir}/config/detekt.yml"))
}

android {
    namespace = "dev.gymapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.gymapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"https://prtracks.com\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID_WEB", "\"259179195778-4ndapfrnm2fc1q0vp0qgvbsqm13ini6o.apps.googleusercontent.com\"")
        buildConfigField("String", "UPDATE_BASE_URL", "\"https://pub-0b1c5314d42240e5859bcab509f9626f.r2.dev\"")
    }

    testOptions {
        animationsDisabled = true
    }

    signingConfigs {
        getByName("debug") {
            // debug uses default debug keystore
        }
        create("release") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "BASE_URL", "\"https://prtracks.com\"")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register("incrementVersion") {
    doLast {
        val buildFile = file("${project.projectDir}/build.gradle.kts")
        val content = buildFile.readText()
        val versionCodeRegex = Regex("versionCode = (\\d+)")
        val versionNameRegex = Regex("versionName = \"([\\d.]+)\"")
        val versionCodeMatch = versionCodeRegex.find(content) ?: error("versionCode not found")
        val versionNameMatch = versionNameRegex.find(content) ?: error("versionName not found")
        val newVersionCode = versionCodeMatch.groupValues[1].toInt() + 1
        val versionName = versionNameMatch.groupValues[1]
        val parts = versionName.split(".")
        val newVersionName = parts.dropLast(1).joinToString(".") + "." + (parts.last().toInt() + 1)
        val newContent = content
            .replace(versionCodeRegex) { "versionCode = $newVersionCode" }
            .replace(versionNameRegex) { "versionName = \"$newVersionName\"" }
        buildFile.writeText(newContent)
        println("Incremented version: $versionName -> $newVersionName, versionCode -> $newVersionCode")
    }
}

tasks.register("writeVersionJson") {
    doLast {
        val versionCode = android.defaultConfig.versionCode
        val versionName = android.defaultConfig.versionName
        val json = """{"versionCode":$versionCode,"versionName":"$versionName"}"""
        file("${rootProject.layout.projectDirectory}/version.json").writeText(json)
        println("Wrote version.json: versionCode=$versionCode, versionName=$versionName")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.12.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.activity:activity-compose:1.8.2")
    androidTestImplementation("androidx.test:runner:1.5.2")
}
