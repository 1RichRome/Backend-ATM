package com.bn.atmcontratos.service;

import com.bn.atmcontratos.dto.*;
import com.bn.atmcontratos.model.entity.*;
import com.bn.atmcontratos.model.enums.*;
import com.bn.atmcontratos.repository.*;
import com.bn.atmcontratos.util.*;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.*;

@Service
@RequiredArgsConstructor
public class ActaService {
 private final ActaConformidadRepository repo;
 private final ContratoRepository contratos;
 private final DocumentoRepository docs;
 private final HistorialService hist;

 @Value("${app.upload-dir:uploads}") String upload;
 @Value("${app.actas-dir:actas_generadas}") String actasDir;
 private static final Locale ES_PE = new Locale("es", "PE");

 public List<ActaConformidad> list(){return repo.findAll();}
 public ActaConformidad get(Long id){return repo.findById(id).orElseThrow(()->new RuntimeException("Acta no encontrada"));}

 @Transactional
 public void delete(Long id) throws Exception{
  ActaConformidad a=get(id);
  List<Documento> relacionados=new ArrayList<>(docs.findByActaId(id));
  for(Documento d:relacionados){
   deletePhysical(d.getRutaArchivo());
   docs.delete(d);
  }
  deletePhysical(a.getRutaArchivoGenerado());
  deletePhysical(a.getRutaArchivoPdf());
  deletePhysical(a.getRutaArchivoFirmado());
  repo.delete(a);
  hist.log("ACTA",id,"ELIMINAR","Eliminación de acta y archivos locales relacionados",a,null);
 }

 private void deletePhysical(String ruta){
  if(ruta==null || ruta.isBlank()) return;
  try{Files.deleteIfExists(Path.of(ruta));}catch(Exception ignored){}
 }

 public List<MesActaResponse> calendarioContrato(Long contratoId, Integer anio){
  int year=anio==null?LocalDate.now().getYear():anio;
  Map<Integer,ActaConformidad> existentes=repo.findByContratoId(contratoId).stream()
   .filter(a->Objects.equals(a.getAnio(),year))
   .collect(Collectors.toMap(ActaConformidad::getMes, a->a, (a,b)->a, LinkedHashMap::new));
  List<MesActaResponse> out=new ArrayList<>();
  for(int mes=1;mes<=12;mes++){
   ActaConformidad a=existentes.get(mes);
   out.add(MesActaResponse.builder()
    .mes(mes)
    .nombreMes(nombreMes(mes))
    .anio(year)
    .estado(a==null?"PENDIENTE":a.getEstado().name())
    .actaId(a==null?null:a.getId())
    .estadoActa(a==null?null:a.getEstado())
    .nombreArchivoGenerado(a==null?null:a.getNombreArchivoGenerado())
    .nombreArchivoPdf(a==null?null:a.getNombreArchivoPdf())
    .nombreArchivoFirmado(a==null?null:a.getNombreArchivoFirmado())
    .tieneActaGenerada(a!=null && a.getNombreArchivoGenerado()!=null)
    .tieneActaPdf(a!=null && a.getNombreArchivoPdf()!=null)
    .tieneActaFirmada(a!=null && a.getNombreArchivoFirmado()!=null)
    .fechaGeneracion(a==null?null:a.getFechaGeneracion())
    .fechaSubidaFirmada(a==null?null:a.getFechaSubidaFirmada())
    .build());
  }
  return out;
 }

 /**
  * Genera el acta mensual y guarda dos archivos en una carpeta local:
  *  - DOCX editable
  *  - PDF listo para revisión/impresión
  */
 public ActaConformidad generar(ActaGenerarRequest r)throws Exception{
  validarRequest(r);
  repo.findByContratoIdAndMesAndAnio(r.contratoId,r.mes,r.anio).ifPresent(a->{throw new RuntimeException("Ya existe acta para ese periodo");});
  Contrato c=contratos.findById(r.contratoId).orElseThrow(()->new RuntimeException("Contrato no encontrado"));
  int cantidadEspacios=cantidadAtmsParaRequest(c,r.atmIds);
  BigDecimal montoPagarDefault=calcularMontoPagoMensual(c,cantidadEspacios);

  ActaConformidad a=ActaConformidad.builder()
   .contrato(c).mes(r.mes).anio(r.anio)
   .fechaEmision(r.fechaEmision==null?LocalDate.now():r.fechaEmision)
   .periodoServicioInicio(r.periodoServicioInicio)
   .periodoServicioFin(r.periodoServicioFin)
   .montoPagar(r.montoPagar==null?montoPagarDefault:r.montoPagar)
   .moneda(def(r.moneda,c.getMoneda()==null?"S/":c.getMoneda()))
   .tienePenalidadMora(Boolean.TRUE.equals(r.tienePenalidadMora))
   .tieneOtrasPenalidades(Boolean.TRUE.equals(r.tieneOtrasPenalidades) || Boolean.TRUE.equals(r.tienePenalidades))
   .tienePenalidades(Boolean.TRUE.equals(r.tienePenalidadMora) || Boolean.TRUE.equals(r.tieneOtrasPenalidades) || Boolean.TRUE.equals(r.tienePenalidades))
   .atmIdsIncluidos(joinIds(r.atmIds))
   .adjuntaInformePenalidades(Boolean.TRUE.equals(r.adjuntaInformePenalidades))
   .adjuntaOtroInforme(Boolean.TRUE.equals(r.adjuntaOtroInforme))
   .ciudadEmision(def(r.ciudadEmision,"Lima"))
   .areaResponsable(def(r.areaResponsable,"Sección de Canales Presenciales"))
   .cuentaContable(def(r.cuentaContable,"4513.01.10.01"))
   .centroCosto(def(r.centroCosto,"6220"))
   .areaPresupuesto(def(r.areaPresupuesto,"5500"))
   .tipoGasto(def(r.tipoGasto,"000000000000"))
   .observaciones(r.observaciones)
   .estado(EstadoActa.GENERADA)
   .build();
  a=repo.save(a);

  Path dir=directorioActa(c,a);
  Files.createDirectories(dir);

  String baseName=FileUtil.clean("ACTA_CONFORMIDAD_"+safeFile(c.getNumeroContrato())+"_"+nombreMes(r.mes).toUpperCase(Locale.ROOT)+"_"+r.anio);
  String docxName=baseName+".docx";
  String pdfName=baseName+".pdf";
  Path docxPath=dir.resolve(docxName);
  Path pdfPath=dir.resolve(pdfName);

  try(OutputStream out=Files.newOutputStream(docxPath)){generarDocx(a,out);}
  try(OutputStream out=Files.newOutputStream(pdfPath)){generarPdf(a,out);}

  a.setNombreArchivoGenerado(docxName); a.setRutaArchivoGenerado(docxPath.toString());
  a.setNombreArchivoPdf(pdfName); a.setRutaArchivoPdf(pdfPath.toString());
  a=repo.save(a);

  docs.save(Documento.builder().acta(a).contrato(c).tipoDocumento(TipoDocumento.ACTA_GENERADA).nombreArchivo(docxName).rutaArchivo(docxPath.toString()).extension("docx").tamanio(Files.size(docxPath)).descripcion("Acta generada editable "+nombreMes(r.mes)+" "+r.anio).usuarioSubida("sistema").build());
  docs.save(Documento.builder().acta(a).contrato(c).tipoDocumento(TipoDocumento.ACTA_GENERADA).nombreArchivo(pdfName).rutaArchivo(pdfPath.toString()).extension("pdf").tamanio(Files.size(pdfPath)).descripcion("Acta generada PDF "+nombreMes(r.mes)+" "+r.anio).usuarioSubida("sistema").build());
  hist.log("ACTA",a.getId(),"GENERAR","Generación de acta mensual "+nombreMes(r.mes)+" "+r.anio+" en carpeta local: "+dir,null,a);
  return a;
 }

