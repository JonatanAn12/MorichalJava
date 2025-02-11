package com.morichal.demo.controllers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import com.morichal.demo.models.Item;

class InventoryControllerTest {

    private InventoryController inventoryController;

    @BeforeEach
    void setUp() {
        inventoryController = new InventoryController();
    }

    @Test
    void testAddItem() {
        // Implementar prueba para agregar un artículo
        assertTrue(inventoryController.addItem(new Item("item1")));
    }

    @Test
    void testRemoveItem() {
        // Implementar prueba para eliminar un artículo
        inventoryController.addItem(new Item("item1"));
        assertTrue(inventoryController.removeItem("item1"));
    }

    @Test
    void testGetItems() {
        // Implementar prueba para obtener artículos
        inventoryController.addItem(new Item("item1"));
        assertEquals(1, inventoryController.getItems().size());
    }
}