plugins {
    `kotlin-dsl`
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

group = "com.noanalyt"
version = libs.versions.noanalyt.get()

gradlePlugin {
    plugins {
        register("noanalytGradlePlugin") {
            id = libs.plugins.noanalyt.compiler.get().pluginId
            implementationClass = "com.noanalyt.NoAnalytGradlePlugin"
        }
    }
}

dependencies {
    compileOnly(libs.gradlePlugin.android)
    compileOnly(libs.gradlePlugin.kotlin)
}