 public ActaLoteResultadoResponse generarLote(ActaLoteRequest r) throws Exception{
  if(r==null) throw new RuntimeException("Solicitud inválida");
  if(r.proveedorId==null) throw new RuntimeException("Debe seleccionar un proveedor");
  if(r.atmIds==null || r.atmIds.isEmpty()) throw new RuntimeException("Debe seleccionar al menos un cajero del proveedor");
  if(r.mes==null || r.mes<1 || r.mes>12) throw new RuntimeException("Mes inválido");
  if(r.anio==null || r.anio<2000) throw new RuntimeException("Año inválido");

  List<Contrato> relacionados = contratos.findByProveedorAndAtmIds(r.proveedorId, r.atmIds);
  List<Long> actaIds = new ArrayList<>();
  List<String> mensajes = new ArrayList<>();
  int generados = 0;
  int omitidos = 0;

  for(Contrato contrato : relacionados){
   try{
    if(repo.findByContratoIdAndMesAndAnio(contrato.getId(), r.mes, r.anio).isPresent()){
     omitidos++;
     mensajes.add("Omitido: el contrato "+contrato.getNumeroContrato()+" ya tiene acta para "+nombreMes(r.mes)+" "+r.anio);
     continue;
    }
    ActaGenerarRequest gr = new ActaGenerarRequest();
    gr.contratoId = contrato.getId();
    gr.mes = r.mes;
    gr.anio = r.anio;
    gr.fechaEmision = r.fechaEmision;
    gr.periodoServicioInicio = r.periodoServicioInicio;
    gr.periodoServicioFin = r.periodoServicioFin;
    gr.montoPagar = r.montoPagar;
    gr.moneda = r.moneda;
    gr.observaciones = r.observaciones;
    gr.tienePenalidades = r.tienePenalidades;
    gr.tienePenalidadMora = r.tienePenalidadMora;
    gr.tieneOtrasPenalidades = r.tieneOtrasPenalidades;
    gr.adjuntaInformePenalidades = r.adjuntaInformePenalidades;
    gr.adjuntaOtroInforme = r.adjuntaOtroInforme;
    gr.ciudadEmision = r.ciudadEmision;
    gr.areaResponsable = r.areaResponsable;
    gr.cuentaContable = r.cuentaContable;
    gr.centroCosto = r.centroCosto;
    gr.areaPresupuesto = r.areaPresupuesto;
    gr.tipoGasto = r.tipoGasto;
    Set<Long> contratoAtms = contrato.getAtms()==null?Collections.emptySet():contrato.getAtms().stream().map(Atm::getId).collect(Collectors.toSet());
    gr.atmIds = r.atmIds.stream().filter(contratoAtms::contains).collect(Collectors.toList());
    ActaConformidad acta = generar(gr);
    actaIds.add(acta.getId());
    generados++;
    mensajes.add("Generado: "+contrato.getNumeroContrato()+" con "+gr.atmIds.size()+" cajero(s).");
   }catch(Exception ex){
    omitidos++;
    mensajes.add("Error en contrato "+contrato.getNumeroContrato()+": "+ex.getMessage());
   }
  }

  if(relacionados.isEmpty()){
   mensajes.add("No se encontraron contratos vinculados a los cajeros seleccionados para este proveedor.");
  }

  return ActaLoteResultadoResponse.builder()
   .solicitados(r.atmIds.size())
   .generados(generados)
   .omitidos(omitidos)
   .actaIds(actaIds)
   .mensajes(mensajes)
   .build();
 }

 private void validarRequest(ActaGenerarRequest r){
  if(r==null) throw new RuntimeException("Solicitud inválida");
  if(r.contratoId==null) throw new RuntimeException("Debe indicar el contrato");
  if(r.mes==null || r.mes<1 || r.mes>12) throw new RuntimeException("Mes inválido");
  if(r.anio==null || r.anio<2000) throw new RuntimeException("Año inválido");
 }

