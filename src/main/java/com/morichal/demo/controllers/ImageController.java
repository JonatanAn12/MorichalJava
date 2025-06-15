package com.morichal.demo.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.morichal.demo.models.imageResponse;
import com.morichal.demo.services.OCRService;
import com.morichal.demo.services.FileStorageService;

@RestController
@RequestMapping("/api/ocr")
public class ImageController {

    @Autowired
    private OCRService ocrService;

    @PostMapping("/extract-text")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
            System.out.println("Recibida imagen para OCR: " + image.getOriginalFilename() + ", tamaño: " + image.getSize());
            Double extractedNumber = ocrService.extractNumberFromImage(image);
            System.out.println("Número extraído: " + extractedNumber);
            imageResponse saved = ocrService.guardar(new imageResponse(extractedNumber));
            System.out.println("Texto extraído guardado: " + saved.getText());
            return ResponseEntity.ok(new ExtractNumberDTO(saved.getText()));
        } catch (IllegalArgumentException e) {
            System.err.println("Error en OCR: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error inesperado al procesar la imagen: " + e.getMessage());
            return ResponseEntity.status(500).body("Error al procesar la imagen.");
        }
    }

    @PostMapping("/extract-full-text")
    public ResponseEntity<?> extractFullText(@RequestParam("image") MultipartFile image) {
        try {
            String fullText = ocrService.extractFullTextFromImage(image);
            System.out.println("Texto completo extraído: " + fullText);
            return ResponseEntity.ok(fullText);
        } catch (Exception e) {
            System.err.println("Error al extraer texto completo: " + e.getMessage());
            return ResponseEntity.status(500).body("Error al procesar la imagen.");
        }
    }

    @PostMapping
    public ResponseEntity<imageResponse> crearManual(
    @RequestParam("categoria") String categoria,
    @RequestParam("text") String text,
    @RequestParam("uM") String uM,
    @RequestParam("estado") String estado,
    @RequestParam(value = "imagen", required = false) MultipartFile imagen) {
    
    try {
        // LLAMAR al método que YA tienes en el service
        imageResponse resultado = ocrService.crearConImagen(categoria, text, uM, estado, imagen);
        return ResponseEntity.ok(resultado);
    } catch (Exception e) {
        return ResponseEntity.status(500).build();
    }
}

    // Ver todos los registros
    @GetMapping
    public List<imageResponse> listarTodos() {
        return ocrService.listarTodos();
    }

    @GetMapping("/{id}")
    public Optional<imageResponse> obtenerPorId(@PathVariable Long id) {
        return ocrService.buscarPorId(id);
    }


    @Autowired 
    private FileStorageService fileStorageService;
    
  @GetMapping("/{id}/imagen")
    public ResponseEntity<Resource> obtenerImagen(@PathVariable Long id) {
    try {
        String nombreImagen = ocrService.obtenerRutaImagen(id);
        Resource resource = fileStorageService.cargarImagen(nombreImagen);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/jpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "inline; filename=\"" + nombreImagen + "\"")
                .body(resource);
                
         } catch (RuntimeException e) {
             return ResponseEntity.notFound().build();
         } catch (Exception e) {
             return ResponseEntity.status(500).build();
         }
    }

 @PutMapping("/{id}")
public ResponseEntity<imageResponse> actualizar(
    @PathVariable Long id,
    @RequestParam("text") String text,
    @RequestParam("uM") String uM,
    @RequestParam("categoria") String categoria,
    @RequestParam("estado") String estado,
    @RequestParam(value = "imagen", required = false) MultipartFile imagen) {
    
    try {
        // LLAMAR al método que YA tienes en el service
        imageResponse resultado = ocrService.actualizarConImagen(id, text, uM, categoria, estado, imagen);
        return ResponseEntity.ok(resultado);
    } catch (RuntimeException e) {
        return ResponseEntity.notFound().build();
    } catch (Exception e) {
        return ResponseEntity.status(500).build();
    }
}

    // Eliminar un registro
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        ocrService.eliminar(id);
    }

    // DTO para la respuesta
    static class ExtractNumberDTO {
        private Double number;

        public ExtractNumberDTO(Double number) {
            this.number = number;
        }

        public Double getNumber() {
            return number;
        }
    }
}

