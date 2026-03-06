plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "io.gitlab.icestom"
version = "0.0.1"

repositories {
    mavenCentral()
}


dependencies {
    implementation("net.minestom:minestom:2026.01.08-1.21.11")
    implementation("dev.hollowcube:polar:1.15.0")
    implementation("it.unimi.dsi:fastutil:8.5.13")

    implementation("ch.qos.logback:logback-classic:1.5.32")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "io.gitlab.icestom.icestom.IceStom"
        }
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("")
    }
}