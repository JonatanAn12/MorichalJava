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

import com.morichal.demo.models.imageResponse;
import com.morichal.demo.services.OCRService;
import com.morichal.demo.services.TensorFlowService;

import jakarta.annotation.Generated;

@RestController
@RequestMapping("/api/ocr")
public class ImageController {

    @Autowired
    private OCRService ocrService;

    @Autowired
    private TensorFlowService tensorFlowService;

    @PostMapping("/extract-text")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body("No se detectó ninguna imagen");
        }
        try {
            Double numeroTesseract = null;
            try {
                numeroTesseract = ocrService.extractNumberFromImage(image);
            } catch (Exception e) {
            }
            String numeroTensorflow = null;
            if (numeroTesseract == null) {
                numeroTensorflow = tensorFlowService.reconocerNumero(image);
            }

            String resultadoFinal = (numeroTesseract != null) ? numeroTesseract.toString() : numeroTensorflow;

            if (resultadoFinal == null || resultadoFinal.isEmpty()) {
                return ResponseEntity.badRequest().body("No se detectó ningún número en la imagen.");
            }
            imageResponse saved = ocrService.guardar(new imageResponse(Double.parseDouble(resultadoFinal)));
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al procesar la imagen.");
        }
    }

    @PostMapping
    public imageResponse crearManual(@RequestBody imageResponse nuevo) {
        return ocrService.guardar(nuevo);
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
    
    // Actualizar un registro
    @PutMapping("/{id}")
    public imageResponse actualizar(@PathVariable Long id, @RequestBody imageResponse nuevo) {
        return ocrService.actualizar(id, nuevo);
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