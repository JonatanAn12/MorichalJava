package com.morichal.demo.controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.morichal.demo.models.imageResponse;
import com.morichal.demo.repositories.imageResponseRepository;
import com.morichal.demo.services.OCRService;

import net.sourceforge.tess4j.TesseractException;

@RestController
@RequestMapping("/api/ocr")
public class ImageController {

    @Autowired
    private OCRService ocrService;

    @Autowired
    private imageResponseRepository imageResponseRepository;

    @PostMapping("/extract-text")
    public imageResponse uploadImage(@RequestParam("image") MultipartFile image) throws IOException, TesseractException {
        System.out.println("Archivo recibido: " + image.getOriginalFilename());
        String extractedText = ocrService.extractTextFromImage(image);
        imageResponse imageResponse = new imageResponse(extractedText);
        return imageResponseRepository.save(imageResponse);
    }
}