# Étape 1 : Build avec Gradle
FROM gradle:8.6-jdk21-alpine AS builder

WORKDIR /app

# Copie les fichiers Gradle d’abord (cache intelligent)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Télécharge les dépendances (cache)
RUN gradle dependencies --no-daemon

# Copie le reste du code source
COPY src ./src

# Build le JAR
RUN gradle build --no-daemon -x test

# Étape 2 : Image finale légère
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copie le JAR généré de l’étape builder
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
