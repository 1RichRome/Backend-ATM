package com.bn.atmcontratos.model.entity;

import com.bn.atmcontratos.model.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="actas_conformidad", uniqueConstraints=@UniqueConstraint(name="uk_acta_periodo", columnNames={"contrato_id","mes","anio"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActaConformidad {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="contrato_id",nullable=false) @JsonIgnoreProperties({"atms"}) private Contrato contrato;
 private Integer mes,anio;
 private LocalDate fechaEmision,periodoServicioInicio,periodoServicioFin;
 @Column(precision=14,scale=2) private BigDecimal montoPagar;
 private String moneda;
 private Boolean tienePenalidades, tienePenalidadMora, tieneOtrasPenalidades, adjuntaInformePenalidades, adjuntaOtroInforme;
 private String ciudadEmision, areaResponsable, cuentaContable, centroCosto, areaPresupuesto, tipoGasto;
 @Column(length=1200) private String observaciones;
 @Column(length=1000) private String atmIdsIncluidos;
 @Enumerated(EnumType.STRING) @Builder.Default private EstadoActa estado=EstadoActa.GENERADA;

 // Archivo editable generado por el sistema.
 private String nombreArchivoGenerado,rutaArchivoGenerado;
 // Archivo PDF generado por el sistema para impresión/revisión.
 private String nombreArchivoPdf,rutaArchivoPdf;
 // Archivo firmado que el usuario sube después de la firma externa.
 private String nombreArchivoFirmado,rutaArchivoFirmado;
 private LocalDateTime fechaGeneracion,fechaSubidaFirmada;
 @PrePersist void pre(){ if(estado==null)estado=EstadoActa.GENERADA; if(fechaGeneracion==null)fechaGeneracion=LocalDateTime.now(); }
}
