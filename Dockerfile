# syntax=docker/dockerfile:1

# ---- Console web: build estático da SPA ----
FROM node:24-alpine AS console
WORKDIR /console
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---- Aplicação: compila e separa o jar em camadas ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Dependências primeiro: esta camada só é refeita quando o pom.xml muda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# O console é servido pela própria aplicação, na mesma origem da API (ADR-008).
COPY --from=console /console/dist/ src/main/resources/static/
# Os testes rodam na pipeline de CI, antes da construção da imagem.
RUN ./mvnw -B -q -DskipTests package \
    && cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Runtime: apenas JRE e a aplicação ----
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --no-create-home workflow
WORKDIR /app

# Da camada que menos muda para a que mais muda.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER workflow
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
