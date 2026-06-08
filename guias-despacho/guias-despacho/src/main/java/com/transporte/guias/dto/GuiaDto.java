package com.transporte.guias.dto;

import com.transporte.guias.model.GuiaDespacho;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GuiaDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "El transportista es obligatorio")
        private String transportista;

        @NotBlank(message = "El destinatario es obligatorio")
        private String destinatario;

        @NotBlank(message = "La dirección de destino es obligatoria")
        private String direccionDestino;

        @NotBlank(message = "La descripción de la carga es obligatoria")
        private String descripcionCarga;

        private Double pesoCarga;

        @NotNull(message = "La fecha de despacho es obligatoria")
        private LocalDate fechaDespacho;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String numeroGuia;
        private String transportista;
        private String destinatario;
        private String direccionDestino;
        private String descripcionCarga;
        private Double pesoCarga;
        private LocalDate fechaDespacho;
        private GuiaDespacho.EstadoGuia estado;
        private String rutaEfs;
        private String rutaS3;
        private LocalDateTime fechaCreacion;
        private LocalDateTime fechaActualizacion;

        public static Response from(GuiaDespacho g) {
            return Response.builder()
                    .id(g.getId())
                    .numeroGuia(g.getNumeroGuia())
                    .transportista(g.getTransportista())
                    .destinatario(g.getDestinatario())
                    .direccionDestino(g.getDireccionDestino())
                    .descripcionCarga(g.getDescripcionCarga())
                    .pesoCarga(g.getPesoCarga())
                    .fechaDespacho(g.getFechaDespacho())
                    .estado(g.getEstado())
                    .rutaEfs(g.getRutaEfs())
                    .rutaS3(g.getRutaS3())
                    .fechaCreacion(g.getFechaCreacion())
                    .fechaActualizacion(g.getFechaActualizacion())
                    .build();
        }
    }
}
