package com.bn.atmcontratos.model.entity;
import com.bn.atmcontratos.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime; import java.util.*;
@Entity @Table(name="usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario implements UserDetails {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(unique=true,nullable=false,length=80) private String username;
 @Column(nullable=false) private String password;
 @Column(nullable=false,length=160) private String nombreCompleto;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private RolNombre rol;
 @Enumerated(EnumType.STRING) @Builder.Default private EstadoGenerico estado=EstadoGenerico.ACTIVO;
 private LocalDateTime fechaRegistro;
 @PrePersist void pre(){fechaRegistro=LocalDateTime.now(); if(estado==null) estado=EstadoGenerico.ACTIVO;}
 public Collection<? extends GrantedAuthority> getAuthorities(){return List.of(new SimpleGrantedAuthority("ROLE_"+rol.name()));}
 public boolean isAccountNonExpired(){return true;} public boolean isAccountNonLocked(){return estado==EstadoGenerico.ACTIVO;}
 public boolean isCredentialsNonExpired(){return true;} public boolean isEnabled(){return estado==EstadoGenerico.ACTIVO;}
}
