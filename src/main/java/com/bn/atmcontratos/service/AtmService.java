package com.bn.atmcontratos.service;

import com.bn.atmcontratos.model.entity.*;
import com.bn.atmcontratos.repository.*;
import com.bn.atmcontratos.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.math.*;
import java.util.*;

@Service @RequiredArgsConstructor public class AtmService{
 private final AtmRepository repo;
 private final ProveedorRepository proveedores;
 private final HistorialService hist;

 public List<Atm> list(String q, Long proveedorId){
  if(proveedorId!=null){
   return q==null||q.isBlank()?repo.findByProveedorId(proveedorId):repo.searchByProveedor(q,proveedorId);
  }
  return q==null||q.isBlank()?repo.findAll():repo.search(q);
 }
 public Atm get(Long id){return repo.findById(id).orElseThrow(()->new RuntimeException("ATM no encontrado"));}
 public Atm save(Atm a){
  if(a.getId()==null && repo.existsByCodigoTerminalIgnoreCase(a.getCodigoTerminal()))throw new RuntimeException("ATM duplicado");
  if(a.getProveedor()!=null && a.getProveedor().getId()!=null){
   a.setProveedor(proveedores.findById(a.getProveedor().getId()).orElseThrow(()->new RuntimeException("Proveedor no encontrado")));
  }
  Atm s=repo.save(a);
  hist.log("ATM",s.getId(),"GUARDAR","Registro/actualización de ATM",null,s);
  return s;
 }
 public Atm update(Long id,Atm a){Atm db=get(id); BeanUtils.copyProperties(a,db,"id","fechaRegistro","contratos"); return save(db);}
 public void delete(Long id){Atm a=get(id); repo.delete(a); hist.log("ATM",id,"ELIMINAR","Eliminación de ATM",a,null);}
 public Map<String,Object> importFile(MultipartFile file)throws Exception{int ins=0,upd=0,err=0; List<String> errors=new ArrayList<>(); for(Map<String,String> r:ExcelUtil.read(file)){try{String codigo=ExcelUtil.get(r,"cTerminal","codigoTerminal","terminal"); if(codigo==null)throw new RuntimeException("sin código terminal"); Atm a=repo.findByCodigoTerminalIgnoreCase(codigo).orElseGet(Atm::new); boolean nuevo=a.getId()==null; a.setCodigoTerminal(codigo); a.setDetalleLobby(ExcelUtil.get(r,"cDet_Lobby")); a.setDepartamento(ExcelUtil.get(r,"cDepartamento","departamento")); a.setCiudad(ExcelUtil.get(r,"cCiudad","ciudad")); a.setDistrito(ExcelUtil.get(r,"cDistrito","distrito")); a.setDireccion(ExcelUtil.get(r,"cDireccion","direccion")); a.setUbicacion(ExcelUtil.get(r,"cUbicación","cUbicacion","ubicacion")); a.setTipoAtmIsla(ExcelUtil.get(r,"ATM BN / ISLA","tipo")); a.setCodigoAgencia(ExcelUtil.get(r,"CODIGO AGENCIA")); a.setNombreOficina(ExcelUtil.get(r,"cNom_Oficina")); a.setTipoOficina(ExcelUtil.get(r,"cTipo_Oficina")); a.setRegion(ExcelUtil.get(r,"cDet_Region")); a.setUbigeo(ExcelUtil.get(r,"cUbigeo")); a.setCoordenadaX(dec(ExcelUtil.get(r,"Coord_X"))); a.setCoordenadaY(dec(ExcelUtil.get(r,"Coord_Y"))); repo.save(a); if(nuevo)ins++; else upd++;}catch(Exception e){err++; errors.add(e.getMessage());}} Map<String,Object> m=new LinkedHashMap<>(); m.put("insertados",ins); m.put("actualizados",upd); m.put("errores",err); m.put("detalleErrores",errors); return m;}
 BigDecimal dec(String s){try{return s==null?null:new BigDecimal(s.trim());}catch(Exception e){return null;}}
}
