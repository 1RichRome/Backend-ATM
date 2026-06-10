package com.bn.atmcontratos.exception;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.time.LocalDateTime; import java.util.*;
@RestControllerAdvice public class ApiExceptionHandler{
 @ExceptionHandler(Exception.class) ResponseEntity<Map<String,Object>> err(Exception e){ Map<String,Object> m=new LinkedHashMap<>(); m.put("timestamp",LocalDateTime.now()); m.put("message",e.getMessage()); return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(m); }
}
