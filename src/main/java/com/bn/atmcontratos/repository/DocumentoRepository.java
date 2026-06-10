package com.bn.atmcontratos.repository;
import com.bn.atmcontratos.model.entity.Documento; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface DocumentoRepository extends JpaRepository<Documento,Long>{ List<Documento> findByContratoId(Long id); List<Documento> findByAtmId(Long id); List<Documento> findByProveedorId(Long id); List<Documento> findByActaId(Long id); }
