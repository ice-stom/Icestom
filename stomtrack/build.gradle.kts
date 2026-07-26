plugins {
    id("java")
    `maven-publish`
    id("com.gradleup.shadow") version("9.4.3")
}

group = "io.gitlab.icestom"
version = "0.0.18"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.1")

    implementation("net.kyori:adventure-api:5.2.0")
    implementation("net.kyori:adventure-text-serializer-gson")

    implementation("io.github.openboatutils:Protocol:0.0.4")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.21.2")
    implementation("com.fasterxml.woodstox:woodstox-core:6.5.0")
}

tasks.shadowJar {
    archiveClassifier.set("")

    relocate("net.kyori", "io.gitlab.icestom.stomtrack.shadow.net.kyori")
    relocate("io.github.openboatutils", "io.gitlab.icestom.stomtrack.shadow.io.github.openboatutils")
    relocate("com.fasterxml", "io.gitlab.icestom.stomtrack.shadow.com.fasterxml")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}