 private Path directorioActa(Contrato c, ActaConformidad a){
  String proveedor=safeFile(c.getProveedor()==null?"SIN_PROVEEDOR":c.getProveedor().getRazonSocial());
  String contrato=safeFile(c.getNumeroContrato());
  String mesFolder=String.format("%02d-%s",a.getMes(),nombreMes(a.getMes()).toUpperCase(Locale.ROOT));
  return Path.of(actasDir, proveedor, contrato, String.valueOf(a.getAnio()), mesFolder);
 }

 // =====================================================
 // DOCX editable
 // =====================================================
 void generarDocx(ActaConformidad a,OutputStream out)throws Exception{
  ActaTexto x=texto(a);
  try(XWPFDocument d=new XWPFDocument()){
   addLogoDocx(d);
   p(d,"ACTA DE CONFORMIDAD",true,ParagraphAlignment.CENTER);
   p(d,x.parrafoContrato,false,ParagraphAlignment.BOTH);
   p(d,x.parrafoServicio,false,ParagraphAlignment.BOTH);
   addTablaContableDocx(d,a);
   p(d,x.checklist,false,ParagraphAlignment.LEFT);
   if(a.getObservaciones()!=null&&!a.getObservaciones().isBlank())p(d,"Observaciones: "+a.getObservaciones(),false,ParagraphAlignment.BOTH);
   p(d,x.cierre,false,ParagraphAlignment.BOTH);
   addFirmaCierreDocx(d);
   addAnexoDocx(d,a);
   d.write(out);
  }
 }

 // =====================================================
 // PDF oficial, tomando la misma lógica del servicio que compartiste.
 // =====================================================
 void generarPdf(ActaConformidad a, OutputStream out)throws Exception{
  ActaTexto x=texto(a);

  // Formato intermedio A4:
  // - No se pega a los bordes.
  // - La letra no queda demasiado pequeña.
  // - El cuerpo principal ocupa mejor la hoja.
  // - El anexo se mantiene en hoja aparte.
  Document documento=new Document(PageSize.A4,58,58,36,36);
  PdfWriter.getInstance(documento,out);
  documento.open();

  Font fuenteTitulo=FontFactory.getFont(FontFactory.HELVETICA_BOLD,14,Font.UNDERLINE);
  Font fuenteNormal=FontFactory.getFont(FontFactory.HELVETICA,10);
  Font fuenteResaltada=FontFactory.getFont(FontFactory.HELVETICA_BOLD,10);
  Font fuenteTabla=FontFactory.getFont(FontFactory.HELVETICA,8.8f);
  Font fuenteTablaBold=FontFactory.getFont(FontFactory.HELVETICA_BOLD,8.8f);
  Font fuenteChecklist=FontFactory.getFont(FontFactory.HELVETICA,9);

  addLogoPdf(documento);

  Paragraph titulo=new Paragraph("ACTA DE CONFORMIDAD",fuenteTitulo);
  titulo.setAlignment(Element.ALIGN_CENTER);
  titulo.setSpacingBefore(6);
  titulo.setSpacingAfter(18);
  documento.add(titulo);

  Paragraph p1=new Paragraph();
  p1.setFont(fuenteNormal);
  addTextWithBoldValues(p1,x.parrafoContrato,fuenteNormal,fuenteResaltada);
  p1.setAlignment(Element.ALIGN_JUSTIFIED);
  p1.setSpacingAfter(14);
  p1.setLeading(14.2f);
  p1.setIndentationLeft(6);
  p1.setIndentationRight(6);
  documento.add(p1);

  Paragraph p2=new Paragraph();
  p2.setFont(fuenteNormal);
  addTextWithBoldValues(p2,x.parrafoServicio,fuenteNormal,fuenteResaltada);
  p2.setAlignment(Element.ALIGN_JUSTIFIED);
  p2.setSpacingAfter(14);
  p2.setLeading(14.2f);
  p2.setIndentationLeft(6);
  p2.setIndentationRight(6);
  documento.add(p2);

  addTablaContablePdf(documento,a,fuenteTabla,fuenteTablaBold);

  Paragraph checklist=new Paragraph(x.checklist,fuenteChecklist);
  checklist.setLeading(12.4f);
  checklist.setSpacingBefore(12);
  checklist.setSpacingAfter(10);
  checklist.setIndentationLeft(6);
  checklist.setIndentationRight(6);
  documento.add(checklist);

  if(a.getObservaciones()!=null&&!a.getObservaciones().isBlank()){
   Paragraph obs=new Paragraph("Observaciones: "+a.getObservaciones(),fuenteChecklist);
   obs.setAlignment(Element.ALIGN_JUSTIFIED);
   obs.setLeading(12.4f);
   obs.setSpacingAfter(8);
   obs.setIndentationLeft(6);
   obs.setIndentationRight(6);
   documento.add(obs);
  }

  Paragraph cierre=new Paragraph(x.cierre,fuenteNormal);
  cierre.setAlignment(Element.ALIGN_JUSTIFIED);
  cierre.setLeading(13.2f);
  cierre.setSpacingBefore(4);
  cierre.setSpacingAfter(18);
  cierre.setIndentationLeft(6);
  cierre.setIndentationRight(6);
  documento.add(cierre);

  addFirmaCierrePdf(documento,fuenteNormal,fuenteResaltada);

  // El anexo se mantiene en hoja independiente.
  addAnexoPdf(documento,a,fuenteNormal,fuenteResaltada);

  documento.close();
 }

 /**
  * Permite marcar valores dinámicos encerrados con ⟦ ⟧ en negrita dentro del PDF.
  */
 private void addTextWithBoldValues(Paragraph paragraph, String text, Font normal, Font bold){
  String[] parts=text.split("(?=⟦)|(?<=⟧)");
  for(String part:parts){
   if(part.startsWith("⟦") && part.endsWith("⟧")) paragraph.add(new Chunk(part.substring(1,part.length()-1),bold));
   else paragraph.add(new Chunk(part,normal));
  }
 }

