pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "turtle-safety-radar"

include(":app")
include(":core")
include(":notification-monitor")
include(":safety-ime")
include(":media-checker")
include(":local-ai")
include(":parent-console")
include(":text-watcher")

project(":app").projectDir = file("apps/app")
project(":core").projectDir = file("platform/core")
project(":notification-monitor").projectDir = file("features/notification-monitor")
project(":safety-ime").projectDir = file("features/safety-ime")
project(":media-checker").projectDir = file("features/media-checker")
project(":local-ai").projectDir = file("features/local-ai")
project(":parent-console").projectDir = file("features/parent-console")
project(":text-watcher").projectDir = file("features/text-watcher")
