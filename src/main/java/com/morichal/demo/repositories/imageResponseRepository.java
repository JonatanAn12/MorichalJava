package com.morichal.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.morichal.demo.models.imageResponse;

@Repository
public interface imageResponseRepository extends JpaRepository<imageResponse, Long> {
}