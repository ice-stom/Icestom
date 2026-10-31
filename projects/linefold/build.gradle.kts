plugins {
    id("java")
}

group = "io.gitlab.icestom"
version = "0.3.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("at.yawk.lz4:lz4-java:1.11.3")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}