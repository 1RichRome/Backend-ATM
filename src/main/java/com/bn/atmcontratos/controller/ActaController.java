package com.bn.atmcontratos.controller;

import com.bn.atmcontratos.dto.ActaGenerarRequest;
import com.bn.atmcontratos.dto.ActaLoteRequest;
import com.bn.atmcontratos.dto.ActaLoteResultadoResponse;
import com.bn.atmcontratos.model.entity.ActaConformidad;
import com.bn.atmcontratos.service.ActaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/actas")
@RequiredArgsConstructor
public class ActaController {
    private final ActaService service;

    @GetMapping
    public List<ActaConformidad> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ActaConformidad get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/generar")
    public ActaConformidad generar(@RequestBody ActaGenerarRequest r) throws Exception {
        return service.generar(r);
    }

    @PostMapping("/generar-lote")
    public ActaLoteResultadoResponse generarLote(@RequestBody ActaLoteRequest r) throws Exception {
        return service.generarLote(r);
    }

    // Compatibilidad: endpoint antiguo. Descarga el DOCX.
    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) throws Exception {
        return descargarDocx(id);
    }

    @GetMapping("/{id}/descargar-docx")
    public ResponseEntity<Resource> descargarDocx(@PathVariable Long id) throws Exception {
        ActaConformidad a = service.get(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .header(HttpHeaders.CONTENT_DISPOSITION, attachment(a.getNombreArchivoGenerado()).toString())
            .body(service.downloadDocx(id));
    }

    // Para descargar PDF como archivo.
    @GetMapping("/{id}/descargar-pdf")
    public ResponseEntity<Resource> descargarPdf(@PathVariable Long id) throws Exception {
        ActaConformidad a = service.get(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, inline(a.getNombreArchivoPdf()).toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(service.downloadPdf(id));
    }

    // Alias más claro para abrir PDF en una pestaña del navegador.
    @GetMapping("/{id}/ver-pdf")
    public ResponseEntity<Resource> verPdf(@PathVariable Long id) throws Exception {
        return descargarPdf(id);
    }

    @PostMapping("/{id}/subir-firmada")
    public ActaConformidad subir(@PathVariable Long id, @RequestParam("file") MultipartFile f) throws Exception {
        return service.subirFirmada(id, f);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) throws Exception {
        service.delete(id);
    }

    private ContentDisposition inline(String filename) {
        return ContentDisposition.inline()
            .filename(filename == null ? "acta.pdf" : filename, StandardCharsets.UTF_8)
            .build();
    }

    private ContentDisposition attachment(String filename) {
        return ContentDisposition.attachment()
            .filename(filename == null ? "acta.docx" : filename, StandardCharsets.UTF_8)
            .build();
    }
}
