package com.bn.atmcontratos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Data
public class ActaLoteRequest {
 public Long proveedorId;
 public List<Long> atmIds = new ArrayList<>();
 public Integer mes, anio;
 public LocalDate fechaEmision, periodoServicioInicio, periodoServicioFin;
 public BigDecimal montoPagar;
 public String moneda, observaciones;
 public Boolean tienePenalidades, tienePenalidadMora, tieneOtrasPenalidades, adjuntaInformePenalidades, adjuntaOtroInforme;
 public String ciudadEmision, areaResponsable, cuentaContable, centroCosto, areaPresupuesto, tipoGasto;
}
