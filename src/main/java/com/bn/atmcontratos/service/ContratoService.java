package com.bn.atmcontratos.service;

import com.bn.atmcontratos.dto.*;
import com.bn.atmcontratos.model.entity.*;
import com.bn.atmcontratos.model.enums.*;
import com.bn.atmcontratos.repository.*;
import com.bn.atmcontratos.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ContratoService{
 private final ContratoRepository repo;
 private final ProveedorRepository proveedores;
 private final AtmRepository atms;
 private final ActaConformidadRepository actas;
 private final DocumentoRepository docs;
 private final HistorialService hist;

 public List<Contrato> list(String q, EstadoContrato e, Long p){
  List<Contrato> l=q==null||q.isBlank()?repo.findAll():repo.search(q);
  l.forEach(c->{
   normalizarContrato(c);
   c.recalcular();
  });
  if(e!=null) l=l.stream().filter(c->c.getEstadoContrato()==e).toList();
  if(p!=null) l=l.stream().filter(c->c.getProveedor()!=null&&p.equals(c.getProveedor().getId())).toList();
  return l;
 }

 public Contrato get(Long id){
  Contrato c=repo.findById(id).orElseThrow(()->new RuntimeException("Contrato no encontrado"));
  normalizarContrato(c);
  c.recalcular();
  return c;
 }

 public Contrato create(ContratoRequest r){
  Contrato c=new Contrato();
  apply(c,r);
  Contrato s=repo.save(c);
  hist.log("CONTRATO",s.getId(),"CREAR","Creación de contrato",null,s);
  return s;
 }

 public Contrato update(Long id,ContratoRequest r){
  Contrato c=get(id);
  apply(c,r);
  Contrato s=repo.save(c);
  hist.log("CONTRATO",id,"EDITAR","Edición de contrato",null,s);
  return s;
 }

 @Transactional
 public void delete(Long id){
  Contrato c=get(id);
  List<Documento> documentos=new ArrayList<>(docs.findByContratoId(id));
  for(Documento d:documentos){ deletePhysical(d.getRutaArchivo()); docs.delete(d); }
  for(ActaConformidad a:new ArrayList<>(actas.findByContratoId(id))){
   deletePhysical(a.getRutaArchivoGenerado());
   deletePhysical(a.getRutaArchivoPdf());
   deletePhysical(a.getRutaArchivoFirmado());
   actas.delete(a);
  }
  c.getAtms().clear();
  repo.save(c);
  repo.delete(c);
  hist.log("CONTRATO",id,"ELIMINAR","Eliminación de contrato, actas y documentos asociados",c,null);
 }

 private void deletePhysical(String ruta){
  if(ruta==null || ruta.isBlank()) return;
  try{Files.deleteIfExists(Path.of(ruta));}catch(Exception ignored){}
 }

 public Contrato addAtms(Long id,AsociarAtmsRequest r){
  Contrato c=get(id);
  List<Atm> seleccionados=atms.findAllById(r.getAtmIds());
  if(c.getProveedor()!=null){
   for(Atm a:seleccionados){
    if(a.getProveedor()!=null&&!Objects.equals(a.getProveedor().getId(),c.getProveedor().getId())){
     throw new RuntimeException("El ATM "+a.getCodigoTerminal()+" pertenece a otro proveedor.");
    }
   }
  }
  c.getAtms().addAll(seleccionados);
  normalizarContrato(c);
  c.recalcular();
  return repo.save(c);
 }

 public Contrato removeAtm(Long id,Long atmId){
  Contrato c=get(id);
  c.getAtms().removeIf(a->a.getId().equals(atmId));
  normalizarContrato(c);
  c.recalcular();
  return repo.save(c);
 }

 public List<Contrato> vencidos(){return repo.findByEstadoContrato(EstadoContrato.VENCIDO);}
 public List<Contrato> porVencer(int d){return repo.findByFechaFinBetween(LocalDate.now(),LocalDate.now().plusDays(d));}

 void apply(Contrato c,ContratoRequest r){
  c.setNumeroInterno(r.numeroInterno);
  c.setNumeroContrato(r.numeroContrato);
  c.setNombreContrato(r.nombreContrato);
  c.setDescripcionContrato(r.descripcionContrato);
  c.setTipoContrato(r.tipoContrato);
  c.setResponsableActas(r.responsableActas);
  c.setMeses(r.meses);
  c.setRentaMensual(r.rentaMensual);
  c.setImpuesto(r.impuesto);
  c.setIncluyeIgv(Boolean.TRUE.equals(r.incluyeIgv));
  c.setOtrosGastos(r.otrosGastos);
  c.setMontoTotalMensual(r.montoTotalMensual);
  c.setMontoContrato(r.montoContrato);
  c.setCuentaContableRenta(def(r.cuentaContableRenta,"4513.01.10.01"));
  c.setCentroCostoRenta(def(r.centroCostoRenta,"5500"));
  c.setAreaPresupuestoRenta(def(r.areaPresupuestoRenta,"5500"));
  c.setTipoGastoRenta(def(r.tipoGastoRenta,"000000000000"));
  c.setCuentaContableGastoComun(def(r.cuentaContableGastoComun,"4513.01.20.04"));
  c.setCentroCostoGastoComun(def(r.centroCostoGastoComun,"5500"));
  c.setAreaPresupuestoGastoComun(def(r.areaPresupuestoGastoComun,"5500"));
  c.setTipoGastoGastoComun(def(r.tipoGastoGastoComun,"SE000000251"));
  c.setMoneda(r.moneda);
  c.setFechaSuscripcion(r.fechaSuscripcion);
  c.setFechaInicio(r.fechaInicio);
  c.setFechaFin(r.fechaFin);
  c.setObservaciones(r.observaciones);
  if(r.proveedorId!=null){
   c.setProveedor(proveedores.findById(r.proveedorId).orElseThrow(()->new RuntimeException("Proveedor no encontrado")));
  }else{
   c.setProveedor(null);
  }
  if(r.atmIds!=null){
   List<Atm> seleccionados=r.atmIds.isEmpty()?new ArrayList<>():atms.findAllById(r.atmIds);
   if(c.getProveedor()!=null){
    for(Atm a:seleccionados){
     if(a.getProveedor()!=null && !Objects.equals(a.getProveedor().getId(), c.getProveedor().getId())){
      throw new RuntimeException("El ATM "+a.getCodigoTerminal()+" pertenece a otro proveedor. Primero actualice el enlace ATM-Proveedor.");
     }
    }
   }
   c.setAtms(new LinkedHashSet<>(seleccionados));
  }
  normalizarContrato(c);
  c.recalcular();
 }

 public Map<String,Object> importFile(MultipartFile file)throws Exception{
  int ins=0,upd=0,err=0;
  List<String> errors=new ArrayList<>();
  for(Map<String,String> r:ExcelUtil.read(file)){
   try{
    String nc=ExcelUtil.get(r,"contrato","numeroContrato");
    if(nc==null) throw new RuntimeException("sin contrato");
    Proveedor p=null;
    String arr=ExcelUtil.get(r,"Arrendador","proveedor");
    if(arr!=null) p=proveedores.findByRazonSocialIgnoreCase(arr).orElseGet(()->proveedores.save(Proveedor.builder().razonSocial(arr).build()));
    Contrato c=new Contrato();
    c.setNumeroContrato(nc);
    c.setNombreContrato(nc);
    c.setTipoContrato(ExcelUtil.get(r,"TIPO"));
    c.setResponsableActas(ExcelUtil.get(r,"Responsable Actas"));
    c.setMeses(ExcelUtil.get(r,"meses"));
    c.setRentaMensual(ExcelUtil.money(ExcelUtil.get(r,"Renta Mensual")));
    String impuestoTexto=ExcelUtil.get(r,"Impuesto");
    c.setImpuesto(impuestoTexto);
    c.setIncluyeIgv(detectarIncluyeIgv(impuestoTexto));
    c.setOtrosGastos(ExcelUtil.money(ExcelUtil.get(r,"Otros gastos")));
    c.setMontoTotalMensual(ExcelUtil.money(ExcelUtil.get(r,"Monto total","Monto toral")));
    c.setMontoContrato(ExcelUtil.money(ExcelUtil.get(r,"MontoContrato")));
    c.setFechaSuscripcion(ExcelUtil.date(ExcelUtil.get(r,"Fecha Suscripcion","Fecha Suscripción","Suscripcion","Suscripción")));
    c.setFechaInicio(ExcelUtil.date(ExcelUtil.get(r,"Inicio")));
    c.setFechaFin(ExcelUtil.date(ExcelUtil.get(r,"Fin")));
    c.setProveedor(p);
    String caj=ExcelUtil.get(r,"CAJERO");
    if(caj!=null){
     for(String cod:caj.replace(".","").split(",")){
      String x=cod.trim();
      atms.findByCodigoTerminalIgnoreCase(x).or(()->atms.findByCodigoTerminalIgnoreCase("S1AD"+x)).ifPresent(a->c.getAtms().add(a));
     }
    }
    normalizarContrato(c);
    c.recalcular();
    repo.save(c);
    ins++;
   }catch(Exception e){
    err++;
    errors.add(e.getMessage());
   }
  }
  Map<String,Object> m=new LinkedHashMap<>();
  m.put("insertados",ins);
  m.put("actualizados",upd);
  m.put("errores",err);
  m.put("detalleErrores",errors);
  return m;
 }

 private String def(String v, String fallback){
  return v==null || v.isBlank()?fallback:v;
 }

 private void normalizarContrato(Contrato c){
  if(c.getIncluyeIgv()==null) c.setIncluyeIgv(false);
  if(c.getMoneda()==null || c.getMoneda().isBlank()) c.setMoneda("S/");
  if(c.getEstadoContrato()==null) c.setEstadoContrato(EstadoContrato.VIGENTE);
  c.aplicarDefaultsContables();
 }

 private boolean detectarIncluyeIgv(String texto){
  if(texto==null || texto.isBlank()) return false;
  String t=texto.toLowerCase(Locale.ROOT)
   .replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u")
   .trim();
  return t.contains("incluido igv") || t.contains("incluye igv") || t.contains("con igv") || t.contains("incluido impuesto") || t.contains("incluye impuesto");
 }
}
