package com.morichal.demo.controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.morichal.demo.models.imageResponse;
import com.morichal.demo.services.OCRService;

import net.sourceforge.tess4j.TesseractException;

@RestController
@RequestMapping("/api/ocr")
public class ImageController {

    @Autowired
    private OCRService ocrService;

    // Procesar imagen y guardar resultado
    @PostMapping("/extract-text")
    public imageResponse uploadImage(@RequestParam("image") MultipartFile image) throws IOException, TesseractException {
        String extractedText = ocrService.extractTextFromImage(image);
        String numeroSolo = extractedText.replaceAll("[^0-9.]", "");
        double valorNumerico = 0.0;
        try {
            valorNumerico = Double.parseDouble(numeroSolo);
        } catch (NumberFormatException e) {}
        imageResponse response = new imageResponse();
        response.setText(valorNumerico);
        return ocrService.guardar(response);
    }

    // Crear registro manualmente (sin imagen)
    @PostMapping
    public imageResponse crearManual(@RequestBody imageResponse nuevo) {
        return ocrService.guardar(nuevo);
    }

    // Ver todos los registros
    @GetMapping
    public List<imageResponse> listarTodos() {
        return ocrService.listarTodos();
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
}