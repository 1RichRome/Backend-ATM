package com.bn.atmcontratos.service;
import com.bn.atmcontratos.model.entity.*; import com.bn.atmcontratos.repository.*; import com.fasterxml.jackson.databind.ObjectMapper; import lombok.RequiredArgsConstructor; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.stereotype.Service; import java.util.*;
@Service @RequiredArgsConstructor public class HistorialService{
 private final HistorialEventoRepository repo; private final ObjectMapper mapper;
 public void log(String e,Long id,String a,String d,Object old,Object nuevo){repo.save(HistorialEvento.builder().entidad(e).entidadId(id).accion(a).descripcion(d).usuario(user()).valorAnterior(json(old)).valorNuevo(json(nuevo)).build());}
 public List<HistorialEvento> all(){return repo.findAll();} public List<HistorialEvento> by(String e,Long id){return repo.findByEntidadAndEntidadIdOrderByFechaHoraDesc(e,id);}
 String user(){var a=SecurityContextHolder.getContext().getAuthentication(); return a==null?"sistema":a.getName();}
 String json(Object o){try{return o==null?null:mapper.writeValueAsString(o);}catch(Exception ex){return String.valueOf(o);}}
}
