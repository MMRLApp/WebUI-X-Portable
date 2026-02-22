plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "party.iroiro.luajava"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    publishing {
        singleVariant("release")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("jniLibs")
        }
    }
}

configurations {
    create("natives")
}

dependencies {
    val lua = "lua51"
    val luaJavaVersion = "4.1.0"

    api("party.iroiro.luajava:luajava:$luaJavaVersion")
    api("party.iroiro.luajava:$lua:$luaJavaVersion")

    add("natives", "party.iroiro.luajava:${lua}-platform:$luaJavaVersion:natives-armeabi-v7a")
    add("natives", "party.iroiro.luajava:${lua}-platform:$luaJavaVersion:natives-arm64-v8a")
    add("natives", "party.iroiro.luajava:${lua}-platform:$luaJavaVersion:natives-x86")
    add("natives", "party.iroiro.luajava:${lua}-platform:$luaJavaVersion:natives-x86_64")
}

tasks.register("copyAndroidNatives") {

    doFirst {

        val libsDir = file("libs")
        val abiDirs = listOf(
            "armeabi-v7a",
            "arm64-v8a",
            "x86",
            "x86_64"
        )

        abiDirs.forEach {
            file("libs/$it").mkdirs()
        }

        val nativesConfig = configurations.getByName("natives")

        nativesConfig.copy().files.forEach { jar ->

            val outputDir = when {
                jar.name.endsWith("natives-arm64-v8a.jar") -> file("libs/arm64-v8a")
                jar.name.endsWith("natives-armeabi-v7a.jar") -> file("libs/armeabi-v7a")
                jar.name.endsWith("natives-x86_64.jar") -> file("libs/x86_64")
                jar.name.endsWith("natives-x86.jar") -> file("libs/x86")
                else -> null
            }

            outputDir?.let {
                copy {
                    from(zipTree(jar))
                    into(it)
                    include("*.so")
                }
            }
        }
    }
}