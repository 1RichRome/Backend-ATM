package com.bn.atmcontratos.dto;

import com.bn.atmcontratos.model.enums.EstadoActa;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MesActaResponse {
 private Integer mes;
 private String nombreMes;
 private Integer anio;
 private String estado;
 private Long actaId;
 private EstadoActa estadoActa;
 private String nombreArchivoGenerado;
 private String nombreArchivoPdf;
 private String nombreArchivoFirmado;
 private Boolean tieneActaGenerada;
 private Boolean tieneActaPdf;
 private Boolean tieneActaFirmada;
 private LocalDateTime fechaGeneracion;
 private LocalDateTime fechaSubidaFirmada;
}
