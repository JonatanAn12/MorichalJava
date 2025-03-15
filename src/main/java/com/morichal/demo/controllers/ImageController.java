package com.morichal.demo.controllers;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/ocr")
@Slf4j
public class ImageController {
    @Value("${tesseract.datapath}")
    private String tessDataPath;

    @PostMapping("/extract-text")
    public String uploadImage(@RequestParam("image") MultipartFile image) throws IOException, TesseractException {
        // Guardar la imagen temporalmente
        File tempFile = File.createTempFile("upload-", ".png");
        image.transferTo(tempFile);

        // Usar Tesseract para reconocer el texto
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath); // Ruta a los datos de Tesseract
        tesseract.setLanguage("eng");
        String text = tesseract.doOCR(tempFile);

        // Eliminar el archivo temporal
        tempFile.delete();

        return text;
    }
}