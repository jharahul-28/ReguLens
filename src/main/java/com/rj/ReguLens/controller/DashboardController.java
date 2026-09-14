package com.rj.ReguLens.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class DashboardController {

    @GetMapping(value = {"/dashboard", "/ui", "/"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> getDashboardHtml() {
        try {
            Resource resource = new ClassPathResource("static/index.html");
            if (!resource.exists()) {
                return ResponseEntity.status(404).body("<h1>Dashboard template not found in classpath.</h1>");
            }
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("<h1>Error loading dashboard: " + e.getMessage() + "</h1>");
        }
    }
}
