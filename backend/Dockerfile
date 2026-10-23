FROM maven:3.9.16-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && \
    useradd --system --gid spring --home-dir /app --shell /usr/sbin/nologin spring

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

EXPOSE 8080

USER spring

ENTRYPOINT ["java", "-jar", "app.jar"]
