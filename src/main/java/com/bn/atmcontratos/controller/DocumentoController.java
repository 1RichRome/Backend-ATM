package com.bn.atmcontratos.controller;

import com.bn.atmcontratos.model.entity.Documento;
import com.bn.atmcontratos.model.enums.TipoDocumento;
import com.bn.atmcontratos.service.DocumentoService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {
    private final DocumentoService service;

    @GetMapping
    public List<Documento> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Documento get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/upload")
    public Documento upload(
        @RequestParam("file") MultipartFile f,
        @RequestParam(defaultValue = "OTRO") TipoDocumento tipoDocumento,
        @RequestParam(required = false) Long contratoId,
        @RequestParam(required = false) Long atmId,
        @RequestParam(required = false) Long proveedorId,
        @RequestParam(required = false) String descripcion
    ) throws Exception {
        return service.upload(f, tipoDocumento, contratoId, atmId, proveedorId, descripcion);
    }

    // Si es PDF se abre en navegador; si es Word/Excel/otro se descarga.
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        Documento d = service.get(id);
        boolean pdf = "pdf".equalsIgnoreCase(d.getExtension()) || safeName(d).toLowerCase().endsWith(".pdf");
        return ResponseEntity.ok()
            .contentType(mediaType(d, pdf))
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition(d, pdf).toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(service.download(id));
    }

    // Alias explícito para visualizar PDF.
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable Long id) throws Exception {
        Documento d = service.get(id);
        return ResponseEntity.ok()
            .contentType(mediaType(d, true))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(safeName(d), StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(service.download(id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) throws Exception {
        service.delete(id);
    }

    private MediaType mediaType(Documento d, boolean pdf) {
        if (pdf) return MediaType.APPLICATION_PDF;
        try {
            String probe = Files.probeContentType(Path.of(d.getRutaArchivo()));
            if (probe != null) return MediaType.parseMediaType(probe);
        } catch (Exception ignored) {}
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private ContentDisposition disposition(Documento d, boolean pdf) {
        ContentDisposition.Builder builder = pdf ? ContentDisposition.inline() : ContentDisposition.attachment();
        return builder.filename(safeName(d), StandardCharsets.UTF_8).build();
    }

    private String safeName(Documento d) {
        return d.getNombreArchivo() == null ? "documento" : d.getNombreArchivo();
    }
}
