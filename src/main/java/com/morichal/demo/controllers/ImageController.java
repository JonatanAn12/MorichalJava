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
@RequestMapping("/api/ocr")
public class ImageController {

    @Autowired
    private OCRService ocrService;

    @PostMapping("/extract-text")
    public String uploadImage(@RequestParam("image") MultipartFile image) throws IOException, TesseractException {
        return ocrService.extractTextFromImage(image);
    }
}