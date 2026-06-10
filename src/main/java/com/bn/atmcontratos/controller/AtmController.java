package com.bn.atmcontratos.controller;

import com.bn.atmcontratos.model.entity.*;
import com.bn.atmcontratos.repository.*;
import com.bn.atmcontratos.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController @RequestMapping("/api/atms") @RequiredArgsConstructor public class AtmController{
 private final AtmService service; private final DocumentoRepository docs;
 @GetMapping public List<Atm> list(@RequestParam(required=false)String q,@RequestParam(required=false)Long proveedorId){return service.list(q,proveedorId);}
 @GetMapping("/{id}") public Atm get(@PathVariable Long id){return service.get(id);}
 @PostMapping public Atm create(@RequestBody Atm a){return service.save(a);}
 @PutMapping("/{id}") public Atm update(@PathVariable Long id,@RequestBody Atm a){return service.update(id,a);}
 @DeleteMapping("/{id}") public void delete(@PathVariable Long id){service.delete(id);}
 @PostMapping("/importar") public Map<String,Object> importar(@RequestParam("file")MultipartFile f)throws Exception{return service.importFile(f);}
 @GetMapping("/{id}/contratos") public Object contratos(@PathVariable Long id){return service.get(id).getContratos();}
 @GetMapping("/{id}/documentos") public Object documentos(@PathVariable Long id){return docs.findByAtmId(id);}
}
