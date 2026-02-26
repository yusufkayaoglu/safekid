# ── Build aşaması ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Bağımlılıkları önce indir (cache katmanı)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Kaynak kodu kopyala ve derle
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Runtime aşaması ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Railway PORT env variable'ını kullan
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
