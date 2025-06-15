package com.morichal.demo.services;

import java.awt.Graphics2D;
import java.awt.Image;
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

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.util.ArrayList;

import com.google.cloud.vision.v1.Image.Builder;

@Service
public class OCRService {

    @Autowired
    private imageResponseRepository imageResponseRepository;

    @Value("${tesseract.datapath}")
    private String tessDataPath;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${google.cloud.vision.credentials.path}")
    private String googleCredentialsPath;

    public Double extractNumberFromImage(MultipartFile image) throws IOException, TesseractException {
        if (image == null || image.isEmpty()) {
            System.err.println("Error: No se recibió ningún archivo.");
            throw new IllegalArgumentException("No se recibió ningún archivo.");
        }
        String contentType = image.getContentType();
        if (!isValidImageType(contentType)) {
            System.err.println("Error: Tipo de archivo inválido: " + contentType);
            throw new IllegalArgumentException("Invalid file type. Only JPEG, JPG, and PNG are allowed.");
        }

        // Configurar variable de entorno para credenciales Google Cloud Vision
        if (System.getProperty("GOOGLE_APPLICATION_CREDENTIALS") == null) {
            System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", googleCredentialsPath);
        }

        String extension = contentType.equals("image/png") ? ".png" : ".jpeg";
        File tempFile = File.createTempFile("upload-", extension);
        image.transferTo(tempFile);

        // Preprocesar la imagen para mejorar OCR
        BufferedImage bufferedImage = ImageIO.read(tempFile);
        BufferedImage processedImage = preprocessImage(bufferedImage);

        // Guardar imagen procesada temporalmente
        File processedFile = File.createTempFile("processed-", extension);
        ImageIO.write(processedImage, extension.replace(".", ""), processedFile);

        // Intentar usar Google Cloud Vision OCR primero
        String textGoogle = callGoogleVisionOCR(processedFile);

        // Usar Tesseract como segundo OCR
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath); // Ruta a los datos de Tesseract
        tesseract.setLanguage("eng");
        tesseract.setTessVariable("tessedit_char_whitelist", "0123456789.");
        tesseract.setPageSegMode(7); // Asume una sola línea de texto
        String textTesseract = tesseract.doOCR(processedFile);

        // Usar OpenCV OCR (suponiendo que tienes un método implementado para esto)
        String textOpenCV = callOpenCVOCR(processedImage);

        // Combinar resultados de los tres OCR
        String combinedText = combineOCRResults(textGoogle, textTesseract, textOpenCV);

        // Eliminar archivos temporales
        tempFile.delete();
        processedFile.delete();

