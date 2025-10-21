package com.argus.gca.repository;

import com.argus.gca.model.Artifact;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArtifactRepository extends MongoRepository<Artifact, String> {
}
