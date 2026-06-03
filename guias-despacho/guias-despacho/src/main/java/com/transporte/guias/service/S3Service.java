package com.transporte.guias.service;

import com.transporte.guias.exception.BusinessException;
import com.transporte.guias.model.GuiaDespacho;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Sube el PDF desde EFS a S3.
     * La ruta en S3 sigue el patrón: /YYYYMMDD/transportistaX/guia123.pdf
     */
    public String subirGuia(GuiaDespacho guia) {
        if (guia.getRutaEfs() == null || guia.getRutaEfs().isBlank()) {
            throw new BusinessException("La guía no tiene PDF generado en EFS. Genere el PDF primero.");
        }

        String s3Key = buildS3Key(guia);

        try {
            byte[] contenido = Files.readAllBytes(Paths.get(guia.getRutaEfs()));

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(contenido));

        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo EFS: " + e.getMessage());
        } catch (S3Exception e) {
            throw new BusinessException("Error al subir a S3: " + e.awsErrorDetails().errorMessage());
        }

        return "s3://" + bucketName + "/" + s3Key;
    }

    /**
     * Descarga el PDF desde S3. Valida que la guía sea del transportista indicado.
     */
    public byte[] descargarGuia(GuiaDespacho guia, String transportistaSolicitante) {
        if (!guia.getTransportista().equalsIgnoreCase(transportistaSolicitante)) {
            throw new BusinessException("No tiene permiso para descargar esta guía.");
        }

        if (guia.getRutaS3() == null || guia.getRutaS3().isBlank()) {
            throw new BusinessException("La guía no ha sido subida a S3 aún.");
        }

        String s3Key = buildS3Key(guia);

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            return response.readAllBytes();

        } catch (NoSuchKeyException e) {
            throw new BusinessException("El archivo no existe en S3.");
        } catch (S3Exception | IOException e) {
            throw new BusinessException("Error al descargar desde S3: " + e.getMessage());
        }
    }

    /**
     * Elimina el objeto en S3 asociado a la guía.
     */
    public void eliminarDeS3(GuiaDespacho guia) {
        if (guia.getRutaS3() == null || guia.getRutaS3().isBlank()) return;

        String s3Key = buildS3Key(guia);

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            throw new BusinessException("Error al eliminar de S3: " + e.awsErrorDetails().errorMessage());
        }
    }

    private String buildS3Key(GuiaDespacho guia) {
        String fecha = guia.getFechaDespacho().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String transportistaNorm = guia.getTransportista().replaceAll("\\s+", "_").toLowerCase();
        String fileName = "guia_" + guia.getNumeroGuia() + ".pdf";
        return fecha + "/" + transportistaNorm + "/" + fileName;
    }
}