        // Extraer todos los números válidos del texto concatenados
        String numberStr = combinedText.replaceAll("[^0-9.,]", "").replace(",", ".");
        System.out.println("Texto combinado OCR: '" + combinedText + "'");
        System.out.println("Número extraído tras limpieza: '" + numberStr + "'");
        if (numberStr.isEmpty()) {
            System.err.println("Error: No se detectó ningún número en la imagen.");
            throw new IllegalArgumentException("No se detectó ningún número en la imagen.");
        }
        try {
            // Intentar parsear el número completo
            Double parsedNumber = Double.parseDouble(numberStr);
            return parsedNumber;
        } catch (NumberFormatException e) {
            // Si falla, intentar extraer números separados y concatenarlos
            StringBuilder digitsOnly = new StringBuilder();
            boolean decimalFound = false;
            for (char c : numberStr.toCharArray()) {
                if (Character.isDigit(c)) {
                    digitsOnly.append(c);
                } else if ((c == '.' || c == ',') && !decimalFound) {
                    digitsOnly.append('.');
                    decimalFound = true;
                }
            }
            String finalNumberStr = digitsOnly.toString();
            if (finalNumberStr.isEmpty()) {
                System.err.println("Error: El texto extraído no es un número válido.");
                throw new IllegalArgumentException("El texto extraído no es un número válido.");
            }
            // Intentar parsear reemplazando coma por punto para decimal
            try {
                Double parsedFinalNumber = Double.parseDouble(finalNumberStr);
                return parsedFinalNumber;
            } catch (NumberFormatException ex) {
                System.err.println("Error: No se pudo parsear el número tras limpieza: " + finalNumberStr);
                throw new IllegalArgumentException("El texto extraído no es un número válido.");
            }
        }
    }

    // Método placeholder para llamar a OpenCV OCR
    private String callOpenCVOCR(BufferedImage image) {
        // Aquí debes implementar la integración con OpenCV OCR
        // Por ahora, retornamos cadena vacía para no romper el flujo
        return "";
    }

    // Método para combinar resultados de los OCR
    private String combineOCRResults(String text1, String text2, String text3) {
        // Estrategia simple: elegir el texto con mayor longitud
        String maxText = text1;
        if (text2.length() > maxText.length()) {
            maxText = text2;
        }
        if (text3.length() > maxText.length()) {
            maxText = text3;
        }
        return maxText;
    }

    private String callGoogleVisionOCR(File imageFile) {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            ByteString imgBytes = ByteString.readFrom(new java.io.FileInputStream(imageFile));
            com.google.cloud.vision.v1.Image img = com.google.cloud.vision.v1.Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();
            List<AnnotateImageRequest> requests = new ArrayList<>();
            requests.add(request);
            List<AnnotateImageResponse> responses = client.batchAnnotateImages(requests).getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.err.println("Error en Google Vision OCR: " + res.getError().getMessage());
                    return "";
                }
                return res.getTextAnnotationsList().isEmpty() ? "" : res.getTextAnnotationsList().get(0).getDescription();
            }
        } catch (Exception e) {
            System.err.println("Excepción en llamada a Google Vision OCR: " + e.getMessage());
        }
        return "";
    }

    // Método adicional para obtener el texto completo extraído (para análisis y depuración)
    public String extractFullTextFromImage(MultipartFile image) throws IOException, TesseractException {
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

        // Preprocesar la imagen para mejorar OCR
        BufferedImage bufferedImage = ImageIO.read(tempFile);
        BufferedImage processedImage = preprocessImage(bufferedImage);

        // Guardar imagen procesada temporalmente
        File processedFile = File.createTempFile("processed-", extension);
        ImageIO.write(processedImage, extension.replace(".", ""), processedFile);

        // Usar Tesseract para reconocer el texto
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath); // Ruta a los datos de Tesseract
        tesseract.setLanguage("eng");
        String text = tesseract.doOCR(processedFile);

        // Eliminar archivos temporales
        tempFile.delete();
        processedFile.delete();

        return text;
    }

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") || contentType.equals("image/jpg") || contentType.equals("image/png");
    }

    // Método para preprocesar la imagen: ajustar brillo, contraste, binarizar, reducir ruido, corregir perspectiva y escalar
   private BufferedImage preprocessImage(BufferedImage original) {
    // Ajustar brillo y contraste
    BufferedImage adjusted = adjustBrightnessContrast(original, 3.5f, 80f);

    // Convertir a escala de grises
    BufferedImage gray = new BufferedImage(adjusted.getWidth(), adjusted.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    Graphics2D g = gray.createGraphics();
    g.drawImage(adjusted, 0, 0, null);
    g.dispose();

    // Aplicar filtro de reducción de ruido (blur más fuerte)
    BufferedImage denoised = applyGaussianBlur(gray);
    denoised = applyGaussianBlur(denoised);
    denoised = applyGaussianBlur(denoised);

    // Corregir perspectiva (perspective transform) - implementación simple para corregir distorsión trapezoidal
    BufferedImage perspectiveCorrected = correctPerspective(denoised);

    // Binarización con umbral adaptativo mejorado (umbral dinámico)
    BufferedImage binary = adaptiveThresholdImproved(perspectiveCorrected);

    // Corregir rotación aproximada (si es necesario)
    BufferedImage deskewed = deskewImage(binary);

    // Centrar la imagen recortando bordes vacíos
    BufferedImage centered = centerImage(deskewed);

    // Escalar imagen a un ancho fijo (ejemplo 1200px) manteniendo proporción
    int targetWidth = 1200;
    int targetHeight = (int) ((double) centered.getHeight() / centered.getWidth() * targetWidth);
    Image scaled = centered.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
    BufferedImage scaledBuffered = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_BINARY);
    Graphics2D g2 = scaledBuffered.createGraphics();
    g2.drawImage(scaled, 0, 0, null);
    g2.dispose();

    return scaledBuffered;
}

