import app.cash.licensee.ViolationAction
import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.self.application)
    alias(libs.plugins.self.compose)
    alias(libs.plugins.self.hilt)
    alias(libs.plugins.licensee)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val baseAppName = "WebUI X"
val mmrlBaseApplicationId = "com.dergoogler.mmrl"
val basePackageName = "$mmrlBaseApplicationId.wx"

android {
    namespace = basePackageName
    compileSdk = 36

    defaultConfig {
        applicationId = namespace
        versionName = "v$commitCount"
        versionCode = commitCount
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    val releaseSigning = if (project.hasReleaseKeyStore) {
        signingConfigs.create("release") {
            storeFile = project.releaseKeyStore
            storePassword = project.releaseKeyStorePassword
            keyAlias = project.releaseKeyAlias
            keyPassword = project.releaseKeyPassword
            enableV2Signing = true
            enableV3Signing = true
        }
    } else {
        signingConfigs.getByName("debug")
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("official") {
            dimension = "distribution"
            applicationId = basePackageName
            resValue("string", "app_name", baseAppName)
            buildConfigField("Boolean", "IS_SPOOFED_BUILD", "false")
        }

        create("spoofed") {
            dimension = "distribution"
            applicationId = generateRandomPackageName()
            resValue("string", "app_name", generateRandomName())
            buildConfigField("Boolean", "IS_SPOOFED_BUILD", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", baseAppName)
            buildConfigField("Boolean", "IS_DEV_VERSION", "false")
            buildConfigField("Boolean", "IS_GOOGLE_PLAY_BUILD", "false")
            isDebuggable = false
            isJniDebuggable = false
            versionNameSuffix = "-release"
            renderscriptOptimLevel = 3
            multiDexEnabled = true

            manifestPlaceholders["webuiPermissionId"] = mmrlBaseApplicationId
        }

        create("playstore") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("debug", "release")
            versionNameSuffix = "-playstore"
        }

        debug {
            resValue("string", "app_name", "$baseAppName Debug")
            buildConfigField("Boolean", "IS_DEV_VERSION", "true")
            buildConfigField("Boolean", "IS_GOOGLE_PLAY_BUILD", "false")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isJniDebuggable = true
            isDebuggable = true
            renderscriptOptimLevel = 0
            isMinifyEnabled = false
            multiDexEnabled = true

            manifestPlaceholders["webuiPermissionId"] = "$mmrlBaseApplicationId.debug"
        }

        all {
            signingConfig = releaseSigning
            buildConfigField("String", "COMPILE_SDK", "\"$COMPILE_SDK\"")
            buildConfigField("String", "BUILD_TOOLS_VERSION", "\"${BUILD_TOOLS_VERSION}\"")
            buildConfigField("String", "MIN_SDK", "\"$MIN_SDK\"")
            buildConfigField("String", "LATEST_COMMIT_ID", "\"${commitId}\"")

            manifestPlaceholders["__packageName__"] = basePackageName
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging.resources.excludes += setOf(
        "META-INF/**",
        "okhttp3/**",
        //"kotlin/**",
        "org/**",
        "**.properties",
        "**.bin",
        "**/*.proto"
    )

    dependenciesInfo.includeInApk = false

    applicationVariants.configureEach {
        outputs.configureEach {
            (this as? ApkVariantOutputImpl)?.outputFileName =
                "WebUI-X-$versionName.apk"
        }
    }
}

licensee {
    bundleAndroidAsset.set(true)
    violationAction(ViolationAction.IGNORE)
}

dependencies {
    implementation(projects.webui)
    implementation(projects.modconf)
    implementation(projects.jna)
    implementation(libs.mmrl.ext)
    implementation(libs.mmrl.ui)
    implementation(libs.mmrl.platform)
    implementation(libs.mmrl.datastore)
    implementation(projects.datastore)
    implementation(libs.mmrl.compat)
    compileOnly(libs.mmrl.hiddenApi)

    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.libsu.io)

    implementation(libs.androidx.lifecycle.process)
    implementation(libs.hiddenApiBypass)

    implementation(libs.semver)
    implementation(libs.coil.compose)

    implementation(libs.rikka.refine.runtime)
    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.apache.commons.compress)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewModel.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.reflect)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.multiplatform.markdown.renderer.m3)
    implementation(libs.multiplatform.markdown.renderer.android)
    implementation(libs.multiplatform.markdown.renderer.coil3)
    implementation(libs.androidx.multidex)
    implementation(libs.dev.rikka.rikkax.parcelablelist)
    implementation(libs.lib.zoomable)
    implementation(libs.process.phoenix)
    // implementation(libs.androidx.adaptive)
    // implementation(libs.androidx.adaptive.android)
    // implementation(libs.androidx.adaptive.layout)
    // implementation(libs.androidx.adaptive.navigation)
    implementation(libs.kotlinx.html.jvm)

    implementation(libs.square.retrofit)
    implementation(libs.square.retrofit.moshi)
    implementation(libs.square.retrofit.kotlinxSerialization)
    implementation(libs.square.okhttp)
    implementation(libs.square.okhttp.dnsoverhttps)
    implementation(libs.square.logging.interceptor)
    implementation(libs.square.moshi)
    ksp(libs.square.moshi.kotlin)

    implementation(libs.composedestinations.core)
    ksp(libs.composedestinations.ksp)
}
