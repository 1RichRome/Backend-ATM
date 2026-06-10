package com.bn.atmcontratos.model.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="historial_eventos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialEvento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String entidad; private Long entidadId; private String accion;
 @Column(length=1200) private String descripcion; private String usuario; private LocalDateTime fechaHora;
 @Lob @Column(columnDefinition="LONGTEXT") private String valorAnterior;
 @Lob @Column(columnDefinition="LONGTEXT") private String valorNuevo;
 @PrePersist void pre(){ if(fechaHora==null)fechaHora=LocalDateTime.now(); }
}
