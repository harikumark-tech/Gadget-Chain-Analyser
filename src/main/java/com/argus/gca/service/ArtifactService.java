package com.argus.gca.service;

import com.argus.gca.model.Artifact;
import com.argus.gca.repository.ArtifactRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final AnalyserService analyserService;

    public ArtifactService(ArtifactRepository artifactRepository,
            AnalyserService analyserService) {
        this.artifactRepository = artifactRepository;
        this.analyserService = analyserService;
    }

    public Artifact saveAndAnalyze(MultipartFile file) throws IOException {
        Artifact artifact = new Artifact();
        artifact.setFileName(file.getOriginalFilename());
        artifact.setUploadTime(LocalDateTime.now());
        artifact.setStatus("PENDING");
        artifact = artifactRepository.save(artifact);

        try {
            byte[] jarBytes = file.getBytes();
            List<Map<String, Object>> findings = analyserService.analyzeJarBytes(jarBytes);

            artifact.setStatus("ANALYZED");
            artifact.setFindingsJson(findings);

        } catch (Exception e) {
            artifact.setStatus("FAILED");
            artifact.setFindingsJson(Map.of("error", e.getMessage()));
        } finally {
            artifactRepository.save(artifact);
        }

        return artifact;
    }
}
