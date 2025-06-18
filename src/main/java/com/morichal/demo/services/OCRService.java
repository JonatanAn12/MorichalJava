package com.morichal.demo.services;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
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

    // MODIFICAR el método extractNumberFromImage para incluir OCR especializado
    public Double extractNumberFromImage(MultipartFile image) throws IOException, TesseractException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo.");
        }
        
        String contentType = image.getContentType();
        if (!isValidImageType(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, JPG, and PNG are allowed.");
        }

        String extension = contentType.equals("image/png") ? ".png" : ".jpeg";
        File tempFile = File.createTempFile("upload-", extension);
        image.transferTo(tempFile);

        System.out.println("=== PROCESAMIENTO OCR MEJORADO ===");
        System.out.println("Archivo: " + image.getOriginalFilename());

        Double result = null;

        // MÉTODO 1: OCR Básico
        result = tryBasicOCR(tempFile);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Básico: " + result);
            tempFile.delete();
            return result;
        }

        // MÉTODO 2: OCR especializado para grameras (NUEVO)
        result = tryGrameraSpecialOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Gramera Especial: " + result);
            tempFile.delete();
            return result;
        }

        // MÉTODO 3: OCR con escalado
        result = tryScaledOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Escalado: " + result);
            tempFile.delete();
            return result;
        }

        // MÉTODO 4: OCR con contraste
        result = tryContrastOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Contraste: " + result);
            tempFile.delete();
            return result;
        }

        // MÉTODO 5: OCR invertido
        result = tryInvertedOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Invertido: " + result);
            tempFile.delete();
            return result;
        }

        // MÉTODO 6: OCR múltiple
        result = tryMultipleOCR(tempFile);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Múltiple: " + result);
            tempFile.delete();
            return result;
        }

        tempFile.delete();
        throw new IllegalArgumentException("No se detectó ningún número válido en la imagen.");
    }

    private Double tryBasicOCR(File imageFile) {
        try {
            System.out.println("--- OCR Básico ---");
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            String text = tesseract.doOCR(imageFile);
            System.out.println("Texto: '" + text + "'");
            return extractNumberFromText(text);
        } catch (Exception e) {
            System.err.println("Error básico: " + e.getMessage());
            return null;
        }
    }

    private Double tryScaledOCR(File imageFile, String extension) {
        try {
            System.out.println("--- OCR Escalado ---");
            BufferedImage original = ImageIO.read(imageFile);
            
            // Solo escalar si es muy pequeña
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
                System.out.println("Texto escalado: '" + text + "'");
                processedFile.delete();
                return extractNumberFromText(text);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error escalado: " + e.getMessage());
            return null;
        }
    }

    private Double tryContrastOCR(File imageFile, String extension) {
        try {
            System.out.println("--- OCR Contraste ---");
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
            System.out.println("Texto contraste: '" + text + "'");
            processedFile.delete();
            return extractNumberFromText(text);
        } catch (Exception e) {
            System.err.println("Error contraste: " + e.getMessage());
            return null;
        }
    }

    private Double tryInvertedOCR(File imageFile, String extension) {
        try {
            System.out.println("--- OCR Invertido ---");
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
            System.out.println("Texto invertido: '" + text + "'");
            processedFile.delete();
            return extractNumberFromText(text);
        } catch (Exception e) {
            System.err.println("Error invertido: " + e.getMessage());
            return null;
        }
    }

    private Double tryMultipleOCR(File imageFile) {
        try {
            System.out.println("--- OCR Múltiple ---");
            int[] psmModes = {8, 7, 6, 13};
            
            for (int psm : psmModes) {
                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath(tessDataPath);
                tesseract.setLanguage("eng");
                tesseract.setPageSegMode(psm);
                tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
                
                try {
                    String text = tesseract.doOCR(imageFile);
                    System.out.println("PSM " + psm + ": '" + text + "'");
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
            System.err.println("Error múltiple: " + e.getMessage());
            return null;
        }
    }

    // Agregar este método después de tryMultipleOCR
    private Double tryGrameraSpecialOCR(File imageFile, String extension) {
        try {
            System.out.println("--- OCR Especializado Gramera ---");
            BufferedImage original = ImageIO.read(imageFile);
            
            // MÉTODO 1: Escalar mucho la imagen para números pequeños
            BufferedImage enlarged = scaleImageSafely(original, 5.0);
            
            // MÉTODO 2: Mejorar contraste específico para LED
            BufferedImage enhanced = enhanceForLED(enlarged);
            
            // MÉTODO 3: Convertir a escala de grises
            BufferedImage gray = convertToGrayscale(enhanced);
            
            File processedFile = File.createTempFile("gramera-", extension);
            ImageIO.write(gray, extension.replace(".", ""), processedFile);
            
            // Probar múltiples configuraciones específicas para números largos
            String[] results = new String[6];
            
            // PSM 8: Una sola palabra uniforme
            results[0] = runTesseractSpecial(processedFile, 8, "0123456789");
            
            // PSM 7: Una sola línea de texto
            results[1] = runTesseractSpecial(processedFile, 7, "0123456789");
            
            // PSM 6: Un solo bloque uniforme de texto
            results[2] = runTesseractSpecial(processedFile, 6, "0123456789");
            
            // PSM 13: Línea cruda - trata la imagen como una sola línea
            results[3] = runTesseractSpecial(processedFile, 13, "0123456789");
            
            // PSM 10: Trata la imagen como un solo carácter
            results[4] = runTesseractSpecial(processedFile, 10, "0123456789");
            
            // PSM 9: Trata la imagen como una sola palabra en un círculo
            results[5] = runTesseractSpecial(processedFile, 9, "0123456789");
            
            processedFile.delete();
            
            // Buscar el resultado más largo y válido (números de gramera suelen ser largos)
            return selectBestGrameraResult(results);
            
        } catch (Exception e) {
            System.err.println("Error OCR gramera: " + e.getMessage());
            return null;
        }
    }

    // MÉTODOS DE PROCESAMIENTO
    private BufferedImage scaleImageSafely(BufferedImage original, double factor) {
        int newWidth = Math.max(50, (int)(original.getWidth() * factor));
        int newHeight = Math.max(20, (int)(original.getHeight() * factor));
        
        Image scaled = original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return result;
    }

    private BufferedImage enhanceContrast(BufferedImage image) {
        float scaleFactor = 1.5f;
        float offset = 20f;
        RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
        BufferedImage enhanced = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        rescaleOp.filter(image, enhanced);
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
            System.out.println("Número extraído: " + result);
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isValidNumber(Double number) {
        if (number == null) return false;
        boolean valid = (number >= 0 && number <= 100000);
        System.out.println("Validando: " + number + " -> " + (valid ? "VÁLIDO" : "INVÁLIDO"));
        return valid;
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") || 
            contentType.equals("image/jpg") || 
            contentType.equals("image/png")
        );
    }

    // MÉTODOS CRUD
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

    // Métodos auxiliares para OCR de gramera
    private BufferedImage enhanceForLED(BufferedImage image) {
        BufferedImage enhanced = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Aumentar contraste para displays LED
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

    private String runTesseractSpecial(File imageFile, int psm, String whitelist) {
        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(psm);
            tesseract.setOcrEngineMode(1);
            
            // Configuración específica para números largos de gramera
            tesseract.setVariable("tessedit_char_whitelist", whitelist);
            tesseract.setVariable("classify_bln_numeric_mode", "1");
            tesseract.setVariable("user_defined_dpi", "300");
            tesseract.setVariable("textord_min_linesize", "1.0");
            tesseract.setVariable("preserve_interword_spaces", "0");
            
            String result = tesseract.doOCR(imageFile).trim();
            System.out.println("Gramera PSM " + psm + ": '" + result + "'");
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error Tesseract PSM " + psm + ": " + e.getMessage());
            return "";
        }
    }

    private Double selectBestGrameraResult(String[] results) {
        System.out.println("=== SELECCIONANDO MEJOR RESULTADO GRAMERA ===");
        
        Double bestResult = null;
        int maxLength = 0;
        
        for (int i = 0; i < results.length; i++) {
            String result = results[i];
            System.out.println("Resultado " + (i+1) + ": '" + result + "'");
            
            if (result != null && !result.isEmpty()) {
                // Limpiar y extraer número
                String cleanResult = result.replaceAll("[^0-9]", "");
                
                if (!cleanResult.isEmpty()) {
                    try {
                        Double number = Double.parseDouble(cleanResult);
                        
                        // Para grameras, preferir números más largos (más dígitos)
                        if (isValidNumber(number) && cleanResult.length() > maxLength) {
                            bestResult = number;
                            maxLength = cleanResult.length();
                            System.out.println("✅ Nuevo mejor: " + number + " (longitud: " + maxLength + ")");
                        }
                    } catch (NumberFormatException e) {
                        // Ignorar resultados que no se pueden parsear
                    }
                }
            }
        }
        
        if (bestResult != null) {
            System.out.println("🎯 Resultado final gramera: " + bestResult);
        } else {
            System.out.println("❌ No se encontró resultado válido para gramera");
        }
        
        return bestResult;
    }
}
