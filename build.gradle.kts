plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "io.gitlab.icestom"
version = "0.0.1"

repositories {
    mavenCentral()

    // Spark everything
    maven("https://repo.hypera.dev/snapshots/")
    maven("https://repo.lucko.me/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}


dependencies {
    implementation("net.minestom:minestom:2026.01.08-1.21.11")
    implementation("net.kyori:adventure-text-minimessage:4.26.1")

    implementation("dev.hollowcube:polar:1.15.0")
    implementation("it.unimi.dsi:fastutil:8.5.13")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")

    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("dev.lu15:spark-minestom:1.10-SNAPSHOT")

    implementation("dev.lu15:luckperms-minestom:5.5-SNAPSHOT")
    implementation("com.h2database:h2:2.1.214")
    compileOnly("com.zaxxer:HikariCP:6.3.0") // LP "SQL like stuff"
    compileOnly("redis.clients:jedis:5.2.0") // LP Redis
    compileOnly("io.nats:jnats:2.21.1") // LP NATS

    implementation("ch.qos.logback:logback-classic:1.5.32")

    implementation("com.electronwill.night-config:toml:3.8.4")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "io.gitlab.icestom.icestom.IceStom"
            attributes["Enable-Native-Access"] = "ALL-UNNAMED"
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