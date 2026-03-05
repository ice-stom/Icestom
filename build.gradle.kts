plugins {
    id("java")
}

group = "io.gitlab.icestom"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2026.01.08-1.21.11")
    implementation("ch.qos.logback:logback-classic:1.5.13")
}