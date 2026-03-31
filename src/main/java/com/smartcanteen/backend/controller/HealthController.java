package com.smartcanteen.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String root() {
        return "Backend is running successfully!";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}