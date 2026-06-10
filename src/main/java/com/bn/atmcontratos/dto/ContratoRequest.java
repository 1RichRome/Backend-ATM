package com.bn.atmcontratos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Data
public class ContratoRequest{
 public String numeroInterno, numeroContrato, nombreContrato, descripcionContrato, tipoContrato, responsableActas, meses, impuesto, moneda, observaciones;
 public Long proveedorId;
 public BigDecimal rentaMensual, otrosGastos, montoTotalMensual, montoContrato;
 public String cuentaContableRenta, centroCostoRenta, areaPresupuestoRenta, tipoGastoRenta;
 public String cuentaContableGastoComun, centroCostoGastoComun, areaPresupuestoGastoComun, tipoGastoGastoComun;
 public LocalDate fechaSuscripcion, fechaInicio, fechaFin;
 public Boolean incluyeIgv;
 public List<Long> atmIds=new ArrayList<>();
}
