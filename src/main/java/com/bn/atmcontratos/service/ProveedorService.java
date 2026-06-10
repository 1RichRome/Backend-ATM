package com.bn.atmcontratos.service;
import com.bn.atmcontratos.model.entity.*; import com.bn.atmcontratos.repository.*; import lombok.RequiredArgsConstructor; import org.springframework.beans.BeanUtils; import org.springframework.stereotype.Service; import java.util.*;
@Service @RequiredArgsConstructor public class ProveedorService{
 private final ProveedorRepository repo; private final HistorialService hist;
 public List<Proveedor> list(String q){return q==null||q.isBlank()?repo.findAll():repo.search(q);}
 public Proveedor get(Long id){return repo.findById(id).orElseThrow(()->new RuntimeException("Proveedor no encontrado"));}
 public Proveedor save(Proveedor p){Proveedor s=repo.save(p); hist.log("PROVEEDOR",s.getId(),"GUARDAR","Registro/actualización de proveedor",null,s); return s;}
 public Proveedor update(Long id,Proveedor p){Proveedor db=get(id); BeanUtils.copyProperties(p,db,"id","fechaRegistro","contratos"); return save(db);}
 public void delete(Long id){Proveedor p=get(id); repo.delete(p); hist.log("PROVEEDOR",id,"ELIMINAR","Eliminación de proveedor",p,null);}
}
