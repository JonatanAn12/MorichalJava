package com.morichal.demo.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    @Value("${app.upload.path}")
    private String uploadPath;

    public String guardarImagen(MultipartFile archivo) {
        try {
            // Crear directorio si no existe
            Path directorioUpload = Paths.get(uploadPath);
            if (!Files.exists(directorioUpload)) {
                Files.createDirectories(directorioUpload);
            }

            // Generar nombre único para el archivo
            String extension = obtenerExtension(archivo.getOriginalFilename());
            String nombreArchivo = UUID.randomUUID().toString() + "." + extension;

            // Guardar archivo
            Path rutaArchivo = directorioUpload.resolve(nombreArchivo);
            Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

            return nombreArchivo;

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen: " + e.getMessage());
        }
    }

    public Resource cargarImagen(String nombreArchivo) {
        try {
            Path rutaArchivo = Paths.get(uploadPath).resolve(nombreArchivo);
            Resource resource = new UrlResource(rutaArchivo.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("No se pudo leer el archivo: " + nombreArchivo);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al cargar la imagen: " + e.getMessage());
        }
    }

    public void eliminarImagen(String nombreArchivo) {
        try {
            Path rutaArchivo = Paths.get(uploadPath).resolve(nombreArchivo);
            Files.deleteIfExists(rutaArchivo);
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar la imagen: " + e.getMessage());
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isEmpty()) {
            return "jpg"; // Extensión por defecto
        }
        
        int ultimoPunto = nombreArchivo.lastIndexOf('.');
        if (ultimoPunto == -1) {
            return "jpg"; // Sin extensión, usar jpg por defecto
        }
        
        return nombreArchivo.substring(ultimoPunto + 1).toLowerCase();
    }

    public boolean existeImagen(String nombreArchivo) {
        Path rutaArchivo = Paths.get(uploadPath).resolve(nombreArchivo);
        return Files.exists(rutaArchivo);
    }
}
