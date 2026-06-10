package com.bn.atmcontratos.controller;

import com.bn.atmcontratos.model.entity.*;
import com.bn.atmcontratos.repository.*;
import com.bn.atmcontratos.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/proveedores") @RequiredArgsConstructor public class ProveedorController{
 private final ProveedorService service;
 private final ContratoRepository contratos;
 private final AtmRepository atms;
 @GetMapping public List<Proveedor> list(@RequestParam(required=false)String q){return service.list(q);}
 @GetMapping("/{id}") public Proveedor get(@PathVariable Long id){return service.get(id);}
 @PostMapping public Proveedor create(@RequestBody Proveedor p){return service.save(p);}
 @PutMapping("/{id}") public Proveedor update(@PathVariable Long id,@RequestBody Proveedor p){return service.update(id,p);}
 @DeleteMapping("/{id}") public void delete(@PathVariable Long id){service.delete(id);}
 @GetMapping("/{id}/contratos") public Object contratos(@PathVariable Long id){return contratos.findByProveedorId(id);}
 @GetMapping("/{id}/atms") public Object atms(@PathVariable Long id,@RequestParam(required=false)String q){return q==null||q.isBlank()?atms.findByProveedorId(id):atms.searchByProveedor(q,id);}
}
