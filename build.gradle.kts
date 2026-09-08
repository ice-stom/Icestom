plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version("9.4.3")
}

group = "io.gitlab.icestom"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    mavenLocal()

    // Spark everything
    maven("https://repo.hypera.dev/snapshots/")
    maven("https://repo.lucko.me/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("io.github.openboatutils:Protocol:0.0.7")
    implementation(project(":stomtrack"))

    implementation("net.minestom:minestom:2026.01.08-1.21.11")
    implementation("net.kyori:adventure-text-minimessage:4.25.0")

    implementation("dev.hollowcube:polar:1.15.0")
    implementation("it.unimi.dsi:fastutil:8.5.13")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")

    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("dev.lu15:spark-minestom:1.10-SNAPSHOT")

    implementation("ch.qos.logback:logback-classic:1.5.32")

    implementation("com.electronwill.night-config:toml:3.8.4")

    implementation("dev.hollowcube:luau:1.2.1")
    implementation("dev.hollowcube:luau-natives-linux-x64:1.2.1")
    implementation("dev.hollowcube:luau-natives-windows-x64:1.2.1")

    implementation("org.jline:jline-terminal-jna:3.30.9")
    implementation("org.jline:jline-terminal-jansi:3.30.9")
    implementation("org.jline:jline-reader:3.30.9")
}

val panelSiteDir = providers.gradleProperty("icestom.panelSite").orNull
    ?.let { file(it) }
    ?: file("../IcestomSite/site")

val bundlePanelSite = tasks.register<Sync>("bundlePanelSite") {
    description = "Copies the event panel site into the jar under web/."

    into(layout.buildDirectory.dir("generated/panel-site/web"))

    if (panelSiteDir.isDirectory) {
        from(panelSiteDir)
    } else {
        doFirst {
            logger.warn(
                "Panel site not found at {}. The jar will build without it and /panel will 404. " +
                    "Clone IcestomSite next to this repository, or set -Picestom.panelSite=<path>.",
                panelSiteDir,
            )
        }
    }
}

sourceSets["main"].resources.srcDir(layout.buildDirectory.dir("generated/panel-site"))

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "io.gitlab.icestom.icestom.IceStom"
            attributes["Enable-Native-Access"] = "ALL-UNNAMED"
        }
        archiveClassifier.set("plain")
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("")
    }

    processResources {
        dependsOn(bundlePanelSite)

        filesMatching("version.properties") {
            expand(
                "version" to project.version
            )
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar)
        }
    }
}