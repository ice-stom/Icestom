plugins {
    id("java")
    `maven-publish`
    id("com.gradleup.shadow") version("9.4.3")
}

group = "io.gitlab.icestom"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("at.yawk.lz4:lz4-java:1.11.3")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}