FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# This layer changes only when Maven configuration changes
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -DskipTests

# Java changes do not invalidate the dependency layer
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]