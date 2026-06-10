package com.bn.atmcontratos.repository;
import com.bn.atmcontratos.model.entity.HistorialEvento; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface HistorialEventoRepository extends JpaRepository<HistorialEvento,Long>{ List<HistorialEvento> findByEntidadAndEntidadIdOrderByFechaHoraDesc(String e,Long id); }
