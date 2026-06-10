package com.bn.atmcontratos.repository;
import com.bn.atmcontratos.model.entity.Contrato; import com.bn.atmcontratos.model.enums.EstadoContrato; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.*; import java.util.*;
public interface ContratoRepository extends JpaRepository<Contrato,Long>{
 List<Contrato> findByEstadoContrato(EstadoContrato e); List<Contrato> findByFechaFinBetween(LocalDate a, LocalDate b); List<Contrato> findByProveedorId(Long id);
 @Query("select distinct c from Contrato c left join c.proveedor p left join c.atms a where lower(c.numeroContrato) like lower(concat('%',:q,'%')) or lower(c.nombreContrato) like lower(concat('%',:q,'%')) or lower(p.razonSocial) like lower(concat('%',:q,'%')) or lower(a.codigoTerminal) like lower(concat('%',:q,'%'))")
 List<Contrato> search(@Param("q") String q);

 @Query("select distinct c from Contrato c join c.atms a where c.proveedor.id=:proveedorId and a.id in :atmIds")
 List<Contrato> findByProveedorAndAtmIds(@Param("proveedorId") Long proveedorId, @Param("atmIds") Collection<Long> atmIds);
}
