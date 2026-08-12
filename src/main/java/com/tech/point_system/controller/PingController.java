package com.tech.point_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ping")
public class PingController {
    @GetMapping
    public ResponseEntity<Void> ping() {
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
