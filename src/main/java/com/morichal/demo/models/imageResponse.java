package com.morichal.demo.models;

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
    private double  text;
    private String uM;
    private String categoria;

    public imageResponse() {

    }

    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public imageResponse(Double text) { 
        this.text = text;
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

}
