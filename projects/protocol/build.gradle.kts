plugins {
    id("java")
    `java-library`
    `maven-publish`
    id("com.google.protobuf") version "0.9.5"
}

group = "io.gitlab.icestom"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.protobuf:protobuf-java:4.31.1")
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.31.1"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}