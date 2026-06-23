# ================================
# Stage 1: Build
# ================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar pom.xml y descargar dependencias (capa cacheada)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# ================================
# Stage 2: Runtime
# ================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear directorio para PDFs
RUN mkdir -p /mnt/efs/guias

# Copiar JAR desde stage de build
COPY --from=build /app/target/guias-despacho-1.0.0.jar app.jar

# Puerto expuesto
EXPOSE 8080

# Variables de entorno con valores por defecto
ENV DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=guias_db \
    DB_USER=root \
    DB_PASSWORD=root \
    AWS_S3_BUCKET=guias-despacho-bucket-b707 \
    AWS_REGION=us-east-1 \
    EFS_MOUNT_PATH=/mnt/efs/guias \
    AZURE_TENANT_ID=4ba371a5-2d60-4c43-bc38-a63974911ffb \
    AZURE_CLIENT_ID=2d754c0b-38db-4f29-9ab8-81f0a7021f33

# Ejecutar con perfil de producción
ENTRYPOINT ["java", "-jar", "app.jar"]
