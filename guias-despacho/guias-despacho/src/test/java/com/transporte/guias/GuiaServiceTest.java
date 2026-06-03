package com.transporte.guias;

import com.transporte.guias.dto.GuiaDto;
import com.transporte.guias.exception.ResourceNotFoundException;
import com.transporte.guias.model.GuiaDespacho;
import com.transporte.guias.repository.GuiaDespachoRepository;
import com.transporte.guias.service.GuiaService;
import com.transporte.guias.service.PdfService;
import com.transporte.guias.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuiaServiceTest {

    @Mock private GuiaDespachoRepository repository;
    @Mock private PdfService pdfService;
    @Mock private S3Service s3Service;

    @InjectMocks
    private GuiaService guiaService;

    private GuiaDto.Request requestValido;
    private GuiaDespacho guiaMock;

    @BeforeEach
    void setUp() {
        requestValido = GuiaDto.Request.builder()
                .transportista("TransporteXYZ")
                .destinatario("Juan Pérez")
                .direccionDestino("Av. Principal 123")
                .descripcionCarga("Electrónica diversa")
                .pesoCarga(15.5)
                .fechaDespacho(LocalDate.now())
                .build();

        guiaMock = GuiaDespacho.builder()
                .id(1L)
                .numeroGuia("20241201-TRA-ABC123")
                .transportista("TransporteXYZ")
                .destinatario("Juan Pérez")
                .direccionDestino("Av. Principal 123")
                .descripcionCarga("Electrónica diversa")
                .pesoCarga(15.5)
                .fechaDespacho(LocalDate.now())
                .estado(GuiaDespacho.EstadoGuia.PDF_GENERADO)
                .rutaEfs("/mnt/efs/guias/20241201/transportexyz/guia_ABC.pdf")
                .build();
    }

    @Test
    @DisplayName("Crear guía genera PDF y guarda en base de datos")
    void crearGuia_exitoso() {
        when(pdfService.generarPdf(any())).thenReturn("/mnt/efs/guias/test.pdf");
        when(repository.save(any())).thenReturn(guiaMock);

        GuiaDto.Response response = guiaService.crearGuia(requestValido);

        assertThat(response).isNotNull();
        assertThat(response.getTransportista()).isEqualTo("TransporteXYZ");
        verify(pdfService, times(1)).generarPdf(any());
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("Subir a S3 actualiza la ruta S3 y el estado")
    void subirGuiaS3_exitoso() {
        when(repository.findById(1L)).thenReturn(Optional.of(guiaMock));
        when(s3Service.subirGuia(guiaMock)).thenReturn("s3://bucket/20241201/transportexyz/guia.pdf");

        GuiaDespacho guardado = GuiaDespacho.builder()
                .id(1L)
                .numeroGuia("20241201-TRA-ABC123")
                .transportista("TransporteXYZ")
                .destinatario("Juan Pérez")
                .direccionDestino("Av. Principal 123")
                .descripcionCarga("Electrónica diversa")
                .pesoCarga(15.5)
                .fechaDespacho(LocalDate.now())
                .estado(GuiaDespacho.EstadoGuia.SUBIDA_S3)
                .rutaEfs("/mnt/efs/guias/test.pdf")
                .rutaS3("s3://bucket/20241201/transportexyz/guia.pdf")
                .build();

        when(repository.save(any())).thenReturn(guardado);

        GuiaDto.Response response = guiaService.subirGuiaS3(1L);

        assertThat(response.getEstado()).isEqualTo(GuiaDespacho.EstadoGuia.SUBIDA_S3);
        assertThat(response.getRutaS3()).isNotBlank();
        verify(s3Service, times(1)).subirGuia(any());
    }

    @Test
    @DisplayName("Obtener guía inexistente lanza ResourceNotFoundException")
    void obtenerGuia_noExiste_lanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guiaService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Eliminar guía llama a S3 si tiene ruta S3")
    void eliminarGuia_conS3_llamaEliminarS3() {
        guiaMock.setRutaS3("s3://bucket/guia.pdf");
        when(repository.findById(1L)).thenReturn(Optional.of(guiaMock));
        doNothing().when(s3Service).eliminarDeS3(any());

        guiaService.eliminarGuia(1L);

        verify(s3Service, times(1)).eliminarDeS3(any());
        verify(repository, times(1)).delete(any());
    }
}
