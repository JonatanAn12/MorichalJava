package com.morichal.demo.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.morichal.demo.models.WeightResponse;
import com.morichal.demo.services.WeightService;

@RestController
public class WeightController {

    @Autowired
    private WeightService weightService;

    @GetMapping("/api/weight")
    public WeightResponse getWeight() {
        Optional<Double> weightOpt = weightService.getWeight();
double weight = weightOpt.orElse(0.0); // Devuelve 0.0 si no se puede obtener el peso
        return new WeightResponse(weight);
    }
}