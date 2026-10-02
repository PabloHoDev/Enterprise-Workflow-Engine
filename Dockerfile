# syntax=docker/dockerfile:1

# ---- Build stage: compila e separa o jar em camadas ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Dependências primeiro: esta camada só é refeita quando o pom.xml muda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Os testes rodam na pipeline de CI, antes da construção da imagem.
RUN ./mvnw -B -q -DskipTests package \
    && cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Runtime stage: apenas JRE e a aplicação ----
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
