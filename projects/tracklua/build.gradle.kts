plugins {
    id("java")
    `maven-publish`
    id("com.gradleup.shadow") version("9.4.3")
}

group = "io.gitlab.icestom"
version = "0.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":stomtrack"))

    implementation(libs.luau)

    implementation("org.jetbrains:annotations:24.0.1")
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}