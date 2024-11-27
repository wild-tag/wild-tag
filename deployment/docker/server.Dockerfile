FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /wild-tag

# Copy the pom.xml and source code into the container
COPY pom.xml .
COPY src ./src

# Package the application (e.g., using Maven)
RUN mvn package -B -DskipTests


FROM eclipse-temurin:21-jdk

WORKDIR /wild-tag

COPY --from=build /wild-tag/target/*.jar /wild-tag/

EXPOSE 8080