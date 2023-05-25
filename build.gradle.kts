plugins {
    id("pvm.base-plugin")
}

dependencies {
    implementation(libs.kord)
    implementation(libs.nebula)

    implementation(libs.coroutines)
    implementation(libs.serialization.json)
    implementation(libs.serialization.hocon)

    implementation(libs.ktor.core)
    implementation(libs.ktor.cio)
    implementation(libs.ktor.negociator)
    implementation(libs.ktor.negociator.json)
}
