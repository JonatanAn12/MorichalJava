package com.morichal.demo.services;

import com.morichal.demo.models.Proveedor;
import com.morichal.demo.repositories.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepository;

    public List<Proveedor> listarProveedores() {
        return proveedorRepository.findAll();
    }

    public Proveedor guardarProveedor(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    public Optional<Proveedor> obtenerProveedor(Long id) {
        return proveedorRepository.findById(id);
    }

    public void eliminarProveedor(Long id) {
        proveedorRepository.deleteById(id);
    }
}