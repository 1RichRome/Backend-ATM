package com.bn.atmcontratos.repository;
import com.bn.atmcontratos.model.entity.Usuario; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface UsuarioRepository extends JpaRepository<Usuario,Long>{ Optional<Usuario> findByUsername(String u); boolean existsByUsername(String u); }
