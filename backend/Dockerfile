FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml ./
RUN mvn -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --create-home --shell /usr/sbin/nologin app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app

COPY --from=build /build/target/zongce-system-0.0.1-SNAPSHOT.jar /app/app.jar

USER app
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
