package com.bn.atmcontratos.model.entity;

import com.bn.atmcontratos.model.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="proveedores", indexes={@Index(name="idx_proveedor_ruc",columnList="ruc"),@Index(name="idx_proveedor_razon",columnList="razonSocial")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Proveedor {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(unique=true,length=20) private String ruc;
 @Column(nullable=false,length=250) private String razonSocial;
 private String nombreComercial,tipoProveedor,nombreContacto,cargoContacto,telefonoContacto,correoContacto,departamento,ciudad,distrito;
 @Column(length=600) private String direccionFiscal;
 @Enumerated(EnumType.STRING) @Builder.Default private EstadoGenerico estado=EstadoGenerico.ACTIVO;
 @Column(length=1200) private String observaciones;
 private LocalDateTime fechaRegistro,fechaActualizacion;
 @OneToMany(mappedBy="proveedor") @JsonIgnore @Builder.Default private List<Contrato> contratos=new ArrayList<>();
 @OneToMany(mappedBy="proveedor") @JsonIgnore @Builder.Default private List<Atm> atms=new ArrayList<>();
 @PrePersist void pre(){fechaRegistro=LocalDateTime.now(); fechaActualizacion=LocalDateTime.now(); norm();}
 @PreUpdate void upd(){fechaActualizacion=LocalDateTime.now(); norm();}
 void norm(){ if(ruc!=null)ruc=ruc.trim(); if(razonSocial!=null)razonSocial=razonSocial.trim().toUpperCase(); }
}
