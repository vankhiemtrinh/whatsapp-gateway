# ──────────────────────────────────────────────────────────────────────────
# Stage 1: Build mit JDK 21 + Maven
# ──────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Maven Wrapper + POM zuerst kopieren -> Dependency-Layer wird gecacht
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Quellcode kopieren und bauen (Tests laufen in CI, nicht im Image-Build)
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ──────────────────────────────────────────────────────────────────────────
# Stage 2: schlankes Runtime-Image, non-root
# ──────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Non-root User
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /workspace/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
