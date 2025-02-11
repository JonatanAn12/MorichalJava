package com.morichal.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.morichal.demo.models.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
