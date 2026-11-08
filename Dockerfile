# ============================================================
# Taller Mecánico — backend Spring Boot + frontend React (SPA)
# El frontend se compila a estáticos que Spring sirve en el
# mismo origen (sin CORS): backend/src/main/resources/static/
# ============================================================

# ---- Etapa 1: compilar la SPA React ----
FROM node:20-alpine AS frontend
WORKDIR /fe
COPY frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---- Etapa 2: compilar el backend con la SPA incluida ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /fe/dist ./src/main/resources/static/
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN mkdir -p /app/logs && chown 1000:1000 /app/logs
COPY --from=build /app/target/taller-mecanico-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
