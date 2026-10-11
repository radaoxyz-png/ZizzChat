
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        //========== START: ZEGOCLOUD REPOSITORIES ==========
        maven { url = uri("https://maven.zego.im") }
        maven { url = uri("https://www.jitpack.io") }
        //========== END: ZEGOCLOUD REPOSITORIES ==========
    }
}

rootProject.name = "ZizzChat"
include(":app")
