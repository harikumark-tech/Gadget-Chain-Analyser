package com.argus.gca.controller;

import com.argus.gca.model.Artifact;
import com.argus.gca.repository.ArtifactRepository;
import com.argus.gca.service.ArtifactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/artifacts")
public class ArtifactController {

    private final ArtifactService artifactService;
    private final ArtifactRepository artifactRepository;

    public ArtifactController(ArtifactService artifactService, ArtifactRepository artifactRepository) {
        this.artifactService = artifactService;
        this.artifactRepository = artifactRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            Artifact artifact = artifactService.saveAndAnalyze(file);
            return ResponseEntity.ok(artifact);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Optional<Artifact> opt = artifactRepository.findById(id);
        return opt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(artifactRepository.findAll());
    }
}
