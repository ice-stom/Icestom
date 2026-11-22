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

    mavenLocal()
    mavenCentral()

    // Spark everything
    maven("https://repo.lucko.me/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    // NOTE: if you're looking for versions go to /gradle/libs.versions.toml

    implementation(project(":stomtrack"))
    implementation(project(":linefold"))
    implementation(project(":protocol"))

    implementation(libs.minestom)
    implementation("net.kyori:adventure-text-minimessage:4.25.0")

    implementation(libs.bundles.polar)

    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation(libs.logback)

    implementation("com.electronwill.night-config:toml:3.8.4")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")

    implementation(project(":lua"))

    implementation(libs.openboatutils.protocol)

    implementation(libs.luau)
    implementation(libs.bundles.luau.natives)

    implementation(libs.bundles.jline)
}

val panelSiteDir = providers.gradleProperty("icestom.panelSite").orNull
    ?.let { file(it) }
    ?: file("../../../IcestomSite/site")

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

    repositories {
        maven {
            name = "GitHubPackages"

            url = uri(
                "https://maven.pkg.github.com/ice-stom/icestom"
            )

            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}