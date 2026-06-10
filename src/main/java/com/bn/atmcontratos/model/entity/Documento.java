package com.bn.atmcontratos.model.entity;
import com.bn.atmcontratos.model.enums.*; import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="documentos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Documento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="contrato_id") @JsonIgnoreProperties({"atms"}) private Contrato contrato;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="atm_id") private Atm atm;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="proveedor_id") private Proveedor proveedor;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="acta_id") private ActaConformidad acta;
 @Enumerated(EnumType.STRING) private TipoDocumento tipoDocumento;
 private String nombreArchivo,rutaArchivo,extension,usuarioSubida; private Long tamanio;
 @Column(length=1200) private String descripcion;
 private LocalDateTime fechaSubida;
 @Enumerated(EnumType.STRING) @Builder.Default private EstadoGenerico estado=EstadoGenerico.ACTIVO;
 @PrePersist void pre(){fechaSubida=LocalDateTime.now(); if(estado==null)estado=EstadoGenerico.ACTIVO;}
}
