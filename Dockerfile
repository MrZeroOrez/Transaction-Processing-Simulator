FROM node:22-alpine AS ui
WORKDIR /ui
COPY frontend/package*.json ./
RUN npm ci
COPY frontend .
RUN npm run build
FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /api
COPY backend/pom.xml .
COPY backend/src ./src
COPY --from=ui /ui/dist ./src/main/resources/static
RUN mvn -B package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
USER app
COPY --from=api /api/target/banking-investment-platform-1.0.0.jar /app/app.jar
ENV SERVER_ADDRESS=0.0.0.0
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-jar","/app/app.jar"]
