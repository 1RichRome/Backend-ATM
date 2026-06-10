package com.bn.atmcontratos.controller;
import com.bn.atmcontratos.model.entity.*; import com.bn.atmcontratos.service.*; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/historial") @RequiredArgsConstructor public class HistorialController{
 private final HistorialService service; @GetMapping public List<HistorialEvento> list(){return service.all();} @GetMapping("/{entidad}/{id}") public List<HistorialEvento> by(@PathVariable String entidad,@PathVariable Long id){return service.by(entidad.toUpperCase(),id);}
}
