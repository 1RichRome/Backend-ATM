package com.bn.atmcontratos.dto;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActaLoteResultadoResponse {
 private int solicitados;
 private int generados;
 private int omitidos;
 private List<Long> actaIds = new ArrayList<>();
 private List<String> mensajes = new ArrayList<>();
}
