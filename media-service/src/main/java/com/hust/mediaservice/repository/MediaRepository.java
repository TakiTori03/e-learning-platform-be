package com.hust.mediaservice.repository;

import com.hust.mediaservice.entity.Media;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaRepository extends MongoRepository<Media, String> {
    Optional<Media> findByHlsFolderName(String hlsFolderName);
    Optional<Media> findByReferenceId(String referenceId);
}
