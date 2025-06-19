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

        System.out.println("=== PROCESAMIENTO OCR ULTRA MEJORADO ===");
        System.out.println("Archivo: " + image.getOriginalFilename());

        Double result = null;

        // MÉTODO 1: OCR Básico (rápido)
        result = tryBasicOCR(tempFile);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Básico: " + result);
            tempFile.delete();
            return result;
        }

        // MÉTODO 2: OCR Seguro (sin errores de escalado)
        result = trySafeOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Seguro: " + result);
            tempFile.delete();
            return result;
        }

        // MÉTODO 3: OCR para decimales (0.336)
        result = tryDecimalOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Decimal: " + result);
            tempFile.delete();
            return result;
        }

        // MÉTODO 4: OCR para números largos (142976)
        result = tryLongNumberOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Número Largo: " + result);
            tempFile.delete();
            return result;
        }

        // MÉTODO 5: OCR con múltiples enfoques
        result = tryMultiApproachOCR(tempFile, extension);
        if (result != null && isValidNumber(result)) {
            System.out.println("✅ Multi Enfoque: " + result);
            tempFile.delete();
            return result;
        }

        tempFile.delete();
        throw new IllegalArgumentException("No se detectó ningún número válido en la imagen.");
    }
        // Método básico simplificado
    private Double tryBasicOCR(File imageFile) {
        try {
            System.out.println("--- OCR Básico ---");
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
            
            String text = tesseract.doOCR(imageFile).trim();
            System.out.println("Texto básico: '" + text + "'");
            
            return extractNumberWithDecimals(text);
            
        } catch (Exception e) {
            System.err.println("Error OCR básico: " + e.getMessage());
            return null;
        }
    }

    // MÉTODO SEGURO: Sin errores de escalado
    private Double trySafeOCR(File imageFile, String extension) {
        try {
            System.out.println("--- OCR Seguro ---");
            BufferedImage original = ImageIO.read(imageFile);
            
            // Solo procesar si la imagen es lo suficientemente grande
            if (original.getWidth() < 10 || original.getHeight() < 10) {
                System.out.println("Imagen demasiado pequeña para procesar");
                return null;
            }
            
            // Escalar de forma segura
            BufferedImage processed = safeScaleAndEnhance(original);
            
            File processedFile = File.createTempFile("safe-", extension);
            ImageIO.write(processed, extension.replace(".", ""), processedFile);
            
            // Configuración conservadora de Tesseract
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(8);
            tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
            tesseract.setVariable("classify_bln_numeric_mode", "1");
            
            String text = tesseract.doOCR(processedFile);
            System.out.println("Texto seguro: '" + text + "'");
            processedFile.delete();
            
            return extractNumberWithDecimals(text);
            
        } catch (Exception e) {
            System.err.println("Error OCR seguro: " + e.getMessage());
            return null;
        }
    }

    // MÉTODO PARA DECIMALES: Específico para 0.336
    private Double tryDecimalOCR(File imageFile, String extension) {
        try {
            System.out.println("--- OCR Decimales ---");
            BufferedImage original = ImageIO.read(imageFile);
            
            if (original.getWidth() < 10 || original.getHeight() < 10) {
                return null;
            }
            
            // Procesamiento específico para decimales
            BufferedImage processed = enhanceForDecimals(original);
            
            File processedFile = File.createTempFile("decimal-", extension);
            ImageIO.write(processed, extension.replace(".", ""), processedFile);
            
            // Múltiples intentos con diferentes PSM
            String[] results = new String[3];
            
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
            
            // PSM 7: Una línea de texto
            tesseract.setPageSegMode(7);
            results[0] = tesseract.doOCR(processedFile).trim();
            
            // PSM 8: Una palabra
            tesseract.setPageSegMode(8);
            results[1] = tesseract.doOCR(processedFile).trim();
            
            // PSM 13: Línea cruda
            tesseract.setPageSegMode(13);
            results[2] = tesseract.doOCR(processedFile).trim();
            
            processedFile.delete();
            
            System.out.println("Resultados decimales:");
            for (int i = 0; i < results.length; i++) {
                System.out.println("  PSM " + (i == 0 ? 7 : i == 1 ? 8 : 13) + ": '" + results[i] + "'");
            }
            
            return selectBestDecimalResult(results);
            
        } catch (Exception e) {
            System.err.println("Error OCR decimales: " + e.getMessage());
            return null;
        }
    }

    // MÉTODO PARA NÚMEROS LARGOS: Específico para 142976
    private Double tryLongNumberOCR(File imageFile, String extension) {
        try {
            System.out.println("--- OCR Números Largos ---");
            BufferedImage original = ImageIO.read(imageFile);
            
            if (original.getWidth() < 20 || original.getHeight() < 10) {
                return null;
            }
            
            // Procesamiento específico para números largos
            BufferedImage processed = enhanceForLongNumbers(original);
            
            File processedFile = File.createTempFile("long-", extension);
            ImageIO.write(processed, extension.replace(".", ""), processedFile);
            
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setVariable("tessedit_char_whitelist", "0123456789");
            tesseract.setVariable("classify_bln_numeric_mode", "1");
            tesseract.setVariable("user_defined_dpi", "300");
            
            // PSM 7 es mejor para líneas largas de números
            tesseract.setPageSegMode(7);
            
            String text = tesseract.doOCR(processedFile).trim();
            System.out.println("Texto número largo: '" + text + "'");
            processedFile.delete();
            
            return extractLongNumber(text);
            
        } catch (Exception e) {
            System.err.println("Error OCR números largos: " + e.getMessage());
            return null;
        }
    }
        // PROCESAMIENTO SEGURO: Evita errores de escalado
    private BufferedImage safeScaleAndEnhance(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Calcular factor de escalado seguro
        double scaleFactor = 1.0;
        if (width < 100 || height < 30) {
            scaleFactor = Math.max(100.0 / width, 30.0 / height);
            scaleFactor = Math.min(scaleFactor, 5.0); // Máximo 5x
        }
        
        int newWidth = Math.max(50, (int)(width * scaleFactor));
        int newHeight = Math.max(20, (int)(height * scaleFactor));
        
        System.out.println("Escalado seguro: " + width + "x" + height + " → " + newWidth + "x" + newHeight);
        
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g2.dispose();
        
        // Mejorar contraste
        return enhanceContrastSafe(scaled);
    }

    // PROCESAMIENTO PARA DECIMALES: Específico para 0.336
    private BufferedImage enhanceForDecimals(BufferedImage original) {
        // Escalar moderadamente
        int newWidth = Math.max(200, original.getWidth() * 3);
        int newHeight = Math.max(60, original.getHeight() * 3);
        
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g2.dispose();
        
        // Mejorar para detectar puntos decimales
        BufferedImage enhanced = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                int rgb = scaled.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Aumentar contraste moderadamente
                r = Math.min(255, Math.max(0, (int)((r - 128) * 1.5 + 128)));
                g = Math.min(255, Math.max(0, (int)((g - 128) * 1.5 + 128)));
                b = Math.min(255, Math.max(0, (int)((b - 128) * 1.5 + 128)));
                
                enhanced.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        
        return enhanced;
    }

    // PROCESAMIENTO PARA NÚMEROS LARGOS: Específico para 142976
    private BufferedImage enhanceForLongNumbers(BufferedImage original) {
        // Escalar horizontalmente más que verticalmente para números largos
        int newWidth = Math.max(400, original.getWidth() * 4);
        int newHeight = Math.max(80, original.getHeight() * 2);
        
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g2.dispose();
        
        // Convertir a escala de grises y mejorar contraste
        BufferedImage gray = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gGray = gray.createGraphics();
        gGray.drawImage(scaled, 0, 0, null);
        gGray.dispose();
        
        // Aplicar filtro de nitidez
        return applySharpenFilter(gray);
    }

    private BufferedImage enhanceContrastSafe(BufferedImage image) {
        try {
            float scaleFactor = 1.3f;
            float offset = 10f;
            RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
            BufferedImage enhanced = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            rescaleOp.filter(image, enhanced);
            return enhanced;
        } catch (Exception e) {
            System.err.println("Error mejorando contraste: " + e.getMessage());
            return image; // Devolver original si falla
        }
    }

    private BufferedImage applySharpenFilter(BufferedImage image) {
        try {
            float[] sharpenKernel = {
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
            };
            
            java.awt.image.ConvolveOp sharpenOp = new java.awt.image.ConvolveOp(
                new java.awt.image.Kernel(3, 3, sharpenKernel)
            );
            return sharpenOp.filter(image, null);
        } catch (Exception e) {
            System.err.println("Error aplicando filtro: " + e.getMessage());
            return image;
        }
    }
        // EXTRACCIÓN MEJORADA DE NÚMEROS
    private Double extractNumberWithDecimals(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("Extrayendo con decimales de: '" + text + "'");
        
        // Preservar puntos decimales
        String cleanText = text.replaceAll("[^0-9.,]", "");
        
        if (cleanText.isEmpty()) {
            return null;
        }
        
        // Reemplazar comas por puntos
        cleanText = cleanText.replace(",", ".");
        
        // Si hay múltiples puntos, mantener solo el que parece decimal
        if (cleanText.indexOf('.') != cleanText.lastIndexOf('.')) {
            // Buscar el punto que tiene 1-3 dígitos después
            String[] parts = cleanText.split("\\.");
            StringBuilder result = new StringBuilder();
            
            for (int i = 0; i < parts.length - 1; i++) {
                result.append(parts[i]);
            }
            
            // Agregar el último punto y parte decimal
            if (parts[parts.length - 1].length() <= 3) {
                result.append(".").append(parts[parts.length - 1]);
            } else {
                result.append(parts[parts.length - 1]);
            }
            
            cleanText = result.toString();
        }
        
        try {
            Double result = Double.parseDouble(cleanText);
            System.out.println("Número con decimales extraído: " + result);
            return result;
        } catch (NumberFormatException e) {
            System.err.println("Error parseando: " + cleanText);
            return null;
        }
    }

    private Double extractLongNumber(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("Extrayendo número largo de: '" + text + "'");
        
        // Para números largos, solo extraer dígitos consecutivos
        String cleanText = text.replaceAll("[^0-9]", "");
        
        if (cleanText.isEmpty()) {
            return null;
        }
        
        try {
            // Si el número es muy largo, podría ser un error
            if (cleanText.length() > 8) {
                System.out.println("Número muy largo, posible error: " + cleanText);
                return null;
            }
            
            Double result = Double.parseDouble(cleanText);
            System.out.println("Número largo extraído: " + result);
            return result;
        } catch (NumberFormatException e) {
            System.err.println("Error parseando número largo: " + cleanText);
            return null;
        }
    }

    private Double selectBestDecimalResult(String[] results) {
        System.out.println("=== SELECCIONANDO MEJOR RESULTADO DECIMAL ===");
        
        // Priorizar resultados que contengan punto decimal
        for (String result : results) {
            if (result != null && result.contains(".")) {
                Double number = extractNumberWithDecimals(result);
                if (number != null && isValidNumber(number)) {
                    System.out.println("✅ Seleccionado (con decimal): " + number);
                    return number;
                }
            }
        }
        
        // Si no hay decimales, tomar el mejor resultado
        for (String result : results) {
            if (result != null && !result.isEmpty()) {
                Double number = extractNumberWithDecimals(result);
                if (number != null && isValidNumber(number)) {
                    System.out.println("✅ Seleccionado (sin decimal): " + number);
                    return number;
                }
            }
        }
        
        return null;
    }

    // VALIDACIÓN MEJORADA
    private boolean isValidNumber(Double number) {
        if (number == null) return false;
        
        // Rango amplio pero lógico
        boolean valid = (number >= 0 && number <= 1000000); // Hasta 1 millón
        
        // Log mejorado
        if (valid) {
            System.out.println("✅ Número válido: " + number);
        } else {
            System.out.println("❌ Número inválido: " + number + " (fuera de rango 0-1,000,000)");
        }
        
        return valid;
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") || 
            contentType.equals("image/jpg") || 
            contentType.equals("image/png")
        );
    }
        // MÉTODO MULTI-ENFOQUE: Último recurso con múltiples estrategias
    private Double tryMultiApproachOCR(File imageFile, String extension) {
        try {
            System.out.println("--- OCR Multi-Enfoque ---");
            BufferedImage original = ImageIO.read(imageFile);
            
            if (original.getWidth() < 5 || original.getHeight() < 5) {
                return null;
            }
            
            // ENFOQUE 1: Imagen original sin procesar
            Double result1 = testWithOriginal(original);
            if (result1 != null && isValidNumber(result1)) {
                System.out.println("Multi-enfoque 1 exitoso: " + result1);
                return result1;
            }
            
            // ENFOQUE 2: Imagen invertida
            Double result2 = testWithInverted(original, extension);
            if (result2 != null && isValidNumber(result2)) {
                System.out.println("Multi-enfoque 2 exitoso: " + result2);
                return result2;
            }
            
            // ENFOQUE 3: Imagen con alto contraste
            Double result3 = testWithHighContrast(original, extension);
            if (result3 != null && isValidNumber(result3)) {
                System.out.println("Multi-enfoque 3 exitoso: " + result3);
                return result3;
            }
            
            // ENFOQUE 4: Múltiples PSM en imagen original
            Double result4 = testMultiplePSM(imageFile);
            if (result4 != null && isValidNumber(result4)) {
                System.out.println("Multi-enfoque 4 exitoso: " + result4);
                return result4;
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("Error multi-enfoque: " + e.getMessage());
            return null;
        }
    }

    private Double testWithOriginal(BufferedImage original) {
        try {
            // Crear archivo temporal con imagen original
            File tempFile = File.createTempFile("original-", ".png");
            ImageIO.write(original, "png", tempFile);
            
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(8);
            tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
            
            String text = tesseract.doOCR(tempFile).trim();
            tempFile.delete();
            
            System.out.println("Original: '" + text + "'");
            return extractNumberWithDecimals(text);
            
        } catch (Exception e) {
            return null;
        }
    }

    private Double testWithInverted(BufferedImage original, String extension) {
        try {
            BufferedImage inverted = invertColorsSafe(original);
            
            File tempFile = File.createTempFile("inverted-", extension);
            ImageIO.write(inverted, extension.replace(".", ""), tempFile);
            
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(7);
            tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
            
            String text = tesseract.doOCR(tempFile).trim();
            tempFile.delete();
            
            System.out.println("Invertido: '" + text + "'");
            return extractNumberWithDecimals(text);
            
        } catch (Exception e) {
            return null;
        }
    }

    private Double testWithHighContrast(BufferedImage original, String extension) {
        try {
            BufferedImage highContrast = applyHighContrast(original);
            
            File tempFile = File.createTempFile("contrast-", extension);
            ImageIO.write(highContrast, extension.replace(".", ""), tempFile);
            
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(6);
            tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
            
            String text = tesseract.doOCR(tempFile).trim();
            tempFile.delete();
            
            System.out.println("Alto contraste: '" + text + "'");
            return extractNumberWithDecimals(text);
            
        } catch (Exception e) {
            return null;
        }
    }

    private Double testMultiplePSM(File imageFile) {
        try {
            int[] psmModes = {8, 7, 6, 13, 10};
            
            for (int psm : psmModes) {
                try {
                    ITesseract tesseract = new Tesseract();
                    tesseract.setDatapath(tessDataPath);
                    tesseract.setLanguage("eng");
                    tesseract.setPageSegMode(psm);
                    tesseract.setVariable("tessedit_char_whitelist", "0123456789.,");
                    tesseract.setVariable("classify_bln_numeric_mode", "1");
                    
                    String text = tesseract.doOCR(imageFile).trim();
                    System.out.println("PSM " + psm + ": '" + text + "'");
                    
                    Double result = extractNumberWithDecimals(text);
                    if (result != null && isValidNumber(result)) {
                        // Priorizar resultados con decimales o números más largos
                        if (text.contains(".") || text.length() > 3) {
                            return result;
                        }
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

    // MÉTODOS AUXILIARES SEGUROS
    private BufferedImage invertColorsSafe(BufferedImage image) {
        BufferedImage inverted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        
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

    private BufferedImage applyHighContrast(BufferedImage image) {
        BufferedImage contrast = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Aplicar alto contraste
                r = Math.min(255, Math.max(0, (int)((r - 128) * 2.5 + 128)));
                g = Math.min(255, Math.max(0, (int)((g - 128) * 2.5 + 128)));
                b = Math.min(255, Math.max(0, (int)((b - 128) * 2.5 + 128)));
                
                contrast.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        
        return contrast;
    }
        // ========== MÉTODOS CRUD ==========

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

        // GUARDAR LA IMAGEN SI EXISTE
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

        // MANEJAR IMAGEN SI SE ENVÍA UNA NUEVA
        if (imagen != null && !imagen.isEmpty()) {
            // Eliminar imagen anterior si existe
            if (existente.getNombreImagen() != null && !existente.getNombreImagen().isEmpty()) {
                try {
                    fileStorageService.eliminarImagen(existente.getNombreImagen());
                } catch (Exception e) {
                    // Log error pero continuar
                    System.err.println("Error al eliminar imagen anterior: " + e.getMessage());
                }
            }

            String nombreImagen = fileStorageService.guardarImagen(imagen);
            existente.setNombreImagen(nombreImagen);
        }

        return imageResponseRepository.save(existente);
    }
}