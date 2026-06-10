package com.bn.atmcontratos.model.entity;

import com.bn.atmcontratos.model.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity
@Table(name="contratos", indexes={
 @Index(name="idx_contrato_estado", columnList="estadoContrato"),
 @Index(name="idx_contrato_fin", columnList="fechaFin")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contrato {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 private Long id;

 private String numeroInterno;

 @Column(nullable=false,length=260)
 private String numeroContrato;

 private String nombreContrato;
 private String tipoContrato;
 private String responsableActas;
 private String meses;
 private String impuesto;
 private String moneda;

 /** Fecha de firma/suscripción del contrato o adenda. Si está vacío se usa fechaInicio como respaldo en el acta. */
 private LocalDate fechaSuscripcion;

 /** Indica si los montos registrados ya incluyen IGV. Solo controla el texto del acta: "con IGV" o "sin IGV". */
 @Column(name="incluye_igv")
 @Builder.Default
 private Boolean incluyeIgv = false;

 @Column(length=1500)
 private String descripcionContrato;

 /** Renta mensual base por espacio/cajero. */
 @Column(precision=14,scale=2)
 private BigDecimal rentaMensual;

 /** Gasto común u otros gastos mensuales por espacio/cajero. */
 @Column(precision=14,scale=2)
 private BigDecimal otrosGastos;

 /** Monto mensual total por espacio/cajero: rentaMensual + otrosGastos. */
 @Column(precision=14,scale=2)
 private BigDecimal montoTotalMensual;

 /** Monto total del contrato/adenda. Si existe se usa en el acta; si no, se calcula con cantidad de espacios y duración. */
 @Column(precision=16,scale=2)
 private BigDecimal montoContrato;

 /**
  * Combinación contable para el acta.
  * Por defecto se cargan los valores usados por Canales Presenciales,
  * pero el usuario puede modificarlos al crear o editar el contrato.
  */
 @Builder.Default private String cuentaContableRenta = "4513.01.10.01";
 @Builder.Default private String centroCostoRenta = "5500";
 @Builder.Default private String areaPresupuestoRenta = "5500";
 @Builder.Default private String tipoGastoRenta = "000000000000";

 @Builder.Default private String cuentaContableGastoComun = "4513.01.20.04";
 @Builder.Default private String centroCostoGastoComun = "5500";
 @Builder.Default private String areaPresupuestoGastoComun = "5500";
 @Builder.Default private String tipoGastoGastoComun = "SE000000251";

 private LocalDate fechaInicio;
 private LocalDate fechaFin;
 private Integer diasPorVencer;

 @Enumerated(EnumType.STRING)
 private EstadoContrato estadoContrato;

 @Column(length=1200)
 private String observaciones;

 private LocalDateTime fechaRegistro;
 private LocalDateTime fechaActualizacion;

 @ManyToOne(fetch=FetchType.EAGER)
 @JoinColumn(name="proveedor_id")
 @JsonIgnoreProperties({"contratos","atms"})
 private Proveedor proveedor;

 @ManyToMany(fetch=FetchType.EAGER)
 @JoinTable(
  name="contratos_atms",
  joinColumns=@JoinColumn(name="contrato_id"),
  inverseJoinColumns=@JoinColumn(name="atm_id"),
  uniqueConstraints=@UniqueConstraint(columnNames={"contrato_id","atm_id"})
 )
 @JsonIgnoreProperties({"contratos"})
 @Builder.Default
 private Set<Atm> atms=new LinkedHashSet<>();

 @PrePersist
 void pre(){
  fechaRegistro=LocalDateTime.now();
  fechaActualizacion=LocalDateTime.now();
  aplicarDefaultsContables();
  recalcular();
 }

 @PreUpdate
 void upd(){
  fechaActualizacion=LocalDateTime.now();
  aplicarDefaultsContables();
  recalcular();
 }

 public void aplicarDefaultsContables(){
  if(moneda==null || moneda.isBlank()) moneda="S/";
  if(incluyeIgv==null) incluyeIgv=false;
  if(cuentaContableRenta==null || cuentaContableRenta.isBlank()) cuentaContableRenta="4513.01.10.01";
  if(centroCostoRenta==null || centroCostoRenta.isBlank()) centroCostoRenta="5500";
  if(areaPresupuestoRenta==null || areaPresupuestoRenta.isBlank()) areaPresupuestoRenta="5500";
  if(tipoGastoRenta==null || tipoGastoRenta.isBlank()) tipoGastoRenta="000000000000";
  if(cuentaContableGastoComun==null || cuentaContableGastoComun.isBlank()) cuentaContableGastoComun="4513.01.20.04";
  if(centroCostoGastoComun==null || centroCostoGastoComun.isBlank()) centroCostoGastoComun="5500";
  if(areaPresupuestoGastoComun==null || areaPresupuestoGastoComun.isBlank()) areaPresupuestoGastoComun="5500";
  if(tipoGastoGastoComun==null || tipoGastoGastoComun.isBlank()) tipoGastoGastoComun="SE000000251";
 }

 public void recalcular(){
  if(incluyeIgv==null) incluyeIgv=false;
  if(fechaFin==null){
   if(estadoContrato==null) estadoContrato=EstadoContrato.VIGENTE;
   return;
  }
  long d=ChronoUnit.DAYS.between(LocalDate.now(),fechaFin);
  diasPorVencer=(int)d;
  if(d<0) estadoContrato=EstadoContrato.VENCIDO;
  else if(d<=30) estadoContrato=EstadoContrato.POR_VENCER_30;
  else if(d<=60) estadoContrato=EstadoContrato.POR_VENCER_60;
  else if(d<=90) estadoContrato=EstadoContrato.POR_VENCER_90;
  else estadoContrato=EstadoContrato.VIGENTE;
 }
}
