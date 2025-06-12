package com.morichal.demo.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class imageResponse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Lob
    private double text;
    
    @JsonProperty("uM")
    private String uM;
    
    private String categoria;
    
    private String estado;
    
    // NUEVO CAMPO PARA LA IMAGEN
    @Column(name = "nombre_imagen")
    private String nombreImagen;

    
    // Constructores existentes
    public imageResponse() {}
    
    public imageResponse(Double text) {
        this.text = text;
    }
    
    // Getters y Setters existentes
    public long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Double getText() {
        return text;
    }
    
    public void setText(Double text) {
        this.text = text;
    }
    
    public String getuM() {
        return uM;
    }
    
    public void setuM(String uM) {
        this.uM = uM;
    }
    
    public String getCategoria() {
        return categoria;
    }
    
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public String getNombreImagen() {
        return nombreImagen;
    }
    
    public void setNombreImagen(String nombreImagen) {
        this.nombreImagen = nombreImagen;
    }
}
