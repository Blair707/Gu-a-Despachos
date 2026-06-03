# ================================
# Stage 1: Build
# ================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ================================
# Stage 2: Runtime
# ================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear directorio EFS (en prod se monta como volumen externo)
RUN mkdir -p /mnt/efs/guias

# Copiar el JAR generado
COPY --from=build /app/target/guias-despacho-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
