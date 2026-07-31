package com.tech.point_system.controller;

import com.tech.point_system.service.DataSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DataSeederController {

    private final DataSeederService dataSeederService;

    @PostMapping("/seed")
    public ResponseEntity<Map<String, String>> seedDatabase() {
        dataSeederService.seedData();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Base de datos poblada exitosamente con datos de prueba."
        ));
    }
}