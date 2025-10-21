package com.argus.gca.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "artifacts")
public class Artifact {

    @Id
    private String id;
    private String fileName;
    private LocalDateTime uploadTime;
    private String status;
    private Object findingsJson;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getFindingsJson() {
        return findingsJson;
    }

    public void setFindingsJson(Object findingsJson) {
        this.findingsJson = findingsJson;
    }
}
