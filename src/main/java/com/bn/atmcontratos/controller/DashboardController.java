package com.bn.atmcontratos.controller;
import com.bn.atmcontratos.dto.*; import com.bn.atmcontratos.service.*; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/dashboard") @RequiredArgsConstructor public class DashboardController{
 private final DashboardService service;
 @GetMapping("/resumen") public DashboardResumen resumen(){return service.resumen();}
 @GetMapping("/contratos-por-estado") public Map<String,Long> porEstado(){return service.porEstado();}
 @GetMapping("/contratos-por-proveedor") public Map<String,Long> porProveedor(){return service.porProveedor();}
 @GetMapping("/contratos-proximos-vencer") public Object proximos(){return service.proximos();}
}