 private ActaTexto texto(ActaConformidad a){
  Contrato c=a.getContrato();
  List<Atm> atms=getAtmsParaActa(a);
  int cantidadEspacios=Math.max(1, atms.size());
  String proveedor=c.getProveedor()==null?"":safe(c.getProveedor().getRazonSocial());
  String duracion=calcularDuracionExacta(c.getFechaInicio(),c.getFechaFin(),c.getMeses());
  String monedaSimbolo=simboloMoneda(def(a.getMoneda(),c.getMoneda()));
  String monedaTexto=textoMoneda(def(a.getMoneda(),c.getMoneda()));
  BigDecimal rentaMensual=nullSafe(c.getRentaMensual());
  BigDecimal gastoComun=nullSafe(c.getOtrosGastos());
  BigDecimal montoMensualEspacio=montoMensualEspacio(c);
  BigDecimal montoContratoTotal=calcularMontoContratoTotal(c,cantidadEspacios);
  BigDecimal montoPagoMes=nullSafe(a.getMontoPagar());
  if(montoPagoMes.compareTo(BigDecimal.ZERO)==0) montoPagoMes=calcularMontoPagoMensual(c,cantidadEspacios);
  String montoEspacioLetras=NumeroLetras.moneda(montoMensualEspacio,monedaTexto);
  String montoContratoLetras=NumeroLetras.moneda(montoContratoTotal,monedaTexto);
  String montoPagoLetras=NumeroLetras.moneda(montoPagoMes,monedaTexto);
  String igv=textoIgv(c);
  String docReferencia=referenciaContrato(c);
  String fechaSuscripcion=fmtTexto(c.getFechaSuscripcion()!=null?c.getFechaSuscripcion():c.getFechaInicio());
  String fechaInicio=fmtDash(c.getFechaInicio());
  String fechaFin=fmtDash(c.getFechaFin());
  String periodoServicio=fmtDash(a.getPeriodoServicioInicio())+" al "+fmtDash(a.getPeriodoServicioFin());

  boolean tieneGastoComun=tieneGastosComunes(c);
  String detalleMonto = tieneGastoComun
   ? ", monto compuesto por una renta mensual de ⟦"+monedaSimbolo+" "+formatMoney(rentaMensual)+"⟧ y gasto común de ⟦"+monedaSimbolo+" "+formatMoney(gastoComun)+"⟧"
   : ", correspondiente a la renta mensual del espacio";

  String p1="En la ciudad de ⟦"+def(a.getCiudadEmision(),"Lima")+"⟧, a los ⟦"+fmtLong(a.getFechaEmision())+"⟧, la ⟦"+def(a.getAreaResponsable(),"Sección de Canales Presenciales")+"⟧ ha verificado el cumplimiento de las prestaciones y plazos del Servicio de arrendamiento de espacios para cajeros automáticos (⟦"+textoEspacios(cantidadEspacios)+"⟧), conforme a lo establecido en ⟦"+docReferencia+"⟧, suscrito el ⟦"+fechaSuscripcion+"⟧, la referida adenda tiene como plazo de duración ⟦"+duracion+"⟧ comprendido desde el ⟦"+fechaInicio+" al "+fechaFin+"⟧. El monto mensual por cada espacio asciende a ⟦"+monedaSimbolo+" "+formatMoney(montoMensualEspacio)+"⟧ (⟦"+montoEspacioLetras+"⟧) ⟦"+igv+"⟧"+detalleMonto+", siendo el monto total de ⟦"+monedaSimbolo+" "+formatMoney(montoContratoTotal)+"⟧ (⟦"+montoContratoLetras+"⟧), ⟦"+igv+"⟧, a favor de ⟦"+proveedor+"⟧.";
  String p2="Se deja constancia que el Contratista ejecutó el servicio en su oportunidad, por el periodo del ⟦"+periodoServicio+"⟧, por la suma de ⟦"+monedaSimbolo+" "+formatMoney(montoPagoMes)+"⟧ (⟦"+montoPagoLetras+"⟧) ⟦"+igv+"⟧. El pago afectará a la siguiente combinación contable:";
  String checklist="• Adjunta Informe de Penalidades: "+yn(a.getAdjuntaInformePenalidades())+"\n"+
   "• Adjunta otro Informe de acuerdo a lo establecido en bases integradas, términos de referencia o especificaciones técnicas: "+yn(a.getAdjuntaOtroInforme())+"\n"+
   "• Tiene penalidades por mora: "+yn(a.getTienePenalidadMora())+"\n"+
   "• Tiene otras penalidades: "+yn(a.getTieneOtrasPenalidades())+"\n";
  String cierre="Por medio del presente documento, la dependencia a mi cargo otorga la conformidad del servicio señalado.\nSe expide la presente para los fines pertinentes.";
  return new ActaTexto(p1,p2,checklist,cierre);
 }

 record ActaTexto(String parrafoContrato,String parrafoServicio,String checklist,String cierre){}

 private void addFirmaCierreDocx(XWPFDocument d){
  XWPFParagraph linea=d.createParagraph();
  linea.setAlignment(ParagraphAlignment.CENTER);
  linea.setSpacingBefore(120);
  linea.setSpacingAfter(0);
  XWPFRun rLinea=linea.createRun();
  rLinea.setFontFamily("Arial");
  rLinea.setFontSize(8);
  rLinea.setText("____________________________________");

  pFirmaDocx(d,"JOSE ANTONIO OCHOA SUYCO",false);
  pFirmaDocx(d,"Subgerente (e) - Canales Alternos",false);
  pFirmaDocx(d,"Gerencia Banca Digital",false);

  XWPFParagraph fuente=d.createParagraph();
  fuente.setAlignment(ParagraphAlignment.CENTER);
  fuente.setSpacingBefore(80);
  fuente.setSpacingAfter(0);
  XWPFRun rf=fuente.createRun();
  rf.setFontFamily("Arial");
  rf.setFontSize(7);
  rf.setBold(true);
  rf.setItalic(true);
  rf.setText("Fuente para la actualización del acta: Resolución N° 262-2017-OSCE/PRE");

  XWPFParagraph std=d.createParagraph();
  std.setAlignment(ParagraphAlignment.CENTER);
  std.setSpacingAfter(60);
  XWPFRun rs=std.createRun();
  rs.setFontFamily("Arial");
  rs.setFontSize(7);
  rs.setBold(true);
  rs.setItalic(true);
  rs.setText("STD:");
 }

