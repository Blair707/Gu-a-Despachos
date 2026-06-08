package com.transporte.guias.service;

import com.transporte.guias.dto.GuiaDto;
import com.transporte.guias.exception.BusinessException;
import com.transporte.guias.exception.ResourceNotFoundException;
import com.transporte.guias.model.GuiaDespacho;
import com.transporte.guias.repository.GuiaDespachoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class GuiaService {

    private final GuiaDespachoRepository repository;
    private final PdfService pdfService;
    private final S3Service s3Service;

    public GuiaService(GuiaDespachoRepository repository,
                       PdfService pdfService,
                       S3Service s3Service) {
        this.repository = repository;
        this.pdfService  = pdfService;
        this.s3Service   = s3Service;
    }

    public GuiaDto.Response crearGuia(GuiaDto.Request request) {
        GuiaDespacho guia = GuiaDespacho.builder()
                .numeroGuia(generarNumeroGuia(request))
                .transportista(request.getTransportista())
                .destinatario(request.getDestinatario())
                .direccionDestino(request.getDireccionDestino())
                .descripcionCarga(request.getDescripcionCarga())
                .pesoCarga(request.getPesoCarga())
                .fechaDespacho(request.getFechaDespacho())
                .estado(GuiaDespacho.EstadoGuia.CREADA)
                .build();

        // Generar PDF en EFS
        String rutaEfs = pdfService.generarPdf(guia);
        guia.setRutaEfs(rutaEfs);
        guia.setEstado(GuiaDespacho.EstadoGuia.PDF_GENERADO);

        return GuiaDto.Response.from(repository.save(guia));
    }

    public GuiaDto.Response subirGuiaS3(Long id) {
        GuiaDespacho guia = obtenerEntidad(id);

        if (guia.getEstado() == GuiaDespacho.EstadoGuia.ANULADA) {
            throw new BusinessException("No se puede subir una guía anulada.");
        }

        String rutaS3 = s3Service.subirGuia(guia);
        guia.setRutaS3(rutaS3);
        guia.setEstado(GuiaDespacho.EstadoGuia.SUBIDA_S3);

        return GuiaDto.Response.from(repository.save(guia));
    }

    @Transactional(readOnly = true)
    public byte[] descargarGuia(Long id, String transportista) {
        GuiaDespacho guia = obtenerEntidad(id);
        return s3Service.descargarGuia(guia, transportista);
    }

    public GuiaDto.Response actualizarGuia(Long id, GuiaDto.Request request) {
        GuiaDespacho guia = obtenerEntidad(id);

        if (guia.getEstado() == GuiaDespacho.EstadoGuia.ANULADA) {
            throw new BusinessException("No se puede modificar una guía anulada.");
        }

        guia.setTransportista(request.getTransportista());
        guia.setDestinatario(request.getDestinatario());
        guia.setDireccionDestino(request.getDireccionDestino());
        guia.setDescripcionCarga(request.getDescripcionCarga());
        guia.setPesoCarga(request.getPesoCarga());
        guia.setFechaDespacho(request.getFechaDespacho());

        // Regenerar PDF con los nuevos datos
        String nuevaRutaEfs = pdfService.generarPdf(guia);
        guia.setRutaEfs(nuevaRutaEfs);
        guia.setRutaS3(null);
        guia.setEstado(GuiaDespacho.EstadoGuia.PDF_GENERADO);

        return GuiaDto.Response.from(repository.save(guia));
    }

    public void eliminarGuia(Long id) {
        GuiaDespacho guia = obtenerEntidad(id);

        // Eliminar de S3 si existe
        if (guia.getRutaS3() != null) {
            s3Service.eliminarDeS3(guia);
        }

        repository.delete(guia);
    }

    @Transactional(readOnly = true)
    public GuiaDto.Response obtenerPorId(Long id) {
        return GuiaDto.Response.from(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public List<GuiaDto.Response> consultarGuias(String transportista, LocalDate fecha) {
        List<GuiaDespacho> resultado;

        if (transportista != null && fecha != null) {
            resultado = repository.findByTransportistaAndFechaDespacho(transportista, fecha);
        } else if (transportista != null) {
            resultado = repository.findByTransportista(transportista);
        } else if (fecha != null) {
            resultado = repository.findByFechaDespacho(fecha);
        } else {
            resultado = repository.findAll();
        }

        return resultado.stream()
                .map(GuiaDto.Response::from)
                .collect(Collectors.toList());
    }

    private GuiaDespacho obtenerEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guía no encontrada con id: " + id));
    }

    private String generarNumeroGuia(GuiaDto.Request request) {
        String fecha = request.getFechaDespacho().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String transportistaCod = request.getTransportista()
                .substring(0, Math.min(3, request.getTransportista().length()))
                .toUpperCase();
        String uid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return fecha + "-" + transportistaCod + "-" + uid;
    }
}
