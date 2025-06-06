package com.morichal.demo.services;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.morichal.demo.models.imageResponse;
import com.morichal.demo.repositories.imageResponseRepository;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;



@Service
public class OCRService {


    @Autowired
    private imageResponseRepository imageResponseRepository;

    @Value("${tesseract.datapath}")
    private String tessDataPath;

    public Double extractNumberFromImage(MultipartFile image) throws IOException, TesseractException {
        //*  Validación de archivo
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo.");
        }
        String contentType = image.getContentType();
        if (!isValidImageType(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, JPG, and PNG are allowed.");
        }

        // Guardar la imagen temporalmente
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        File tempFile = File.createTempFile("upload-", extension);
        image.transferTo(tempFile);

        // Usar Tesseract para reconocer el texto
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath); // Ruta a los datos de Tesseract
        tesseract.setLanguage("eng");
        String text = tesseract.doOCR(tempFile);
        tempFile.delete();

        // Extraer el primer número válido del texto
        String numberStr = text.replaceAll("[^0-9.,]", "").replace(",", ".");
        if (numberStr.isEmpty()) {
            throw new IllegalArgumentException("No se detectó ningún número en la imagen.");
        }
        try {
            return Double.parseDouble(numberStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El texto extraído no es un número válido.");
        }
    }

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") || contentType.equals("image/jpg") || contentType.equals("image/png");
    }

    //* imageResponse

    public List<imageResponse> listarTodos() {
        return imageResponseRepository.findAll();
    }

    public Optional<imageResponse> buscarPorId(Long id) {
        return imageResponseRepository.findById(id);
    }

    public imageResponse guardar(imageResponse response) {
        return imageResponseRepository.save(response);
    }

    public imageResponse actualizar(Long id, imageResponse nuevo) {
        imageResponse existente = imageResponseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Registro no encontrado"));
        existente.setText(nuevo.getText());
        existente.setuM(nuevo.getuM());
        existente.setCategoria(nuevo.getCategoria());
        existente.setEstado(nuevo.getEstado());
        return imageResponseRepository.save(existente);
    }

    public void eliminar(Long id) {
        imageResponseRepository.deleteById(id);
    }
}