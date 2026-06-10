package com.bn.atmcontratos.repository;
import com.bn.atmcontratos.model.entity.Proveedor; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface ProveedorRepository extends JpaRepository<Proveedor,Long>{
 Optional<Proveedor> findByRuc(String ruc); Optional<Proveedor> findByRazonSocialIgnoreCase(String r);
 @Query("select p from Proveedor p where lower(p.ruc) like lower(concat('%',:q,'%')) or lower(p.razonSocial) like lower(concat('%',:q,'%')) or lower(p.nombreContacto) like lower(concat('%',:q,'%'))")
 List<Proveedor> search(@Param("q") String q);
}
