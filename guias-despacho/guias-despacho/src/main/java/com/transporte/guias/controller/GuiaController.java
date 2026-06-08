package com.transporte.guias.controller;

import com.transporte.guias.dto.GuiaDto;
import com.transporte.guias.service.GuiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
@Tag(name = "Guías de Despacho", description = "Gestión de guías de despacho y archivos PDF")
public class GuiaController {

    private final GuiaService guiaService;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    @PostMapping
    @Operation(summary = "Crear una nueva guía de despacho",
               description = "Genera la guía, crea el PDF y lo guarda en el EFS")
    public ResponseEntity<GuiaDto.Response> crear(
            @Valid @RequestBody GuiaDto.Request request) {
        GuiaDto.Response response = guiaService.crearGuia(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/upload")
    @Operation(summary = "Subir la guía generada a AWS S3")
    public ResponseEntity<GuiaDto.Response> subirS3(
            @PathVariable Long id) {
        return ResponseEntity.ok(guiaService.subirGuiaS3(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Descargar la guía desde S3 con validación de permisos")
    public ResponseEntity<byte[]> descargar(
            @PathVariable Long id,
            @Parameter(description = "Nombre del transportista que solicita la descarga")
            @RequestParam String transportista) {

        byte[] pdfBytes = guiaService.descargarGuia(id, transportista);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "guia_" + id + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar o actualizar una guía existente")
    public ResponseEntity<GuiaDto.Response> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GuiaDto.Request request) {
        return ResponseEntity.ok(guiaService.actualizarGuia(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una guía específica (también la elimina de S3)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        guiaService.eliminarGuia(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Consultar guías por transportista y/o fecha")
    public ResponseEntity<List<GuiaDto.Response>> consultar(
            @Parameter(description = "Filtrar por transportista")
            @RequestParam(required = false) String transportista,
            @Parameter(description = "Filtrar por fecha de despacho (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(guiaService.consultarGuias(transportista, fecha));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una guía por su ID")
    public ResponseEntity<GuiaDto.Response> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.obtenerPorId(id));
    }
}
