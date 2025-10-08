enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.noanalyt") {
                useModule("com.noanalyt:noanalyt-compiler-plugin:${requested.version}")
            }
        }
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

includeBuild("noanalyt-gradle-plugin")
includeBuild("noanalyt-compose-compiler-reference")
includeBuild("noanalyt-compiler-plugin") {
    dependencySubstitution {
        substitute(module("com.noanalyt:noanalyt-compiler-plugin"))
            .using(project(":"))
            .because("Here, we need to work with the source directly.")

    }
}
includeBuild("noanalyt-runtime-api") {
    dependencySubstitution {
        substitute(module("com.noanalyt:noanalyt-runtime-api"))
            .using(project(":"))
            .because("Here, we need to work with the source directly.")

    }

}
includeBuild("noanalyt-runtime-impl") {
    dependencySubstitution {
        substitute(module("com.noanalyt:noanalyt-runtime-impl"))
            .using(project(":"))
            .because("Here, we need to work with the source directly.")

    }
}

rootProject.name = "noanalyt"
include(":sample-androidApp")
include(":sample-shared")

