package com.morichal.demo.services;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReaderSpi;
import com.twelvemonkeys.imageio.plugins.png.PNGImageReaderSpi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import com.morichal.demo.repositories.imageResponseRepository;
import com.morichal.demo.models.imageResponse;


@Service
public class OCRService {

    @Value("${tesseract.datapath}")
    private String tessDataPath;

    @Autowired
    private imageResponseRepository imageResponseRepository;

    public String extractTextFromImage(MultipartFile image) throws IOException, TesseractException {
        // Registrar plugins de TwelveMonkeys explícitamente (JPEG y PNG)
        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new JPEGImageReaderSpi());
        registry.registerServiceProvider(new PNGImageReaderSpi());
        ImageIO.scanForPlugins();

        // Validar el tipo de archivo
        String contentType = image.getContentType();
        if (!isValidImageType(contentType)) {
            throw new IllegalArgumentException("Tipo de archivo no válido. Solo se permiten PNG, JPEG y JPG.");
        }

        // Guardar la imagen temporalmente con la extensión correcta
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        File tempFile = File.createTempFile("upload-", extension);
        image.transferTo(tempFile);

        // Leer imagen y mejorar contraste
        BufferedImage bufferedImage = ImageIO.read(tempFile);
        if (bufferedImage == null) {
            throw new IOException("No se pudo leer la imagen. Verifica el formato.");
        }
        RescaleOp rescaleOp = new RescaleOp(1.2f, 0, null);
        bufferedImage = rescaleOp.filter(bufferedImage, null);

        // Guardar imagen mejorada temporalmente en el mismo formato
        String formatName = contentType.equals("image/png") ? "png" : "jpg";
        File enhancedFile = File.createTempFile("enhanced-", "." + formatName);
        ImageIO.write(bufferedImage, formatName, enhancedFile);

        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
        String text = tesseract.doOCR(enhancedFile);

        tempFile.delete();
        enhancedFile.delete();

        return text;
    }

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/png") ||
               contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg");
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
        return imageResponseRepository.save(existente);
    }

    public void eliminar(Long id) {
        imageResponseRepository.deleteById(id);
    }
}