package com.morichal.demo.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.morichal.demo.models.Inventory;
import com.morichal.demo.repositories.InventoryRepository;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    public List<Inventory> getAllInventories(){
        return inventoryRepository.findAll();
    }

    public Optional<Inventory> getInventoryById(long id) {
        return inventoryRepository.findById(id);
    }

    public Inventory createInventory(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    public Inventory updateInventory(Long id, Inventory inventoryDetails) {
        Inventory inventory = inventoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Inventory not found with id: " + id));

        inventory.setName(inventoryDetails.getName());
        inventory.setQuantity(inventoryDetails.getQuantity());
        inventory.setPrecio(inventoryDetails.getPrecio());

        return inventoryRepository.save(inventory);
    }

    public boolean  deleteInventory(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Inventory not found with id: " + id));

        inventoryRepository.delete(inventory);
        return true;
    }
}
