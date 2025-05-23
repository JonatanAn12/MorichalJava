package com.morichal.demo.services;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class OCRService {

    @Value("${tesseract.datapath}")
    private String tessDataPath;

    public String extractTextFromImage(MultipartFile image) throws IOException, TesseractException {
        // Validar el tipo de archivo
        String contentType = image.getContentType();
        if (!isValidImageType(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, JPG, and PNG are allowed.");
        }

        // Guardar la imagen temporalmente
        File tempFile = File.createTempFile("upload-", ".png");
        image.transferTo(tempFile);

        // Usar Tesseract para reconocer el texto
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath); // Ruta a los datos de Tesseract
        tesseract.setLanguage("eng");
        String text = tesseract.doOCR(tempFile);

        // Eliminar el archivo temporal
        tempFile.delete();

        return text;
    }

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") || contentType.equals("image/jpg") || contentType.equals("image/png");
    }
}