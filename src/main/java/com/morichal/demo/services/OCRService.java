package com.morichal.demo.services;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.morichal.demo.models.imageResponse;
import com.morichal.demo.repositories.imageResponseRepository;
import com.morichal.demo.services.FileStorageService;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class OCRService {
    @Autowired
    private imageResponseRepository imageResponseRepository;
    @Value("${tesseract.datapath}")
    private String tessDataPath;
    @Autowired
    private FileStorageService fileStorageService;

    // === MÉTODO PRINCIPAL ===
    public Double extractNumberFromImage(MultipartFile image) throws IOException, TesseractException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo.");
        }
        String contentType = image.getContentType();
        if (!isValidImageType(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, JPG, and PNG are allowed.");
        }
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        File tempFile = File.createTempFile("upload-", extension);
        image.transferTo(tempFile);

        Double result = null;
        // 1. OCR Básico
        result = tryBasicOCR(tempFile);
        if (result != null && isValidNumber(result)) {
            tempFile.delete();
            return result;
        }
        // 2. OCR Especializado para grameras
        result = tryGrameraSpecialOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            tempFile.delete();
            return result;
        }
        // 3. OCR con escalado
        result = tryScaledOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            tempFile.delete();
            return result;
        }
        // 4. OCR con contraste
        result = tryContrastOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            tempFile.delete();
            return result;
        }
        // 5. OCR invertido
        result = tryInvertedOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            tempFile.delete();
            return result;
        }
        // 6. OCR múltiple
        result = tryMultipleOCR(tempFile);
        tempFile.delete();
        if (result != null && isValidNumber(result)) {
            return result;
        }
        throw new IllegalArgumentException("No se detectó ningún número válido en la imagen.");
    }

    // === MÉTODOS DE OCR ===
    private Double tryBasicOCR(File imageFile) {
        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            String text = tesseract.doOCR(imageFile);
            return extractNumberFromText(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Double tryScaledOCR(File imageFile, String extension) {
        try {
            BufferedImage original = ImageIO.read(imageFile);
            if (original.getWidth() < 200 || original.getHeight() < 50) {
                BufferedImage scaled = scaleImageSafely(original, 3.0);
                File processedFile = File.createTempFile("scaled-", extension);
                ImageIO.write(scaled, extension.replace(".", ""), processedFile);
                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath(tessDataPath);
                tesseract.setLanguage("eng");
                tesseract.setPageSegMode(8);
                tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
                String text = tesseract.doOCR(processedFile);
                processedFile.delete();
                return extractNumberFromText(text);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Double tryContrastOCR(File imageFile, String extension) {
        try {
            BufferedImage original = ImageIO.read(imageFile);
            BufferedImage enhanced = enhanceContrast(original);
            File processedFile = File.createTempFile("contrast-", extension);
            ImageIO.write(enhanced, extension.replace(".", ""), processedFile);
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(7);
            tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
            String text = tesseract.doOCR(processedFile);
            processedFile.delete();
            return extractNumberFromText(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Double tryInvertedOCR(File imageFile, String extension) {
        try {
            BufferedImage original = ImageIO.read(imageFile);
            BufferedImage inverted = invertColors(original);
            File processedFile = File.createTempFile("inverted-", extension);
            ImageIO.write(inverted, extension.replace(".", ""), processedFile);
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(6);
            tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
            String text = tesseract.doOCR(processedFile);
            processedFile.delete();
            return extractNumberFromText(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Double tryMultipleOCR(File imageFile) {
        try {
            int[] psmModes = {8, 7, 6, 13};
            for (int psm : psmModes) {
                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath(tessDataPath);
                tesseract.setLanguage("eng");
                tesseract.setPageSegMode(psm);
                tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
                try {
                    String text = tesseract.doOCR(imageFile);
                    Double result = extractNumberFromText(text);
                    if (result != null && isValidNumber(result) && result > 0) {
                        return result;
                    }
                } catch (Exception e) {
                    // Continuar con siguiente PSM
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Double tryGrameraSpecialOCR(File imageFile, String extension) {
        try {
            BufferedImage original = ImageIO.read(imageFile);
            BufferedImage enlarged = scaleImageSafely(original, 5.0);
            BufferedImage enhanced = enhanceForLED(enlarged);
            BufferedImage gray = convertToGrayscale(enhanced);
            BufferedImage binary = binarizeImage(gray);
            File processedFile = File.createTempFile("gramera-", extension);
            ImageIO.write(binary, extension.replace(".", ""), processedFile);
            String[] results = new String[6];
            results[0] = runTesseractSpecial(processedFile, 8, "0123456789");
            results[1] = runTesseractSpecial(processedFile, 7, "0123456789");
            results[2] = runTesseractSpecial(processedFile, 6, "0123456789");
            results[3] = runTesseractSpecial(processedFile, 13, "0123456789");
            results[4] = runTesseractSpecial(processedFile, 10, "0123456789");
            results[5] = runTesseractSpecial(processedFile, 9, "0123456789");
            processedFile.delete();
            return selectBestGrameraResult(results);
        } catch (Exception e) {
            return null;
        }
    }

    // === PREPROCESAMIENTO ===
    private BufferedImage scaleImageSafely(BufferedImage original, double factor) {
        int newWidth = Math.max(50, (int)(original.getWidth() * factor));
        int newHeight = Math.max(20, (int)(original.getHeight() * factor));
        Image scaled = original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = result.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return result;
    }

    private BufferedImage enhanceContrast(BufferedImage image) {
        float scaleFactor = 1.5f;
        float offset = 20f;
        BufferedImage enhanced = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                r = Math.min(255, Math.max(0, (int)(r * scaleFactor + offset)));
                g = Math.min(255, Math.max(0, (int)(g * scaleFactor + offset)));
                b = Math.min(255, Math.max(0, (int)(b * scaleFactor + offset)));
                int newRgb = (r << 16) | (g << 8) | b;
                enhanced.setRGB(x, y, (0xFF << 24) | newRgb);
            }
        }
        return enhanced;
    }

    private BufferedImage invertColors(BufferedImage image) {
        BufferedImage inverted = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = 255 - ((rgb >> 16) & 0xFF);
                int g = 255 - ((rgb >> 8) & 0xFF);
                int b = 255 - (rgb & 0xFF);
                inverted.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return inverted;
    }

    private BufferedImage enhanceForLED(BufferedImage image) {
        BufferedImage enhanced = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                r = Math.min(255, Math.max(0, (int)((r - 128) * 2.0 + 128)));
                g = Math.min(255, Math.max(0, (int)((g - 128) * 2.0 + 128)));
                b = Math.min(255, Math.max(0, (int)((b - 128) * 2.0 + 128)));
                enhanced.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return enhanced;
    }

    private BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return gray;
    }

    private BufferedImage binarizeImage(BufferedImage grayImage) {
        BufferedImage binary = new BufferedImage(grayImage.getWidth(), grayImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = binary.createGraphics();
        g.drawImage(grayImage, 0, 0, null);
        g.dispose();
        return binary;
    }

    // === TESSERACT ESPECIAL ===
    private String runTesseractSpecial(File imageFile, int psm, String whitelist) {
        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(psm);
            tesseract.setOcrEngineMode(1);
            tesseract.setVariable("tessedit_char_whitelist", whitelist);
            tesseract.setVariable("classify_bln_numeric_mode", "1");
            tesseract.setVariable("user_defined_dpi", "300");
            tesseract.setVariable("textord_min_linesize", "1.0");
            tesseract.setVariable("preserve_interword_spaces", "0");
            String result = tesseract.doOCR(imageFile).trim();
            return result;
        } catch (Exception e) {
            return "";
        }
    }

    private Double selectBestGrameraResult(String[] results) {
        Double bestResult = null;
        int maxLength = 0;
        for (String result : results) {
            if (result != null && !result.isEmpty()) {
                String cleanResult = result.replaceAll("[^0-9.]", "");
                if (!cleanResult.isEmpty()) {
                    try {
                        Double number = Double.parseDouble(cleanResult);
                        if (isValidNumber(number) && cleanResult.length() > maxLength) {
                            bestResult = number;
                            maxLength = cleanResult.length();
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        return bestResult;
    }

    // === EXTRACCIÓN DE NÚMERO ===
    private Double extractNumberFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String cleanText = text.replaceAll("[^0-9.,]", "").replace(",", ".");
        if (cleanText.isEmpty() || cleanText.equals(".") || cleanText.equals("...")) {
            return null;
        }
        try {
            if (cleanText.indexOf('.') != cleanText.lastIndexOf('.')) {
                int lastDotIndex = cleanText.lastIndexOf('.');
                String integerPart = cleanText.substring(0, lastDotIndex).replace(".", "");
                String decimalPart = cleanText.substring(lastDotIndex);
                cleanText = integerPart + decimalPart;
            }
            if (cleanText.startsWith(".")) {
                cleanText = "0" + cleanText;
            }
            if (cleanText.endsWith(".")) {
                cleanText = cleanText.substring(0, cleanText.length() - 1);
            }
            Double result = Double.parseDouble(cleanText);
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isValidNumber(Double number) {
        if (number == null) return false;
        return (number >= 0 && number <= 100000);
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/jpg") ||
            contentType.equals("image/png")
        );
    }

    // === CRUD (sin cambios) ===
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
        if (nuevo.getNombreImagen() != null) {
            existente.setNombreImagen(nuevo.getNombreImagen());
        }
        return imageResponseRepository.save(existente);
    }
    public void eliminar(Long id) {
        imageResponseRepository.deleteById(id);
    }
    public String obtenerRutaImagen(Long id) {
        imageResponse registro = imageResponseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Registro no encontrado"));
        if (registro.getNombreImagen() == null || registro.getNombreImagen().isEmpty()) {
            throw new RuntimeException("El registro no tiene imagen asociada");
        }
        return registro.getNombreImagen();
    }
    public imageResponse crearConImagen(String categoria, String text, String uM, String estado, MultipartFile imagen) {
        imageResponse nuevo = new imageResponse();
        nuevo.setCategoria(categoria);
        nuevo.setText(Double.parseDouble(text));
        nuevo.setuM(uM);
        nuevo.setEstado(estado);
        if (imagen != null && !imagen.isEmpty()) {
            String nombreImagen = fileStorageService.guardarImagen(imagen);
            nuevo.setNombreImagen(nombreImagen);
        }
        return imageResponseRepository.save(nuevo);
    }
    public imageResponse actualizarConImagen(Long id, String text, String uM, String categoria, String estado, MultipartFile imagen) {
        imageResponse existente = imageResponseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Registro no encontrado"));
        existente.setText(Double.parseDouble(text));
        existente.setuM(uM);
        existente.setCategoria(categoria);
        existente.setEstado(estado);
        if (imagen != null && !imagen.isEmpty()) {
            if (existente.getNombreImagen() != null && !existente.getNombreImagen().isEmpty()) {
                try {
                    fileStorageService.eliminarImagen(existente.getNombreImagen());
                } catch (Exception e) {
                    System.err.println("Error al eliminar imagen anterior: " + e.getMessage());
                }
            }
            String nombreImagen = fileStorageService.guardarImagen(imagen);
            existente.setNombreImagen(nombreImagen);
        }
        return imageResponseRepository.save(existente);
    }
}