 private void pFirmaDocx(XWPFDocument d,String text,boolean bold){
  XWPFParagraph par=d.createParagraph();
  par.setAlignment(ParagraphAlignment.CENTER);
  par.setSpacingBefore(0);
  par.setSpacingAfter(0);
  XWPFRun run=par.createRun();
  run.setFontFamily("Arial");
  run.setFontSize(8);
  run.setBold(bold);
  run.setText(text);
 }

 private void addFirmaCierrePdf(Document documento, Font normal, Font bold)throws Exception{
  Font firmaFont=FontFactory.getFont(FontFactory.HELVETICA,9.5f);
  Font fuenteFinal=FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE,8.2f);

  Paragraph linea=new Paragraph("____________________________________",firmaFont);
  linea.setAlignment(Element.ALIGN_CENTER);
  linea.setSpacingBefore(18);
  linea.setSpacingAfter(3);
  documento.add(linea);

  addCenteredPdf(documento,"JOSE ANTONIO OCHOA SUYCO",firmaFont,0,1);
  addCenteredPdf(documento,"Subgerente (e) - Canales Alternos",firmaFont,0,1);
  addCenteredPdf(documento,"Gerencia Banca Digital",firmaFont,0,12);

  addCenteredPdf(documento,"Fuente para la actualización del acta: Resolución N° 262-2017-OSCE/PRE",fuenteFinal,0,2);
  addCenteredPdf(documento,"STD:",fuenteFinal,0,0);
 }

 private void addCenteredPdf(Document documento,String text,Font font,float before,float after)throws Exception{
  Paragraph p=new Paragraph(text,font);
  p.setAlignment(Element.ALIGN_CENTER);
  p.setSpacingBefore(before);
  p.setSpacingAfter(after);
  documento.add(p);
 }

 private void addAnexoDocx(XWPFDocument d, ActaConformidad a){
  List<Atm> atms=getAtmsParaActa(a);
  Contrato c=a.getContrato();
  XWPFParagraph br=d.createParagraph();
  br.createRun().addBreak(BreakType.PAGE);
  p(d,"ANEXO - DETALLE DE CAJEROS ASOCIADOS",true,ParagraphAlignment.CENTER);
  p(d,"Contrato: "+referenciaContrato(c)+"\nProveedor: "+(c.getProveedor()==null?"-":safe(c.getProveedor().getRazonSocial())),false,ParagraphAlignment.LEFT);
  XWPFTable t=d.createTable(Math.max(2,atms.size()+1),7);
  cell(t,0,0,"Item");
  cell(t,0,1,"N° ATM");
  cell(t,0,2,"Ubicación");
  cell(t,0,3,"Distrito");
  cell(t,0,4,"Provincia");
  cell(t,0,5,"Total");
  cell(t,0,6,"Ubicación / Dirección en SISCAL");
  if(atms.isEmpty()){
   cell(t,1,0,"-"); cell(t,1,1,"Sin cajeros asociados"); cell(t,1,2,"-"); cell(t,1,3,"-"); cell(t,1,4,"-"); cell(t,1,5,"-"); cell(t,1,6,"-");
   return;
  }
  String moneda=simboloMoneda(def(a.getMoneda(),c.getMoneda()));
  BigDecimal monto=montoMensualEspacio(c);
  for(int i=0;i<atms.size();i++){
   Atm atm=atms.get(i);
   int r=i+1;
   cell(t,r,0,String.valueOf(r));
   cell(t,r,1,atmLabel(atm));
   cell(t,r,2,def(atm.getUbicacion(),"-"));
   cell(t,r,3,def(atm.getDistrito(),"-"));
   cell(t,r,4,def(atm.getCiudad(),"-"));
   cell(t,r,5,moneda+" "+formatMoney(monto));
   cell(t,r,6,def(atm.getDireccion(),"-"));
  }
 }

 private void addAnexoPdf(Document documento, ActaConformidad a, Font normal, Font bold)throws Exception{
  List<Atm> atms=getAtmsParaActa(a);
  Contrato c=a.getContrato();
  documento.newPage();
  Paragraph titulo=new Paragraph("ANEXO - DETALLE DE CAJEROS ASOCIADOS",bold);
  titulo.setAlignment(Element.ALIGN_CENTER);
  titulo.setSpacingAfter(12);
  documento.add(titulo);
  Paragraph ref=new Paragraph("Contrato: "+referenciaContrato(c)+"\nProveedor: "+(c.getProveedor()==null?"-":safe(c.getProveedor().getRazonSocial())),normal);
  ref.setSpacingAfter(12);
  documento.add(ref);

  PdfPTable tabla=new PdfPTable(7);
  tabla.setWidthPercentage(100);
  tabla.setWidths(new float[]{0.7f,1.2f,2.7f,1.5f,1.5f,1.3f,3.0f});
  addHeaderCell(tabla,"Item",bold);
  addHeaderCell(tabla,"N° ATM",bold);
  addHeaderCell(tabla,"Ubicación",bold);
  addHeaderCell(tabla,"Distrito",bold);
  addHeaderCell(tabla,"Provincia",bold);
  addHeaderCell(tabla,"Total",bold);
  addHeaderCell(tabla,"Ubicación / Dirección en SISCAL",bold);

  String moneda=simboloMoneda(def(a.getMoneda(),c.getMoneda()));
  BigDecimal monto=montoMensualEspacio(c);
  if(atms.isEmpty()){
   addBodyCell(tabla,"-",normal); addBodyCell(tabla,"Sin cajeros asociados",normal); addBodyCell(tabla,"-",normal); addBodyCell(tabla,"-",normal); addBodyCell(tabla,"-",normal); addBodyCell(tabla,"-",normal); addBodyCell(tabla,"-",normal);
  }else{
   for(int i=0;i<atms.size();i++){
    Atm atm=atms.get(i);
    addBodyCell(tabla,String.valueOf(i+1),normal);
    addBodyCell(tabla,atmLabel(atm),normal);
    addBodyCell(tabla,def(atm.getUbicacion(),"-"),normal);
    addBodyCell(tabla,def(atm.getDistrito(),"-"),normal);
    addBodyCell(tabla,def(atm.getCiudad(),"-"),normal);
    addBodyCell(tabla,moneda+" "+formatMoney(monto),normal);
    addBodyCell(tabla,def(atm.getDireccion(),"-"),normal);
   }
  }
  documento.add(tabla);
 }

 private void addHeaderCell(PdfPTable table,String text,Font font){
  PdfPCell cell=new PdfPCell(new Phrase(text==null?"":text,font));
  cell.setPadding(5);
  cell.setHorizontalAlignment(Element.ALIGN_CENTER);
  cell.setBackgroundColor(new java.awt.Color(221,235,247));
  table.addCell(cell);
 }

 private void addBodyCell(PdfPTable table,String text,Font font){
  PdfPCell cell=new PdfPCell(new Phrase(text==null?"":text,font));
  cell.setPadding(4);
  table.addCell(cell);
 }

 private void addTablaContableDocx(XWPFDocument d, ActaConformidad a){
  Contrato c=a.getContrato();
  int cantidadEspacios=Math.max(1,getAtmsParaActa(a).size());
  String moneda=simboloMoneda(def(a.getMoneda(),c.getMoneda()));
  BigDecimal totalRenta=totalRentaPeriodo(c,cantidadEspacios);
  BigDecimal totalGasto=totalGastoComunPeriodo(c,cantidadEspacios);
  boolean tieneGastoComun=tieneGastosComunes(c);

  XWPFTable t=d.createTable(6,tieneGastoComun?3:2);
  cell(t,0,0,"");
  cell(t,0,1,"Renta");
  if(tieneGastoComun) cell(t,0,2,"Gastos Comunes");

  cell(t,1,0,"Cuenta Contable");
  cell(t,1,1,def(c.getCuentaContableRenta(),"4513.01.10.01"));
  if(tieneGastoComun) cell(t,1,2,def(c.getCuentaContableGastoComun(),"4513.01.20.04"));

  cell(t,2,0,"Centro de Costo");
  cell(t,2,1,def(c.getCentroCostoRenta(),"5500"));
  if(tieneGastoComun) cell(t,2,2,def(c.getCentroCostoGastoComun(),"5500"));

  cell(t,3,0,"Área de Presupuesto");
  cell(t,3,1,def(c.getAreaPresupuestoRenta(),"5500"));
  if(tieneGastoComun) cell(t,3,2,def(c.getAreaPresupuestoGastoComun(),"5500"));

  cell(t,4,0,"Tipo de Gasto");
  cell(t,4,1,def(c.getTipoGastoRenta(),"000000000000"));
  if(tieneGastoComun) cell(t,4,2,def(c.getTipoGastoGastoComun(),"SE000000251"));

  cell(t,5,0,"Total");
  cell(t,5,1,moneda+" "+formatMoney(totalRenta));
  if(tieneGastoComun) cell(t,5,2,moneda+" "+formatMoney(totalGasto));
 }

 private void addTablaContablePdf(Document documento, ActaConformidad a, Font normal, Font bold)throws Exception{
  Contrato c=a.getContrato();
  int cantidadEspacios=Math.max(1,getAtmsParaActa(a).size());
  String moneda=simboloMoneda(def(a.getMoneda(),c.getMoneda()));
  BigDecimal totalRenta=totalRentaPeriodo(c,cantidadEspacios);
  BigDecimal totalGasto=totalGastoComunPeriodo(c,cantidadEspacios);
  boolean tieneGastoComun=tieneGastosComunes(c);

  PdfPTable tabla=new PdfPTable(tieneGastoComun?3:2);
  tabla.setWidthPercentage(tieneGastoComun?66:52);
  tabla.setHorizontalAlignment(Element.ALIGN_CENTER);
  tabla.setSpacingBefore(2);
  tabla.setSpacingAfter(12);
  tabla.setWidths(tieneGastoComun?new float[]{2.15f,1.70f,1.90f}:new float[]{2.15f,1.75f});

  addContableCell(tabla,"",bold,true);
  addContableCell(tabla,"Renta",bold,true);
  if(tieneGastoComun) addContableCell(tabla,"Gastos Comunes",bold,true);

  addContableCell(tabla,"Cuenta Contable",normal,false);
  addContableCell(tabla,def(c.getCuentaContableRenta(),"4513.01.10.01"),normal,false);
  if(tieneGastoComun) addContableCell(tabla,def(c.getCuentaContableGastoComun(),"4513.01.20.04"),normal,false);

  addContableCell(tabla,"Centro de Costo",normal,false);
  addContableCell(tabla,def(c.getCentroCostoRenta(),"5500"),normal,false);
  if(tieneGastoComun) addContableCell(tabla,def(c.getCentroCostoGastoComun(),"5500"),normal,false);

  addContableCell(tabla,"Área de Presupuesto",normal,false);
  addContableCell(tabla,def(c.getAreaPresupuestoRenta(),"5500"),normal,false);
  if(tieneGastoComun) addContableCell(tabla,def(c.getAreaPresupuestoGastoComun(),"5500"),normal,false);

  addContableCell(tabla,"Tipo de Gasto",normal,false);
  addContableCell(tabla,def(c.getTipoGastoRenta(),"000000000000"),normal,false);
  if(tieneGastoComun) addContableCell(tabla,def(c.getTipoGastoGastoComun(),"SE000000251"),normal,false);

  addContableCell(tabla,"Total",bold,false);
  addContableCell(tabla,moneda+" "+formatMoney(totalRenta),bold,false);
  if(tieneGastoComun) addContableCell(tabla,moneda+" "+formatMoney(totalGasto),bold,false);

  documento.add(tabla);
 }

 private void addContableCell(PdfPTable table, String text, Font font, boolean header){
  PdfPCell cell=new PdfPCell(new Phrase(text==null?"":text,font));
  cell.setPadding(4.2f);
  cell.setBorder(Rectangle.BOX);
  if(header) cell.setBackgroundColor(new java.awt.Color(230,230,230));
  table.addCell(cell);
 }

 private boolean tieneGastosComunes(Contrato c){
  return c!=null && c.getOtrosGastos()!=null && c.getOtrosGastos().compareTo(BigDecimal.ZERO)>0;
 }

 private String textoEspacios(int cantidad){
  return cantidad==1?"1 espacio":cantidad+" espacios";
 }

 private int cantidadAtmsParaRequest(Contrato c, List<Long> ids){
  if(ids!=null && !ids.isEmpty()) return Math.max(1,(int)ids.stream().filter(Objects::nonNull).distinct().count());
  int size=c.getAtms()==null?0:c.getAtms().size();
  return Math.max(1,size);
 }

 private BigDecimal montoMensualEspacio(Contrato c){
  if(c.getMontoTotalMensual()!=null && c.getMontoTotalMensual().compareTo(BigDecimal.ZERO)>0) return c.getMontoTotalMensual();
  return nullSafe(c.getRentaMensual()).add(nullSafe(c.getOtrosGastos()));
 }

 private BigDecimal calcularMontoPagoMensual(Contrato c, int cantidadEspacios){
  return montoMensualEspacio(c).multiply(BigDecimal.valueOf(Math.max(1,cantidadEspacios)));
 }

 private BigDecimal totalRentaPeriodo(Contrato c, int cantidadEspacios){
  return nullSafe(c.getRentaMensual()).multiply(BigDecimal.valueOf(Math.max(1,cantidadEspacios)));
 }

 private BigDecimal totalGastoComunPeriodo(Contrato c, int cantidadEspacios){
  return nullSafe(c.getOtrosGastos()).multiply(BigDecimal.valueOf(Math.max(1,cantidadEspacios)));
 }

 private BigDecimal calcularMontoContratoTotal(Contrato c, int cantidadEspacios){
  if(c.getMontoContrato()!=null && c.getMontoContrato().compareTo(BigDecimal.ZERO)>0) return c.getMontoContrato();
  int meses=duracionMeses(c);
  return calcularMontoPagoMensual(c,cantidadEspacios).multiply(BigDecimal.valueOf(Math.max(1,meses)));
 }

 private int duracionMeses(Contrato c){
  if(c.getMeses()!=null){
   java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)").matcher(c.getMeses());
   if(m.find()){
    try{return Math.max(1,Integer.parseInt(m.group(1)));}catch(Exception ignored){}
   }
  }
  if(c.getFechaInicio()!=null && c.getFechaFin()!=null){
   Period p=Period.between(c.getFechaInicio(),c.getFechaFin().plusDays(1));
   int meses=p.getYears()*12+p.getMonths();
   if(p.getDays()>0) meses++;
   return Math.max(1,meses);
  }
  return 1;
 }

 private String textoIgv(Contrato c){
  return Boolean.TRUE.equals(c.getIncluyeIgv())?"con IGV":"sin IGV";
 }

 private String referenciaContrato(Contrato c){
  String nombre=def(c.getNombreContrato(),"");
  if(!nombre.isBlank()) return nombre;
  return "Contrato N° "+def(c.getNumeroContrato(),"S/N");
 }

 String fmtDash(LocalDate d){return d==null?"---":d.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));}
 String fmtTexto(LocalDate d){return d==null?"---":d.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",ES_PE));}

 private List<Atm> getAtmsParaActa(ActaConformidad a){
  Contrato c=a.getContrato();
  List<Atm> todos=new ArrayList<>(c.getAtms()==null?Collections.emptySet():c.getAtms());
  if(a.getAtmIdsIncluidos()==null || a.getAtmIdsIncluidos().isBlank()) return todos;
  Set<Long> ids=Arrays.stream(a.getAtmIdsIncluidos().split(","))
   .map(String::trim)
   .filter(x->!x.isBlank())
   .map(x->{try{return Long.valueOf(x);}catch(Exception e){return null;}})
   .filter(Objects::nonNull)
   .collect(Collectors.toSet());
  return todos.stream().filter(atm->ids.contains(atm.getId())).collect(Collectors.toList());
 }

 private String joinIds(List<Long> ids){
  if(ids==null || ids.isEmpty()) return null;
  return ids.stream().filter(Objects::nonNull).distinct().map(String::valueOf).collect(Collectors.joining(","));
 }

 private URL logoUrl(){
  URL u=getClass().getResource("/static/img/ic_banco.png");
  if(u==null) u=getClass().getResource("/static/img/ic_banco.png");
  return u;
 }

 private void addLogoPdf(Document documento){
  try{
   URL u=logoUrl();
   if(u==null) return;
   Image logo=Image.getInstance(u);
   logo.scaleToFit(132,52);
   logo.setAlignment(Element.ALIGN_LEFT);
   logo.setSpacingAfter(0);
   documento.add(logo);
  }catch(Exception ignored){}
 }

 private void addLogoDocx(XWPFDocument d){
  try{
   URL u=logoUrl();
   if(u==null) return;
   XWPFParagraph par=d.createParagraph();
   par.setAlignment(ParagraphAlignment.LEFT);
   XWPFRun run=par.createRun();
   try(InputStream in=u.openStream()){
    run.addPicture(in, XWPFDocument.PICTURE_TYPE_PNG, "ic_banco.png", Units.toEMU(150), Units.toEMU(55));
   }
  }catch(Exception ignored){}
 }

 private void addCellToTable(PdfPTable table,String text,Font font){
  PdfPCell cell=new PdfPCell(new Phrase(text!=null?text:"",font));
  cell.setPadding(4);
  cell.setBorder(Rectangle.NO_BORDER);
  table.addCell(cell);
 }

 void p(XWPFDocument d,String s,boolean b,ParagraphAlignment align){
  XWPFParagraph p=d.createParagraph();
  p.setAlignment(align);
  p.setSpacingBefore(0);
  p.setSpacingAfter(b?100:90);
  p.setSpacingBetween(b?1.0:0.85);
  XWPFRun r=p.createRun();
  r.setFontFamily("Arial");
  r.setFontSize(b?12:9);
  r.setBold(b);
  String cleaned=s.replace("⟦","").replace("⟧","");
  String[] parts=cleaned.split("\n",-1);
  for(int i=0;i<parts.length;i++){r.setText(parts[i]); if(i<parts.length-1)r.addBreak();}
 }

 void cell(XWPFTable t,int r,int c,String s){t.getRow(r).getCell(c).setText(s==null?"":s);}
 String fmt(LocalDate d){return d==null?"---":d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
 String fmtLong(LocalDate d){return d==null?"---":d.format(DateTimeFormatter.ofPattern("dd 'días del mes de' MMMM 'del' yyyy",ES_PE));}
 String yn(Boolean b){return Boolean.TRUE.equals(b)?"SI":"NO";}
 String safe(Object o){return o==null?"":String.valueOf(o).trim();}
 String def(String v,String d){return v==null||v.isBlank()?d:v;}
 String atmLabel(Atm atm){return atm==null?"":def(atm.getCodigoTerminal(),"S/N");}
 String nombreMes(int m){return Month.of(m).getDisplayName(TextStyle.FULL,ES_PE);}
 BigDecimal nullSafe(BigDecimal b){return b==null?BigDecimal.ZERO:b;}
 String safeFile(String v){return def(v,"SIN_NUMERO").replaceAll("[^A-Za-z0-9ÁÉÍÓÚáéíóúÑñ._-]","_").replaceAll("_+","_");}
 String formatMoney(BigDecimal n){return new java.text.DecimalFormat("#,##0.00").format(nullSafe(n).setScale(2, RoundingMode.HALF_UP));}

 String simboloMoneda(String moneda){
  String m=moneda==null?"":moneda.toUpperCase(Locale.ROOT);
  if(m.contains("US") || m.contains("DOLAR") || m.contains("DÓLAR")) return "US$";
  return "S/";
 }
 String textoMoneda(String moneda){
  String m=moneda==null?"":moneda.toUpperCase(Locale.ROOT);
  if(m.contains("US") || m.contains("DOLAR") || m.contains("DÓLAR")) return "dólares americanos";
  return "soles";
 }

 private String calcularDuracionExacta(LocalDate inicio, LocalDate fin, String fallback){
  if(inicio==null || fin==null) return def(fallback,"0 meses");
  try{
   Period periodo=Period.between(inicio,fin.plusDays(1));
   int meses=(periodo.getYears()*12)+periodo.getMonths();
   int dias=periodo.getDays();
   if(meses==0) return dias+(dias==1?" día":" días");
   if(dias==0) return meses+(meses==1?" mes":" meses");
   return meses+(meses==1?" mes y ":" meses y ")+dias+(dias==1?" día":" días");
  }catch(Exception e){return def(fallback,"0 meses");}
 }

 public Resource downloadDocx(Long id)throws Exception{return resource(Path.of(get(id).getRutaArchivoGenerado()));}
 public Resource downloadPdf(Long id)throws Exception{return resource(Path.of(get(id).getRutaArchivoPdf()));}
 // Compatibilidad con el endpoint antiguo /descargar: devuelve DOCX.
 public Resource download(Long id)throws Exception{return downloadDocx(id);}
 private Resource resource(Path path)throws Exception{
  if(path==null || !Files.exists(path)) throw new RuntimeException("Archivo no encontrado en carpeta local");
  return new UrlResource(path.toUri());
 }

 public ActaConformidad subirFirmada(Long id,MultipartFile f)throws Exception{
  ActaConformidad a=get(id);
  Path dir=Path.of(actasDir, safeFile(a.getContrato().getProveedor()==null?"SIN_PROVEEDOR":a.getContrato().getProveedor().getRazonSocial()), safeFile(a.getContrato().getNumeroContrato()), String.valueOf(a.getAnio()), String.format("%02d-%s",a.getMes(),nombreMes(a.getMes()).toUpperCase(Locale.ROOT)), "FIRMADAS");
  Files.createDirectories(dir);
  String name=FileUtil.ts(f.getOriginalFilename());
  Path path=dir.resolve(name);
  f.transferTo(path);
  a.setEstado(EstadoActa.FIRMADA); a.setNombreArchivoFirmado(name); a.setRutaArchivoFirmado(path.toString()); a.setFechaSubidaFirmada(LocalDateTime.now()); a=repo.save(a);
  docs.save(Documento.builder().acta(a).contrato(a.getContrato()).tipoDocumento(TipoDocumento.ACTA_FIRMADA).nombreArchivo(name).rutaArchivo(path.toString()).extension(FileUtil.ext(name)).tamanio(Files.size(path)).descripcion("Acta firmada "+nombreMes(a.getMes())+" "+a.getAnio()).usuarioSubida("usuario").build());
  hist.log("ACTA",a.getId(),"SUBIR_FIRMADA","Subida de acta firmada en carpeta local: "+dir,null,a);
  return a;
 }
}
