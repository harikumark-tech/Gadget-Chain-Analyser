package com.argus.gca.controller;

import com.argus.gca.model.Artifact;
import com.argus.gca.service.ArtifactService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequestMapping
public class WebController {

    private final ArtifactService artifactService;

    public WebController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @GetMapping("/")
    public String index() {
        return "index"; // Thymeleaf template name
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("file") MultipartFile file, Model model) {
        try {
            Artifact artifact = artifactService.saveAndAnalyze(file);

            // Prepare a simple result map for the template
            Map<String, Object> result = Map.of(
                    "filename", artifact.getFileName(),
                    "output", artifact.getFindingsJson() == null ? "No output" : artifact.getFindingsJson().toString()
            );

            model.addAttribute("result", result);
            return "result";
        } catch (Exception e) {
            model.addAttribute("result", Map.of(
                    "filename", file == null ? "" : file.getOriginalFilename(),
                    "output", "Error: " + e.getMessage()
            ));
            return "result";
        }
    }
}
