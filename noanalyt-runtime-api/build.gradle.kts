import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.atomicfu)
}

group = "com.noanalyt.runtime.api"
version = libs.versions.noanalyt.get()

val noanalytRuntime = file("build/noanalyt/com/noanalyt/runtime")
if (!noanalytRuntime.exists()) {
    noanalytRuntime.mkdirs()
}
tasks.register("createVersionKt") {
    File(noanalytRuntime, "Version.kt").writeText("""
        package com.noanalyt.runtime
        val noAnalytVersion: String = "${libs.versions.noanalyt.get()}"
    """.trimIndent())
}

tasks.findByPath(":build")?.dependsOn("createVersionKt")

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }

    jvm()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "noanalyt-runtime-api"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(file("build/noanalyt"))
            dependencies {
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.atomicfu)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.example.noanalyt"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
