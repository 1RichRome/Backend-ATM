package com.bn.atmcontratos.repository;
import com.bn.atmcontratos.model.entity.ActaConformidad; import com.bn.atmcontratos.model.enums.EstadoActa; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ActaConformidadRepository extends JpaRepository<ActaConformidad,Long>{ List<ActaConformidad> findByContratoId(Long id); Optional<ActaConformidad> findByContratoIdAndMesAndAnio(Long c,Integer m,Integer a); List<ActaConformidad> findByMesAndAnio(Integer m,Integer a); List<ActaConformidad> findByEstado(EstadoActa e); }
