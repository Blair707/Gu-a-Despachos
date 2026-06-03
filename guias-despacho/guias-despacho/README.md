# Sistema de Gestión de Guías de Despacho

Microservicio Spring Boot para gestión de pedidos y generación de guías de despacho, con integración a AWS EFS y S3, y despliegue automatizado vía GitHub Actions.

## Tecnologías

- Java 17 + Spring Boot 3.2
- MySQL 8
- AWS SDK v2 (S3)
- iText 5 (generación de PDFs)
- Docker
- GitHub Actions (CI/CD)

## Endpoints disponibles

| Método   | Ruta                        | Descripción                                      |
|----------|-----------------------------|--------------------------------------------------|
| `POST`   | `/api/guias`                | Crear guía y generar PDF en EFS                  |
| `POST`   | `/api/guias/{id}/upload`    | Subir PDF de EFS a S3                            |
| `GET`    | `/api/guias/{id}/download`  | Descargar PDF desde S3 (con validación)          |
| `PUT`    | `/api/guias/{id}`           | Actualizar guía (regenera PDF)                   |
| `DELETE` | `/api/guias/{id}`           | Eliminar guía (también la elimina de S3)         |
| `GET`    | `/api/guias`                | Consultar guías (filtros: transportista, fecha)  |
| `GET`    | `/api/guias/{id}`           | Obtener guía por ID                              |

Swagger UI disponible en: `http://localhost:8080/swagger-ui.html`

## Estructura del proyecto

```
guias-despacho/
├── src/main/java/com/transporte/guias/
│   ├── config/          # AwsConfig
│   ├── controller/      # GuiaController
│   ├── dto/             # GuiaDto (Request / Response)
│   ├── exception/       # GlobalExceptionHandler, custom exceptions
│   ├── model/           # GuiaDespacho (entidad JPA)
│   ├── repository/      # GuiaDespachoRepository
│   └── service/         # GuiaService, PdfService, S3Service
├── src/test/            # Tests unitarios
├── .github/workflows/   # Pipeline CI/CD
├── Dockerfile
└── pom.xml
```

## Configuración local (desarrollo)

### 1. Requisitos previos
- Java 17+
- Maven 3.9+
- MySQL 8 corriendo localmente
- (Opcional) Credenciales AWS para probar S3

### 2. Crear base de datos MySQL

```sql
CREATE DATABASE guias_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Variables de entorno (o editar application.properties)

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=guias_db
export DB_USER=root
export DB_PASSWORD=tu_password

# Opcional para S3 local:
export AWS_ACCESS_KEY=tu_access_key
export AWS_SECRET_KEY=tu_secret_key
export AWS_S3_BUCKET=nombre-del-bucket
export AWS_REGION=us-east-1

# EFS (en local usa cualquier carpeta):
export EFS_MOUNT_PATH=/tmp/guias
```

### 4. Ejecutar

```bash
mvn spring-boot:run
```

## Ejecutar con Docker (local)

```bash
docker build -t guias-despacho .

docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_USER=root \
  -e DB_PASSWORD=root \
  -e DB_NAME=guias_db \
  -e EFS_MOUNT_PATH=/tmp/guias \
  guias-despacho
```

## Secrets requeridos en GitHub

Configurar en `Settings > Secrets and variables > Actions`:

| Secret              | Descripción                                        |
|---------------------|----------------------------------------------------|
| `DOCKERHUB_USERNAME`| Usuario de Docker Hub                              |
| `DOCKERHUB_TOKEN`   | Token de acceso de Docker Hub                      |
| `EC2_HOST`          | IP pública de la instancia EC2                     |
| `EC2_USER`          | Usuario SSH de EC2 (normalmente `ec2-user` o `ubuntu`) |
| `EC2_SSH_KEY`       | Clave privada PEM para conectar a EC2              |
| `DB_HOST`           | Host de MySQL en EC2                               |
| `DB_PORT`           | Puerto MySQL (por defecto 3306)                    |
| `DB_NAME`           | Nombre de la base de datos                         |
| `DB_USER`           | Usuario MySQL                                      |
| `DB_PASSWORD`       | Contraseña MySQL                                   |
| `AWS_S3_BUCKET`     | Nombre del bucket S3                               |
| `AWS_REGION`        | Región AWS (ej. `us-east-1`)                       |

> En EC2 se recomienda usar un **IAM Role** con permisos S3 en vez de AWS keys.  
> En ese caso NO se necesitan `AWS_ACCESS_KEY` y `AWS_SECRET_KEY`.

## Estructura S3

Las guías se almacenan con el patrón:
```
s3://bucket-name/YYYYMMDD/transportista/guia_NUMERO.pdf
```

Ejemplo:
```
s3://guias-despacho-bucket/20241201/transportexyz/guia_20241201-TRA-A1B2C3.pdf
```
