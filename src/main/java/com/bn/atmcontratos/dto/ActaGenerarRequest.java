package com.bn.atmcontratos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ActaGenerarRequest{
 public Long contratoId;
 public Integer mes,anio;
 public LocalDate fechaEmision,periodoServicioInicio,periodoServicioFin;
 public BigDecimal montoPagar;
 public String moneda, observaciones;
 public Boolean tienePenalidades, tienePenalidadMora, tieneOtrasPenalidades, adjuntaInformePenalidades, adjuntaOtroInforme;
 public List<Long> atmIds;
 public String ciudadEmision, areaResponsable, cuentaContable, centroCosto, areaPresupuesto, tipoGasto;
}
