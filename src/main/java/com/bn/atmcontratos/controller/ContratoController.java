package com.bn.atmcontratos.controller;
import com.bn.atmcontratos.dto.*; import com.bn.atmcontratos.model.entity.*; import com.bn.atmcontratos.model.enums.*; import com.bn.atmcontratos.repository.*; import com.bn.atmcontratos.service.*; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.util.*;
@RestController @RequestMapping("/api/contratos") @RequiredArgsConstructor public class ContratoController{
 private final ContratoService service; private final ActaConformidadRepository actas; private final DocumentoRepository docs; private final ActaService actaService;
 @GetMapping public List<Contrato> list(@RequestParam(required=false)String q,@RequestParam(required=false)EstadoContrato estado,@RequestParam(required=false)Long proveedorId){return service.list(q,estado,proveedorId);}
 @GetMapping("/{id}") public Contrato get(@PathVariable Long id){return service.get(id);}
 @PostMapping public Contrato create(@RequestBody ContratoRequest r){return service.create(r);}
 @PutMapping("/{id}") public Contrato update(@PathVariable Long id,@RequestBody ContratoRequest r){return service.update(id,r);}
 @DeleteMapping("/{id}") public void delete(@PathVariable Long id){service.delete(id);}
 @PostMapping("/importar") public Map<String,Object> importar(@RequestParam("file")MultipartFile f)throws Exception{return service.importFile(f);}
 @PostMapping("/{id}/atms") public Contrato addAtms(@PathVariable Long id,@RequestBody AsociarAtmsRequest r){return service.addAtms(id,r);}
 @DeleteMapping("/{id}/atms/{atmId}") public Contrato removeAtm(@PathVariable Long id,@PathVariable Long atmId){return service.removeAtm(id,atmId);}
 @GetMapping("/vencidos") public List<Contrato> vencidos(){return service.vencidos();}
 @GetMapping("/por-vencer") public List<Contrato> porVencer(@RequestParam(defaultValue="30")int dias){return service.porVencer(dias);}
 @GetMapping("/{id}/actas/calendario") public Object calendarioActas(@PathVariable Long id,@RequestParam(required=false)Integer anio){return actaService.calendarioContrato(id,anio);}
 @GetMapping("/{id}/actas") public Object actas(@PathVariable Long id){return actas.findByContratoId(id);}
 @GetMapping("/{id}/documentos") public Object documentos(@PathVariable Long id){return docs.findByContratoId(id);}
}
