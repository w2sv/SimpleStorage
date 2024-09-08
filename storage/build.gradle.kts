plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.anggrayudi.storage"
    resourcePrefix = "ss_"

    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        buildConfig = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.w2sv"
            artifactId = "simplestorage"
            version = version.toString()
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    api(libs.documentfile)
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.fragment)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.viewmodel.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)

    // TODO: Replace with MockK after feature "mock java.io.File" has been fixed: https://github.com/mockk/mockk/issues/603
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.all)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.powermock.api.mockito2)
    testImplementation(libs.powermock.module.junit4)
}