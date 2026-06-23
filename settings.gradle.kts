enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val user: String? = providers.gradleProperty("gpr.user").orNull
    ?: System.getenv("ACTOR")
val pass: String? = providers.gradleProperty("gpr.key").orNull
    ?: System.getenv("GH_TOKEN")

dependencyResolutionManagement drm@{
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")

        if (user != null && pass != null) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/MMRLApp/X")
                credentials {
                    username = user
                    password = pass
                }
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "WebUIX"
include(
    ":app",
    ":webui",
    ":helper",
    ":datastore",
    ":jna",
    ":modconf",
    ":lua",
    ":hwui"
)
