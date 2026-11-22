plugins {
    id("java")
    `maven-publish`
}

group = "io.gitlab.icestom"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")

    implementation(libs.luau)

    implementation("org.jetbrains:annotations:24.0.1")
}

tasks.withType<JavaCompile> {
    options.release.set(25)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}