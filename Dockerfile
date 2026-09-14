FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build

# Copy only Gradle wrapper + build files first (for caching)
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle* settings.gradle* ./
RUN chmod +x gradlew
COPY m2-repo/io/github/openboatutils/Protocol/0.0.7/ /root/.m2/repository/io/github/openboatutils/Protocol/0.0.7/
RUN ./gradlew dependencies --no-daemon || true

COPY . .

RUN ./gradlew clean build --no-daemon

FROM eclipse-temurin:25-jre
RUN mkdir -p /opt/icestom
COPY --from=builder /build/build/libs/*.jar /opt/icestom/Icestom.jar
WORKDIR /icestom
ENTRYPOINT ["java", "-jar", "/opt/icestom/Icestom.jar"]