FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir /app/data && useradd --system --uid 10001 app && chown -R app /app
USER app
COPY --from=build /build/target/banking-investment-platform-1.0.0.jar /app/app.jar
ENV SERVER_ADDRESS=0.0.0.0 PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-jar","/app/app.jar"]
