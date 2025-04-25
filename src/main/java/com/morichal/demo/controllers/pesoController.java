package com.morichal.demo.controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.morichal.demo.services.OCRService;

import net.sourceforge.tess4j.TesseractException;

@RestController
@RequestMapping("/api")
public class pesoController {

    @Autowired
    private OCRService ocrService;

    @PostMapping("/peso")
    public PesoResponse obtenerPeso(@RequestParam("image") MultipartFile image) {
        try {
            //* */ Usar el servicio OCR para extraer el texto de la imagen
            String textoExtraido = ocrService.extractTextFromImage(image);
            System.out.println("Texto extraído de la imagen: " + textoExtraido);

            //* */ Convertir el texto extraído a un valor de peso (double)
            double peso = Double.parseDouble(textoExtraido.trim());
            return new PesoResponse(peso);
        } catch (IOException | TesseractException | NumberFormatException e) {
            //* */ Manejar errores de extracción de texto o conversión
            e.printStackTrace();
            return new PesoResponse(0.0); // Devolver un valor por defecto en caso de error
        }
    }

    public static class PesoResponse {
        private double peso;

        public PesoResponse(double peso) {
            this.peso = peso;
        }

        public double getPeso() {
            return peso;
        }

        public void setPeso(double peso) {
            this.peso = peso;
        }
    }
}