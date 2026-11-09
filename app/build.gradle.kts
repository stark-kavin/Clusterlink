import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// local.properties is never committed: MAPS_API_KEY, and CLUSTERLINK_SIGNING = path to the release
// signing.properties (storeFile, storePassword, keyAlias, keyPassword), kept outside the repo.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY", "")
val signingProps: Properties? = localProps.getProperty("CLUSTERLINK_SIGNING")?.let { path ->
    val f = file(path)
    if (f.exists()) Properties().apply { f.inputStream().use { load(it) } } else null
}

android {
    namespace = "com.clusterlink.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.clusterlink.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.1.1"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
    signingConfigs {
        if (signingProps != null) create("clusterlink") {
            storeFile = file(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        // With the release key available, every build is signed with it, so any build can update an
        // installed app (including the in-app updater installing GitHub releases). Without it: debug key.
        val key = signingConfigs.findByName("clusterlink")
        debug {
            if (key != null) signingConfig = key
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (key != null) signingConfig = key
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    // Maps pulls in an old Fragment; activity-result APIs need 1.3.0+ (release lint check).
    implementation("androidx.fragment:fragment:1.8.5")
    debugImplementation(libs.androidx.ui.tooling)
}
