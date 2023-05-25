import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.triumphteam.dev/snapshots")
}

dependencies {
    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            javaParameters = true
            languageVersion = "1.9"
            freeCompilerArgs = listOf(
                "-Xcontext-receivers",
                "-opt-in=" + listOf(
                    "kotlin.RequiresOptIn",
                    "kotlin.time.ExperimentalTime",
                    "kotlin.io.path.ExperimentalPathApi",
                    "kotlin.io.path.ExperimentalSerializationApi",
                    "kotlin.ExperimentalStdlibApi",
                    "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "kotlinx.serialization.InternalSerializationApi",
                    "kotlinx.serialization.ExperimentalSerializationApi",
                ).joinToString(","),
            )
        }
    }
}
