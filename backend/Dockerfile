# Etapa 1: compilar con Maven
FROM maven:3.9-eclipse-temurin-21 AS compilar
WORKDIR /app
# primero el pom: las dependencias quedan en cache si no cambian
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Etapa 2: solo Java y el .jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilar /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
