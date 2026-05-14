package com.supplyforge.ai.api;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "supplyforge-ai", "status", "ok");
    }
}
