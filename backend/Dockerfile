# Etapa 1: compilar con Maven (esta imagen es grande y no se usa para correr la app)
FROM maven:3.9-eclipse-temurin-21 AS compilar
WORKDIR /app
# Primero solo el pom: Docker guarda en cache las dependencias y no las baja de nuevo si no cambian
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Etapa 2: imagen final, solo con Java y el .jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilar /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
