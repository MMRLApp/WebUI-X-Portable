plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sun.jna"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release { 
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("jniLibs")
        }
    }
}

dependencies {
    api(files("libs/jna.jar"))
}