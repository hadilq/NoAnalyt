plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
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

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
    compileOnly(libs.kotlin.compose.compiler)

    testImplementation(libs.kotlin.compiler.embeddable)
    testImplementation(libs.kotlin.compiler.testing)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.truth)
}