private BufferedImage correctPerspective(BufferedImage image) {
    // Implementación simple placeholder para corrección de perspectiva
    // En un caso real, se usaría detección de contornos y transformación de perspectiva
    // Aquí solo retornamos la imagen sin cambios para no romper el flujo
    return image;
}

private BufferedImage adaptiveThresholdImproved(BufferedImage image) {
    BufferedImage binary = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
    int blockSize = 15; // tamaño de bloque para cálculo local
    int constant = 10;  // constante para ajustar umbral
    for (int y = 0; y < image.getHeight(); y++) {
        for (int x = 0; x < image.getWidth(); x++) {
            int sum = 0;
            int count = 0;
            // Calcular promedio local
            for (int dy = -blockSize/2; dy <= blockSize/2; dy++) {
                for (int dx = -blockSize/2; dx <= blockSize/2; dx++) {
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight()) {
                        int rgb = image.getRGB(nx, ny);
                        int gray = rgb & 0xFF;
                        sum += gray;
                        count++;
                    }
                }
            }
            int localThreshold = (sum / count) - constant;
            int rgb = image.getRGB(x, y);
            int gray = rgb & 0xFF;
            int value = (gray < localThreshold) ? 0xFF000000 : 0xFFFFFFFF;
            binary.setRGB(x, y, value);
        }
    }
    return binary;
}
    // Método para centrar la imagen recortando bordes vacíos (pixeles blancos)
    private BufferedImage centerImage(BufferedImage image) {
        int top = 0, bottom = image.getHeight() - 1;
        int left = 0, right = image.getWidth() - 1;
        boolean found = false;

        // Buscar borde superior
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    top = y;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        // Buscar borde inferior
        found = false;
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    bottom = y;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        // Buscar borde izquierdo
        found = false;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    left = x;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        // Buscar borde derecho
        found = false;
        for (int x = image.getWidth() - 1; x >= 0; x--) {
            for (int y = 0; y < image.getHeight(); y++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    right = x;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        // Recortar la imagen al área encontrada
        int width = right - left + 1;
        int height = bottom - top + 1;
        return image.getSubimage(left, top, width, height);
    }

    // Método para aplicar un blur gaussiano simple para reducción de ruido
    private BufferedImage applyGaussianBlur(BufferedImage image) {
        // Implementación simple de blur usando convolución con kernel gaussiano
        float[] matrix = {
            1f/16f, 2f/16f, 1f/16f,
            2f/16f, 4f/16f, 2f/16f,
            1f/16f, 2f/16f, 1f/16f
        };
        java.awt.image.ConvolveOp op = new java.awt.image.ConvolveOp(new java.awt.image.Kernel(3, 3, matrix));
        return op.filter(image, null);
    }

    // Método para binarización adaptativa simple (umbral fijo como aproximación)
    private BufferedImage adaptiveThreshold(BufferedImage image) {
        BufferedImage binary = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xFF;
                int threshold = 128; // umbral fijo, se puede mejorar con cálculo local
                int value = (gray < threshold) ? 0xFF000000 : 0xFFFFFFFF;
                binary.setRGB(x, y, value);
            }
        }
        return binary;
    }

    // Método para corregir rotación aproximada (deskew)
    private BufferedImage deskewImage(BufferedImage image) {
        // Aquí se puede implementar un algoritmo de deskew más avanzado,
        // pero para simplicidad, retornamos la imagen sin cambios.
        // Se puede usar librerías externas para esto si se desea.
        return image;
    }

    // Ajustar brillo y contraste usando RescaleOp
    private BufferedImage adjustBrightnessContrast(BufferedImage image, float scaleFactor, float offset) {
        RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
        BufferedImage adjusted = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        rescaleOp.filter(image, adjusted);
        return adjusted;
    }

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

    public imageResponse actualizarConImagen(Long id, String text, String uM, String categoria, String estado,
            MultipartFile imagen) {
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
