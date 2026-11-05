plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
    // Contract files are shared with the Python side; tests read them from the repo root.
    systemProperty("contracts.dir", rootProject.projectDir.resolve("contracts").absolutePath)
}
