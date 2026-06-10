package com.bn.atmcontratos.model.entity;

import com.bn.atmcontratos.model.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="atms", indexes={
 @Index(name="idx_atm_codigo",columnList="codigoTerminal"),
 @Index(name="idx_atm_departamento",columnList="departamento"),
 @Index(name="idx_atm_proveedor",columnList="proveedor_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Atm {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotBlank @Column(nullable=false,unique=true,length=50) private String codigoTerminal;
 private String detalleLobby, departamento, ciudad, distrito, tipoAtmIsla, codigoAgencia, nombreOficina, tipoOficina, region, ubigeo;
 @Column(length=700) private String direccion;
 @Column(length=700) private String ubicacion;
 @Column(precision=18, scale=12) private BigDecimal coordenadaX;
 @Column(precision=18, scale=12) private BigDecimal coordenadaY;
 @Enumerated(EnumType.STRING) @Builder.Default private EstadoGenerico estado=EstadoGenerico.ACTIVO;
 @Column(length=1200) private String observaciones;
 private LocalDateTime fechaRegistro, fechaActualizacion;

 @ManyToOne(fetch=FetchType.EAGER)
 @JoinColumn(name="proveedor_id")
 @JsonIgnoreProperties({"contratos","atms"})
 private Proveedor proveedor;

 @ManyToMany(mappedBy="atms") @JsonIgnore @Builder.Default private Set<Contrato> contratos=new LinkedHashSet<>();
 @PrePersist void pre(){fechaRegistro=LocalDateTime.now(); fechaActualizacion=LocalDateTime.now(); norm();}
 @PreUpdate void upd(){fechaActualizacion=LocalDateTime.now(); norm();}
 void norm(){ if(codigoTerminal!=null)codigoTerminal=codigoTerminal.trim(); if(departamento!=null)departamento=departamento.trim().toUpperCase(); if(ciudad!=null)ciudad=ciudad.trim().toUpperCase(); if(distrito!=null)distrito=distrito.trim().toUpperCase(); }
}
