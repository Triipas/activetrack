# Imagen única para los tres servicios: se elige el módulo con --build-arg MODULE=...
FROM maven:3.9.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /src
# Primero solo los pom.xml para que Docker reutilice la caché de dependencias
COPY pom.xml .
COPY common-events/pom.xml common-events/
COPY cronometraje-service/pom.xml cronometraje-service/
COPY resultados-service/pom.xml resultados-service/
COPY notificaciones-service/pom.xml notificaciones-service/
COPY inscripciones-service/pom.xml inscripciones-service/
RUN mvn -B -q -pl ${MODULE} -am dependency:go-offline || true
COPY . .
RUN mvn -B -q -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
