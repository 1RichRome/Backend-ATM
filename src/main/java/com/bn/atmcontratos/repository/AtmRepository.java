package com.bn.atmcontratos.repository;

import com.bn.atmcontratos.model.entity.Atm;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AtmRepository extends JpaRepository<Atm,Long>{
 Optional<Atm> findByCodigoTerminalIgnoreCase(String codigoTerminal);
 boolean existsByCodigoTerminalIgnoreCase(String codigoTerminal);
 List<Atm> findByProveedorId(Long proveedorId);

 @Query("select a from Atm a where " +
  "lower(a.codigoTerminal) like lower(concat('%',:q,'%')) or " +
  "lower(a.departamento) like lower(concat('%',:q,'%')) or " +
  "lower(a.ciudad) like lower(concat('%',:q,'%')) or " +
  "lower(a.distrito) like lower(concat('%',:q,'%')) or " +
  "lower(a.ubicacion) like lower(concat('%',:q,'%')) or " +
  "lower(a.nombreOficina) like lower(concat('%',:q,'%'))")
 List<Atm> search(@Param("q") String q);

 @Query("select a from Atm a where a.proveedor.id=:proveedorId and (" +
  "lower(a.codigoTerminal) like lower(concat('%',:q,'%')) or " +
  "lower(a.departamento) like lower(concat('%',:q,'%')) or " +
  "lower(a.ciudad) like lower(concat('%',:q,'%')) or " +
  "lower(a.distrito) like lower(concat('%',:q,'%')) or " +
  "lower(a.ubicacion) like lower(concat('%',:q,'%')) or " +
  "lower(a.nombreOficina) like lower(concat('%',:q,'%')))")
 List<Atm> searchByProveedor(@Param("q") String q, @Param("proveedorId") Long proveedorId);
}
