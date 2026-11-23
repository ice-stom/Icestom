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
    implementation(project(":lua"))

    implementation(libs.luau)
    implementation("com.google.jimfs:jimfs:1.3.2")

    runtimeOnly(libs.logback)
    runtimeOnly(libs.bundles.luau.natives)

    implementation("org.slf4j:slf4j-api:2.0.17")
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